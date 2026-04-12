package com.awal.cineq.payment.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Map;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@JsonInclude(JsonInclude.Include.NON_NULL)
public class InitiatePaymentResponse {

    private String paymentId;           // Internal reference: PAY-<uuid>
    private String bookingReference;    // e.g. "BK1715000000000"
    private BigDecimal totalAmount;     // Server-calculated total (never trust client)
    private String paymentMethod;
    private String status;              // "INITIATED"
    private LocalDateTime expiresAt;    // 15 min from now — seats released after this

    // Khalti: redirect user to this URL
    private String paymentUrl;

    // eSewa: render a hidden form and POST to formActionUrl with these fields
    private String formActionUrl;
    private Map<String, String> formFields;
}
