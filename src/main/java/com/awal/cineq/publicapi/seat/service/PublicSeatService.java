package com.awal.cineq.publicapi.seat.service;

import com.awal.cineq.dto.ApiResponse;
import com.awal.cineq.publicapi.seat.dto.SeatStatusResponse;
import com.awal.cineq.publicapi.seat.dto.SeatTypeResponse;

import java.util.List;

public interface PublicSeatService {
    ApiResponse<List<SeatTypeResponse>> getAllSeatTypes();
    ApiResponse<List<SeatStatusResponse>> getAllSeatStatuses();
}
