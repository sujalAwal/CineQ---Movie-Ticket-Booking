package com.awal.cineq.frontend.showtimes.service;

import com.awal.cineq.dto.PaginationResponse;
import com.awal.cineq.frontend.showtimes.dto.ShowtimeDTO;
import com.awal.cineq.frontend.showtimes.dto.request.ShowtimePageRequest;

public interface FrontendShowtimeService {
  PaginationResponse<ShowtimeDTO> getAllShowtimes(ShowtimePageRequest pageRequest);
  ShowtimeDTO getShowtimeById(String id);
  PaginationResponse<ShowtimeDTO> getShowtimesByMovieId(String movieId, int page, int size);
  PaginationResponse<ShowtimeDTO> getShowtimesByTheatreId(String theatreId, int page, int size);
  PaginationResponse<ShowtimeDTO> getShowtimesByScreenId(String screenId, int page, int size);
}
