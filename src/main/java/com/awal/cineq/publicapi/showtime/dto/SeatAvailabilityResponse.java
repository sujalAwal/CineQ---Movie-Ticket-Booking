package com.awal.cineq.publicapi.showtime.dto;

import lombok.*;
import com.fasterxml.jackson.annotation.JsonInclude;

import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@JsonInclude(JsonInclude.Include.NON_NULL)
public class SeatAvailabilityResponse {
    private String showtimeId;
    private String movieId;
    private String theatreId;
    private String theatreName;
    private String screenId;
    private String screenName;
    private String showDate;
    private String showTime;
    private String language;
    private String format;
    private String statusCode;
    private Double basePrice;
    private Integer rows;
    private Integer columns;
    private Integer totalSeats;
    private Integer availableSeats;
    private List<SeatInfo> seats;
}
