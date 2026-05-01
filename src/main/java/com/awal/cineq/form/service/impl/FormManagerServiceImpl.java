package com.awal.cineq.form.service.impl;

import com.awal.cineq.config.ApplicationProperties;
import com.awal.cineq.dto.PaginationResponse;
import com.awal.cineq.exception.BusinessException;
import com.awal.cineq.exception.ResourceNotFoundException;
import com.awal.cineq.form.dto.request.FormManagerRequest;
import com.awal.cineq.form.dto.request.FormStepRequest;
import com.awal.cineq.form.dto.response.FormManagerResponse;
import com.awal.cineq.form.dto.response.FormStepResponse;
import com.awal.cineq.form.enums.FormAction;
import com.awal.cineq.form.model.FormManager;
import com.awal.cineq.form.model.FormStep;
import com.awal.cineq.form.repository.FormManagerRepository;
import com.awal.cineq.form.repository.FormStepRepository;
import com.awal.cineq.form.service.FormConfigCacheService;
import com.awal.cineq.form.service.FormManagerService;
import com.awal.cineq.module.dto.ModuleRequestDTO;
import com.awal.cineq.module.service.ModuleService;
import com.awal.cineq.rolehasmodule.dto.request.RoleHasModuleRequestDto;
import com.awal.cineq.rolehasmodule.service.RoleHasModuleService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * MongoDB Implementation of FormManagerService
 * No Hibernate filters needed - MongoDB queries handle soft-delete
 */
@Slf4j
@Service
@RequiredArgsConstructor
@Transactional
public class FormManagerServiceImpl implements FormManagerService {

    private final FormManagerRepository formManagerRepository;
    private final FormStepRepository formStepRepository;
    private final ModuleService moduleService;
    private final FormConfigCacheService formConfigCacheService;
    private final ApplicationProperties applicationProperties;
    private final RoleHasModuleService roleHasModuleService;
    private final MongoTemplate mongoTemplate;

    @Override
    @Transactional(readOnly = true)
    public PaginationResponse<FormManagerResponse> getAllFormManagers(int page, int size, String sortBy,
                                                                      String sortDirection, String search) {
        log.info("getAllFormManagers STARTED: page={}, size={}, sortBy={}, sortDirection={}",
                page, size, sortBy, sortDirection);
        try {
            Sort.Direction direction = "desc".equalsIgnoreCase(sortDirection) ? Sort.Direction.DESC : Sort.Direction.ASC;
            PageRequest pageRequest = PageRequest.of(page - 1, size, Sort.by(direction, sortBy));

            // Use optimized repository methods with DB-level projection (excludes formSteps from query)
            Page<FormManager> formManagers;
            if (search != null && !search.trim().isEmpty()) {
                formManagers = formManagerRepository.findByTitleContainingIgnoreCaseForListing(search, pageRequest);
            } else {
                formManagers = formManagerRepository.findAllActiveForListing(pageRequest);
            }

            // Map to response (formSteps already excluded at DB level, no extra query needed)
            List<FormManagerResponse> responses = formManagers.stream()
                    .map(this::toResponseWithoutSteps)
                    .collect(Collectors.toList());

            log.info("getAllFormManagers END: found {} form managers", responses.size());
            return PaginationResponse.success(
                    "Form managers fetched successfully",
                    responses,
                    formManagers.getNumber() + 1,
                    formManagers.getSize(),
                    formManagers.getTotalPages(),
                    formManagers.getTotalElements(),
                    formManagers.hasNext(),
                    formManagers.hasPrevious()
            );
        } catch (Exception e) {
            log.error("getAllFormManagers ERROR", e);
            throw new BusinessException("Failed to fetch form managers", e);
        }
    }

    @Override
    @Transactional(readOnly = true)
    public FormManagerResponse getFormManagerById(String id) {
        log.info("getFormManagerById STARTED: id={}", id);
        try {
            FormManager formManager = formManagerRepository.findById(id)
                    .orElseThrow(() -> new ResourceNotFoundException("Form manager not found with id: " + id));

            // Check if soft-deleted
            if (formManager.getDeletedAt() != null) {
                throw new ResourceNotFoundException("Form manager not found with id: " + id);
            }

            FormManagerResponse response = toResponse(formManager);
            log.info("getFormManagerById END");
            return response;
        } catch (ResourceNotFoundException e) {
            log.error("getFormManagerById NOT FOUND", e);
            throw e;
        } catch (Exception e) {
            log.error("getFormManagerById ERROR", e);
            throw new BusinessException("Failed to fetch form manager by id", e);
        }
    }

