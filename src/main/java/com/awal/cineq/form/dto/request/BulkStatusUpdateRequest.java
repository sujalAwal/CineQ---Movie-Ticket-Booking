package com.awal.cineq.form.dto.request;

import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * DTO for bulk status update requests
 * Allows updating isActive status for multiple form submissions in a single request
 *
 * Example:
 * {
 *   "ids": ["507f1f77bcf86cd799439011", "507f1f77bcf86cd799439012"],
 *   "isActive": true
 * }
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class BulkStatusUpdateRequest {

    @NotEmpty(message = "IDs array cannot be empty")
    private List<String> ids;  // MongoDB ObjectIds as Strings (minimum 1 ID required)

    @NotNull(message = "isActive value is required")
    private Boolean isActive;  // true = activate, false = deactivate
}

