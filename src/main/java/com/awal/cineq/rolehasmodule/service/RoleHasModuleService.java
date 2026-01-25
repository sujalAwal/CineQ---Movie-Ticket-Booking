package com.awal.cineq.rolehasmodule.service;

import com.awal.cineq.dto.PaginationResponse;
import com.awal.cineq.rolehasmodule.dto.RoleHasModuleDTO;
import com.awal.cineq.rolehasmodule.dto.request.RoleHasModuleRequestDto;
import com.awal.cineq.rolehasmodule.dto.request.RoleHasModulePageRequest;

import java.util.List;

/**
 * Service interface for RoleHasModule operations
 * Defines contract for all business logic related to role-module permissions
 */
public interface RoleHasModuleService {

    /**
     * Get paginated list of role-module permissions
     * @param request pagination and filter parameters
     * @return paginated response with role-module permissions
     */
    PaginationResponse<RoleHasModuleDTO> getRoleHasModules(RoleHasModulePageRequest request);

    /**
     * Create a new role-module permission
     * @param requestDto role-module permission details
     * @return created role-module permission DTO
     */
    RoleHasModuleDTO createRoleHasModule(RoleHasModuleRequestDto requestDto);

    /**
     * Get role-module permission by ID
     * @param id role-module permission ID
     * @return role-module permission DTO
     */
    RoleHasModuleDTO getRoleHasModuleById(String id);

    /**
     * Update existing role-module permission
     * @param id role-module permission ID
     * @param requestDto updated role-module permission details
     * @return updated role-module permission DTO
     */
    RoleHasModuleDTO updateRoleHasModule(String id, RoleHasModuleRequestDto requestDto);

    /**
     * Delete (soft-delete) role-module permission
     * @param id role-module permission ID
     */
    void deleteRoleHasModule(String id);

    /**
     * Bulk enable/disable role-module permissions
     * @param ids list of role-module permission IDs
     * @param enabled true to enable, false to disable
     */
    void bulkEnableRoleHasModules(List<String> ids, boolean enabled);
}
