package com.awal.cineq.payment.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Map;

/**
 * Payment list view DTO with user/customer information
 * Used for admin portal payments listing
 * Returns all fields from payments collection plus enriched user details
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@JsonInclude(JsonInclude.Include.NON_NULL)
public class PaymentListDTO {

    // Core payment fields
    @JsonProperty("_id")
    private String id;
    private String paymentId;
    private String bookingId;
    private String customerId;
    private BigDecimal amount;
    private String paymentMethod;
    private String status;
    private String gatewayTransactionId;
    private String paymentUrl;

    // Gateway metadata
    private Map<String, String> gatewayMetadata;

    // Timestamps
    @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss")
    private LocalDateTime createdAt;
    
    @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss")
    private LocalDateTime updatedAt;
    
    private LocalDateTime deletedAt;

    // User information (joined from customers collection)
    private UserDetailsDTO userDetails;
}
