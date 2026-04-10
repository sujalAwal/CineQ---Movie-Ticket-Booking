package com.awal.cineq.publicapi.showtime.service;

import com.awal.cineq.dto.ApiResponse;
import com.awal.cineq.dto.PaginationResponse;
import com.awal.cineq.frontend.showtimes.dto.ShowtimeDTO;
import com.awal.cineq.frontend.showtimes.dto.request.ShowtimePageRequest;
import com.awal.cineq.publicapi.showtime.dto.SeatAvailabilityResponse;

public interface PublicShowtimeService {

    PaginationResponse<ShowtimeDTO> getAllShowtimes(ShowtimePageRequest pageRequest);

    ApiResponse<ShowtimeDTO> getShowtimeById(String id);

    PaginationResponse<ShowtimeDTO> getShowtimesByMovieId(String movieId, int page, int size);

    PaginationResponse<ShowtimeDTO> getShowtimesByTheatreId(String theatreId, int page, int size);

    ApiResponse<SeatAvailabilityResponse> getSeatAvailability(String showtimeId);
}
