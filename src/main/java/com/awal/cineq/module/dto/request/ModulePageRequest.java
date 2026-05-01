package com.awal.cineq.module.dto.request;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;

/**
 * Module Pagination Request DTO
 * Handles pagination, sorting, and search parameters
 * 
 * Defaults:
 * - page: 1 (1-based for API, converted to 0-based internally)
 * - size: 25
 * - sortBy: createdAt
 * - sortDirection: desc
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class ModulePageRequest {

    /**
     * Page number (1-based for API)
     */
    @Min(value = 1, message = "Page must be at least 1")
    private int page = 1;

    /**
     * Items per page
     */
    @Min(value = 1, message = "Size must be at least 1")
    @Max(value = 100, message = "Size must not exceed 100")
    private int size = 25;  // Default 25 items per page

    /**
     * Sort field (default: createdAt)
     */
    private String sortBy = "createdAt";

    /**
     * Sort direction (asc/desc, default: desc)
     */
    private String sortDirection = "desc";

    /**
     * Optional search term for name filtering
     */
    private String search;

    /**
     * Check if search parameter is provided
     */
    public boolean hasSearch() {
        return search != null && !search.trim().isEmpty();
    }

    /**
     * Convert to Spring Data PageRequest (0-based)
     */
    public PageRequest toPageRequest() {
        Sort.Direction direction = "desc".equalsIgnoreCase(sortDirection)
                ? Sort.Direction.DESC
                : Sort.Direction.ASC;

        return PageRequest.of(page - 1, size, Sort.by(direction, sortBy));
    }
}
