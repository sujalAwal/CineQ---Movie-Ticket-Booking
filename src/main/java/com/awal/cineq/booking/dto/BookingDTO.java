package com.awal.cineq.booking.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import jakarta.validation.constraints.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

import com.awal.cineq.booking.model.Booking;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class BookingDTO {
    private Long id;
    
    private String bookingReference;
    
    @NotNull(message = "User ID is required")
    private Long userId;
    
    @NotNull(message = "Showtime ID is required")
    private Long showtimeId;
    
    private LocalDateTime bookingDate;
    
    @NotNull(message = "Number of seats is required")
    @Positive(message = "Number of seats must be positive")
    private Integer numberOfSeats;
    
    @NotNull(message = "Total amount is required")
    @DecimalMin(value = "0.0", inclusive = false, message = "Total amount must be positive")
    @Digits(integer = 8, fraction = 2, message = "Total amount format is invalid")
    private BigDecimal totalAmount;
    
    private Booking.BookingStatus bookingStatus;
    
    private Booking.PaymentStatus paymentStatus;
    
    @Size(max = 50, message = "Payment method must not exceed 50 characters")
    private String paymentMethod;
    
    @Size(max = 100, message = "Payment reference must not exceed 100 characters")
    private String paymentReference;
    
    @NotNull(message = "Seat IDs are required")
    @NotEmpty(message = "At least one seat must be selected")
    private List<Long> seatIds;
    
    // Additional fields for response
    private String userName;
    private String movieTitle;
    private String theaterName;
    private LocalDateTime showDateTime;
    private List<BookingDetailDTO> bookingDetails;
}