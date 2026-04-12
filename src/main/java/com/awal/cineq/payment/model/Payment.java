package com.awal.cineq.payment.model;

import com.awal.cineq.payment.enums.PaymentMethod;
import com.awal.cineq.payment.enums.PaymentStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.Id;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Map;

/**
 * MongoDB Document for a payment transaction.
 * One Payment is created per booking initiation attempt.
 */
@Document(collection = "payments")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Payment {

    @Id
    private String id;

    @Indexed(unique = true)
    private String paymentId;               // Internal reference: PAY-<uuid>

    @Indexed
    private String bookingId;               // Booking._id (MongoDB ObjectId string)

    @Indexed
    private String customerId;

    private BigDecimal amount;
    private PaymentMethod paymentMethod;
    private PaymentStatus status;

    private String gatewayTransactionId;    // pidx (Khalti) or transaction_uuid (eSewa)
    private String paymentUrl;              // Redirect URL for Khalti

    private Map<String, String> gatewayMetadata; // eSewa form fields

    @CreatedDate
    private LocalDateTime createdAt;

    @LastModifiedDate
    private LocalDateTime updatedAt;

    private LocalDateTime deletedAt;
}
