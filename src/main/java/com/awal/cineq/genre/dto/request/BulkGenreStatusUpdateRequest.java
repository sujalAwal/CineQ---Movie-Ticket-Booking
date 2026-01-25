package com.awal.cineq.genre.dto.request;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * Request DTO for bulk genre status updates
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class BulkGenreStatusUpdateRequest {
    private List<String> ids;  // MongoDB ObjectIds as Strings
}

