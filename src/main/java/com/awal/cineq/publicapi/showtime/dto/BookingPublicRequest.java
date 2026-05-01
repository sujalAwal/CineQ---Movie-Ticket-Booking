package com.awal.cineq.publicapi.showtime.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Public API request DTO for querying bookings by showtime
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class BookingPublicRequest {
    
    @NotBlank(message = "Showtime ID is required")
    private String showtimeId;
}
