package com.awal.cineq.dashboard.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class RecentBookingDTO {

    private String bookingReference;
    private String customerName;
    private String movieTitle;
    private String theatreName;
    private Integer numberOfSeats;
    private BigDecimal totalAmount;
    private String paymentStatus;
    private Integer seatStatusCode;
    private String seatStatusLabel;
    private String bookingDate;
}
