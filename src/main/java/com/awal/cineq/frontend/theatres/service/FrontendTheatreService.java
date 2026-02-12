package com.awal.cineq.frontend.theatres.service;

import com.awal.cineq.dto.PaginationResponse;
import com.awal.cineq.frontend.theatres.dto.TheatreDTO;
import com.awal.cineq.frontend.theatres.dto.request.TheatrePageRequest;

public interface FrontendTheatreService {
  PaginationResponse<TheatreDTO> getAllTheatres(TheatrePageRequest pageRequest);
  TheatreDTO getTheatreById(String id);
  PaginationResponse<TheatreDTO> getTheatresByCity(String city, int page, int size);
  PaginationResponse<TheatreDTO> getTheatresByState(String state, int page, int size);
}
