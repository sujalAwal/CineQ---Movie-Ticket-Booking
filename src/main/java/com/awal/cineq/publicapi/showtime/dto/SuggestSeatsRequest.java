package com.awal.cineq.publicapi.showtime.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SuggestSeatsRequest {
    @NotBlank(message = "Showtime ID is required")
    private String showtimeId;

    @NotNull(message = "Number of seats is required")
    @Min(value = 2, message = "Minimum 2 seats required")
    @Max(value = 6, message = "Maximum 6 seats allowed")
    private Integer seats;

    @Builder.Default
    private SeatPreference seatPreference = SeatPreference.MIDDLE;
}
