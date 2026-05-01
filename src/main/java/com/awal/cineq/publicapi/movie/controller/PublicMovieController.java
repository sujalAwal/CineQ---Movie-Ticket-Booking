package com.awal.cineq.publicapi.movie.controller;

import com.awal.cineq.dto.ApiResponse;
import com.awal.cineq.dto.PaginationResponse;
import com.awal.cineq.frontend.movies.dto.MovieDTO;
import com.awal.cineq.frontend.movies.dto.request.MoviePageRequest;
import com.awal.cineq.publicapi.movie.dto.PublicMovieDetailResponse;
import com.awal.cineq.publicapi.movie.service.PublicMovieService;
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

/**
 * Public Movie Controller
 *
 * Provides completely open, no-auth endpoints for the customer portal
 * to browse movies and fetch full movie details.
 *
 * Routes:
 *   GET /public/movies               – paginated list of active movies (with optional releaseStatus filter)
 *   GET /public/movies/{id}          – full detail for a single movie
 *
 * WHY no @PreAuthorize?
 * SecurityConfig already has .requestMatchers("/public/**").permitAll()
 * so no JWT or role checks are applied to this controller at all.
 */
@RestController
@RequestMapping("/public/movies")
@RequiredArgsConstructor
@Slf4j
public class PublicMovieController {

    private final PublicMovieService publicMovieService;

    /**
     * GET /public/movies?page=1&size=10&releaseStatus=NOW_SHOWING&search=&sortBy=title
     *
     * Returns a paginated list of active movies with optional filters.
     * releaseStatus must be a valid code from movieReleaseStatuses collection and must be active.
     *
     * @param pageRequest pagination + filter parameters bound from query string
     * @return {@link PaginationResponse} wrapping {@link MovieDTO} list
     * Returns per movie: title, releaseDate, duration, poster, genres, status
     */
    @GetMapping(path = {"", "/"})
    public PaginationResponse<MovieDTO> getAllMovies(@Valid MoviePageRequest pageRequest) {
        log.info("GET /public/movies STARTED: pageRequest={}", pageRequest);
        try {
            PaginationResponse<MovieDTO> response = publicMovieService.getAllMovies(pageRequest);
            log.info("GET /public/movies END – {} movies returned", response.getData() != null ? response.getData().size() : 0);
            return response;
        } catch (IllegalArgumentException e) {
            log.error("GET /public/movies ERROR – invalid releaseStatus: {}", e.getMessage());
            throw e;
        } catch (Exception e) {
            log.error("GET /public/movies ERROR", e);
            throw e;
        }
    }

    /**
     * GET /public/movies/{id}
     *
     * Returns the full detail of a single active movie including resolved genre names.
     * Returns all fields: title, description, poster, banner, trailerUrl, duration, releaseDate,
     * language, country, certification, formats, status, director, starcast, genres.
     *
     * @param id      MongoDB document ID (path variable)
     * @param request the raw HTTP request (used for error path reporting)
     * @return {@link ResponseEntity} wrapping {@link ApiResponse} of {@link PublicMovieDetailResponse}
     */
    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<PublicMovieDetailResponse>> getMovieById(
            @PathVariable String id,
            HttpServletRequest request) {
        log.info("GET /public/movies/{} STARTED", id);
        try {
            ApiResponse<PublicMovieDetailResponse> response = publicMovieService.getMovieById(id);
            log.info("GET /public/movies/{} END", id);
            return ResponseEntity.ok(response);
        } catch (com.awal.cineq.exception.ResourceNotFoundException e) {
            log.error("GET /public/movies/{} ERROR – not found", id);
            return ResponseEntity.status(404).body(ApiResponse.error(e.getMessage()));
        } catch (Exception e) {
            log.error("GET /public/movies/{} ERROR", id, e);
            return ResponseEntity.internalServerError().body(
                    ApiResponse.error("Failed to fetch movie: " + e.getMessage())
            );
        }
    }
}
