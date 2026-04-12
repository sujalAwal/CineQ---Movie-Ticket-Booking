package com.awal.cineq.payment.service.impl;

import com.awal.cineq.booking.dto.BookingDetailResponse;
import com.awal.cineq.booking.dto.BookingResponse;
import com.awal.cineq.booking.model.Booking;
import com.awal.cineq.booking.model.BookingDetail;
import com.awal.cineq.booking.repository.BookingRepository;
import com.awal.cineq.config.ApplicationProperties;
import com.awal.cineq.exception.BusinessException;
import com.awal.cineq.exception.ResourceNotFoundException;
import com.awal.cineq.frontend.screens.repository.FrontendScreenRepository;
import com.awal.cineq.frontend.showtimes.repository.FrontendShowtimeRepository;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
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

    private final PaymentRepository paymentRepository;
    private final BookingRepository bookingRepository;
    private final FrontendShowtimeRepository showtimeRepository;
    private final FrontendScreenRepository screenRepository;
    private final MongoTemplate mongoTemplate;
    private final WebClient.Builder webClientBuilder;
    private final ApplicationProperties appProperties;
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
            throw new BusinessException("eSewa payment was not completed. Status: " + status);
        }

        return confirmBookingAndPayment(payment);
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
            throw new BusinessException("Khalti payment was not completed. Status: " + khaltiStatus);
        }

        return confirmBookingAndPayment(payment);
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
}
