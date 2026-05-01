package com.awal.cineq.module.controller;

import com.awal.cineq.dto.ApiResponse;
import com.awal.cineq.dto.PaginationResponse;
import com.awal.cineq.module.dto.ModuleRequestDTO;
import com.awal.cineq.module.dto.ModuleResponseDTO;
import com.awal.cineq.module.dto.request.BulkModuleStatusUpdateRequest;
import com.awal.cineq.module.dto.request.ModulePageRequest;
import com.awal.cineq.module.service.ModuleService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * REST Controller for Module endpoints
 * Provides CRUD operations for modules with auto-permission generation
 * 
 * Base path: /api/modules
 */
@RestController
@RequiredArgsConstructor
@RequestMapping("/modules")
@Slf4j
public class ModuleController {

    private final ModuleService moduleService;

    /**
     * GET /api/modules/parents - Get all active parent modules
     * Returns modules where: isEnabled=true, parentId=null, api=null
     * Used for navigation menus and parent module selection dropdowns
     */
    @GetMapping("/parents")
    public ResponseEntity<ApiResponse<List<ModuleResponseDTO>>> getActiveParentModules(
            HttpServletRequest httpRequest) {
        log.info("getActiveParentModules STARTED");
        try {
            List<ModuleResponseDTO> modules = moduleService.getActiveParentModules();
            ApiResponse<List<ModuleResponseDTO>> response = ApiResponse.success(
                "Active parent modules fetched successfully",
                modules
            );
            response.setPath(httpRequest.getRequestURI());
            log.info("getActiveParentModules END: count={}", modules.size());
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            log.error("getActiveParentModules ERROR", e);
            throw e;
        }
    }

    /**
     * GET /api/modules/my-modules - Get all modules for current logged-in user
     * Returns only active modules (deletedAt=null) mapped to user's role via role_has_modules
     *
     * @param principal Contains authenticated user details with username/email
     * @return List of modules the user has access to based on their role
     */
    @GetMapping("/my-modules")
    public ResponseEntity<ApiResponse<List<ModuleResponseDTO>>> getMyModules(
            @AuthenticationPrincipal UserDetails principal,
            HttpServletRequest httpRequest) {
        log.info("getMyModules STARTED: user={}", principal.getUsername());
        try {
            List<ModuleResponseDTO> modules = moduleService.getModulesByUserRole(principal.getUsername());
            ApiResponse<List<ModuleResponseDTO>> response = ApiResponse.success(
                "User modules fetched successfully",
                modules
            );
            response.setPath(httpRequest.getRequestURI());
            log.info("getMyModules END: count={}", modules.size());
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            log.error("getMyModules ERROR", e);
            throw e;
        }
    }

    /**
     * GET /api/modules - Get all modules (paginated)
     * Query params: page (default: 1), size (default: 25), sortBy (default: createdAt), sortDirection (default: desc), search
     */
    @GetMapping(path = {"", "/"})
    public PaginationResponse<ModuleResponseDTO> getAllModules(@Valid ModulePageRequest pageRequest) {
        log.info("getAllModules STARTED");
        try {
            PaginationResponse<ModuleResponseDTO> response = moduleService.getModules(pageRequest);
            log.info("getAllModules END");
            return response;
        } catch (Exception e) {
            log.error("getAllModules ERROR", e);
            throw e;
        }
    }

    /**
     * POST /api/modules - Create new module
     * Automatically generates permissions for all active actions
     * Code is auto-generated as auto-increment integer
     */
    @PostMapping(path = {"", "/"})
    public ResponseEntity<ApiResponse<ModuleResponseDTO>> createModule(
            @RequestBody @Valid ModuleRequestDTO request,
            HttpServletRequest httpRequest) {
        log.info("createModule STARTED");
        try {
            ModuleResponseDTO created = moduleService.createModule(request);
            ApiResponse<ModuleResponseDTO> response = ApiResponse.success(
                "Module created successfully with " + created.getPermissionCount() + " permissions",
                created
            );
            response.setPath(httpRequest.getRequestURI());
            log.info("createModule END: id={}, code={}", created.getId(), created.getCode());
            return ResponseEntity.status(201).body(response);
        } catch (Exception e) {
            log.error("createModule ERROR", e);
            throw e;
        }
    }

