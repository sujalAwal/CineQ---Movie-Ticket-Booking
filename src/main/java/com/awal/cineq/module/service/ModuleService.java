package com.awal.cineq.module.service;

import com.awal.cineq.dto.PaginationResponse;
import com.awal.cineq.module.dto.ModuleRequestDTO;
import com.awal.cineq.module.dto.ModuleResponseDTO;
import com.awal.cineq.module.dto.request.ModulePageRequest;

import java.util.List;

/**
 * Module Service Interface
 * Defines business operations for module management
 */
public interface ModuleService {

    /**
     * Create new module with auto-permission generation
     * Generates permissions for all active actions
     * 
     * @param request Module creation data
     * @return Created module with permission count
     */
    ModuleResponseDTO createModule(ModuleRequestDTO request);

    /**
     * Get all modules (paginated, exclude soft-deleted)
     * Default: page=1, size=25, sort=createdAt DESC
     * 
     * @param pageRequest Pagination and search parameters
     * @return Paginated list of modules
     */
    PaginationResponse<ModuleResponseDTO> getModules(ModulePageRequest pageRequest);

    /**
     * Get single module by ID (exclude soft-deleted)
     * 
     * @param id Module ID
     * @return Module details with permission count
     */
    ModuleResponseDTO getModuleById(String id);

    /**
     * Update module (code is immutable, throws error if changed)
     * Can update: name, displayName, description, isEnabled
     * 
     * @param id Module ID
     * @param request Updated module data
     * @return Updated module
     */
    ModuleResponseDTO updateModule(String id, ModuleRequestDTO request);

    /**
     * Soft delete module (set deletedAt = now)
     * Associated permissions are kept for audit trail
     * 
     * @param id Module ID
     */
    void deleteModule(String id);

    /**
     * Bulk enable/disable modules
     * Updates isEnabled flag for multiple modules
     * 
     * @param ids List of module IDs
     * @param enabled true to enable, false to disable
     */
    void bulkEnableModules(List<String> ids, boolean enabled);

    /**
     * Get all active parent modules
     * Conditions: isEnabled=true, parentId=null, api=null
     * Used for navigation menus and parent module selection
     *
     * @return List of active parent modules
     */
    List<ModuleResponseDTO> getActiveParentModules();

    /**
     * Get all modules accessible to user based on their role
     * Fetches user's role, then finds all role-module mappings,
     * then returns active modules (deletedAt=null)
     *
     * @param email User email/username
     * @return List of modules the user has access to via their role
     */
    List<ModuleResponseDTO> getModulesByUserRole(String email);
}
