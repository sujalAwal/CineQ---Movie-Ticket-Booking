package com.awal.cineq.controller;

import com.awal.cineq.dto.ApiResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

@RestController
@Slf4j
@CrossOrigin(origins = "*", maxAge = 3600)
public class HealthController {
    
    @GetMapping("/")
    public ResponseEntity<ApiResponse<String>> home() {
        return ResponseEntity.ok(ApiResponse.success("CineQ Movie Booking System is running!"));
    }
    
    @GetMapping("/health")
    public ResponseEntity<ApiResponse<String>> health() {
        return ResponseEntity.ok(ApiResponse.success("Application is healthy", "OK"));
    }

    @GetMapping("/api/v1/health")
    public ResponseEntity<ApiResponse<Map<String, Object>>> healthApi() {
        Map<String, Object> healthData = new HashMap<>();
        healthData.put("status", "UP");
        healthData.put("timestamp", LocalDateTime.now());
        healthData.put("application", "CineQ Movie Ticket Booking System");
        healthData.put("version", "1.0.0");
        
        return ResponseEntity.ok(ApiResponse.success("Application is running", healthData));
    }
}