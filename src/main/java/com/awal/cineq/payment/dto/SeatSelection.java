package com.awal.cineq.payment.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Represents a single seat selected by the customer during payment initiation.
 * seatName is used to look up the seat in showtime.seatLayout[].
 * Price is NOT accepted from the client — always resolved server-side.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class SeatSelection {

    @NotBlank(message = "Seat name is required")
    private String seatName; // e.g. "G3" — primary lookup key

    private String row;      // e.g. "G"  — informational, validated against showtime
    private Integer col;     // e.g. 3    — informational, validated against showtime
    private String code;     // e.g. "P"  — seat type code, informational
}
