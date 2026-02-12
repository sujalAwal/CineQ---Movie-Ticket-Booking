package com.awal.cineq.frontend.showtimes.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ShowtimeDTO {
    private String id;

    @JsonProperty("movieId")
    private String movieId;

    @JsonProperty("screenId")
    private String screenId;

    @JsonProperty("theatreId")
    private String theatreId;

    @JsonProperty("showDate")
    private String showDate;

    @JsonProperty("showTime")
    private String showTime;

    @JsonProperty("language")
    private String language;

    @JsonProperty("format")
    private String format;

    @JsonProperty("statusCode")
    private String statusCode;

    @JsonProperty("basePrice")
    private Double basePrice;

    @JsonProperty("is_active")
    private boolean isActive;

    @JsonProperty("created_at")
    private Long createdAt;

    @JsonProperty("updated_at")
    private Long updatedAt;
}
