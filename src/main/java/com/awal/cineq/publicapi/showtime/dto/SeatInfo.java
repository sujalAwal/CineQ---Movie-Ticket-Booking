package com.awal.cineq.publicapi.showtime.dto;

import lombok.*;
import com.fasterxml.jackson.annotation.JsonInclude;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@JsonInclude(JsonInclude.Include.NON_NULL)
public class SeatInfo {
    private String seatNumber;  // e.g. "A1", "B12"
    private String row;         // e.g. "A"
    private Integer column;     // e.g. 1
    private String seatType;    // "STANDARD", "PREMIUM", "VIP"
    private Double price;       // calculated from basePrice * multiplier
    private Boolean isAvailable;
}
