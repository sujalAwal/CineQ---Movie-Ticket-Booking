package com.awal.cineq.module.service.impl;

import com.awal.cineq.action.model.Action;
import com.awal.cineq.action.repository.ActionRepository;
import com.awal.cineq.config.ApplicationProperties;
import com.awal.cineq.dto.PaginationResponse;
import com.awal.cineq.exception.BusinessException;
import com.awal.cineq.exception.DuplicateResourceException;
import com.awal.cineq.exception.ResourceNotFoundException;
import com.awal.cineq.module.dto.ModuleRequestDTO;
import com.awal.cineq.module.dto.ModuleResponseDTO;
import com.awal.cineq.module.dto.request.ModulePageRequest;
import com.awal.cineq.module.model.Module;
import com.awal.cineq.module.repository.ModuleRepository;
import com.awal.cineq.module.service.ModuleService;
import com.awal.cineq.permission.model.Permission;
import com.awal.cineq.permission.repository.PermissionRepository;
import com.awal.cineq.rolehasmodule.model.RoleHasModule;
import com.awal.cineq.rolehasmodule.repository.RoleHasModuleRepository;
import com.awal.cineq.user.model.User;
import com.awal.cineq.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.modelmapper.ModelMapper;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * Module Service Implementation
 * Handles all business logic for module management with comprehensive logging
 */
@Service
@RequiredArgsConstructor
@Transactional
@Slf4j
public class ModuleServiceImpl implements ModuleService {

    private final ModuleRepository moduleRepository;
    private final ActionRepository actionRepository;
    private final PermissionRepository permissionRepository;
    private final UserRepository userRepository;
    private final RoleHasModuleRepository roleHasModuleRepository;
    private final ModelMapper modelMapper;
    private final ApplicationProperties applicationProperties;

    @Override
    public ModuleResponseDTO createModule(ModuleRequestDTO request) {
        log.info("createModule STARTED:");
        try {
            log.debug("createModule: creating module entity");
            Module module = new Module();

            Integer nextCode = getNextCode();
            log.debug("createModule: assigned code={}", nextCode);
            module.setCode(nextCode);

            module.setName(request.getName());
            module.setDisplayName(request.getDisplayName());
            module.setApi(request.getApi());
            module.setDescription(request.getDescription());
            module.setIsEnabled(request.getIsEnabled() != null ? request.getIsEnabled() : true);
            module.setParentId(request.getParentId());

            log.debug("createModule: saving module entity with code={}", nextCode);
            Module savedModule = moduleRepository.save(module);
            log.info("createModule: module saved with id={}, code={}", savedModule.getId(), savedModule.getCode());

            log.debug("createModule: fetching active actions for permission generation");
            List<Action> activeActions = actionRepository.findAllActive();
            log.info("createModule: found {} active actions for permission generation", activeActions.size());

            List<Permission> permissions = new ArrayList<>();
            for (Action action : activeActions) {
                Permission permission = new Permission();
                permission.setCode(savedModule.getCode() + "_" + action.getCode());
                permission.setModuleId(savedModule.getId());
                permission.setModuleName(savedModule.getName());
                permission.setModuleCode(savedModule.getCode());
                permission.setActionId(action.getId());
                permission.setActionName(action.getName());
                permission.setActionCode(action.getCode());
                permission.setIsEnabled(true);

                permissions.add(permission);
                log.debug("createModule: prepared permission code={}", permission.getCode());
            }

            if (!permissions.isEmpty()) {
                log.debug("createModule: saving {} permissions", permissions.size());
                permissionRepository.saveAll(permissions);
                log.info("createModule: {} permissions created for module code={}", permissions.size(), savedModule.getCode());
            } else {
                log.warn("createModule: no active actions found, no permissions generated");
            }

            ModuleResponseDTO response = modelMapper.map(savedModule, ModuleResponseDTO.class);
            response.setPermissionCount((long) permissions.size());

            log.info("createModule END: id={}, code={}, permissionCount={}",
                     savedModule.getId(), savedModule.getCode(), permissions.size());
            return response;

        } catch (DuplicateResourceException e) {
            log.error("createModule DUPLICATE ERROR: {}", e.getMessage());
            throw e;
        } catch (Exception e) {
            log.error("createModule ERROR: Failed to create module", e);
            throw new BusinessException("Failed to create module", e);
        }
    }

