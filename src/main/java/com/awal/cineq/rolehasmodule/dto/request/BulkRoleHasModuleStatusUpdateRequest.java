package com.awal.cineq.rolehasmodule.dto.request;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import jakarta.validation.constraints.NotEmpty;

import java.util.List;

/**
 * DTO for bulk status update requests
 * Used for enabling/disabling multiple role-module permissions at once
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class BulkRoleHasModuleStatusUpdateRequest {

    @NotEmpty(message = "IDs list cannot be empty")
    private List<String> ids;
}
