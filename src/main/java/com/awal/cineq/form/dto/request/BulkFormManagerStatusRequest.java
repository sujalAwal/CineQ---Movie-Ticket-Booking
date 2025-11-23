package com.awal.cineq.form.dto.request;

import jakarta.validation.constraints.NotEmpty;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;
import java.util.UUID;

/**
 * Bulk Form Manager Status Request DTO
 * Used for bulk activating or deactivating form managers
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class BulkFormManagerStatusRequest {

    @NotEmpty(message = "IDs list cannot be empty")
    private List<UUID> ids;
}

