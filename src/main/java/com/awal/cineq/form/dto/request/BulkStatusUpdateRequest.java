package com.awal.cineq.form.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * DTO for bulk status update requests
 * Allows updating isActive status for multiple documents in a single request
 *
 * Example:
 * {
 *   "formSlug": "role",
 *   "ids": ["507f1f77bcf86cd799439011", "507f1f77bcf86cd799439012"],
 *   "isActive": true
 * }
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class BulkStatusUpdateRequest {

    @NotBlank(message = "Form slug is required")
    private String formSlug;  // Form slug to determine target collection (e.g., "role", "banner")

    @NotEmpty(message = "IDs array cannot be empty")
    private List<String> ids;  // MongoDB ObjectIds as Strings (minimum 1 ID required)

    @NotNull(message = "isActive value is required")
    private Boolean isActive;  // true = activate, false = deactivate
}

