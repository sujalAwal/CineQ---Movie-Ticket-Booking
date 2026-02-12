package com.awal.cineq.frontend.theatres.controller;

import com.awal.cineq.dto.ApiResponse;
import com.awal.cineq.dto.PaginationResponse;
import com.awal.cineq.exception.ResourceNotFoundException;
import com.awal.cineq.frontend.theatres.dto.TheatreDTO;
import com.awal.cineq.frontend.theatres.dto.request.TheatrePageRequest;
import com.awal.cineq.frontend.theatres.service.FrontendTheatreService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

/**
 * REST Controller for Frontend Theatre endpoints
 * Provides public GET-only API for customer theatre browsing
 */
@RestController
@RequestMapping("/frontend/theatres")
@RequiredArgsConstructor
@Slf4j
public class FrontendTheatreController {

  private final FrontendTheatreService service;

  @GetMapping(path = {"", "/"})
  public PaginationResponse<TheatreDTO> getAllTheatres(@Valid TheatrePageRequest pageRequest) {
    log.info("getAllTheatres STARTED");
    try {
      PaginationResponse<TheatreDTO> response = service.getAllTheatres(pageRequest);
      log.info("getAllTheatres END");
      return response;
    } catch (Exception e) {
      log.error("getAllTheatres ERROR", e);
      throw e;
    }
  }

  @GetMapping("/{id}")
  public ResponseEntity<ApiResponse<TheatreDTO>> getTheatreById(
    @PathVariable String id,
    HttpServletRequest request
  ) {
    log.info("getTheatreById STARTED: id={}", id);
    try {
      TheatreDTO theatre = service.getTheatreById(id);
      if (theatre == null) {
        throw new ResourceNotFoundException("Theatre not found with id: " + id);
      }
      ApiResponse<TheatreDTO> response = ApiResponse.success("Theatre fetched successfully", theatre);
      response.setPath(request.getRequestURI());
      log.info("getTheatreById END");
      return ResponseEntity.ok(response);
    } catch (Exception e) {
      log.error("getTheatreById ERROR", e);
      throw e;
    }
  }

  @GetMapping("/city/{city}")
  public PaginationResponse<TheatreDTO> getTheatresByCity(
    @PathVariable String city,
    @RequestParam(defaultValue = "1") int page,
    @RequestParam(defaultValue = "20") int size
  ) {
    log.info("getTheatresByCity STARTED: city={}", city);
    try {
      PaginationResponse<TheatreDTO> response = service.getTheatresByCity(city, page, size);
      log.info("getTheatresByCity END");
      return response;
    } catch (Exception e) {
      log.error("getTheatresByCity ERROR", e);
      throw e;
    }
  }

  @GetMapping("/state/{state}")
  public PaginationResponse<TheatreDTO> getTheatresByState(
    @PathVariable String state,
    @RequestParam(defaultValue = "1") int page,
    @RequestParam(defaultValue = "20") int size
  ) {
    log.info("getTheatresByState STARTED: state={}", state);
    try {
      PaginationResponse<TheatreDTO> response = service.getTheatresByState(state, page, size);
      log.info("getTheatresByState END");
      return response;
    } catch (Exception e) {
      log.error("getTheatresByState ERROR", e);
      throw e;
    }
  }
}
