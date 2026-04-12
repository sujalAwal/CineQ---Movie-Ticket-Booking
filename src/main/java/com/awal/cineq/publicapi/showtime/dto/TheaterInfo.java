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
public class TheaterInfo {
    
    @JsonProperty("id")
    private String id;
    
    @JsonProperty("name")
    private String name;
}