    @Override
    @Transactional(readOnly = true)
    public PaginationResponse<ModuleResponseDTO> getModules(ModulePageRequest pageRequest) {
        log.info("getModules STARTED: page={}, size={}, sortBy={}, sortDirection={}", 
                 pageRequest.getPage(), pageRequest.getSize(), pageRequest.getSortBy(), pageRequest.getSortDirection());
        try {
            PageRequest pageable = pageRequest.toPageRequest();
            log.debug("getModules: PageRequest created with sort={}", pageable.getSort());

            Page<Module> modules = findModules(pageRequest, pageable);
            Long total = modules.getTotalElements();
            log.debug("getModules: total modules found={}", total);

            List<ModuleResponseDTO> result = modules.stream()
                    .map(module -> {
                        ModuleResponseDTO dto = modelMapper.map(module, ModuleResponseDTO.class);
                        // Add permission count for each module
                        long permCount = permissionRepository.countByModuleIdAndDeletedAtIsNull(module.getId());
                        dto.setPermissionCount(permCount);
                        log.debug("getModules: module code={}, permissionCount={}", module.getCode(), permCount);
                        return dto;
                    })
                    .toList();

            log.debug("getModules: result count={}", result.size());
            log.info("getModules END: returned {} modules", result.size());

            return PaginationResponse.success(
                    "Modules fetched successfully",
                    result,
                    modules.getNumber() + 1, // Convert 0-based to 1-based
                    modules.getSize(),
                    modules.getTotalPages(),
                    modules.getTotalElements(),
                    modules.hasNext(),
                    modules.hasPrevious()
            );

        } catch (Exception e) {
            log.error("getModules ERROR: Failed to fetch modules", e);
            throw new BusinessException("Failed to fetch modules", e);
        }
    }

    @Override
    @Transactional(readOnly = true)
    public ModuleResponseDTO getModuleById(String id) {
        log.info("getModuleById STARTED: id={}", id);
        try {
            Module module = moduleRepository.findByIdAndDeletedAtIsNull(id)
                    .orElseThrow(() -> {
                        log.error("getModuleById NOT FOUND: id={}", id);
                        return new ResourceNotFoundException("Module not found with id: " + id);
                    });

            log.debug("getModuleById: found module code={}", module.getCode());

            ModuleResponseDTO response = modelMapper.map(module, ModuleResponseDTO.class);

            // Add permission count
            long permCount = permissionRepository.countByModuleIdAndDeletedAtIsNull(module.getId());
            response.setPermissionCount(permCount);

            log.debug("getModuleById: module code={}, permissionCount={}", module.getCode(), permCount);
            log.info("getModuleById END: id={}, code={}", id, module.getCode());

            return response;

        } catch (ResourceNotFoundException e) {
            log.error("getModuleById NOT FOUND: {}", e.getMessage());
            throw e;
        } catch (Exception e) {
            log.error("getModuleById ERROR: Failed to fetch module by id", e);
            throw new BusinessException("Failed to fetch module by id", e);
        }
    }

    @Override
    public ModuleResponseDTO updateModule(String id, ModuleRequestDTO request) {
        log.info("updateModule STARTED: id={}", id);
        try {
            Module module = moduleRepository.findByIdAndDeletedAtIsNull(id)
                    .orElseThrow(() -> {
                        log.error("updateModule NOT FOUND: id={}", id);
                        return new ResourceNotFoundException("Module not found with id: " + id);
                    });

            log.debug("updateModule: updating module fields for code={}", module.getCode());
            module.setName(request.getName());
            module.setDisplayName(request.getDisplayName());
            module.setApi(request.getApi());
            module.setDescription(request.getDescription());
            module.setIcon(request.getIcon());
            module.setIsEnabled(request.getIsEnabled() != null ? request.getIsEnabled() : module.getIsEnabled());
            module.setParentId(request.getParentId());

            Module updated = moduleRepository.save(module);
            log.info("updateModule: module updated id={}, code={}", updated.getId(), updated.getCode());

            ModuleResponseDTO response = modelMapper.map(updated, ModuleResponseDTO.class);
            long permCount = permissionRepository.countByModuleIdAndDeletedAtIsNull(module.getId());
            response.setPermissionCount(permCount);

            log.debug("updateModule: response prepared with permissionCount={}", permCount);
            log.info("updateModule END: id={}, code={}", id, updated.getCode());

            return response;

        } catch (ResourceNotFoundException e) {
            log.error("updateModule NOT FOUND: {}", e.getMessage());
            throw e;
        } catch (Exception e) {
            log.error("updateModule ERROR: Failed to update module", e);
            throw new BusinessException("Failed to update module", e);
        }
    }

