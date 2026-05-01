package com.awal.cineq.movie.dto.request;

import jakarta.validation.constraints.NotEmpty;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * Bulk movie status update request DTO
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class BulkMovieStatusRequest {
    
    @NotEmpty(message = "Movie IDs cannot be empty")
    private List<String> ids;
    
    private boolean isActive;
}
