package com.awal.cineq.booking.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class BookingDetailDTO {
    private Long id;
    private Long bookingId;
    private Long seatId;
    private String seatNumber;
    private String rowNumber;
    private BigDecimal seatPrice;
}