    @Override
    @Transactional(readOnly = true)
    public FormManagerResponse getFormManagerBySlug(String slug) {
        log.info("getFormManagerBySlug STARTED: slug={}", slug);
        try {
            FormManager formManager = formManagerRepository.findBySlug(slug)
                    .orElseThrow(() -> new ResourceNotFoundException("Form manager not found with slug: " + slug));

            FormManagerResponse response = toResponse(formManager);
            log.info("getFormManagerBySlug END");
            return response;
        } catch (ResourceNotFoundException e) {
            log.error("getFormManagerBySlug NOT FOUND", e);
            throw e;
        } catch (Exception e) {
            log.error("getFormManagerBySlug ERROR", e);
            throw new BusinessException("Failed to fetch form manager by slug", e);
        }
    }

    @Override
    public FormManagerResponse createFormManager(FormManagerRequest request) {
        log.info("createFormManager STARTED: slug={}", request.getSlug());
        try {
            // Check if slug already exists
            if (formManagerRepository.countBySlug(request.getSlug()) > 0) {
                throw new BusinessException("Form manager with slug '" + request.getSlug() + "' already exists");
            }
            // Create module for this form manager
            ModuleRequestDTO moduleRequest = createModuleRequestFromFormManager(request);
            var createdModule = moduleService.createModule(moduleRequest);
            log.debug("createFormManager: Auto-created module with code={}, slug={}",
                    createdModule.getCode(), request.getSlug());

            // Create form manager
            FormManager formManager = new FormManager();
            formManager.setTitle(request.getTitle());
            formManager.setSlug(request.getSlug());
            formManager.setDescription(request.getDescription());
            formManager.setModelName(request.getModelName());
            formManager.setIsActive(request.getIsActive() != null ? request.getIsActive() : true);
            formManager.setModuleCode(createdModule.getCode());  // Link to auto-created module

            FormManager savedFormManager = formManagerRepository.save(formManager);
            log.debug("createFormManager: Saved form manager with id={}, moduleCode={}",
                    savedFormManager.getId(), savedFormManager.getModuleCode());

            // Create form steps
            List<FormStep> formSteps = new ArrayList<>();
            for (FormStepRequest stepRequest : request.getFormSteps()) {
                FormStep step = createFormStep(savedFormManager.getId(), stepRequest);
                formSteps.add(formStepRepository.save(step));
            }

            // Associate form steps with form manager
            savedFormManager.setFormSteps(formSteps);

            // Save again to persist the formSteps relationship in MongoDB
            savedFormManager = formManagerRepository.save(savedFormManager);
            log.debug("createFormManager: Persisted form steps count={}", formSteps.size());

            // Assign prominent role to the newly created module
            assignProminentRoleToModule(savedFormManager, createdModule);

            FormManagerResponse response = toResponse(savedFormManager);
            log.info("createFormManager END: id={}, moduleCode={}, stepsCount={}",
                    savedFormManager.getId(), savedFormManager.getModuleCode(), formSteps.size());
            return response;
        } catch (BusinessException e) {
            log.error("createFormManager BUSINESS ERROR", e);
            throw e;
        } catch (Exception e) {
            log.error("createFormManager ERROR", e);
            throw new BusinessException("Failed to create form manager", e);
        }
    }

