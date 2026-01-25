package com.awal.cineq.form.dto.request;

import jakarta.validation.constraints.NotEmpty;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * Bulk Form Manager Status Request DTO
 * Used for bulk activating or deactivating form managers
 * IDs are MongoDB ObjectIds stored as String
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class BulkFormManagerStatusRequest {

    @NotEmpty(message = "IDs list cannot be empty")
    private List<String> ids;  // MongoDB ObjectIds
}

