package com.awal.cineq.publicapi.theatre.service;

import com.awal.cineq.dto.ApiResponse;
import com.awal.cineq.dto.PaginationResponse;
import com.awal.cineq.frontend.theatres.dto.TheatreDTO;
import com.awal.cineq.frontend.theatres.dto.request.TheatrePageRequest;

import java.util.List;

/**
 * Service interface for the public theatres API.
 *
 * Provides read-only access to active theatres for the customer portal.
 * Delegates to {@link com.awal.cineq.frontend.theatres.service.FrontendTheatreService}
 * and adds city-listing functionality.
 */
public interface PublicTheatreService {

    /**
     * Returns a paginated list of all active theatres.
     * Supports search, sort, and filter options via {@link TheatrePageRequest}.
     *
     * @param pageRequest pagination + filter parameters
     * @return paginated {@link TheatreDTO} list
     */
    PaginationResponse<TheatreDTO> getAllTheatres(TheatrePageRequest pageRequest);

    /**
     * Returns the full detail of a single active theatre by its ID.
     *
     * @param id MongoDB document ID
     * @return {@link ApiResponse} wrapping {@link TheatreDTO}
     * @throws com.awal.cineq.exception.ResourceNotFoundException if theatre not found or inactive
     */
    ApiResponse<TheatreDTO> getTheatreById(String id);

    /**
     * Returns a paginated list of active theatres in the given city.
     *
     * @param city city name (exact match)
     * @param page 1-based page number
     * @param size page size
     * @return paginated {@link TheatreDTO} list
     */
    PaginationResponse<TheatreDTO> getTheatresByCity(String city, int page, int size);

    /**
     * Returns a paginated list of active theatres in the given state.
     *
     * @param state state name (exact match)
     * @param page  1-based page number
     * @param size  page size
     * @return paginated {@link TheatreDTO} list
     */
    PaginationResponse<TheatreDTO> getTheatresByState(String state, int page, int size);

    /**
     * Returns a distinct, sorted list of all cities that have at least one active theatre.
     *
     * @return alphabetically sorted list of city names, never null, may be empty
     */
    List<String> getCities();
}
