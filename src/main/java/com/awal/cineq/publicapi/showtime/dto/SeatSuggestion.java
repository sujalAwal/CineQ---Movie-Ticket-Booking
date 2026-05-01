package com.awal.cineq.publicapi.showtime.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class SeatSuggestion {
    private Integer rank;
    private String row;
    private Integer startCol;
    private Integer endCol;
    private Double score;
    private List<SeatInfo> seats;
    private Double totalPrice;
}
