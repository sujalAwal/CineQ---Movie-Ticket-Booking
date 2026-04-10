package com.awal.cineq.payment.service.impl;

import com.awal.cineq.booking.model.Booking;
import com.awal.cineq.booking.repository.BookingRepository;
import com.awal.cineq.config.ApplicationProperties;
import com.awal.cineq.exception.BusinessException;
import com.awal.cineq.exception.ResourceNotFoundException;
import com.awal.cineq.payment.dto.InitiatePaymentRequest;
import com.awal.cineq.payment.dto.InitiatePaymentResponse;
import com.awal.cineq.payment.model.Payment;
import com.awal.cineq.payment.repository.PaymentRepository;
import com.awal.cineq.payment.service.PaymentService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientResponseException;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.util.Base64;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class PaymentServiceImpl implements PaymentService {

    private final PaymentRepository paymentRepository;
    private final BookingRepository bookingRepository;
    private final WebClient.Builder webClientBuilder;
    private final ApplicationProperties appProperties;

    // ─────────────────────────────────────────────────────────────────────────
    // Public API
    // ─────────────────────────────────────────────────────────────────────────

    @Override
    @Transactional
    public InitiatePaymentResponse initiatePayment(String customerId, InitiatePaymentRequest request) {
        log.info("initiatePayment STARTED — customerId={}, bookingId={}, method={}",
                customerId, request.getBookingId(), request.getPaymentMethod());

        // 1. Validate booking exists and belongs to this customer
        Booking booking = bookingRepository.findByBookingReference(request.getBookingId())
                .orElseThrow(() -> new ResourceNotFoundException("Booking not found: " + request.getBookingId()));

        if (!customerId.equals(booking.getCustomerId())) {
            throw new BusinessException("Booking does not belong to this customer", HttpStatus.FORBIDDEN);
        }

        if (booking.getBookingStatus() == Booking.BookingStatus.CANCELLED
                || booking.getBookingStatus() == Booking.BookingStatus.EXPIRED) {
            throw new BusinessException("Cannot pay for a " + booking.getBookingStatus().name().toLowerCase() + " booking");
        }

        if (booking.getPaymentStatus() == Booking.PaymentStatus.COMPLETED) {
            throw new BusinessException("Booking is already paid");
        }

        // 2. Create internal Payment record
        String paymentId = "PAY-" + UUID.randomUUID().toString().replace("-", "").substring(0, 16).toUpperCase();
        String normalizedMethod = request.getPaymentMethod().toUpperCase();

        Payment payment = Payment.builder()
                .paymentId(paymentId)
                .bookingId(booking.getBookingReference())
                .customerId(customerId)
                .amount(request.getAmount())
                .paymentMethod(normalizedMethod)
                .status(Payment.PaymentStatus.INITIATED)
                .createdAt(LocalDateTime.now())
                .build();

        // 3. Delegate to gateway
        InitiatePaymentResponse response = switch (normalizedMethod) {
            case "ESEWA"      -> initiateEsewa(payment, booking);
            case "KHALTI"     -> initiateKhalti(payment, booking);
            case "CONNECTIPS" -> initiateConnectIPS(payment, booking);
            default -> throw new BusinessException("Unsupported payment method: " + normalizedMethod);
        };

        paymentRepository.save(payment);

        log.info("initiatePayment END — paymentId={}, method={}", paymentId, normalizedMethod);
        return response;
    }

    // ─────────────────────────────────────────────────────────────────────────
    // eSewa  (v2 — HMAC-SHA256 signed form POST)
    // Docs: https://developer.esewa.com.np/pages/Epay
    // ─────────────────────────────────────────────────────────────────────────

    private InitiatePaymentResponse initiateEsewa(Payment payment, Booking booking) {
        log.debug("initiateEsewa — paymentId={}", payment.getPaymentId());

        ApplicationProperties.Payment.Esewa cfg = appProperties.getPayment().getEsewa();

        String transactionUuid = UUID.randomUUID().toString();
        BigDecimal amount      = payment.getAmount();
        BigDecimal taxAmount   = BigDecimal.ZERO;
        BigDecimal totalAmount = amount.add(taxAmount);

        // Fields to sign (order matters): total_amount,transaction_uuid,product_code
        String signatureMessage = "total_amount=" + totalAmount
                + ",transaction_uuid=" + transactionUuid
                + ",product_code=" + cfg.getMerchantCode();
        String signature = hmacSha256Base64(signatureMessage, cfg.getSecretKey());

        Map<String, String> formFields = new LinkedHashMap<>();
        formFields.put("amount",                    amount.toPlainString());
        formFields.put("tax_amount",                taxAmount.toPlainString());
        formFields.put("total_amount",              totalAmount.toPlainString());
        formFields.put("transaction_uuid",          transactionUuid);
        formFields.put("product_code",              cfg.getMerchantCode());
        formFields.put("product_service_charge",    "0");
        formFields.put("product_delivery_charge",   "0");
        formFields.put("success_url",               appProperties.getPayment().getSuccessUrl());
        formFields.put("failure_url",               appProperties.getPayment().getFailureUrl());
        formFields.put("signed_field_names",        "total_amount,transaction_uuid,product_code");
        formFields.put("signature",                 signature);

        payment.setGatewayTransactionId(transactionUuid);
        payment.setGatewayMetadata(formFields);
        payment.setPaymentUrl(cfg.getPaymentUrl());

        return InitiatePaymentResponse.builder()
                .paymentId(payment.getPaymentId())
                .bookingId(payment.getBookingId())
                .amount(payment.getAmount())
                .paymentMethod("ESEWA")
                .status("INITIATED")
                .formActionUrl(cfg.getPaymentUrl())
                .formFields(formFields)
                .build();
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Khalti  (v2 — server-side API call, returns payment_url)
    // Docs: https://docs.khalti.com/khalti-epayment
    // ─────────────────────────────────────────────────────────────────────────

    private InitiatePaymentResponse initiateKhalti(Payment payment, Booking booking) {
        log.debug("initiateKhalti — paymentId={}", payment.getPaymentId());

        ApplicationProperties.Payment.Khalti cfg = appProperties.getPayment().getKhalti();
        ApplicationProperties.Payment      shared = appProperties.getPayment();

        // Khalti expects amount in paisa (NPR × 100)
        long amountInPaisa = payment.getAmount().multiply(BigDecimal.valueOf(100)).longValue();

        Map<String, Object> requestBody = new HashMap<>();
        requestBody.put("return_url",           shared.getSuccessUrl());
        requestBody.put("website_url",          shared.getWebsiteUrl());
        requestBody.put("amount",               amountInPaisa);
        requestBody.put("purchase_order_id",    payment.getPaymentId());
        requestBody.put("purchase_order_name",  "CineQ Booking - " + booking.getBookingReference());

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
            log.error("initiateKhalti GATEWAY_ERROR — status={}, body={}", e.getStatusCode(), e.getResponseBodyAsString());
            throw new BusinessException("Khalti payment initiation failed: " + e.getMessage());
        }

        if (khaltiResponse == null || !khaltiResponse.containsKey("pidx")) {
            throw new BusinessException("Invalid response from Khalti gateway");
        }

        String pidx       = (String) khaltiResponse.get("pidx");
        String paymentUrl = (String) khaltiResponse.get("payment_url");

        payment.setGatewayTransactionId(pidx);
        payment.setPaymentUrl(paymentUrl);

        return InitiatePaymentResponse.builder()
                .paymentId(payment.getPaymentId())
                .bookingId(payment.getBookingId())
                .amount(payment.getAmount())
                .paymentMethod("KHALTI")
                .status("INITIATED")
                .paymentUrl(paymentUrl)
                .build();
    }

    // ─────────────────────────────────────────────────────────────────────────
    // ConnectIPS  (placeholder — extend when credentials are available)
    // ─────────────────────────────────────────────────────────────────────────

    private InitiatePaymentResponse initiateConnectIPS(Payment payment, Booking booking) {
        log.debug("initiateConnectIPS — paymentId={}", payment.getPaymentId());
        throw new BusinessException("ConnectIPS integration is not yet configured", HttpStatus.NOT_IMPLEMENTED);
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Utility
    // ─────────────────────────────────────────────────────────────────────────

    private String hmacSha256Base64(String message, String secretKey) {
        try {
            Mac mac = Mac.getInstance("HmacSHA256");
            mac.init(new SecretKeySpec(secretKey.getBytes(StandardCharsets.UTF_8), "HmacSHA256"));
            byte[] rawHmac = mac.doFinal(message.getBytes(StandardCharsets.UTF_8));
            return Base64.getEncoder().encodeToString(rawHmac);
        } catch (Exception e) {
            throw new BusinessException("Failed to generate payment signature: " + e.getMessage());
        }
    }
}
