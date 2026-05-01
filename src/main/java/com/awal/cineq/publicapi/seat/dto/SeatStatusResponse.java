package com.awal.cineq.publicapi.seat.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SeatStatusResponse {
    private String id;
    private Integer code;
    private String name;
    private String description;
    private String color;
    private Boolean isActive;
}
