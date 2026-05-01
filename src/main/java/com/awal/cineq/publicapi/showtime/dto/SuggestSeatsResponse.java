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
public class SuggestSeatsResponse {
    private String showtimeId;
    private Integer requestedSeats;
    private List<SeatSuggestion> suggestions;
}