    @Override
    public void deleteModule(String id) {
        log.info("deleteModule STARTED: id={}", id);
        try {
            Module module = moduleRepository.findByIdAndDeletedAtIsNull(id)
                    .orElseThrow(() -> {
                        log.error("deleteModule NOT FOUND: id={}", id);
                        return new ResourceNotFoundException("Module not found with id: " + id);
                    });

            log.debug("deleteModule: soft deleting module code={}", module.getCode());
            // Soft delete: set deletedAt timestamp
            module.setDeletedAt(LocalDateTime.now());
            moduleRepository.save(module);

            log.info("deleteModule: module soft-deleted id={}, code={}", id, module.getCode());

            // NOTE: Permissions are NOT soft-deleted (kept for audit trail)
            // This preserves historical permission assignments in roles
            log.debug("deleteModule: associated permissions kept for audit trail");

            log.info("deleteModule END: id={}, code={}", id, module.getCode());

        } catch (ResourceNotFoundException e) {
            log.error("deleteModule NOT FOUND: {}", e.getMessage());
            throw e;
        } catch (Exception e) {
            log.error("deleteModule ERROR: Failed to delete module", e);
            throw new BusinessException("Failed to delete module", e);
        }
    }

    @Override
    @Transactional
    public void bulkEnableModules(List<String> ids, boolean enabled) {
        log.info("bulkEnableModules STARTED: count={}, enabled={}", ids.size(), enabled);
        try {
            List<Module> modules = moduleRepository.findAllByIdInAndDeletedAtIsNull(ids);
            log.debug("bulkEnableModules: found {} modules", modules.size());

            if (modules.size() != ids.size()) {
                log.error("bulkEnableModules ERROR: some modules not found. Expected={}, Found={}", ids.size(), modules.size());
                throw new ResourceNotFoundException("Some modules not found for the provided IDs");
            }

            if (modules.isEmpty()) {
                log.error("bulkEnableModules ERROR: no active modules found");
                throw new ResourceNotFoundException("No active modules found for the provided IDs");
            }

            for (Module module : modules) {
                log.debug("bulkEnableModules: updating module code={}, enabled={}", module.getCode(), enabled);
                module.setIsEnabled(enabled);
            }

            moduleRepository.saveAll(modules);
            log.info("bulkEnableModules END: updated {} modules", modules.size());

        } catch (ResourceNotFoundException e) {
            log.error("bulkEnableModules ERROR: {}", e.getMessage());
            throw e;
        } catch (Exception e) {
            log.error("bulkEnableModules ERROR: Failed to bulk update modules", e);
            throw new BusinessException("Failed to bulk update modules", e);
        }
    }

    /**
     * Private helper method to find modules with optional search
     */
    private Page<Module> findModules(ModulePageRequest request, PageRequest pageRequest) {
        log.debug("findModules: hasSearch={}", request.hasSearch());

        if (request.hasSearch()) {
            log.debug("findModules: searching by name pattern={}", request.getSearch());
            return moduleRepository.findByNameContainingIgnoreCaseAndDeletedAtIsNull(request.getSearch(), pageRequest);
        } else {
            log.debug("findModules: fetching all modules");
            return moduleRepository.findAllByDeletedAtIsNull(pageRequest);
        }
    }

    /**
     * Generate next auto-increment code
     * MongoDB pattern: max(code) + 1
     * First module gets code 1, second gets 2, etc.
     *
     * @return Next available code
     */
    private Integer getNextCode() {
        log.debug("getNextCode: finding max code");
        try {
            // Use Optional pattern to safely handle query results
            // findMaxCode() returns the module with highest code, sorted descending
            Optional<Module> maxModuleOpt = moduleRepository.findMaxCode();

            if (maxModuleOpt.isEmpty() || maxModuleOpt.get().getCode() == null) {
                log.debug("getNextCode: no modules found, assigning code=1");
                return 1;
            }

            Integer maxCode = maxModuleOpt.get().getCode();
            Integer nextCode = maxCode + 1;
            log.debug("getNextCode: max code={}, next code={}", maxCode, nextCode);
            return nextCode;

        } catch (Exception e) {
            log.error("getNextCode ERROR: Failed to calculate next code", e);
            throw new BusinessException("Failed to generate auto-increment code", e);
        }
    }

