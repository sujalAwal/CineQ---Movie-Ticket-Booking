package com.awal.cineq.module.dto.request;

import jakarta.validation.constraints.NotEmpty;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * Bulk Module Status Update Request DTO
 * Used for bulk enable/disable operations
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class BulkModuleStatusUpdateRequest {

    /**
     * List of module IDs to update
     */
    @NotEmpty(message = "IDs list cannot be empty")
    private List<String> ids;
}
