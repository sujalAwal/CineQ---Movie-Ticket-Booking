package com.awal.cineq.rolehasmodule.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * DTO for RoleHasModule entity
 * Used for API responses
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class RoleHasModuleDTO {
    private String id;  // MongoDB ObjectId stored as String

    @NotBlank(message = "Role ID is required")
    private String roleId;

    @NotBlank(message = "Module ID is required")
    private String moduleId;

    @NotBlank(message = "Role name is required")
    private String role;

    @NotBlank(message = "Module name is required")
    private String module;

    @JsonProperty("is_active")
    private boolean isActive;
}
