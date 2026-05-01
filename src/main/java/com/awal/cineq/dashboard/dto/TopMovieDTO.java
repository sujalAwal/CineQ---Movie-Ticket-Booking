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
public class TopMovieDTO {

    private String movieId;
    private String title;
    private String poster;
    private long totalBookings;
    private BigDecimal totalRevenue;
}
