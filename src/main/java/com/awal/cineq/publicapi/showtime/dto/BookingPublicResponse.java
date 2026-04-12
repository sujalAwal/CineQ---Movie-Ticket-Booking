package com.awal.cineq.publicapi.showtime.dto;

import com.awal.cineq.booking.dto.BookingDetailResponse;
import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Public API response DTO for booking details
 * Returns limited fields for public consumption: paymentStatus, bookingDetails, createdAt
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@JsonInclude(JsonInclude.Include.NON_NULL)
public class BookingPublicResponse {
    private String paymentStatus;
    private List<BookingDetailResponse> bookingDetails;
    private LocalDateTime createdAt;
}
