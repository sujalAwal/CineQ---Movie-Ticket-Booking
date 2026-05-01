package com.awal.cineq.rolehasmodule.controller;

import com.awal.cineq.dto.ApiResponse;
import com.awal.cineq.dto.PaginationResponse;
import com.awal.cineq.rolehasmodule.dto.RoleHasModuleDTO;
import com.awal.cineq.rolehasmodule.dto.request.RoleHasModulePageRequest;
import com.awal.cineq.rolehasmodule.dto.request.RoleHasModuleRequestDto;
import com.awal.cineq.rolehasmodule.dto.request.BulkRoleHasModuleStatusUpdateRequest;
import com.awal.cineq.rolehasmodule.service.RoleHasModuleService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

/**
 * REST Controller for RoleHasModule endpoints
 * Provides CRUD operations for role-module permissions
 */
@RestController
@RequiredArgsConstructor
@RequestMapping("role-has-module")
@Slf4j
public class RoleHasModuleController {

    private final RoleHasModuleService roleHasModuleService;

    @GetMapping(path = {"", "/"})
    public PaginationResponse<RoleHasModuleDTO> getAllRoleHasModules(@Valid RoleHasModulePageRequest request) {
        log.info("getAllRoleHasModules STARTED");
        try {
            PaginationResponse<RoleHasModuleDTO> response = roleHasModuleService.getRoleHasModules(request);
            log.info("getAllRoleHasModules END");
            return response;
        } catch (Exception e) {
            log.error("getAllRoleHasModules ERROR", e);
            throw e;
        }
    }

    @PostMapping(path = {"", "/"})
    public ResponseEntity<ApiResponse<RoleHasModuleDTO>> createRoleHasModule(
            @RequestBody @Valid RoleHasModuleRequestDto requestDto,
            HttpServletRequest request) {
        log.info("createRoleHasModule STARTED");
        try {
            RoleHasModuleDTO created = roleHasModuleService.createRoleHasModule(requestDto);
            ApiResponse<RoleHasModuleDTO> response = ApiResponse.success("Role-module permission created successfully", created);
            response.setPath(request.getRequestURI());
            log.info("createRoleHasModule END");
            return ResponseEntity.status(201).body(response);
        } catch (Exception e) {
            log.error("createRoleHasModule ERROR", e);
            throw e;
        }
    }

    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<RoleHasModuleDTO>> getRoleHasModuleById(
            @PathVariable String id,
            HttpServletRequest request) {
        log.info("getRoleHasModuleById STARTED: id={}", id);
        try {
            RoleHasModuleDTO roleHasModule = roleHasModuleService.getRoleHasModuleById(id);
            ApiResponse<RoleHasModuleDTO> response = ApiResponse.success("Role-module permission fetched successfully", roleHasModule);
            response.setPath(request.getRequestURI());
            log.info("getRoleHasModuleById END");
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            log.error("getRoleHasModuleById ERROR", e);
            throw e;
        }
    }

    @PutMapping("/{id}")
    public ResponseEntity<ApiResponse<RoleHasModuleDTO>> updateRoleHasModule(
            @PathVariable String id,
            @RequestBody @Valid RoleHasModuleRequestDto requestDto,
            HttpServletRequest request) {
        log.info("updateRoleHasModule STARTED: id={}", id);
        try {
            RoleHasModuleDTO updated = roleHasModuleService.updateRoleHasModule(id, requestDto);
            ApiResponse<RoleHasModuleDTO> response = ApiResponse.success("Role-module permission updated successfully", updated);
            response.setPath(request.getRequestURI());
            log.info("updateRoleHasModule END");
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            log.error("updateRoleHasModule ERROR", e);
            throw e;
        }
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<ApiResponse<Void>> deleteRoleHasModule(
            @PathVariable String id,
            HttpServletRequest request) {
        log.info("deleteRoleHasModule STARTED: id={}", id);
        try {
            roleHasModuleService.deleteRoleHasModule(id);
            ApiResponse<Void> response = ApiResponse.success("Role-module permission deleted successfully", null);
            response.setPath(request.getRequestURI());
            log.info("deleteRoleHasModule END");
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            log.error("deleteRoleHasModule ERROR", e);
            throw e;
        }
    }

    @PostMapping("/bulk-enable")
    public ResponseEntity<ApiResponse<Void>> bulkEnableRoleHasModules(
            @RequestBody @Valid BulkRoleHasModuleStatusUpdateRequest bulkRequest,
            HttpServletRequest request) {
        log.info("bulkEnableRoleHasModules STARTED");
        try {
            roleHasModuleService.bulkEnableRoleHasModules(bulkRequest.getIds(), true);
            ApiResponse<Void> response = ApiResponse.success("Role-module permissions enabled successfully", null);
            response.setPath(request.getRequestURI());
            log.info("bulkEnableRoleHasModules END");
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            log.error("bulkEnableRoleHasModules ERROR", e);
            throw e;
        }
    }

    @PostMapping("/bulk-disable")
    public ResponseEntity<ApiResponse<Void>> bulkDisableRoleHasModules(
            @RequestBody @Valid BulkRoleHasModuleStatusUpdateRequest bulkRequest,
            HttpServletRequest request) {
        log.info("bulkDisableRoleHasModules STARTED");
        try {
            roleHasModuleService.bulkEnableRoleHasModules(bulkRequest.getIds(), false);
            ApiResponse<Void> response = ApiResponse.success("Role-module permissions disabled successfully", null);
            response.setPath(request.getRequestURI());
            log.info("bulkDisableRoleHasModules END");
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            log.error("bulkDisableRoleHasModules ERROR", e);
            throw e;
        }
    }
}
