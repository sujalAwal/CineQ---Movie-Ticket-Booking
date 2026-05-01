package com.awal.cineq.form.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * DTO for bulk soft-delete requests
 * Allows soft-deleting multiple documents in a single request
 *
 * Example:
 * {
 *   "formSlug": "role",
 *   "ids": ["507f1f77bcf86cd799439011", "507f1f77bcf86cd799439012"]
 * }
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class BulkDeleteRequest {

    @NotBlank(message = "Form slug is required")
    private String formSlug;  // Form slug to determine target collection (e.g., "role", "banner")

    @NotEmpty(message = "IDs array cannot be empty")
    private List<String> ids;  // MongoDB ObjectIds as Strings (minimum 1 ID required)
}

