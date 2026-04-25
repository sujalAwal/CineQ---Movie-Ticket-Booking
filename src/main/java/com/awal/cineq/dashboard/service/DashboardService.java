package com.awal.cineq.dashboard.service;

import com.awal.cineq.dashboard.dto.DashboardStatsDTO;

public interface DashboardService {

    DashboardStatsDTO getDashboardStats(int period);
}