    @Override
    @Transactional(readOnly = true)
    public List<ModuleResponseDTO> getActiveParentModules() {
        log.info("getActiveParentModules STARTED");
        try {
            List<Module> modules = moduleRepository.findAllActiveParentModules();
            log.debug("getActiveParentModules: found {} active parent modules", modules.size());

            List<ModuleResponseDTO> result = modules.stream()
                    .map(module -> {
                        ModuleResponseDTO dto = modelMapper.map(module, ModuleResponseDTO.class);
                        // Add permission count for each module
                        long permCount = permissionRepository.countByModuleIdAndDeletedAtIsNull(module.getId());
                        dto.setPermissionCount(permCount);
                        return dto;
                    })
                    .toList();

            log.info("getActiveParentModules END: returned {} modules", result.size());
            return result;

        } catch (Exception e) {
            log.error("getActiveParentModules ERROR: Failed to fetch active parent modules", e);
            throw new BusinessException("Failed to fetch active parent modules", e);
        }
    }

    @Override
    @Transactional(readOnly = true)
    public List<ModuleResponseDTO> getModulesByUserRole(String email) {
        log.info("getModulesByUserRole STARTED: email={}", email);
        try {
            // Step 1: Find user by email
            User user = userRepository.findByEmail(email)
                    .orElseThrow(() -> new ResourceNotFoundException("User not found with email: " + email));

            String userRole = user.getRole();
            log.debug("getModulesByUserRole: user found with id={}, role={}", user.getId(), userRole);

            // Step 2: Check if user has prominent role (SUPERADMIN, ADMIN, etc.)
            // If prominent, return all enabled modules; otherwise, use role-based access
            List<Module> modules;

            if (isProminentRole(userRole)) {
                log.info("getModulesByUserRole: prominent role detected ({}), returning all enabled modules", userRole);
                modules = moduleRepository.findAllByIsEnabledTrueAndDeletedAtIsNull();
            } else {
                // Step 3: Find all role-module mappings for this user's role (active mappings only, excluding soft-deleted)
                List<RoleHasModule> roleModules = roleHasModuleRepository.findByRoleAndIsActiveTrueAndDeletedAtIsNull(userRole);

                if (roleModules.isEmpty()) {
                    log.info("getModulesByUserRole: no modules assigned to role={}", userRole);
                    return Collections.emptyList();
                }

                log.debug("getModulesByUserRole: found {} role-module mappings for role={}", roleModules.size(), userRole);

                // Step 4: Extract module IDs from role-module mappings
                List<String> moduleIds = roleModules.stream()
                        .map(RoleHasModule::getModuleId)
                        .distinct()
                        .collect(Collectors.toList());

                log.debug("getModulesByUserRole: extracted {} unique module IDs", moduleIds.size());

                // Step 5: Fetch all modules by IDs, excluding soft-deleted ones and only enabled modules
                modules = moduleRepository.findAllByIdInAndIsEnabledTrueAndDeletedAtIsNull(moduleIds);
                log.info("getModulesByUserRole: fetched {} active and enabled modules for role={}", modules.size(), userRole);
            }

            // Step 6: Convert to DTOs with permission counts
            List<ModuleResponseDTO> result = modules.stream()
                    .map(module -> {
                        ModuleResponseDTO dto = modelMapper.map(module, ModuleResponseDTO.class);
                        long permCount = permissionRepository.countByModuleIdAndDeletedAtIsNull(module.getId());
                        dto.setPermissionCount(permCount);
                        return dto;
                    })
                    .collect(Collectors.toList());

            log.info("getModulesByUserRole END: returned {} modules for user={}", result.size(), userRole);
            return result;

        } catch (ResourceNotFoundException e) {
            log.error("getModulesByUserRole ERROR: User not found with email={}", email, e);
            throw e;
        } catch (Exception e) {
            log.error("getModulesByUserRole ERROR: Failed to fetch modules for user", e);
            throw new BusinessException("Failed to fetch modules for logged-in user", e);
        }
    }

    /**
     * Check if role is prominent (has access to all modules)
     * Reads from app.security.prominent-role property
     * Can be configured as: "SUPERADMIN" or "SUPERADMIN,ADMIN" (comma-separated)
     */
    private boolean isProminentRole(String role) {
        if (role == null) return false;

        String prominentRoles = applicationProperties.getSecurity().getProminentRole();
        if (prominentRoles == null || prominentRoles.isBlank()) {
            log.warn("isProminentRole: app.security.prominent-role not configured");
            return false;
        }

        // Split by comma to support multiple prominent roles (e.g., "SUPERADMIN,ADMIN")
        String[] roleArray = prominentRoles.split(",");
        for (String prominentRole : roleArray) {
            if (role.equalsIgnoreCase(prominentRole.trim())) {
                log.debug("isProminentRole: role={} is prominent", role);
                return true;
            }
        }

        return false;
    }
}
