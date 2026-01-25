package com.awal.cineq.booking.model;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.Id;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.mongodb.core.index.CompoundIndex;
import org.springframework.data.mongodb.core.index.CompoundIndexes;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;
import org.springframework.data.mongodb.core.mapping.Field;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

/**
 * MongoDB Document for Booking
 * Stores movie ticket bookings with embedded booking details
 * Uses soft-delete pattern: deletedAt = null means active, not null means deleted
 */
@Document(collection = "bookings")
@CompoundIndexes({
    @CompoundIndex(name = "booking_reference_active_idx", def = "{'booking_reference': 1, 'deletedAt': 1}", unique = true)
})
@Data
@NoArgsConstructor
@AllArgsConstructor
public class Booking {
    
    @Id
    private String id;  // MongoDB ObjectId stored as String

    @Field("booking_reference")
    private String bookingReference;  // Uniqueness enforced by compound index (booking_reference, deletedAt)

    @Field("showtime_id")
    private String showtimeId;  // Reference to Showtime document (denormalized)

    @Field("user_id")
    private String userId;  // Reference to User document

    @Field("customer_id")
    private String customerId;  // Reference to Customer document (if applicable)

    @Field("booking_date")
    private LocalDateTime bookingDate;
    
    @Field("number_of_seats")
    private Integer numberOfSeats;
    
    @Field("total_amount")
    private BigDecimal totalAmount;
    
    @Field("booking_status")
    private BookingStatus bookingStatus = BookingStatus.PENDING;
    
    @Field("payment_status")
    private PaymentStatus paymentStatus = PaymentStatus.PENDING;
    
    @Field("payment_method")
    private String paymentMethod;
    
    @Field("payment_reference")
    private String paymentReference;
    
    @Field("created_at")
    @CreatedDate
    private LocalDateTime createdAt;
    
    @Field("updated_at")
    @LastModifiedDate
    private LocalDateTime updatedAt;
    
    // Embed booking details directly (MongoDB best practice for 1-to-many small collections)
    @Field("booking_details")
    private List<BookingDetail> bookingDetails;
    
    public enum BookingStatus {
        PENDING, CONFIRMED, CANCELLED, EXPIRED
    }
    
    public enum PaymentStatus {
        PENDING, COMPLETED, FAILED, REFUNDED
    }
}