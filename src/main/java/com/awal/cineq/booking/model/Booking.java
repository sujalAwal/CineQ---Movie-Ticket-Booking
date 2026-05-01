package com.awal.cineq.booking.model;

import com.awal.cineq.payment.enums.PaymentMethod;
import com.awal.cineq.payment.enums.PaymentStatus;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.Id;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.mongodb.core.index.CompoundIndex;
import org.springframework.data.mongodb.core.index.CompoundIndexes;
import org.springframework.data.mongodb.core.mapping.Document;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

/**
 * MongoDB Document for Booking.
 *
 * seatStatusCode references the seat_statuses collection:
 *   3 = Reserved  → booking is PENDING (awaiting payment)
 *   2 = Booked    → booking is CONFIRMED (payment successful)
 *   1 = Available → booking was CANCELLED/EXPIRED (deletedAt is also set)
 *
 * Soft-delete pattern: deletedAt = null means active; set = removed from unique seat index.
 */
@Document(collection = "bookings")
@CompoundIndexes({
    @CompoundIndex(
        name = "booking_reference_active_idx",
        def = "{'bookingReference': 1, 'deletedAt': 1}",
        unique = true
    )
})
@Data
@NoArgsConstructor
@AllArgsConstructor
public class Booking {

    @Id
    private String id;

    private String bookingReference;

    private String showtimeId;
    private String customerId;

    private LocalDateTime bookingDate;
    private Integer numberOfSeats;
    private BigDecimal totalAmount;

    /**
     * Status code from seat_statuses collection:
     * 3 = Reserved (PENDING), 2 = Booked (CONFIRMED), 1 = Available (CANCELLED)
     */
    private Integer seatStatusCode = 3;

    private PaymentStatus paymentStatus = PaymentStatus.INITIATED;
    private PaymentMethod paymentMethod;
    private String paymentReference;

    /** PENDING bookings expire after 15 minutes; null once confirmed. */
    private LocalDateTime expiresAt;

    @CreatedDate
    private LocalDateTime createdAt;

    @LastModifiedDate
    private LocalDateTime updatedAt;

    /** Soft-delete timestamp; null = active. Cancelled bookings are soft-deleted so seats are freed in the unique index. */
    private LocalDateTime deletedAt;

    private List<BookingDetail> bookingDetails;
}
