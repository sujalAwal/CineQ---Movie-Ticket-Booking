package com.awal.cineq.rolehasmodule.dto.request;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import jakarta.validation.constraints.NotBlank;

/**
 * DTO for RoleHasModule creation and update requests
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class RoleHasModuleRequestDto {

    @NotBlank(message = "Role ID is required")
    private String roleId;

    @NotBlank(message = "Module ID is required")
    private String moduleId;

    @NotBlank(message = "Role name is required")
    private String role;

    @NotBlank(message = "Module name is required")
    private String module;

    private boolean is_active;
}
