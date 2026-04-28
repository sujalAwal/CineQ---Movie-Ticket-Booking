package com.awal.cineq.payment.service.impl;

import com.awal.cineq.booking.dto.BookingDetailResponse;
import com.awal.cineq.booking.dto.BookingResponse;
import com.awal.cineq.booking.model.Booking;
import com.awal.cineq.booking.model.BookingDetail;
import com.awal.cineq.booking.repository.BookingRepository;
import com.awal.cineq.common.util.EmailHelper;
import com.awal.cineq.config.ApplicationProperties;
import com.awal.cineq.customer.model.Customer;
import com.awal.cineq.customer.repository.CustomerRepository;
import com.awal.cineq.email.model.EmailTemplate;
import com.awal.cineq.email.repository.EmailTemplateRepository;
import com.awal.cineq.exception.BusinessException;
import com.awal.cineq.exception.ResourceNotFoundException;
import com.awal.cineq.frontend.screens.repository.FrontendScreenRepository;
import com.awal.cineq.frontend.showtimes.repository.FrontendShowtimeRepository;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.data.domain.Sort;
import com.awal.cineq.payment.dto.InitiatePaymentRequest;
import com.awal.cineq.payment.dto.InitiatePaymentResponse;
import com.awal.cineq.payment.dto.SeatSelection;
import com.awal.cineq.payment.enums.PaymentMethod;
import com.awal.cineq.payment.enums.PaymentStatus;
import com.awal.cineq.payment.model.Payment;
import com.awal.cineq.payment.repository.PaymentRepository;
import com.awal.cineq.payment.service.PaymentService;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientResponseException;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.time.Year;
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class PaymentServiceImpl implements PaymentService {

    // Seat status codes from seat_statuses collection
    private static final int STATUS_AVAILABLE = 1;
    private static final int STATUS_BOOKED    = 2;
    private static final int STATUS_RESERVED  = 3;

    private static final int BOOKING_EXPIRY_MINUTES = 15;

    private static final String SUCCESS_EMAIL_TEMPLATE_SLUG = "ticket-confirmation";
    private static final String FAILURE_EMAIL_TEMPLATE_SLUG = "payment-failure";

    private final PaymentRepository paymentRepository;
    private final BookingRepository bookingRepository;
    private final FrontendShowtimeRepository showtimeRepository;
    private final FrontendScreenRepository screenRepository;
    private final CustomerRepository customerRepository;
    private final EmailTemplateRepository emailTemplateRepository;
    private final MongoTemplate mongoTemplate;
    private final WebClient.Builder webClientBuilder;
    private final ApplicationProperties appProperties;
    private final EmailHelper emailHelper;
    private final ObjectMapper objectMapper;

    // ─────────────────────────────────────────────────────────────────────────
    // initiateBookingPayment — the single entry point for the booking+pay flow
    // ─────────────────────────────────────────────────────────────────────────

    @Override
    public InitiatePaymentResponse initiateBookingPayment(String customerId, InitiatePaymentRequest request) {
        log.info("initiateBookingPayment START — customerId={}, showtimeId={}, seats={}, method={}",
                customerId, request.getShowtimeId(), request.getSeats().size(), request.getPaymentMethod());

        // 1. Load showtime — use MongoTemplate with explicit ObjectId conversion so all
        //    nested fields (seatLayout, pricePerLayout) are returned correctly.
        //    The FrontendShowtimeRepository @Query does not auto-convert String→ObjectId
        //    for raw Map repositories, which causes seatLayout to be silently missing.
        @SuppressWarnings("unchecked")
        Map<String, Object> showtime = (Map<String, Object>) (Map<?, ?>) mongoTemplate.findOne(
                new Query(Criteria.where("_id").is(new org.bson.types.ObjectId(request.getShowtimeId()))
                        .and("isActive").is(true)
                        .and("deletedAt").is(null)),
                Map.class, "showtimes");
        if (showtime == null) {
            throw new ResourceNotFoundException("Showtime not found: " + request.getShowtimeId());
        }

        // 2. Check showtime is bookable
        String statusCode = str(showtime, "statusCode");
        if ("FULL".equals(statusCode) || "CANCELLED".equals(statusCode)) {
            throw new BusinessException("Showtime is not available for booking (status: " + statusCode + ")");
        }

        // 3. Build lookup map: seatName → seat info (with resolved price)
        //    Primary source:  showtime.seatLayout[]  (each element already has price)
        //    Fallback source: screen.seatLayout[]  +  showtime.pricePerLayout[]  (price per code)
        @SuppressWarnings("unchecked")
        List<Map<String, Object>> seatLayout = (List<Map<String, Object>>) showtime.get("seatLayout");

        if (seatLayout == null || seatLayout.isEmpty()) {
            seatLayout = buildSeatLayoutFromScreen(showtime);
        }
        if (seatLayout == null || seatLayout.isEmpty()) {
            throw new BusinessException("Showtime has no seat layout configured");
        }

        Map<String, Map<String, Object>> layoutMap = new HashMap<>();
        for (Map<String, Object> seat : seatLayout) {
            String name = str(seat, "seatName");
            if (name != null) layoutMap.put(name, seat);
        }

        // 4. Validate seats + resolve prices server-side (NEVER from client)
        List<BookingDetail> bookingDetails = new ArrayList<>();
        BigDecimal totalAmount = BigDecimal.ZERO;
        List<String> requestedSeatNames = new ArrayList<>();

        for (SeatSelection sel : request.getSeats()) {
            Map<String, Object> layoutSeat = layoutMap.get(sel.getSeatName());
            if (layoutSeat == null) {
                throw new BusinessException("Seat '" + sel.getSeatName() + "' does not exist in this showtime");
            }

            Object priceObj = layoutSeat.get("price");
            if (priceObj == null) {
                throw new BusinessException("Seat '" + sel.getSeatName() + "' has no price configured");
            }
            BigDecimal seatPrice = new BigDecimal(priceObj.toString());

            Object colObj = layoutSeat.get("col");
            int col = colObj instanceof Number n ? n.intValue() : 0;

            BookingDetail detail = new BookingDetail();
            detail.setSeatName(sel.getSeatName());
            detail.setRow(str(layoutSeat, "row"));
            detail.setCol(col);
            detail.setSeatCode(str(layoutSeat, "code"));
            detail.setSeatPrice(seatPrice);
            detail.setSeatStatusCode(STATUS_RESERVED); // Reserved until payment confirmed

            bookingDetails.add(detail);
            totalAmount = totalAmount.add(seatPrice);
            requestedSeatNames.add(sel.getSeatName());
        }

        // 5. Application-level conflict check (fast path; DB unique index handles races)
        List<Booking> conflicts = bookingRepository.findConflictingBookings(
                request.getShowtimeId(), requestedSeatNames);
        if (!conflicts.isEmpty()) {
            Set<String> takenSeats = conflicts.stream()
                    .filter(b -> b.getBookingDetails() != null)
                    .flatMap(b -> b.getBookingDetails().stream())
                    .map(BookingDetail::getSeatName)
                    .filter(requestedSeatNames::contains)
                    .collect(Collectors.toSet());
            throw new BusinessException("Seat(s) already taken: " + takenSeats, HttpStatus.CONFLICT);
        }

        // 6. Create Booking with status Reserved (3)
        String bookingReference = "BK-" + UUID.randomUUID().toString().replace("-", "").substring(0, 12).toUpperCase();
        Booking booking = new Booking();
        booking.setBookingReference(bookingReference);
        booking.setShowtimeId(request.getShowtimeId());
        booking.setCustomerId(customerId);
        booking.setBookingDate(LocalDateTime.now());
        booking.setNumberOfSeats(requestedSeatNames.size());
        booking.setTotalAmount(totalAmount);
        booking.setSeatStatusCode(STATUS_RESERVED);
        booking.setPaymentStatus(PaymentStatus.INITIATED);
        booking.setPaymentMethod(request.getPaymentMethod());
        booking.setExpiresAt(LocalDateTime.now().plusMinutes(BOOKING_EXPIRY_MINUTES));
        booking.setBookingDetails(bookingDetails);
        booking.setCreatedAt(LocalDateTime.now());
        booking.setUpdatedAt(LocalDateTime.now());

        // 7. Save booking — unique index on (showtimeId, bookingDetails.seatName) catches races
        try {
            booking = bookingRepository.save(booking);
        } catch (DuplicateKeyException e) {
            log.warn("Trying to save booking but seat conflict detected at DB level.", booking.toString());
            log.error("initiateBookingPayment — seat booking conflict detected for showtimeId={}, seats={}",
                    request.getShowtimeId(), requestedSeatNames, e);
            throw new BusinessException(
                    "One or more selected seats were just taken. Please choose different seats.",
                    HttpStatus.CONFLICT);
        }

        // 8. Create Payment record
        String paymentId = "PAY-" + UUID.randomUUID().toString().replace("-", "").substring(0, 16).toUpperCase();
        Payment payment = Payment.builder()
                .paymentId(paymentId)
                .bookingId(booking.getId())
                .customerId(customerId)
                .amount(totalAmount)
                .paymentMethod(request.getPaymentMethod())
                .status(PaymentStatus.INITIATED)
                .createdAt(LocalDateTime.now())
                .build();

        // 9. Delegate to gateway
        InitiatePaymentResponse response = switch (request.getPaymentMethod()) {
            case ESEWA      -> initiateEsewa(payment, booking);
            case KHALTI     -> initiateKhalti(payment, booking);
            case CONNECTIPS -> throw new BusinessException(
                    "ConnectIPS integration is not yet configured", HttpStatus.NOT_IMPLEMENTED);
        };

        paymentRepository.save(payment);

        log.info("initiateBookingPayment END — bookingRef={}, paymentId={}", bookingReference, paymentId);
        return response;
    }

    // ─────────────────────────────────────────────────────────────────────────
    // verifyEsewaPayment
    // ─────────────────────────────────────────────────────────────────────────

    @Override
    public BookingResponse verifyEsewaPayment(String customerId, String encodedData) {
        log.info("verifyEsewaPayment START — customerId={}", customerId);

        // Decode base64 → JSON
        Map<String, Object> esewaData;
        try {
            String decoded = new String(Base64.getDecoder().decode(encodedData), StandardCharsets.UTF_8);
            esewaData = objectMapper.readValue(decoded, Map.class);
        } catch (Exception e) {
            throw new BusinessException("Invalid eSewa response data: " + e.getMessage());
        }

        String status          = str(esewaData, "status");
        String transactionUuid = str(esewaData, "transaction_uuid");
        String signedFields    = str(esewaData, "signed_field_names");
        String receivedSig     = str(esewaData, "signature");

        log.info("verifyEsewaPayment — status={}, transactionUuid={}", status, transactionUuid);

        // Verify HMAC signature first (fraud prevention)
        if (signedFields != null && receivedSig != null) {
            ApplicationProperties.Payment.Esewa cfg = appProperties.getPayment().getEsewa();
            String signatureMessage = Arrays.stream(signedFields.split(","))
                    .map(field -> field.trim() + "=" + esewaData.getOrDefault(field.trim(), ""))
                    .collect(Collectors.joining(","));
            String expectedSig = hmacSha256Base64(signatureMessage, cfg.getSecretKey());
            if (!expectedSig.equals(receivedSig)) {
                log.error("verifyEsewaPayment — HMAC mismatch! Possible fraud attempt.");
                throw new BusinessException("Payment signature verification failed");
            }
        }

        // Find our payment by gateway transaction ID
        Payment payment = paymentRepository.findByGatewayTransactionId(transactionUuid)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Payment not found for transaction: " + transactionUuid));

        if (!customerId.equals(payment.getCustomerId())) {
            throw new BusinessException("Payment does not belong to this customer", HttpStatus.FORBIDDEN);
        }

        if (!"COMPLETE".equalsIgnoreCase(status)) {
            // Payment was not successful — cancel the booking and free seats
            cancelBookingAndPaymentInternal(payment, "eSewa status: " + status);
            
            // Send failure notifications to customer and admin
            sendFailureNotification(payment, "eSewa payment was not completed. Status: " + status);
            sendAdminFailureNotification(payment, "eSewa status: " + status);
            
            throw new BusinessException("eSewa payment was not completed. Status: " + status);
        }

        BookingResponse bookingResponse =  confirmBookingAndPayment(payment);
        sendSuccessNotification(bookingResponse);
        return bookingResponse;
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Email Notifications
    // ─────────────────────────────────────────────────────────────────────────

    /**
     * Send successful payment confirmation email to customer
     */
    private void sendSuccessNotification(BookingResponse bookingResponse) {
        try {
            log.info("sendSuccessNotification — bookingId={}", bookingResponse.getId());

            // Get customer
            Customer customer = customerRepository.findById(bookingResponse.getCustomerId())
                    .orElse(null);
            if (customer == null) {
                log.warn("Customer not found for booking: {}", bookingResponse.getId());
                return;
            }

            // Get template
            EmailTemplate template = emailTemplateRepository
                    .findBySlugAndIsActiveTrueAndDeletedAtNull(SUCCESS_EMAIL_TEMPLATE_SLUG)
                    .orElse(null);
            if (template == null) {
                log.warn("Success email template not found (slug: {})", SUCCESS_EMAIL_TEMPLATE_SLUG);
                return;
            }

            // Get showtime, movie, theater details for placeholders
            Map<String, String> placeholders = buildEmailPlaceholders(bookingResponse, customer, true);

            // Replace placeholders in subject and message
            String subject = replacePlaceholders(template.getSubject(), placeholders);
            String message = replacePlaceholders(template.getMessage(), placeholders);

            // Send email to customer
            boolean sent = emailHelper.sendEmail(customer.getEmail(), subject, message);
            if (sent) {
                log.info("Success notification email sent to customer: {}", customer.getEmail());
            } else {
                log.warn("Failed to send success notification email to: {}", customer.getEmail());
            }

            // Also send admin notification
            if (template.getAdminMessage() != null && template.getAdminSubject() != null) {
                String adminSubject = replacePlaceholders(template.getAdminSubject(), placeholders);
                String adminMessage = replacePlaceholders(template.getAdminMessage(), placeholders);
                List<String> adminEmails = appProperties.getCustomer().getAdminEmailList();
                if (!adminEmails.isEmpty()) {
                    boolean adminSent = emailHelper.sendEmail(adminEmails, adminSubject, adminMessage);
                    if (adminSent) {
                        log.info("Success notification email sent to admins");
                    } else {
                        log.warn("Failed to send success notification to admins");
                    }
                }
            }
        } catch (Exception e) {
            log.error("Error sending success notification for booking: {}", bookingResponse.getId(), e);
        }
    }

    /**
     * Send payment failure notification to customer
     */
    private void sendFailureNotification(Payment payment, String failureReason) {
        try {
            log.info("sendFailureNotification — paymentId={}, reason={}", payment.getPaymentId(), failureReason);

            // Get customer
            Customer customer = customerRepository.findById(payment.getCustomerId())
                    .orElse(null);
            if (customer == null) {
                log.warn("Customer not found for payment: {}", payment.getPaymentId());
                return;
            }

            // Get template
            EmailTemplate template = emailTemplateRepository
                    .findBySlugAndIsActiveTrueAndDeletedAtNull(FAILURE_EMAIL_TEMPLATE_SLUG)
                    .orElse(null);
            if (template == null) {
                log.warn("Failure email template not found (slug: {})", FAILURE_EMAIL_TEMPLATE_SLUG);
                return;
            }

            // Get booking to fetch showtime, movie details
            Booking booking = bookingRepository.findById(payment.getBookingId())
                    .orElse(null);
            if (booking == null) {
                log.warn("Booking not found for payment: {}", payment.getPaymentId());
                return;
            }

            // Build response object for placeholder extraction
            BookingResponse bookingResponse = toBookingResponse(booking);

            // Build placeholders
            Map<String, String> placeholders = buildEmailPlaceholders(bookingResponse, customer, false);
            placeholders.put("failureReason", failureReason);

            // Replace placeholders in subject and message
            String subject = replacePlaceholders(template.getSubject(), placeholders);
            String message = replacePlaceholders(template.getMessage(), placeholders);

            // Send email to customer
            boolean sent = emailHelper.sendEmail(customer.getEmail(), subject, message);
            if (sent) {
                log.info("Failure notification email sent to customer: {}", customer.getEmail());
            } else {
                log.warn("Failed to send failure notification email to: {}", customer.getEmail());
            }
        } catch (Exception e) {
            log.error("Error sending failure notification for payment: {}", payment.getPaymentId(), e);
        }
    }

    /**
     * Send payment failure notification to admin
     */
    private void sendAdminFailureNotification(Payment payment, String failureReason) {
        try {
            log.info("sendAdminFailureNotification — paymentId={}, reason={}", payment.getPaymentId(), failureReason);

            // Get template
            EmailTemplate template = emailTemplateRepository
                    .findBySlugAndIsActiveTrueAndDeletedAtNull(FAILURE_EMAIL_TEMPLATE_SLUG)
                    .orElse(null);
            if (template == null || template.getAdminMessage() == null) {
                log.warn("Failure email template admin message not found");
                return;
            }

            // Get customer
            Customer customer = customerRepository.findById(payment.getCustomerId())
                    .orElse(null);
            if (customer == null) {
                log.warn("Customer not found for payment: {}", payment.getPaymentId());
                return;
            }

            // Get booking
            Booking booking = bookingRepository.findById(payment.getBookingId())
                    .orElse(null);
            if (booking == null) {
                log.warn("Booking not found for payment: {}", payment.getPaymentId());
                return;
            }

            // Build response object
            BookingResponse bookingResponse = toBookingResponse(booking);

            // Build placeholders
            Map<String, String> placeholders = buildEmailPlaceholders(bookingResponse, customer, false);
            placeholders.put("failureReason", failureReason);
            placeholders.put("paymentMethod", payment.getPaymentMethod() != null ? payment.getPaymentMethod().name() : "Unknown");
            placeholders.put("timestamp", LocalDateTime.now().toString());

            // Replace placeholders
            String adminSubject = replacePlaceholders(template.getAdminSubject(), placeholders);
            String adminMessage = replacePlaceholders(template.getAdminMessage(), placeholders);

            // Send email to all admin emails
            List<String> adminEmails = appProperties.getCustomer().getAdminEmailList();
            if (!adminEmails.isEmpty()) {
                boolean sent = emailHelper.sendEmail(adminEmails, adminSubject, adminMessage);
                if (sent) {
                    log.info("Failure notification sent to {} admin(s)", adminEmails.size());
                } else {
                    log.warn("Failed to send failure notification to admins");
                }
            } else {
                log.warn("No admin emails configured for failure notification");
            }
        } catch (Exception e) {
            log.error("Error sending admin failure notification for payment: {}", payment.getPaymentId(), e);
        }
    }

    /**
     * Build placeholder map for email template replacement
     */
    private Map<String, String> buildEmailPlaceholders(BookingResponse booking, Customer customer, boolean isSuccess) {
        Map<String, String> placeholders = new HashMap<>();

        // Customer info
        placeholders.put("userName", customer.getFirstName() + " " + (customer.getLastName() != null ? customer.getLastName() : ""));
        placeholders.put("userEmail", customer.getEmail());
        placeholders.put("userPhone", customer.getPhone() != null ? customer.getPhone() : "N/A");
        placeholders.put("customerId", customer.getId());

        // Booking info
        placeholders.put("bookingId", booking.getBookingReference());
        placeholders.put("totalAmount", booking.getTotalAmount() != null ? String.format("%.2f", booking.getTotalAmount()) : "0.00");
        placeholders.put("numberOfSeats", booking.getNumberOfSeats() != null ? booking.getNumberOfSeats().toString() : "0");

        // Seats info
        if (booking.getBookingDetails() != null && !booking.getBookingDetails().isEmpty()) {
            String seatNumbers = booking.getBookingDetails().stream()
                    .map(BookingDetailResponse::getSeatName)
                    .collect(Collectors.joining(", "));
            placeholders.put("seatNumbers", seatNumbers);
        } else {
            placeholders.put("seatNumbers", "N/A");
        }

        // Showtime and Movie info — fetch from database
        if (booking.getShowtimeId() != null) {
            try {
                @SuppressWarnings("unchecked")
                Map<String, Object> showtime = (Map<String, Object>) (Map<?, ?>) mongoTemplate.findById(
                        booking.getShowtimeId(), Map.class, "showtimes");
                if (showtime != null) {
                    String movieId = str(showtime, "movieId");
                    Object showDateObj = showtime.get("showDate");
                    Object showTimeObj = showtime.get("showTime");

                    placeholders.put("showDate", showDateObj != null ? showDateObj.toString() : "N/A");
                    placeholders.put("showTime", showTimeObj != null ? showTimeObj.toString() : "N/A");

                    // Get movie details
                    if (movieId != null) {
                        @SuppressWarnings("unchecked")
                        Map<String, Object> movie = (Map<String, Object>) (Map<?, ?>) mongoTemplate.findById(
                                movieId, Map.class, "movies");
                        if (movie != null) {
                            placeholders.put("movieName", str(movie, "title"));
                            placeholders.put("movieId", movieId);
                        }
                    }

                    // Get screen/hall details
                    String screenId = str(showtime, "screenId");
                    if (screenId != null) {
                        @SuppressWarnings("unchecked")
                        Map<String, Object> screen = (Map<String, Object>) (Map<?, ?>) mongoTemplate.findById(
                                screenId, Map.class, "screens");
                        if (screen != null) {
                            placeholders.put("hallName", str(screen, "screenName"));
                        }
                    }
                }
            } catch (Exception e) {
                log.warn("Error fetching showtime/movie details for email: {}", e.getMessage());
            }
        }

        // Default values for missing placeholders
        placeholders.putIfAbsent("movieName", "N/A");
        placeholders.putIfAbsent("movieId", "N/A");
        placeholders.putIfAbsent("hallName", "N/A");
        placeholders.putIfAbsent("showDate", "N/A");
        placeholders.putIfAbsent("showTime", "N/A");
            placeholders.putIfAbsent("year", String.valueOf(Year.now().getValue()));
            placeholders.putIfAbsent("appUrl", appProperties.getCustomer().getFrontendUrl());
        placeholders.putIfAbsent("timestamp", LocalDateTime.now().toString());
        return placeholders;    
    }

    /**
     * Replace all placeholders in template text
     */
    private String replacePlaceholders(String template, Map<String, String> placeholders) {
        String result = template;
        for (Map.Entry<String, String> entry : placeholders.entrySet()) {
            String placeholder = "{{" + entry.getKey() + "}}";
            String value = entry.getValue() != null ? entry.getValue() : "";
            result = result.replace(placeholder, value);
        }
        return result;
    }

    // ─────────────────────────────────────────────────────────────────────────
    // verifyKhaltiPayment
    // ─────────────────────────────────────────────────────────────────────────

    @Override
    public BookingResponse verifyKhaltiPayment(String customerId, String pidx) {
        log.info("verifyKhaltiPayment START — customerId={}, pidx={}", customerId, pidx);

        ApplicationProperties.Payment.Khalti cfg = appProperties.getPayment().getKhalti();

        // Call Khalti lookup API to check payment status
        Map<String, Object> khaltiResponse;
        try {
            khaltiResponse = webClientBuilder.build()
                    .post()
                    .uri("https://a.khalti.com/api/v2/epayment/lookup/")
                    .header("Authorization", "Key " + cfg.getSecretKey())
                    .contentType(MediaType.APPLICATION_JSON)
                    .bodyValue(Map.of("pidx", pidx))
                    .retrieve()
                    .bodyToMono(Map.class)
                    .block();
        } catch (WebClientResponseException e) {
            log.error("verifyKhaltiPayment GATEWAY_ERROR — {}", e.getResponseBodyAsString());
            throw new BusinessException("Khalti verification failed: " + e.getMessage());
        }

        if (khaltiResponse == null) {
            throw new BusinessException("No response from Khalti verification API");
        }

        String khaltiStatus = str(khaltiResponse, "status");
        log.info("verifyKhaltiPayment — khaltiStatus={}", khaltiStatus);

        // Find our payment by pidx
        Payment payment = paymentRepository.findByGatewayTransactionId(pidx)
                .orElseThrow(() -> new ResourceNotFoundException("Payment not found for pidx: " + pidx));

        if (!customerId.equals(payment.getCustomerId())) {
            throw new BusinessException("Payment does not belong to this customer", HttpStatus.FORBIDDEN);
        }

        if (!"Completed".equalsIgnoreCase(khaltiStatus)) {
            cancelBookingAndPaymentInternal(payment, "Khalti status: " + khaltiStatus);
            
            // Send failure notifications to customer and admin
            sendFailureNotification(payment, "Khalti payment was not completed. Status: " + khaltiStatus);
            sendAdminFailureNotification(payment, "Khalti status: " + khaltiStatus);
            
            throw new BusinessException("Khalti payment was not completed. Status: " + khaltiStatus);
        }

        BookingResponse bookingResponse = confirmBookingAndPayment(payment);
        sendSuccessNotification(bookingResponse);
        return bookingResponse;
    }

    // ─────────────────────────────────────────────────────────────────────────
    // eSewa gateway — HMAC-SHA256 signed form POST
    // ─────────────────────────────────────────────────────────────────────────

    private InitiatePaymentResponse initiateEsewa(Payment payment, Booking booking) {
        ApplicationProperties.Payment.Esewa cfg = appProperties.getPayment().getEsewa();

        String transactionUuid = UUID.randomUUID().toString();
        BigDecimal amount      = payment.getAmount();
        BigDecimal taxAmount   = BigDecimal.ZERO;
        BigDecimal totalAmount = amount.add(taxAmount);

        // Signature covers: total_amount, transaction_uuid, product_code (order matters)
        String signatureMessage = "total_amount=" + totalAmount
                + ",transaction_uuid=" + transactionUuid
                + ",product_code=" + cfg.getMerchantCode();
        String signature = hmacSha256Base64(signatureMessage, cfg.getSecretKey());

        Map<String, String> formFields = new LinkedHashMap<>();
        formFields.put("amount",                   amount.toPlainString());
        formFields.put("tax_amount",               taxAmount.toPlainString());
        formFields.put("total_amount",             totalAmount.toPlainString());
        formFields.put("transaction_uuid",         transactionUuid);
        formFields.put("product_code",             cfg.getMerchantCode());
        formFields.put("product_service_charge",   "0");
        formFields.put("product_delivery_charge",  "0");
        formFields.put("success_url",              appProperties.getPayment().getSuccessUrl());
        formFields.put("failure_url",              appProperties.getPayment().getFailureUrl());
        formFields.put("signed_field_names",       "total_amount,transaction_uuid,product_code");
        formFields.put("signature",                signature);

        payment.setGatewayTransactionId(transactionUuid);
        payment.setGatewayMetadata(formFields);
        payment.setPaymentUrl(cfg.getPaymentUrl());

        return InitiatePaymentResponse.builder()
                .paymentId(payment.getPaymentId())
                .bookingReference(booking.getBookingReference())
                .totalAmount(payment.getAmount())
                .paymentMethod(PaymentMethod.ESEWA.name())
                .status(PaymentStatus.INITIATED.name())
                .expiresAt(booking.getExpiresAt())
                .formActionUrl(cfg.getPaymentUrl())
                .formFields(formFields)
                .build();
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Khalti gateway — server-side API call, returns payment_url
    // ─────────────────────────────────────────────────────────────────────────

    private InitiatePaymentResponse initiateKhalti(Payment payment, Booking booking) {
        ApplicationProperties.Payment.Khalti cfg = appProperties.getPayment().getKhalti();

        // Khalti expects amount in paisa (NPR × 100)
        long amountInPaisa = payment.getAmount().multiply(BigDecimal.valueOf(100)).longValue();

        Map<String, Object> requestBody = new HashMap<>();
        requestBody.put("return_url",          appProperties.getPayment().getSuccessUrl());
        requestBody.put("website_url",         appProperties.getPayment().getWebsiteUrl());
        requestBody.put("amount",              amountInPaisa);
        requestBody.put("purchase_order_id",   payment.getPaymentId());
        requestBody.put("purchase_order_name", "CineQ - " + booking.getBookingReference());

        Map<String, Object> khaltiResponse;
        try {
            khaltiResponse = webClientBuilder.build()
                    .post()
                    .uri(cfg.getInitiateUrl())
                    .header("Authorization", "Key " + cfg.getSecretKey())
                    .contentType(MediaType.APPLICATION_JSON)
                    .bodyValue(requestBody)
                    .retrieve()
                    .bodyToMono(Map.class)
                    .block();
        } catch (WebClientResponseException e) {
            log.error("initiateKhalti GATEWAY_ERROR — {}", e.getResponseBodyAsString());
            cancelBookingInternal(booking);
            throw new BusinessException("Khalti payment initiation failed: " + e.getMessage());
        }

        if (khaltiResponse == null || !khaltiResponse.containsKey("pidx")) {
            cancelBookingInternal(booking);
            throw new BusinessException("Invalid response from Khalti gateway");
        }

        String pidx       = (String) khaltiResponse.get("pidx");
        String paymentUrl = (String) khaltiResponse.get("payment_url");

        payment.setGatewayTransactionId(pidx);
        payment.setPaymentUrl(paymentUrl);

        return InitiatePaymentResponse.builder()
                .paymentId(payment.getPaymentId())
                .bookingReference(booking.getBookingReference())
                .totalAmount(payment.getAmount())
                .paymentMethod(PaymentMethod.KHALTI.name())
                .status(PaymentStatus.INITIATED.name())
                .expiresAt(booking.getExpiresAt())
                .paymentUrl(paymentUrl)
                .build();
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Internal: confirm booking after verified payment
    // ─────────────────────────────────────────────────────────────────────────

    private BookingResponse confirmBookingAndPayment(Payment payment) {
        // Update payment to COMPLETED
        payment.setStatus(PaymentStatus.COMPLETED);
        payment.setUpdatedAt(LocalDateTime.now());
        paymentRepository.save(payment);

        // Find and confirm the booking
        Booking booking = bookingRepository.findById(payment.getBookingId())
                .orElseThrow(() -> new ResourceNotFoundException("Booking not found: " + payment.getBookingId()));

        booking.setSeatStatusCode(STATUS_BOOKED);          // Booked (2)
        booking.setPaymentStatus(PaymentStatus.COMPLETED);
        booking.setPaymentReference(payment.getPaymentId());
        booking.setExpiresAt(null); // confirmed — no longer expires
        booking.setUpdatedAt(LocalDateTime.now());
        if (booking.getBookingDetails() != null) {
            booking.getBookingDetails().forEach(d -> d.setSeatStatusCode(STATUS_BOOKED));
        }

        Booking saved = bookingRepository.save(booking);
        log.info("confirmBookingAndPayment — bookingRef={} confirmed", saved.getBookingReference());
        return toBookingResponse(saved);
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Internal: cancel booking + mark payment failed, freeing seats
    // ─────────────────────────────────────────────────────────────────────────

    private void cancelBookingAndPaymentInternal(Payment payment, String reason) {
        log.info("cancelBookingAndPaymentInternal — bookingId={}, reason={}", payment.getBookingId(), reason);
        payment.setStatus(PaymentStatus.FAILED);
        payment.setUpdatedAt(LocalDateTime.now());
        paymentRepository.save(payment);

        bookingRepository.findById(payment.getBookingId()).ifPresent(this::cancelBookingInternal);
    }

    private void cancelBookingInternal(Booking booking) {
        booking.setSeatStatusCode(STATUS_AVAILABLE);
        booking.setPaymentStatus(PaymentStatus.FAILED);
        booking.setDeletedAt(LocalDateTime.now()); // soft-delete: frees seats from unique index
        booking.setUpdatedAt(LocalDateTime.now());
        if (booking.getBookingDetails() != null) {
            booking.getBookingDetails().forEach(d -> d.setSeatStatusCode(STATUS_AVAILABLE));
        }
        bookingRepository.save(booking);
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Minimal BookingResponse builder (used after confirm — no enrichment needed)
    // ─────────────────────────────────────────────────────────────────────────

    private BookingResponse toBookingResponse(Booking booking) {
        List<BookingDetailResponse> details = null;
        if (booking.getBookingDetails() != null) {
            details = booking.getBookingDetails().stream()
                    .map(d -> BookingDetailResponse.builder()
                            .seatName(d.getSeatName())
                            .row(d.getRow())
                            .col(d.getCol())
                            .seatCode(d.getSeatCode())
                            .seatPrice(d.getSeatPrice() != null ? d.getSeatPrice().doubleValue() : null)
                            .seatStatusCode(d.getSeatStatusCode())
                            .build())
                    .collect(Collectors.toList());
        }

        return BookingResponse.builder()
                .id(booking.getId())
                .bookingReference(booking.getBookingReference())
                .showtimeId(booking.getShowtimeId())
                .customerId(booking.getCustomerId())
                .bookingDate(booking.getBookingDate() != null ? booking.getBookingDate().toString() : null)
                .numberOfSeats(booking.getNumberOfSeats())
                .totalAmount(booking.getTotalAmount() != null ? booking.getTotalAmount().doubleValue() : null)
                .seatStatusCode(booking.getSeatStatusCode())
                .paymentStatus(booking.getPaymentStatus() != null ? booking.getPaymentStatus().name() : null)
                .paymentMethod(booking.getPaymentMethod() != null ? booking.getPaymentMethod().name() : null)
                .paymentReference(booking.getPaymentReference())
                .bookingDetails(details)
                .build();
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Utilities
    // ─────────────────────────────────────────────────────────────────────────

    /**
     * Builds a seatLayout list by combining:
     *   - Physical seat positions from the screen document (seatLayout[])
     *   - Prices from the showtime's pricePerLayout[] (code → basePrice)
     *   - If pricePerLayout is absent, falls back to showtime.basePrice with standard multipliers
     *
     * Returns null if neither the screen nor any price info is found.
     */
    @SuppressWarnings("unchecked")
    private List<Map<String, Object>> buildSeatLayoutFromScreen(Map<String, Object> showtime) {
        String screenId = str(showtime, "screenId");
        if (screenId == null) {
            log.warn("buildSeatLayoutFromScreen: showtime has no screenId");
            return null;
        }

        // 1. Load screen document — use MongoTemplate for correct ObjectId conversion
        @SuppressWarnings("unchecked")
        Map<String, Object> screenDoc = (Map<String, Object>) (Map<?, ?>) mongoTemplate.findOne(
                new Query(Criteria.where("_id").is(new org.bson.types.ObjectId(screenId))
                        .and("isActive").is(true)
                        .and("deletedAt").is(null)),
                Map.class, "screens");
        if (screenDoc == null) {
            log.warn("buildSeatLayoutFromScreen: screen not found for id={}", screenId);
            return null;
        }

        List<Map<String, Object>> screenSeats = (List<Map<String, Object>>) screenDoc.get("seatLayout");
        if (screenSeats == null || screenSeats.isEmpty()) {
            log.warn("buildSeatLayoutFromScreen: screen {} has no seatLayout", screenId);
            return null;
        }

        // 2. Build price map from showtime.pricePerLayout[{code, basePrice}]
        Map<String, BigDecimal> priceByCode = new HashMap<>();
        Object pplObj = showtime.get("pricePerLayout");
        if (pplObj instanceof List<?> ppl) {
            for (Object entry : ppl) {
                if (entry instanceof Map<?, ?> priceEntry) {
                    String code = str((Map<String, Object>) priceEntry, "code");
                    Object priceVal = priceEntry.get("basePrice");
                    if (code != null && priceVal != null) {
                        priceByCode.put(code, new BigDecimal(priceVal.toString()));
                    }
                }
            }
        }

        // 3. If still no prices, fall back to showtime.basePrice with standard multipliers
        if (priceByCode.isEmpty()) {
            Object basePriceObj = showtime.get("basePrice");
            if (basePriceObj != null) {
                BigDecimal base = new BigDecimal(basePriceObj.toString());
                priceByCode.put("R", base);
                priceByCode.put("P", base.multiply(BigDecimal.valueOf(1.5)).setScale(0, java.math.RoundingMode.HALF_UP));
                priceByCode.put("V", base.multiply(BigDecimal.valueOf(2.0)).setScale(0, java.math.RoundingMode.HALF_UP));
                priceByCode.put("X", BigDecimal.ZERO);
            }
        }

        if (priceByCode.isEmpty()) {
            log.warn("buildSeatLayoutFromScreen: no pricing info found on showtime {}", str(showtime, "_id"));
            return null;
        }

        // 4. Merge: copy each screen seat and inject the resolved price
        List<Map<String, Object>> enriched = new ArrayList<>();
        for (Map<String, Object> seat : screenSeats) {
            Map<String, Object> s = new HashMap<>(seat);
            String code = str(s, "code");
            if (!s.containsKey("price") && code != null) {
                BigDecimal price = priceByCode.get(code);
                if (price != null) {
                    s.put("price", price);
                }
            }
            enriched.add(s);
        }

        log.info("buildSeatLayoutFromScreen: built {} seats from screen {} for showtime",
                enriched.size(), screenId);
        return enriched;
    }

    private String str(Map<String, Object> map, String key) {
        Object val = map.get(key);
        return val != null ? val.toString() : null;
    }

    private String hmacSha256Base64(String message, String secretKey) {
        try {
            Mac mac = Mac.getInstance("HmacSHA256");
            mac.init(new SecretKeySpec(secretKey.getBytes(StandardCharsets.UTF_8), "HmacSHA256"));
            return Base64.getEncoder().encodeToString(
                    mac.doFinal(message.getBytes(StandardCharsets.UTF_8)));
        } catch (Exception e) {
            throw new BusinessException("Failed to generate payment signature: " + e.getMessage());
        }
    }

    // ─────────────────────────────────────────────────────────────────────────
    // getPaymentsWithFilters — Admin API for listing payments with filters
    // ─────────────────────────────────────────────────────────────────────────

    @Override
    public com.awal.cineq.payment.dto.PaymentPageResponse getPaymentsWithFilters(
            com.awal.cineq.payment.dto.PaymentListFilterRequest filterRequest) {
        
        log.info("getPaymentsWithFilters — filters: status={}, method={}, customerId={}, page={}",
                filterRequest.getStatus(), filterRequest.getPaymentMethod(), 
                filterRequest.getCustomerId(), filterRequest.getPage());

        // Build MongoDB query with filters
        Query query = new Query();
        Criteria criteria = new Criteria();

        // Add status filter
        if (filterRequest.getStatus() != null) {
            criteria.and("status").is(filterRequest.getStatus());
        }

        // Add payment method filter
        if (filterRequest.getPaymentMethod() != null) {
            criteria.and("paymentMethod").is(filterRequest.getPaymentMethod());
        }

        // Add customer ID filter
        if (filterRequest.getCustomerId() != null) {
            criteria.and("customerId").is(filterRequest.getCustomerId());
        }

        // Add booking ID filter
        if (filterRequest.getBookingId() != null) {
            criteria.and("bookingId").is(filterRequest.getBookingId());
        }

        // Add amount range filter
        if (filterRequest.getAmountMin() != null || filterRequest.getAmountMax() != null) {
            if (filterRequest.getAmountMin() != null && filterRequest.getAmountMax() != null) {
                criteria.and("amount").gte(new BigDecimal(filterRequest.getAmountMin()))
                        .lte(new BigDecimal(filterRequest.getAmountMax()));
            } else if (filterRequest.getAmountMin() != null) {
                criteria.and("amount").gte(new BigDecimal(filterRequest.getAmountMin()));
            } else {
                criteria.and("amount").lte(new BigDecimal(filterRequest.getAmountMax()));
            }
        }

        // Add date range filter on updatedAt
        if (filterRequest.getStartDate() != null || filterRequest.getEndDate() != null) {
            if (filterRequest.getStartDate() != null && filterRequest.getEndDate() != null) {
                criteria.and("updatedAt").gte(filterRequest.getStartDate().atStartOfDay())
                        .lte(filterRequest.getEndDate().plusDays(1).atStartOfDay());
            } else if (filterRequest.getStartDate() != null) {
                criteria.and("updatedAt").gte(filterRequest.getStartDate().atStartOfDay());
            } else {
                criteria.and("updatedAt").lte(filterRequest.getEndDate().plusDays(1).atStartOfDay());
            }
        }

        // Always exclude deleted payments
        criteria.and("deletedAt").is(null);

        query.addCriteria(criteria);

        // Get total count before pagination
        long totalElements = mongoTemplate.count(query, Payment.class);

        // Add sorting
        String sortDirection = "desc".equalsIgnoreCase(filterRequest.getSortDirection()) ? "desc" : "asc";
        String sortBy = filterRequest.getSortBy() != null ? filterRequest.getSortBy() : "updatedAt";
        
        Sort.Direction direction = 
                "desc".equalsIgnoreCase(sortDirection) ? 
                Sort.Direction.DESC : 
                Sort.Direction.ASC;
        
        query.with(Sort.by(direction, sortBy));

        // Add pagination
        int page = filterRequest.getPage() != null ? filterRequest.getPage() : 0;
        int size = filterRequest.getSize() != null ? filterRequest.getSize() : 10;
        query.skip((long) page * size).limit(size);

        // Execute query to get payments
        List<Payment> payments = mongoTemplate.find(query, Payment.class);

        // Convert to DTOs and enrich with customer details
        List<com.awal.cineq.payment.dto.PaymentListDTO> paymentDTOs = payments.stream()
                .map(payment -> {
                    com.awal.cineq.payment.dto.PaymentListDTO dto = 
                            com.awal.cineq.payment.dto.PaymentListDTO.builder()
                            .id(payment.getId())
                            .paymentId(payment.getPaymentId())
                            .bookingId(payment.getBookingId())
                            .customerId(payment.getCustomerId())
                            .amount(payment.getAmount())
                            .paymentMethod(payment.getPaymentMethod().toString())
                            .status(payment.getStatus().toString())
                            .gatewayTransactionId(payment.getGatewayTransactionId())
                            .paymentUrl(payment.getPaymentUrl())
                            .gatewayMetadata(payment.getGatewayMetadata())
                            .createdAt(payment.getCreatedAt())
                            .updatedAt(payment.getUpdatedAt())
                            .deletedAt(payment.getDeletedAt())
                            .build();

                    // Fetch customer details from customers collection
                    Customer customer = customerRepository.findById(payment.getCustomerId()).orElse(null);
                    if (customer != null) {
                        // Build full customer name
                        StringBuilder nameBuilder = new StringBuilder();
                        if (customer.getFirstName() != null) {
                            nameBuilder.append(customer.getFirstName());
                        }
                        if (customer.getMiddleName() != null) {
                            if (nameBuilder.length() > 0) nameBuilder.append(" ");
                            nameBuilder.append(customer.getMiddleName());
                        }
                        if (customer.getLastName() != null) {
                            if (nameBuilder.length() > 0) nameBuilder.append(" ");
                            nameBuilder.append(customer.getLastName());
                        }
                        
                        String fullName = nameBuilder.length() > 0 ? nameBuilder.toString() : customer.getEmail();
                        
                        dto.setUserDetails(com.awal.cineq.payment.dto.UserDetailsDTO.builder()
                                .customerId(customer.getId())
                                .email(customer.getEmail())
                                .customerName(fullName)
                                .build());
                    }

                    return dto;
                })
                .collect(Collectors.toList());

        // Build pagination response
        int totalPages = (int) Math.ceil((double) totalElements / size);
        boolean hasNext = page < totalPages - 1;
        boolean hasPrevious = page > 0;

        return com.awal.cineq.payment.dto.PaymentPageResponse.builder()
                .payments(paymentDTOs)
                .page(page)
                .size(size)
                .totalElements(totalElements)
                .totalPages(totalPages)
                .hasNext(hasNext)
                .hasPrevious(hasPrevious)
                .build();
    }
}