    @Override
    public FormManagerResponse updateFormManager(String id, FormManagerRequest request) {
        log.info("updateFormManager STARTED: id={}", id);
        try {
            FormManager formManager = formManagerRepository.findById(id)
                    .orElseThrow(() -> new ResourceNotFoundException("Form manager not found with id: " + id));

            // Check if soft-deleted
            if (formManager.getDeletedAt() != null) {
                throw new ResourceNotFoundException("Form manager not found with id: " + id);
            }

            // Update basic fields
            formManager.setTitle(request.getTitle());
            formManager.setDescription(request.getDescription());
            formManager.setModelName(request.getModelName());
            formManager.setIsActive(request.getIsActive() != null ? request.getIsActive() : true);

            // Soft delete old steps
            List<FormStep> oldSteps = formStepRepository.findByFormManagerId(id);
            for (FormStep oldStep : oldSteps) {
                oldStep.setDeletedAt(LocalDateTime.now());
                oldStep.setIsActive(false);
            }
            formStepRepository.saveAll(oldSteps);

            // Create new steps
            List<FormStep> newSteps = new ArrayList<>();
            for (FormStepRequest stepRequest : request.getFormSteps()) {
                FormStep step = createFormStep(id, stepRequest);
                newSteps.add(formStepRepository.save(step));
            }

            formManager.setFormSteps(newSteps);
            FormManager updated = formManagerRepository.save(formManager);

            // Evict cache for updated FormManager and its steps
            formConfigCacheService.evictAllForFormManager(formManager.getId(), formManager.getSlug());
            log.debug("updateFormManager: Cache evicted for id={}, slug={}", formManager.getId(), formManager.getSlug());

            FormManagerResponse response = toResponse(updated);
            log.info("updateFormManager END");
            return response;
        } catch (ResourceNotFoundException e) {
            log.error("updateFormManager NOT FOUND", e);
            throw e;
        } catch (Exception e) {
            log.error("updateFormManager ERROR", e);
            throw new BusinessException("Failed to update form manager", e);
        }
    }

    @Override
    public void deleteFormManager(String id) {
        log.info("deleteFormManager STARTED: id={}", id);
        try {
            FormManager formManager = formManagerRepository.findById(id)
                    .orElseThrow(() -> new ResourceNotFoundException("Form manager not found with id: " + id));

            // Check if already soft-deleted
            if (formManager.getDeletedAt() != null) {
                throw new ResourceNotFoundException("Form manager not found with id: " + id);
            }

            // Store moduleCode before deletion (needed for cleanup)
            Integer moduleCode = formManager.getModuleCode();

            // Step 1: Remove module from prominent role permissions (non-blocking)
            if (moduleCode != null) {
                removeModuleFromProminentRole(moduleCode);
            }

            // Step 2: Soft-delete the module created during CREATE (non-blocking)
            if (moduleCode != null) {
                softDeleteModule(moduleCode);
            }

            // Step 3: Soft delete form manager
            formManager.setDeletedAt(LocalDateTime.now());
            formManager.setIsActive(false);
            formManagerRepository.save(formManager);
            log.debug("deleteFormManager: Soft-deleted FormManager id={}", id);

            // Step 4: Soft delete all associated steps
            List<FormStep> steps = formStepRepository.findByFormManagerId(id);
            for (FormStep step : steps) {
                step.setDeletedAt(LocalDateTime.now());
                step.setIsActive(false);
            }
            formStepRepository.saveAll(steps);
            log.debug("deleteFormManager: Soft-deleted {} FormSteps", steps.size());

            // Step 5: Evict cache for deleted FormManager and its steps
            formConfigCacheService.evictAllForFormManager(formManager.getId(), formManager.getSlug());
            log.debug("deleteFormManager: Cache evicted for id={}, slug={}", formManager.getId(), formManager.getSlug());

            log.info("deleteFormManager END: id={}, moduleCode={}", id, moduleCode);
        } catch (ResourceNotFoundException e) {
            log.error("deleteFormManager NOT FOUND", e);
            throw e;
        } catch (Exception e) {
            log.error("deleteFormManager ERROR", e);
            throw new BusinessException("Failed to delete form manager", e);
        }
    }

    @Override
    public void bulkEnableFormManagers(List<String> ids, boolean enabled) {
        log.info("bulkEnableFormManagers STARTED: count={}, enabled={}", ids.size(), enabled);
        try {
            List<FormManager> formManagers = formManagerRepository.findAllById(ids);
            if (formManagers.size() != ids.size()) {
                throw new ResourceNotFoundException("Some form managers not found for the provided IDs");
            }

            // Filter out soft-deleted
            List<FormManager> activeManagers = formManagers.stream()
                    .filter(fm -> fm.getDeletedAt() == null)
                    .toList();

            for (FormManager formManager : activeManagers) {
                formManager.setIsActive(enabled);
            }
            formManagerRepository.saveAll(activeManagers);

            // Evict cache for all updated FormManagers
            for (FormManager formManager : activeManagers) {
                formConfigCacheService.evictAllForFormManager(formManager.getId(), formManager.getSlug());
            }
            log.debug("bulkEnableFormManagers: Cache evicted for {} FormManagers", activeManagers.size());

            log.info("bulkEnableFormManagers END: updated={}", activeManagers.size());
        } catch (Exception e) {
            log.error("bulkEnableFormManagers ERROR", e);
            throw new BusinessException("Failed to bulk update form managers", e);
        }
    }

