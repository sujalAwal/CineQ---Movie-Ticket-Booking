package com.awal.cineq.publicapi.seat.service.impl;

import com.awal.cineq.dto.ApiResponse;
import com.awal.cineq.publicapi.seat.dto.SeatStatusResponse;
import com.awal.cineq.publicapi.seat.dto.SeatTypeResponse;
import com.awal.cineq.publicapi.seat.service.PublicSeatService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.bson.types.ObjectId;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class PublicSeatServiceImpl implements PublicSeatService {

    private final MongoTemplate mongoTemplate;

    @Override
    public ApiResponse<List<SeatTypeResponse>> getAllSeatTypes() {
        log.info("STARTED getAllSeatTypes");
        
        // Query for active seat types with soft delete filter
        Query query = new Query(Criteria.where("isActive").is(true)
                .and("deletedAt").is(null));
        
        @SuppressWarnings("unchecked")
        List<Map<String, Object>> seatTypes = 
            (List<Map<String, Object>>) (List<?>) mongoTemplate.find(query, Map.class, "seat_types");
        
        log.info("Found {} active seat types", seatTypes.size());
        
        // Convert to DTOs
        List<SeatTypeResponse> responses = seatTypes.stream()
                .map(this::convertMapToSeatTypeResponse)
                .collect(Collectors.toList());
        
        log.info("END getAllSeatTypes");
        return ApiResponse.success("Seat types fetched successfully", responses);
    }

    @Override
    public ApiResponse<List<SeatStatusResponse>> getAllSeatStatuses() {
        log.info("STARTED getAllSeatStatuses");
        
        // Query for active seat statuses with soft delete filter
        Query query = new Query(Criteria.where("isActive").is(true)
                .and("deletedAt").is(null));
        
        @SuppressWarnings("unchecked")
        List<Map<String, Object>> seatStatuses = 
            (List<Map<String, Object>>) (List<?>) mongoTemplate.find(query, Map.class, "seat_statuses");
        
        log.info("Found {} active seat statuses", seatStatuses.size());
        
        // Convert to DTOs
        List<SeatStatusResponse> responses = seatStatuses.stream()
                .map(this::convertMapToSeatStatusResponse)
                .collect(Collectors.toList());
        
        log.info("END getAllSeatStatuses");
        return ApiResponse.success("Seat statuses fetched successfully", responses);
    }

    /**
     * Convert raw MongoDB Map to SeatTypeResponse
     */
    private SeatTypeResponse convertMapToSeatTypeResponse(Map<String, Object> map) {
        String id = map.get("_id") != null ? map.get("_id").toString() : null;
        String code = (String) map.get("code");
        String name = (String) map.get("name");
        String description = (String) map.get("description");
        String color = (String) map.get("color");
        Boolean isActive = (Boolean) map.get("isActive");
        
        return SeatTypeResponse.builder()
                .id(id)
                .code(code)
                .name(name)
                .description(description)
                .color(color)
                .isActive(isActive)
                .build();
    }

    /**
     * Convert raw MongoDB Map to SeatStatusResponse
     */
    private SeatStatusResponse convertMapToSeatStatusResponse(Map<String, Object> map) {
        String id = map.get("_id") != null ? map.get("_id").toString() : null;
        Object codeObj = map.get("code");
        Integer code = codeObj instanceof Integer ? (Integer) codeObj : 
                      (codeObj instanceof Number ? ((Number) codeObj).intValue() : null);
        String name = (String) map.get("name");
        String description = (String) map.get("description");
        String color = (String) map.get("color");
        Boolean isActive = (Boolean) map.get("isActive");
        
        return SeatStatusResponse.builder()
                .id(id)
                .code(code)
                .name(name)
                .description(description)
                .color(color)
                .isActive(isActive)
                .build();
    }
}
