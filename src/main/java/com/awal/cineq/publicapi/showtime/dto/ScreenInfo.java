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
public class ScreenInfo {
    
    @JsonProperty("id")
    private String id;
    
    @JsonProperty("title")
    private String title;
}