    private FormStep createFormStep(String formManagerId, FormStepRequest stepRequest) {
        FormStep step = new FormStep();
        step.setFormManagerId(formManagerId);
        step.setStepTitle(stepRequest.getStepTitle());
        step.setStepSlug(stepRequest.getStepSlug());
        step.setStepOrder(stepRequest.getStepOrder());
        step.setValidationRules(stepRequest.getValidationRules());
        step.setFormSchema(stepRequest.getFormSchema());
        step.setUiSchema(stepRequest.getUiSchema());
        step.setMetadata(stepRequest.getMetadata());
        step.setWorkflowRules(stepRequest.getWorkflowRules());
        step.setIsActive(stepRequest.getIsActive() != null ? stepRequest.getIsActive() : true);
        return step;
    }

    private FormManagerResponse toResponse(FormManager formManager) {
        FormManagerResponse response = new FormManagerResponse();
        response.setId(formManager.getId());
        response.setTitle(formManager.getTitle());
        response.setSlug(formManager.getSlug());
        response.setDescription(formManager.getDescription());
        response.setModelName(formManager.getModelName());
        response.setIsActive(formManager.getIsActive());
        response.setModuleCode(formManager.getModuleCode());

        // Get active form steps
        List<FormStep> activeSteps = formStepRepository.findByFormManagerIdAndActiveOrderByStepOrder(formManager.getId());

        List<FormStepResponse> stepResponses = activeSteps.stream()
                .map(this::toStepResponse)
                .collect(Collectors.toList());

        response.setFormSteps(stepResponses);

        return response;
    }

    /**
     * Convert FormManager to response WITHOUT fetching formSteps
     * Used for listing pages where only FormManager columns are needed
     * This avoids N+1 queries when fetching multiple FormManagers
     *
     * @param formManager The FormManager entity
     * @return FormManagerResponse without formSteps (formSteps will be null)
     */
    private FormManagerResponse toResponseWithoutSteps(FormManager formManager) {
        FormManagerResponse response = new FormManagerResponse();
        response.setId(formManager.getId());
        response.setTitle(formManager.getTitle());
        response.setSlug(formManager.getSlug());
        response.setDescription(formManager.getDescription());
        response.setModelName(formManager.getModelName());
        response.setIsActive(formManager.getIsActive());
        response.setModuleCode(formManager.getModuleCode());
        // formSteps intentionally not set (remains null) for listing optimization
        return response;
    }

    private FormStepResponse toStepResponse(FormStep step) {
        FormStepResponse response = new FormStepResponse();
        response.setId(step.getId());
        response.setStepTitle(step.getStepTitle());
        response.setStepSlug(step.getStepSlug());
        response.setStepOrder(step.getStepOrder());
        response.setValidationRules(step.getValidationRules());
        response.setFormSchema(step.getFormSchema());
        response.setUiSchema(step.getUiSchema());
        response.setMetadata(step.getMetadata());
        response.setWorkflowRules(step.getWorkflowRules());
        response.setIsActive(step.getIsActive());
        return response;
    }

    /**
     * Convert FormManagerRequest to ModuleRequest
     * - slug → name (capitalize, replace hyphens/underscores)
     * - title → displayName
     * - description → description
     * - api → /api/{slug}
     * - isEnabled → true
     */
    private ModuleRequestDTO createModuleRequestFromFormManager(FormManagerRequest formManagerRequest) {
        ModuleRequestDTO moduleRequest = new ModuleRequestDTO();

        // Convert slug to name: "role-management" → "Role Management"
        String name = slugToName(formManagerRequest.getSlug());
        moduleRequest.setName(name);

        // title → displayName
        moduleRequest.setDisplayName(formManagerRequest.getTitle());

        // api: /api/{slug}
        moduleRequest.setApi("/api/" + formManagerRequest.getSlug());

        // description → description
        moduleRequest.setDescription(formManagerRequest.getDescription());

        // Always enabled
        moduleRequest.setIsEnabled(true);

        log.debug("createModuleRequestFromFormManager: name={}, displayName={}, api={}",
                name, moduleRequest.getDisplayName(), moduleRequest.getApi());

        return moduleRequest;
    }

