package com.awal.cineq.frontend.screens.controller;

import com.awal.cineq.dto.ApiResponse;
import com.awal.cineq.dto.PaginationResponse;
import com.awal.cineq.exception.ResourceNotFoundException;
import com.awal.cineq.frontend.screens.dto.ScreenDTO;
import com.awal.cineq.frontend.screens.dto.request.ScreenPageRequest;
import com.awal.cineq.frontend.screens.service.FrontendScreenService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

/**
 * REST Controller for Frontend Screen endpoints
 * Provides public GET-only API for customer screen browsing
 */
@RestController
@RequestMapping("/frontend/screens")
@RequiredArgsConstructor
@Slf4j
public class FrontendScreenController {

  private final FrontendScreenService service;

  @GetMapping(path = {"", "/"})
  public PaginationResponse<ScreenDTO> getAllScreens(@Valid ScreenPageRequest pageRequest) {
    log.info("getAllScreens STARTED");
    try {
      PaginationResponse<ScreenDTO> response = service.getAllScreens(pageRequest);
      log.info("getAllScreens END");
      return response;
    } catch (Exception e) {
      log.error("getAllScreens ERROR", e);
      throw e;
    }
  }

  @GetMapping("/{id}")
  public ResponseEntity<ApiResponse<ScreenDTO>> getScreenById(
    @PathVariable String id,
    HttpServletRequest request
  ) {
    log.info("getScreenById STARTED: id={}", id);
    try {
      ScreenDTO screen = service.getScreenById(id);
      if (screen == null) {
        throw new ResourceNotFoundException("Screen not found with id: " + id);
      }
      ApiResponse<ScreenDTO> response = ApiResponse.success("Screen fetched successfully", screen);
      response.setPath(request.getRequestURI());
      log.info("getScreenById END");
      return ResponseEntity.ok(response);
    } catch (Exception e) {
      log.error("getScreenById ERROR", e);
      throw e;
    }
  }

  @GetMapping("/theatre/{theatreId}")
  public PaginationResponse<ScreenDTO> getScreensByTheatreId(
    @PathVariable String theatreId,
    @RequestParam(defaultValue = "1") int page,
    @RequestParam(defaultValue = "20") int size
  ) {
    log.info("getScreensByTheatreId STARTED: theatreId={}", theatreId);
    try {
      PaginationResponse<ScreenDTO> response = service.getScreensByTheatreId(theatreId, page, size);
      log.info("getScreensByTheatreId END");
      return response;
    } catch (Exception e) {
      log.error("getScreensByTheatreId ERROR", e);
      throw e;
    }
  }

  @GetMapping("/type/{screenType}")
  public PaginationResponse<ScreenDTO> getScreensByType(
    @PathVariable String screenType,
    @RequestParam(defaultValue = "1") int page,
    @RequestParam(defaultValue = "20") int size
  ) {
    log.info("getScreensByType STARTED: screenType={}", screenType);
    try {
      PaginationResponse<ScreenDTO> response = service.getScreensByType(screenType, page, size);
      log.info("getScreensByType END");
      return response;
    } catch (Exception e) {
      log.error("getScreensByType ERROR", e);
      throw e;
    }
  }
}
