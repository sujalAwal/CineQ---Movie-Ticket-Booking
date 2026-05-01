package com.awal.cineq.booking.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Booking list view DTO with customer information
 * Used for admin portal bookings listing
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@JsonInclude(JsonInclude.Include.NON_NULL)
public class BookingListDTO {

    private String id;
    private String bookingReference;
    private String showtimeId;

    // Customer information (joined from customers collection)
    private CustomerDTO customer;

    // Booking details
    private Integer numberOfSeats;
    private Double totalAmount;
    private LocalDateTime bookingDate;

    // Seat status
    private Integer seatStatusCode;
    private String seatStatusName;
    private String seatStatusColor;

    // Payment information
    private String paymentStatus;
    private String paymentMethod;
    private String paymentReference;

    private List<BookingDetailResponse> bookingDetails;

    // Enriched showtime information
    private String movieId;
    private String movieTitle;
    private String moviePoster;
    private String theatreId;
    private String theatreName;
    private String screenId;
    private String screenName;
    private String showDate;
    private String showTime;
    private String language;
    private String format;

    // Timestamps
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private LocalDateTime expiresAt;
    private LocalDateTime deletedAt;

    // Computed status
    private String status;
}
