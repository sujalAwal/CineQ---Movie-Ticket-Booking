package com.awal.cineq.publicapi.seat.controller;

import com.awal.cineq.dto.ApiResponse;
import com.awal.cineq.publicapi.seat.dto.SeatStatusResponse;
import com.awal.cineq.publicapi.seat.dto.SeatTypeResponse;
import com.awal.cineq.publicapi.seat.service.PublicSeatService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/public")
@RequiredArgsConstructor
public class PublicSeatController {

    private final PublicSeatService publicSeatService;

    /**
     * Get all active seat types
     * @return List of seat types
     */
    @GetMapping("/seat-types")
    public ResponseEntity<ApiResponse<List<SeatTypeResponse>>> getAllSeatTypes() {
        return ResponseEntity.ok(publicSeatService.getAllSeatTypes());
    }

    /**
     * Get all active seat statuses
     * @return List of seat statuses
     */
    @GetMapping("/seat-statuses")
    public ResponseEntity<ApiResponse<List<SeatStatusResponse>>> getAllSeatStatuses() {
        return ResponseEntity.ok(publicSeatService.getAllSeatStatuses());
    }
}

