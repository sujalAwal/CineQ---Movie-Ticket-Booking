package com.awal.cineq.dashboard.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class DashboardStatsDTO {

    private KpiCardsDTO kpiCards;
    private List<RevenueTrendItemDTO> revenueTrend;
    private List<StatusDistributionDTO> bookingsByStatus;
    private List<TopMovieDTO> topMovies;
    private List<TopTheatreDTO> topTheatres;
    private List<MovieShowcaseDTO> nowShowingMovies;
    private List<MovieShowcaseDTO> comingSoonMovies;
    private List<RecentBookingDTO> recentBookings;
    private List<PaymentMethodDTO> paymentMethodDistribution;
}