package com.awal.cineq.screen.service;

import com.awal.cineq.dto.PaginationResponse;
import com.awal.cineq.screen.dto.ScreenDTO;

import java.util.List;

/**
 * Service interface for Screen operations
 * Defines business logic methods for screen management
 */
public interface ScreenService {

    /**
     * Get all screens for a specific theatre
     * @param theatreId Theatre identifier
     * @return List of screen DTOs
     */
    List<ScreenDTO> getScreensByTheatreId(String theatreId);

    /**
     * Get paginated screens for a specific theatre
     * @param theatreId Theatre identifier
     * @param page Page number
     * @param size Page size
     * @return Paginated response with screens
     */
    PaginationResponse<ScreenDTO> getScreensByTheatreIdPaginated(String theatreId, int page, int size);

    /**
     * Get screen by ID
     * @param id Screen identifier
     * @return Screen DTO
     */
    ScreenDTO getScreenById(String id);

    /**
     * Get all active screens
     * @return List of all screen DTOs
     */
    List<ScreenDTO> getAllScreens();

    /**
     * Get all active screens (paginated)
     * @param page Page number
     * @param size Page size
     * @return Paginated response with screens
     */
    PaginationResponse<ScreenDTO> getAllScreensPaginated(int page, int size);

    /**
     * Count screens in a theatre
     * @param theatreId Theatre identifier
     * @return Number of screens
     */
    long getScreenCountByTheatre(String theatreId);
}
