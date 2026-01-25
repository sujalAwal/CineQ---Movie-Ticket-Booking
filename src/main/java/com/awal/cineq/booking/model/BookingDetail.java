package com.awal.cineq.booking.model;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.mongodb.core.mapping.Field;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * BookingDetail - Embedded document within Booking
 * Represents individual seat bookings (NOT a separate collection)
 * MongoDB best practice: embed small related data instead of references
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class BookingDetail {
    
    @Field("seat_id")
    private String seatId;  // Reference to Seat document

    @Field("seat_number")
    private String seatNumber;  // Denormalized for quick access

    @Field("seat_type")
    private String seatType;  // Denormalized: REGULAR, VIP, PREMIUM

    @Field("seat_price")
    private BigDecimal seatPrice;
    
    @Field("created_at")
    private LocalDateTime createdAt = LocalDateTime.now();
}