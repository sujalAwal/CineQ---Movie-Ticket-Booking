package com.awal.cineq.frontend.showtimes.controller;

import com.awal.cineq.dto.ApiResponse;
import com.awal.cineq.dto.PaginationResponse;
import com.awal.cineq.exception.ResourceNotFoundException;
import com.awal.cineq.frontend.showtimes.dto.ShowtimeDTO;
import com.awal.cineq.frontend.showtimes.dto.request.ShowtimePageRequest;
import com.awal.cineq.frontend.showtimes.service.FrontendShowtimeService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

/**
 * REST Controller for Frontend Showtime endpoints
 * Provides public GET-only API for customer showtime browsing
 */
@RestController
@RequestMapping("/frontend/showtimes")
@RequiredArgsConstructor
@Slf4j
public class FrontendShowtimeController {

  private final FrontendShowtimeService service;

  @GetMapping(path = {"", "/"})
  public PaginationResponse<ShowtimeDTO> getAllShowtimes(@Valid ShowtimePageRequest pageRequest) {
    log.info("getAllShowtimes STARTED");
    try {
      PaginationResponse<ShowtimeDTO> response = service.getAllShowtimes(pageRequest);
      log.info("getAllShowtimes END");
      return response;
    } catch (Exception e) {
      log.error("getAllShowtimes ERROR", e);
      throw e;
    }
  }

  @GetMapping("/{id}")
  public ResponseEntity<ApiResponse<ShowtimeDTO>> getShowtimeById(
    @PathVariable String id,
    HttpServletRequest request
  ) {
    log.info("getShowtimeById STARTED: id={}", id);
    try {
      ShowtimeDTO showtime = service.getShowtimeById(id);
      if (showtime == null) {
        throw new ResourceNotFoundException("Showtime not found with id: " + id);
      }
      ApiResponse<ShowtimeDTO> response = ApiResponse.success("Showtime fetched successfully", showtime);
      response.setPath(request.getRequestURI());
      log.info("getShowtimeById END");
      return ResponseEntity.ok(response);
    } catch (Exception e) {
      log.error("getShowtimeById ERROR", e);
      throw e;
    }
  }

  @GetMapping("/movie/{movieId}")
  public PaginationResponse<ShowtimeDTO> getShowtimesByMovieId(
    @PathVariable String movieId,
    @RequestParam(defaultValue = "1") int page,
    @RequestParam(defaultValue = "20") int size
  ) {
    log.info("getShowtimesByMovieId STARTED: movieId={}", movieId);
    try {
      PaginationResponse<ShowtimeDTO> response = service.getShowtimesByMovieId(movieId, page, size);
      log.info("getShowtimesByMovieId END");
      return response;
    } catch (Exception e) {
      log.error("getShowtimesByMovieId ERROR", e);
      throw e;
    }
  }

  @GetMapping("/theatre/{theatreId}")
  public PaginationResponse<ShowtimeDTO> getShowtimesByTheatreId(
    @PathVariable String theatreId,
    @RequestParam(defaultValue = "1") int page,
    @RequestParam(defaultValue = "20") int size
  ) {
    log.info("getShowtimesByTheatreId STARTED: theatreId={}", theatreId);
    try {
      PaginationResponse<ShowtimeDTO> response = service.getShowtimesByTheatreId(theatreId, page, size);
      log.info("getShowtimesByTheatreId END");
      return response;
    } catch (Exception e) {
      log.error("getShowtimesByTheatreId ERROR", e);
      throw e;
    }
  }

  @GetMapping("/screen/{screenId}")
  public PaginationResponse<ShowtimeDTO> getShowtimesByScreenId(
    @PathVariable String screenId,
    @RequestParam(defaultValue = "1") int page,
    @RequestParam(defaultValue = "20") int size
  ) {
    log.info("getShowtimesByScreenId STARTED: screenId={}", screenId);
    try {
      PaginationResponse<ShowtimeDTO> response = service.getShowtimesByScreenId(screenId, page, size);
      log.info("getShowtimesByScreenId END");
      return response;
    } catch (Exception e) {
      log.error("getShowtimesByScreenId ERROR", e);
      throw e;
    }
  }
}
