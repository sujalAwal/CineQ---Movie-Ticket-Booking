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
public class TopTheatreDTO {

    private String theatreId;
    private String name;
    private String district;
    private long totalBookings;
    private BigDecimal totalRevenue;
}
