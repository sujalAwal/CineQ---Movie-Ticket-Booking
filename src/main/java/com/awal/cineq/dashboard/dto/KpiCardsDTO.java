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
public class KpiCardsDTO {

    private long totalBookings;
    private long todaysBookings;
    private BigDecimal totalRevenue;
    private BigDecimal todaysRevenue;
    private long nowShowingMovies;
    private long activeTheatres;
}
