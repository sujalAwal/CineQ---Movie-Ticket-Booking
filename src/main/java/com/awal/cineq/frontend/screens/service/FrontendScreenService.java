package com.awal.cineq.frontend.screens.service;

import com.awal.cineq.dto.PaginationResponse;
import com.awal.cineq.frontend.screens.dto.ScreenDTO;
import com.awal.cineq.frontend.screens.dto.request.ScreenPageRequest;

public interface FrontendScreenService {
  PaginationResponse<ScreenDTO> getAllScreens(ScreenPageRequest pageRequest);
  ScreenDTO getScreenById(String id);
  PaginationResponse<ScreenDTO> getScreensByTheatreId(String theatreId, int page, int size);
  PaginationResponse<ScreenDTO> getScreensByType(String screenType, int page, int size);
}