    /**
     * Convert slug to proper name format
     * Examples:
     * - "role" → "Role"
     * - "role-management" → "Role Management"
     * - "user_profile" → "User Profile"
     * - "user-profile-management" → "User Profile Management"
     */
    private String slugToName(String slug) {
        if (slug == null || slug.isBlank()) {
            return "";
        }

        // Replace hyphens and underscores with spaces
        String withSpaces = slug.replaceAll("[\\-_]+", " ");

        // Capitalize each word
        StringBuilder result = new StringBuilder();
        String[] words = withSpaces.split("\\s+");

        for (int i = 0; i < words.length; i++) {
            if (words[i].length() > 0) {
                result.append(words[i].substring(0, 1).toUpperCase())
                        .append(words[i].substring(1).toLowerCase());
            }
            if (i < words.length - 1) {
                result.append(" ");
            }
        }

        return result.toString();
    }

    private void assignProminentRoleToModule(FormManager formManager,
                                              com.awal.cineq.module.dto.ModuleResponseDTO createdModule) {
        log.info("assignProminentRoleToModule STARTED: module={}, moduleCode={}",
                createdModule.getName(), createdModule.getCode());

        try {
            String prominentRoleName = applicationProperties.getSecurity().getProminentRole();

            if (prominentRoleName == null || prominentRoleName.isBlank()) {
                log.warn("Prominent role not configured. Skipping role assignment.");
                return;
            }

            // Get module code
            Integer moduleCode = createdModule.getCode();
            if (moduleCode == null) {
                log.error("Module code is null. Cannot assign to role.");
                return;
            }

            log.debug("Module code: {}", moduleCode);

            // Query for existing role
            Query roleQuery = new Query(
                Criteria.where("name").is(prominentRoleName).and("deletedAt").is(null)
            );
            Map<String, Object> roleData = mongoTemplate.findOne(roleQuery, Map.class, "roles");

            if (roleData == null) {
                log.warn("Prominent role '{}' NOT FOUND. Create via API first.", prominentRoleName);
                return;
            }

            Object roleIdObj = roleData.get("_id");
            String roleId = roleIdObj != null ? roleIdObj.toString() : null;

            if (roleId == null || roleId.isBlank()) {
                log.error("Role has invalid _id.");
                return;
            }

            log.debug("Found role: id={}, name={}", roleId, prominentRoleName);

            // Get existing permissions (preserve them)
            @SuppressWarnings("unchecked")
            Map<String, Object> existingPermissions = (Map<String, Object>) roleData.get("permissions");

            if (existingPermissions == null) {
                existingPermissions = new java.util.HashMap<>();
            }

            // Get all FormAction codes dynamically from enum
            List<Integer> allPermissionCodes = new ArrayList<>();
            for (FormAction action : FormAction.values()) {
                allPermissionCodes.add(action.getCode());
            }

            // Add new module with all available permissions
            String moduleCodeStr = String.valueOf(moduleCode);
            existingPermissions.put(moduleCodeStr, allPermissionCodes);

            log.debug("Updated permissions with all actions: {}. All modules: {}",
                    allPermissionCodes, existingPermissions.keySet());

            // Update role document
            Query updateQuery = new Query(Criteria.where("_id").is(roleId));
            org.springframework.data.mongodb.core.query.Update update = new org.springframework.data.mongodb.core.query.Update()
                    .set("permissions", existingPermissions)
                    .set("updatedAt", LocalDateTime.now());

            mongoTemplate.updateFirst(updateQuery, update, "roles");

            log.info("assignProminentRoleToModule SUCCESS: Module {} added to role '{}' with all available permissions: {}",
                    moduleCode, prominentRoleName, allPermissionCodes);

        } catch (Exception e) {
            log.error("assignProminentRoleToModule ERROR (non-blocking): {}", e.getMessage(), e);
        }
    }

