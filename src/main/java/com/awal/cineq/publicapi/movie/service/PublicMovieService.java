package com.awal.cineq.publicapi.movie.service;

import com.awal.cineq.dto.ApiResponse;
import com.awal.cineq.dto.PaginationResponse;
import com.awal.cineq.frontend.movies.dto.MovieDTO;
import com.awal.cineq.frontend.movies.dto.request.MoviePageRequest;
import com.awal.cineq.publicapi.movie.dto.PublicMovieDetailResponse;

/**
 * Service interface for the public movies API.
 *
 * Provides read-only access to active movies for the customer portal.
 * Supports paginated listing, status-based filtering, and full detail fetching.
 */
public interface PublicMovieService {

    /**
     * Returns a paginated list of all active movies.
     * Supports search, sort, filter options (including releaseStatus) via {@link MoviePageRequest}.
     *
     * @param pageRequest pagination + filter parameters
     * @return paginated {@link MovieDTO} list
     */
    PaginationResponse<MovieDTO> getAllMovies(MoviePageRequest pageRequest);

    /**
     * Returns the full detail of a single active movie by its ID,
     * including resolved genre names.
     *
     * @param id MongoDB document ID
     * @return {@link ApiResponse} wrapping {@link PublicMovieDetailResponse}
     * @throws com.awal.cineq.exception.ResourceNotFoundException if movie not found or inactive
     */
    ApiResponse<PublicMovieDetailResponse> getMovieById(String id);
}
