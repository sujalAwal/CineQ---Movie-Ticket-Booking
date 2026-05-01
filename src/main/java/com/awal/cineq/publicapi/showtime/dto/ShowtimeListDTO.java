package com.awal.cineq.publicapi.showtime.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ShowtimeListDTO {
    
    @JsonProperty("id")
    private String id;
    
    @JsonProperty("movieId")
    private String movieId;
    
    @JsonProperty("showDate")
    private String showDate;
    
    @JsonProperty("showTime")
    private String showTime;
    
    @JsonProperty("theater")
    private TheaterInfo theater;
    
    @JsonProperty("screen")
    private ScreenInfo screen;
    
    @JsonProperty("statusCode")
    private String statusCode;
}
