package com.awal.cineq.dashboard.controller;

import com.awal.cineq.dashboard.dto.DashboardStatsDTO;
import com.awal.cineq.dashboard.service.DashboardService;
import com.awal.cineq.dto.ApiResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/dashboard")
@RequiredArgsConstructor
@Slf4j
public class DashboardController {

    private final DashboardService dashboardService;

    @GetMapping("/stats")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'ADMIN')")
    public ResponseEntity<ApiResponse<DashboardStatsDTO>> getDashboardStats(
            @RequestParam(defaultValue = "7") int period) {

        int normalizedPeriod = normalizePeriod(period);
        if (period != normalizedPeriod) {
            log.warn("Invalid dashboard period={} received, defaulting to {}", period, normalizedPeriod);
        }

        log.info("Fetching dashboard stats with period={} days", normalizedPeriod);
        DashboardStatsDTO stats = dashboardService.getDashboardStats(normalizedPeriod);

        return ResponseEntity.ok(ApiResponse.success("Dashboard stats fetched successfully", stats));
    }

    private int normalizePeriod(int period) {
        return (period == 7 || period == 15 || period == 30) ? period : 7;
    }
}