package com.awal.cineq.payment.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.Id;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;
import org.springframework.data.mongodb.core.mapping.Field;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Map;

/**
 * MongoDB Document for Payment
 * Tracks payment transactions initiated by customers for bookings.
 * Uses soft-delete pattern: deletedAt = null means active.
 */
@Document(collection = "payments")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Payment {

    @Id
    private String id;

    @Field("payment_id")
    @Indexed(unique = true)
    private String paymentId; // Internal UUID-based reference (e.g. PAY-<uuid>)

    @Field("booking_id")
    @Indexed
    private String bookingId;

    @Field("customer_id")
    @Indexed
    private String customerId;

    @Field("amount")
    private BigDecimal amount;

    @Field("payment_method")
    private String paymentMethod; // ESEWA, KHALTI, CONNECTIPS

    @Field("status")
    private PaymentStatus status;

    @Field("gateway_transaction_id")
    private String gatewayTransactionId; // pidx (Khalti) or transaction_uuid (eSewa)

    @Field("payment_url")
    private String paymentUrl; // URL returned to frontend for redirect

    // Gateway-specific metadata (form fields for eSewa, etc.)
    @Field("gateway_metadata")
    private Map<String, String> gatewayMetadata;

    @Field("created_at")
    @CreatedDate
    private LocalDateTime createdAt;

    @Field("updated_at")
    @LastModifiedDate
    private LocalDateTime updatedAt;

    @Field("deleted_at")
    private LocalDateTime deletedAt;

    public enum PaymentStatus {
        INITIATED,
        PENDING,
        COMPLETED,
        FAILED,
        REFUNDED
    }
}
