package com.awal.cineq.frontend.screens.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ScreenDTO {
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

    @JsonProperty("soundSystem")
    private String soundSystem;

    @JsonProperty("breakTime")
    private Integer breakTime;

    @JsonProperty("is_active")
    private boolean isActive;

    @JsonProperty("created_at")
    private Long createdAt;

    @JsonProperty("updated_at")
    private Long updatedAt;
}
