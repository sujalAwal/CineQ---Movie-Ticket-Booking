package com.awal.cineq.frontend.movies.controller;

import com.awal.cineq.dto.ApiResponse;
import com.awal.cineq.dto.PaginationResponse;
import com.awal.cineq.exception.ResourceNotFoundException;
import com.awal.cineq.frontend.movies.dto.MovieDTO;
import com.awal.cineq.frontend.movies.dto.request.MoviePageRequest;
import com.awal.cineq.frontend.movies.service.FrontendMovieService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

/**
 * REST Controller for Frontend Movie endpoints
 * Provides public GET-only API for customer movie browsing
 */
@RestController
@RequestMapping("/frontend/movies")
@RequiredArgsConstructor
@Slf4j
public class FrontendMovieController {

  private final FrontendMovieService service;

  @GetMapping(path = {"", "/"})
  public PaginationResponse<MovieDTO> getAllMovies(@Valid MoviePageRequest pageRequest) {
    log.info("getAllMovies STARTED");
    try {
      PaginationResponse<MovieDTO> response = service.getAllMovies(pageRequest);
      log.info("getAllMovies END");
      return response;
    } catch (Exception e) {
      log.error("getAllMovies ERROR", e);
      throw e;
    }
  }

  @GetMapping("/{id}")
  public ResponseEntity<ApiResponse<MovieDTO>> getMovieById(
    @PathVariable String id,
    HttpServletRequest request
  ) {
    log.info("getMovieById STARTED: id={}", id);
    try {
      MovieDTO movie = service.getMovieById(id);
      if (movie == null) {
        throw new ResourceNotFoundException("Movie not found with id: " + id);
      }
      ApiResponse<MovieDTO> response = ApiResponse.success("Movie fetched successfully", movie);
      response.setPath(request.getRequestURI());
      log.info("getMovieById END");
      return ResponseEntity.ok(response);
    } catch (Exception e) {
      log.error("getMovieById ERROR", e);
      throw e;
    }
  }
}