    /**
     * GET /api/modules/{id} - Get single module by ID
     */
    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<ModuleResponseDTO>> getModuleById(
            @PathVariable String id,
            HttpServletRequest httpRequest) {
        log.info("getModuleById STARTED: id={}", id);
        try {
            ModuleResponseDTO module = moduleService.getModuleById(id);
            ApiResponse<ModuleResponseDTO> response = ApiResponse.success("Module fetched successfully", module);
            response.setPath(httpRequest.getRequestURI());
            log.info("getModuleById END: id={}, code={}", id, module.getCode());
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            log.error("getModuleById ERROR", e);
            throw e;
        }
    }

    /**
     * PUT /api/modules/{id} - Update module
     * Code field is immutable and cannot be changed
     */
    @PutMapping("/{id}")
    public ResponseEntity<ApiResponse<ModuleResponseDTO>> updateModule(
            @PathVariable String id,
            @RequestBody @Valid ModuleRequestDTO request,
            HttpServletRequest httpRequest) {
        log.info("updateModule STARTED: id={}", id);
        try {
            ModuleResponseDTO updated = moduleService.updateModule(id, request);
            ApiResponse<ModuleResponseDTO> response = ApiResponse.success("Module updated successfully", updated);
            response.setPath(httpRequest.getRequestURI());
            log.info("updateModule END: id={}, code={}", id, updated.getCode());
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            log.error("updateModule ERROR", e);
            throw e;
        }
    }

    /**
     * DELETE /api/modules/{id} - Soft delete module
     * Associated permissions are kept for audit trail
     */
    @DeleteMapping("/{id}")
    public ResponseEntity<ApiResponse<Void>> deleteModule(
            @PathVariable String id,
            HttpServletRequest httpRequest) {
        log.info("deleteModule STARTED: id={}", id);
        try {
            moduleService.deleteModule(id);
            ApiResponse<Void> response = ApiResponse.success("Module deleted successfully", null);
            response.setPath(httpRequest.getRequestURI());
            log.info("deleteModule END: id={}", id);
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            log.error("deleteModule ERROR", e);
            throw e;
        }
    }

    /**
     * POST /api/modules/bulk-enable - Enable multiple modules
     */
    @PostMapping("/bulk-enable")
    public ResponseEntity<ApiResponse<Void>> bulkEnableModules(
            @RequestBody @Valid BulkModuleStatusUpdateRequest bulkRequest,
            HttpServletRequest httpRequest) {
        log.info("bulkEnableModules STARTED: count={}", bulkRequest.getIds().size());
        try {
            moduleService.bulkEnableModules(bulkRequest.getIds(), true);
            ApiResponse<Void> response = ApiResponse.success("Modules enabled successfully", null);
            response.setPath(httpRequest.getRequestURI());
            log.info("bulkEnableModules END");
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            log.error("bulkEnableModules ERROR", e);
            throw e;
        }
    }

    /**
     * POST /api/modules/bulk-disable - Disable multiple modules
     */
    @PostMapping("/bulk-disable")
    public ResponseEntity<ApiResponse<Void>> bulkDisableModules(
            @RequestBody @Valid BulkModuleStatusUpdateRequest bulkRequest,
            HttpServletRequest httpRequest) {
        log.info("bulkDisableModules STARTED: count={}", bulkRequest.getIds().size());
        try {
            moduleService.bulkEnableModules(bulkRequest.getIds(), false);
            ApiResponse<Void> response = ApiResponse.success("Modules disabled successfully", null);
            response.setPath(httpRequest.getRequestURI());
            log.info("bulkDisableModules END");
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            log.error("bulkDisableModules ERROR", e);
            throw e;
        }
    }
}
