package com.awal.cineq.publicapi.theatre.controller;

import com.awal.cineq.dto.ApiResponse;
import com.awal.cineq.dto.PaginationResponse;
import com.awal.cineq.frontend.theatres.dto.TheatreDTO;
import com.awal.cineq.frontend.theatres.dto.request.TheatrePageRequest;
import com.awal.cineq.publicapi.theatre.service.PublicTheatreService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * Public Theatre Controller
 *
 * Provides completely open, no-auth endpoints for the customer portal
 * to browse theatres, filter by city/state, and fetch full theatre details.
 *
 * Routes:
 *   GET /public/theatres                 – paginated list of all active theatres
 *   GET /public/theatres/cities          – distinct sorted list of cities with theatres
 *   GET /public/theatres/city/{city}     – theatres filtered by city
 *   GET /public/theatres/{id}            – full detail for a single theatre
 *
 * IMPORTANT: /cities is declared BEFORE /{id} to avoid Spring treating "cities"
 * as a path variable value when routing GET /public/theatres/cities.
 *
 * WHY no @PreAuthorize?
 * SecurityConfig already has .requestMatchers("/public/**").permitAll()
 * so no JWT or role checks are applied to this controller at all.
 */
@RestController
@RequestMapping("/public/theatres")
@RequiredArgsConstructor
@Slf4j
public class PublicTheatreController {

    private final PublicTheatreService publicTheatreService;

    /**
     * GET /public/theatres?page=1&size=10&search=&sortBy=name&city=&state=
     *
     * Returns a paginated list of all active theatres with optional search and filter.
     *
     * @param pageRequest pagination + filter parameters bound from query string
     * @return {@link PaginationResponse} wrapping {@link TheatreDTO} list
     */
    @GetMapping(path = {"", "/"})
    public PaginationResponse<TheatreDTO> getAllTheatres(@Valid TheatrePageRequest pageRequest) {
        log.info("GET /public/theatres STARTED: pageRequest={}", pageRequest);
        try {
            PaginationResponse<TheatreDTO> response = publicTheatreService.getAllTheatres(pageRequest);
            log.info("GET /public/theatres END – {} theatres returned", response.getData() != null ? response.getData().size() : 0);
            return response;
        } catch (Exception e) {
            log.error("GET /public/theatres ERROR", e);
            throw e;
        }
    }

    /**
     * GET /public/theatres/cities
     *
     * Returns a distinct, alphabetically sorted list of all cities that have
     * at least one active theatre. Used to populate city-filter dropdowns.
     *
     * NOTE: This mapping MUST appear before /{id} to avoid path-variable conflict.
     *
     * @return {@link ResponseEntity} wrapping {@link ApiResponse} of city name list
     */
    @GetMapping("/cities")
    public ResponseEntity<ApiResponse<List<String>>> getCities() {
        log.info("GET /public/theatres/cities STARTED");
        try {
            List<String> cities = publicTheatreService.getCities();
            log.info("GET /public/theatres/cities END – {} cities returned", cities.size());
            return ResponseEntity.ok(ApiResponse.success("Cities fetched successfully", cities));
        } catch (Exception e) {
            log.error("GET /public/theatres/cities ERROR", e);
            return ResponseEntity.internalServerError().body(
                    ApiResponse.error("Failed to fetch cities: " + e.getMessage())
            );
        }
    }

    /**
     * GET /public/theatres/city/{city}?page=1&size=10
     *
     * Returns a paginated list of active theatres in the given city.
     *
     * @param city city name (path variable, exact match)
     * @param page 1-based page number (default: 1)
     * @param size page size (default: 10)
     * @return {@link PaginationResponse} wrapping {@link TheatreDTO} list
     */
    @GetMapping("/city/{city}")
    public PaginationResponse<TheatreDTO> getTheatresByCity(
            @PathVariable String city,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "10") int size) {
        log.info("GET /public/theatres/city/{} STARTED: page={}, size={}", city, page, size);
        try {
            PaginationResponse<TheatreDTO> response = publicTheatreService.getTheatresByCity(city, page, size);
            log.info("GET /public/theatres/city/{} END – {} theatres returned", city, response.getData() != null ? response.getData().size() : 0);
            return response;
        } catch (Exception e) {
            log.error("GET /public/theatres/city/{} ERROR", city, e);
            throw e;
        }
    }

    /**
     * GET /public/theatres/state/{state}?page=1&size=10
     *
     * Returns a paginated list of active theatres in the given state.
     *
     * @param state state name (path variable, exact match)
     * @param page  1-based page number (default: 1)
     * @param size  page size (default: 10)
     * @return {@link PaginationResponse} wrapping {@link TheatreDTO} list
     */
    @GetMapping("/state/{state}")
    public PaginationResponse<TheatreDTO> getTheatresByState(
            @PathVariable String state,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "10") int size) {
        log.info("GET /public/theatres/state/{} STARTED: page={}, size={}", state, page, size);
        try {
            PaginationResponse<TheatreDTO> response = publicTheatreService.getTheatresByState(state, page, size);
            log.info("GET /public/theatres/state/{} END – {} theatres returned", state, response.getData() != null ? response.getData().size() : 0);
            return response;
        } catch (Exception e) {
            log.error("GET /public/theatres/state/{} ERROR", state, e);
            throw e;
        }
    }

    /**
     * GET /public/theatres/{id}
     *
     * Returns the full detail of a single active theatre by its ID.
     *
     * NOTE: This mapping MUST appear after /cities to avoid path-variable conflict.
     *
     * @param id      MongoDB document ID (path variable)
     * @param request the raw HTTP request (used for error path reporting)
     * @return {@link ResponseEntity} wrapping {@link ApiResponse} of {@link TheatreDTO}
     */
    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<TheatreDTO>> getTheatreById(
            @PathVariable String id,
            HttpServletRequest request) {
        log.info("GET /public/theatres/{} STARTED", id);
        try {
            ApiResponse<TheatreDTO> response = publicTheatreService.getTheatreById(id);
            log.info("GET /public/theatres/{} END", id);
            return ResponseEntity.ok(response);
        } catch (com.awal.cineq.exception.ResourceNotFoundException e) {
            log.error("GET /public/theatres/{} ERROR – not found", id);
            return ResponseEntity.status(404).body(ApiResponse.error(e.getMessage()));
        } catch (Exception e) {
            log.error("GET /public/theatres/{} ERROR", id, e);
            return ResponseEntity.internalServerError().body(
                    ApiResponse.error("Failed to fetch theatre: " + e.getMessage())
            );
        }
    }
}
