package com.awal.cineq.booking.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class SeatRequest {

    @NotBlank(message = "Seat number is required")
    private String seatNumber; // e.g. "A1", "B5"

    @NotBlank(message = "Seat type is required")
    private String seatType;   // "STANDARD", "PREMIUM", "VIP"
}
