package com.awal.cineq.payment.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.Map;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@JsonInclude(JsonInclude.Include.NON_NULL)
public class InitiatePaymentResponse {

    private String paymentId;         // Internal reference (e.g. PAY-<uuid>)
    private String bookingId;
    private BigDecimal amount;
    private String paymentMethod;
    private String status;            // INITIATED

    // For redirect-based gateways (Khalti)
    private String paymentUrl;

    // For form-POST gateways (eSewa) — frontend submits these as form fields
    private String formActionUrl;
    private Map<String, String> formFields;
}