    /**
     * Remove module from prominent role permissions during FormManager deletion
     * IMPORTANT: Only removes the specific module we added, preserves other modules
     *
     * @param moduleCode The module code to remove from role permissions
     */
    private void removeModuleFromProminentRole(Integer moduleCode) {
        log.info("removeModuleFromProminentRole STARTED: moduleCode={}", moduleCode);

        try {
            String prominentRoleName = applicationProperties.getSecurity().getProminentRole();

            if (prominentRoleName == null || prominentRoleName.isBlank()) {
                log.warn("Prominent role not configured. Skipping role cleanup.");
                return;
            }

            // Find the role
            Query roleQuery = new Query(
                Criteria.where("name").is(prominentRoleName).and("deletedAt").is(null)
            );
            Map<String, Object> roleData = mongoTemplate.findOne(roleQuery, Map.class, "roles");

            if (roleData == null) {
                log.warn("Prominent role '{}' NOT FOUND. Cannot cleanup permissions.", prominentRoleName);
                return;
            }

            Object roleIdObj = roleData.get("_id");
            String roleId = roleIdObj != null ? roleIdObj.toString() : null;

            if (roleId == null || roleId.isBlank()) {
                log.error("Role has invalid _id. Cannot cleanup permissions.");
                return;
            }

            log.debug("Found role: id={}, name={}", roleId, prominentRoleName);

            // Get existing permissions
            @SuppressWarnings("unchecked")
            Map<String, Object> permissions = (Map<String, Object>) roleData.get("permissions");

            if (permissions == null || permissions.isEmpty()) {
                log.warn("Role has no permissions to remove.");
                return;
            }

            // IMPORTANT: Only remove the module we added during CREATE
            // Do NOT remove other modules' permissions
            String moduleCodeStr = String.valueOf(moduleCode);

            if (permissions.containsKey(moduleCodeStr)) {
                permissions.remove(moduleCodeStr);
                log.debug("Removed moduleCode {} from permissions map", moduleCode);

                // Update role document
                Query updateQuery = new Query(Criteria.where("_id").is(roleId));
                org.springframework.data.mongodb.core.query.Update update =
                    new org.springframework.data.mongodb.core.query.Update()
                        .set("permissions", permissions)
                        .set("updatedAt", LocalDateTime.now());

                mongoTemplate.updateFirst(updateQuery, update, "roles");

                log.info("removeModuleFromProminentRole SUCCESS: Module {} removed from role '{}'. " +
                        "Remaining modules: {}", moduleCode, prominentRoleName, permissions.keySet());
            } else {
                log.warn("Module {} not found in role permissions. Nothing to remove.", moduleCode);
            }

        } catch (Exception e) {
            log.error("removeModuleFromProminentRole ERROR (non-blocking): {}", e.getMessage(), e);
            // Non-blocking: continue with deletion even if role cleanup fails
        }
    }

    /**
     * Soft-delete the module created during FormManager creation
     * Sets deletedAt timestamp to mark module as inactive
     *
     * @param moduleCode The module code to soft-delete
     */
    private void softDeleteModule(Integer moduleCode) {
        log.info("softDeleteModule STARTED: moduleCode={}", moduleCode);

        try {
            // Query for module by code
            Query moduleQuery = new Query(
                Criteria.where("code").is(moduleCode).and("deletedAt").is(null)
            );
            Map<String, Object> moduleData = mongoTemplate.findOne(moduleQuery, Map.class, "modules");

            if (moduleData == null) {
                log.warn("Module with code {} NOT FOUND. Cannot soft-delete.", moduleCode);
                return;
            }

            Object moduleIdObj = moduleData.get("_id");
            String moduleId = moduleIdObj != null ? moduleIdObj.toString() : null;

            if (moduleId == null || moduleId.isBlank()) {
                log.error("Module has invalid _id. Cannot soft-delete.");
                return;
            }

            log.debug("Found module: id={}, code={}", moduleId, moduleCode);

            // Soft-delete module (set deletedAt timestamp)
            Query deleteQuery = new Query(Criteria.where("_id").is(moduleId));
            org.springframework.data.mongodb.core.query.Update update =
                new org.springframework.data.mongodb.core.query.Update()
                    .set("deletedAt", LocalDateTime.now())
                    .set("isEnabled", false)
                    .set("updatedAt", LocalDateTime.now());

            mongoTemplate.updateFirst(deleteQuery, update, "modules");

            log.info("softDeleteModule SUCCESS: Module code {} soft-deleted (id={})", moduleCode, moduleId);

        } catch (Exception e) {
            log.error("softDeleteModule ERROR (non-blocking): {}", e.getMessage(), e);
            // Non-blocking: continue with deletion even if module soft-delete fails
        }
    }
}

