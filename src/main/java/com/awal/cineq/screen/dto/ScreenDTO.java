package com.awal.cineq.screen.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;
import java.util.Map;

/**
 * DTO for Screen responses
 * Used in API responses to expose screen data
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ScreenDTO {

    @JsonProperty("id")
    private String id;

    @JsonProperty("screenName")
    private String screenName;

    @JsonProperty("theatreId")
    private String theatreId;

    @JsonProperty("rows")
    private Integer rows;

    @JsonProperty("columns")
    private Integer columns;

    @JsonProperty("screenType")
    private String screenType;

    @JsonProperty("seatLayout")
    private List<Map<String, Object>> seatLayout;

    @JsonProperty("soundSystem")
    private String soundSystem;

    @JsonProperty("breakTime")
    private Integer breakTime;

    @JsonProperty("isActive")
    private Boolean isActive;
}
