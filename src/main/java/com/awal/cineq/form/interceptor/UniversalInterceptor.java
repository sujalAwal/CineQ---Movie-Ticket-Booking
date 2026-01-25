package com.awal.cineq.form.interceptor;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.Cache;
import org.springframework.cache.CacheManager;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.data.mongodb.core.query.Update;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Universal Interceptor - Contains ALL handler methods for ALL modules.
 *
 * This is the single class where all interceptor handler methods are defined.
 * Add new handler methods here as needed for any module.
 *
 * Method naming convention:
 * - Method name = handler name in JSON config
 * - Must accept single parameter: InterceptorContext
 * - Can be any return type (return value is ignored)
 *
 * Example JSON usage:
 * {
 *   "handler": "updateModuleHasRole",
 *   "args": { "action": "CREATE" },
 *   "when": null
 * }
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class UniversalInterceptor {

    private final MongoTemplate mongoTemplate;
    private final CacheManager cacheManager;

    // ==================================================================================
    // ROLE MODULE HANDLERS
    // ==================================================================================

    /**
     * Handler: updateModuleHasRole
     * Updates module-role relationships when a role is created/updated.
     *
     * Args:
     * - action: "CREATE" or "UPDATE"
     * - entity: "Role" (optional, for logging)
     */
    public void updateModuleHasRole(InterceptorContext context) {
        Map<String, Object> args = context.getArgs() != null ? context.getArgs() : Map.of();
        Map<String, Object> formData = context.getFormData();
        InterceptorContext.InterceptorResult result = context.getResult();

        String action = (String) args.getOrDefault("action", context.getAction());
        String roleId = extractId(result, formData, context.getDocumentId());

        if (roleId == null) {
            log.warn("updateModuleHasRole: Could not extract role ID, skipping");
            return;
        }

        @SuppressWarnings("unchecked")
        List<String> permissions = (List<String>) formData.get("permissions");

        log.info("updateModuleHasRole STARTED: action={}, roleId={}, permissionCount={}",
                action, roleId, permissions != null ? permissions.size() : 0);

        if (permissions == null || permissions.isEmpty()) {
            log.info("updateModuleHasRole: No permissions to sync");
            return;
        }

        try {
            // For UPDATE, remove old associations first
            if ("UPDATE".equalsIgnoreCase(action)) {
                removeRoleFromModules(roleId);
            }

            // Add new associations
            addRoleToModules(roleId, permissions);

            log.info("updateModuleHasRole END: Successfully synced {} permissions for role {}",
                    permissions.size(), roleId);

        } catch (Exception e) {
            log.error("updateModuleHasRole ERROR: {}", e.getMessage(), e);
            // Don't throw - afterReturning handlers shouldn't break the response
        }
    }

    /**
     * Handler: syncPermissions
     * Syncs permissions to a target collection.
     *
     * Args:
     * - targetCollection: collection name to sync to
     */
    public void syncPermissions(InterceptorContext context) {
        Map<String, Object> args = context.getArgs() != null ? context.getArgs() : Map.of();
        String targetCollection = (String) args.get("targetCollection");

        if (targetCollection == null || targetCollection.isBlank()) {
            log.warn("syncPermissions: targetCollection not specified");
            return;
        }

        log.info("syncPermissions: targetCollection={}", targetCollection);
        // Add sync logic as needed
    }

    /**
     * Handler: assignModulesToRole
     * Assigns modules to a role based on permissions submitted in form.
     *
     * Request body example:
     * {
     *   "formData": {
     *     "name": "ADMIN",
     *     "slug": "admin",
     *     "permissions": {
     *       "1": [1,2,3,4],   // key=module code, value=permission IDs
     *       "2": [5,6,7]
     *     }
     *   }
     * }
     *
     * Flow:
     * 1. Get role name from formData.name → Find role ID from 'roles' collection
     * 2. Get permissions object → Keys are module codes
     * 3. For each module code → Find module ID from 'modules' collection
     * 4. Soft-delete all existing role_has_modules entries for this role
     * 5. Insert new entries for each module
     *
     * Args:
     * - entity: target collection (default: "role_has_modules")
     */
    public void assignModulesToRole(InterceptorContext context) {
        Map<String, Object> args = context.getArgs() != null ? context.getArgs() : Map.of();
        Map<String, Object> formData = context.getFormData();

        String entity = (String) args.getOrDefault("entity", "role_has_modules");

        log.info("assignModulesToRole STARTED: entity={}", entity);

        try {
            // Step 1: Get role name from formData and find role ID
            String roleName = (String) formData.get("name");
            if (roleName == null || roleName.isBlank()) {
                log.warn("assignModulesToRole: Role name not found in formData");
                return;
            }

            // Find role by name in 'roles' collection
            Query roleQuery = Query.query(
                Criteria.where("name").is(roleName)
                        .and("deletedAt").is(null)
            );
            Map<String, Object> role = mongoTemplate.findOne(roleQuery, Map.class, "roles");

            if (role == null) {
                log.warn("assignModulesToRole: Role not found with name: {}", roleName);
                return;
            }

            String roleId = role.get("_id").toString();
            log.debug("assignModulesToRole: Found role - name={}, id={}", roleName, roleId);

            // Step 2: Get permissions object (keys are module codes)
            Object permissionsObj = formData.get("permissions");
            if (permissionsObj == null) {
                log.warn("assignModulesToRole: No permissions found in formData");
                return;
            }

            @SuppressWarnings("unchecked")
            Map<String, Object> permissions = (Map<String, Object>) permissionsObj;

            if (permissions.isEmpty()) {
                log.info("assignModulesToRole: Empty permissions, only soft-deleting existing entries");
            }

            // Step 3: Soft-delete all existing role_has_modules entries for this role
            Query softDeleteQuery = Query.query(
                Criteria.where("roleId").is(roleId)
                        .and("deletedAt").is(null)
            );
            Update softDeleteUpdate = new Update()
                .set("deletedAt", LocalDateTime.now())
                .set("updatedAt", LocalDateTime.now());

            var deleteResult = mongoTemplate.updateMulti(softDeleteQuery, softDeleteUpdate, entity);
            log.info("assignModulesToRole: Soft-deleted {} existing entries for role {}",
                    deleteResult.getModifiedCount(), roleId);

            // Step 4: For each module code in permissions, find module and create entry
            int insertedCount = 0;

            for (String moduleCodeStr : permissions.keySet()) {
                try {
                    // Parse module code (can be String or Integer)
                    int moduleCode;
                    try {
                        moduleCode = Integer.parseInt(moduleCodeStr);
                    } catch (NumberFormatException e) {
                        log.warn("assignModulesToRole: Invalid module code: {}", moduleCodeStr);
                        continue;
                    }

                    // Find module by code in 'modules' collection
                    Query moduleQuery = Query.query(
                        Criteria.where("code").is(moduleCode)
                                .and("deletedAt").is(null)
                    );
                    Map<String, Object> module = mongoTemplate.findOne(moduleQuery, Map.class, "modules");

                    if (module == null) {
                        log.warn("assignModulesToRole: Module not found with code: {}", moduleCode);
                        continue;
                    }

                    String moduleId = module.get("_id").toString();
                    String moduleName = (String) module.get("name");

                    log.debug("assignModulesToRole: Found module - code={}, id={}, name={}",
                            moduleCode, moduleId, moduleName);

                    // Step 5: Create new role_has_modules entry
                    Map<String, Object> roleHasModule = new HashMap<>();
                    roleHasModule.put("roleId", roleId);
                    roleHasModule.put("moduleId", moduleId);
                    roleHasModule.put("role", roleName);
                    roleHasModule.put("module", moduleName);
                    roleHasModule.put("isActive", true);
                    roleHasModule.put("createdAt", LocalDateTime.now());
                    roleHasModule.put("updatedAt", LocalDateTime.now());
                    roleHasModule.put("deletedAt", null);

                    // Store the permission IDs for this module (optional, for reference)
                    Object permissionIds = permissions.get(moduleCodeStr);
                    if (permissionIds != null) {
                        roleHasModule.put("permissionIds", permissionIds);
                    }

                    mongoTemplate.insert(roleHasModule, entity);
                    insertedCount++;

                    log.debug("assignModulesToRole: Created entry - roleId={}, moduleId={}",
                            roleId, moduleId);

                } catch (Exception e) {
                    log.error("assignModulesToRole: Error processing module code {}: {}",
                            moduleCodeStr, e.getMessage());
                }
            }

            log.info("assignModulesToRole END: Inserted {} entries for role {} ({})",
                    insertedCount, roleName, roleId);

        } catch (Exception e) {
            log.error("assignModulesToRole ERROR: {}", e.getMessage(), e);
            // Don't throw - afterReturning handlers shouldn't break the response
        }
    }

    // ==================================================================================
    // BOOKING MODULE HANDLERS
    // ==================================================================================

    /**
     * Handler: sendBookingConfirmation
     * Sends confirmation after booking is created.
     *
     * Args:
     * - emailField: field name containing email (default: "email")
     */
    public void sendBookingConfirmation(InterceptorContext context) {
        Map<String, Object> args = context.getArgs() != null ? context.getArgs() : Map.of();
        Map<String, Object> formData = context.getFormData();

        String emailField = (String) args.getOrDefault("emailField", "email");
        String email = (String) formData.get(emailField);

        log.info("sendBookingConfirmation: email={}", email);
        // Add email sending logic here
    }

    /**
     * Handler: updateSeatAvailability
     * Updates seat availability after booking.
     *
     * Args:
     * - seatField: field containing seat IDs (default: "seatIds")
     * - showtimeField: field containing showtime ID (default: "showtimeId")
     */
    public void updateSeatAvailability(InterceptorContext context) {
        Map<String, Object> args = context.getArgs() != null ? context.getArgs() : Map.of();
        Map<String, Object> formData = context.getFormData();

        String seatField = (String) args.getOrDefault("seatField", "seatIds");
        String showtimeField = (String) args.getOrDefault("showtimeField", "showtimeId");

        @SuppressWarnings("unchecked")
        List<String> seatIds = (List<String>) formData.get(seatField);
        String showtimeId = (String) formData.get(showtimeField);

        log.info("updateSeatAvailability: showtimeId={}, seatCount={}",
                showtimeId, seatIds != null ? seatIds.size() : 0);

        if (seatIds == null || seatIds.isEmpty() || showtimeId == null) {
            log.warn("updateSeatAvailability: Missing required data");
            return;
        }

        try {
            Query query = Query.query(
                    Criteria.where("_id").is(showtimeId)
                            .and("seats._id").in(seatIds)
            );
            Update update = new Update().set("seats.$.status", "BOOKED");
            mongoTemplate.updateMulti(query, update, "showtimes");

            log.info("updateSeatAvailability: Updated {} seats for showtime {}",
                    seatIds.size(), showtimeId);

        } catch (Exception e) {
            log.error("updateSeatAvailability ERROR: {}", e.getMessage(), e);
        }
    }

    // ==================================================================================
    // COMMON HANDLERS
    // ==================================================================================

    /**
     * Handler: auditLog
     * Creates an audit log entry in MongoDB.
     *
     * Args:
     * - action: action name (default: from context)
     * - entity: entity name (default: from targetCollection)
     * - collection: audit collection name (default: "audit_logs")
     * - includeData: include form data in log (default: false)
     */
    public void auditLog(InterceptorContext context) {
        Map<String, Object> args = context.getArgs() != null ? context.getArgs() : Map.of();

        String action = (String) args.getOrDefault("action", context.getAction());
        String entity = (String) args.getOrDefault("entity", context.getTargetCollection());
        String collection = (String) args.getOrDefault("collection", "audit_logs");
        boolean includeData = Boolean.TRUE.equals(args.get("includeData"));

        Map<String, Object> auditEntry = new HashMap<>();
        auditEntry.put("action", action);
        auditEntry.put("entity", entity);
        auditEntry.put("userId", context.getCurrentUser());
        auditEntry.put("userRoles", context.getUserRoles());
        auditEntry.put("targetCollection", context.getTargetCollection());
        auditEntry.put("documentId", context.getDocumentId());
        auditEntry.put("formManagerId", context.getFormManagerId());
        auditEntry.put("formStepId", context.getFormStepId());
        auditEntry.put("timestamp", LocalDateTime.now());

        // Include result info
        if (context.getResult() != null) {
            auditEntry.put("success", context.getResult().isSuccess());
            auditEntry.put("message", context.getResult().getMessage());

            if (context.getResult().getException() != null) {
                auditEntry.put("errorType", context.getResult().getException().getClass().getSimpleName());
                auditEntry.put("errorMessage", context.getResult().getException().getMessage());
            }
        }

        // Include form data if requested
        if (includeData && context.getFormData() != null) {
            Map<String, Object> sanitizedData = new HashMap<>(context.getFormData());
            // Remove sensitive fields
            sanitizedData.remove("password");
            sanitizedData.remove("secret");
            sanitizedData.remove("token");
            auditEntry.put("formData", sanitizedData);
        }

        try {
            mongoTemplate.insert(auditEntry, collection);
            log.info("auditLog: action={}, entity={}, userId={}", action, entity, context.getCurrentUser());
        } catch (Exception e) {
            log.error("auditLog ERROR: {}", e.getMessage(), e);
        }
    }

    /**
     * Handler: invalidateCache
     * Invalidates specified cache entries.
     *
     * Args:
     * - cacheNames: list of cache names to clear
     * - keys: specific keys to evict (optional, clears all if not specified)
     */
    public void invalidateCache(InterceptorContext context) {
        Map<String, Object> args = context.getArgs() != null ? context.getArgs() : Map.of();

        @SuppressWarnings("unchecked")
        List<String> cacheNames = (List<String>) args.get("cacheNames");
        @SuppressWarnings("unchecked")
        List<String> keys = (List<String>) args.get("keys");

        if (cacheNames == null || cacheNames.isEmpty()) {
            log.warn("invalidateCache: No cache names specified");
            return;
        }

        for (String cacheName : cacheNames) {
            Cache cache = cacheManager.getCache(cacheName);
            if (cache == null) {
                log.debug("invalidateCache: Cache '{}' not found", cacheName);
                continue;
            }

            if (keys == null || keys.isEmpty()) {
                cache.clear();
                log.info("invalidateCache: Cleared all entries from cache '{}'", cacheName);
            } else {
                for (String key : keys) {
                    cache.evict(key);
                }
                log.info("invalidateCache: Evicted {} keys from cache '{}'", keys.size(), cacheName);
            }
        }
    }

    /**
     * Handler: logAction
     * Simple logging handler for debugging.
     *
     * Args:
     * - message: custom message (optional)
     * - level: log level - DEBUG, INFO, WARN, ERROR (default: INFO)
     * - includeFormData: log form data (default: false)
     * - includeContext: log full context (default: false)
     */
    public void logAction(InterceptorContext context) {
        Map<String, Object> args = context.getArgs() != null ? context.getArgs() : Map.of();

        String message = (String) args.get("message");
        String level = (String) args.getOrDefault("level", "INFO");
        boolean includeFormData = Boolean.TRUE.equals(args.get("includeFormData"));
        boolean includeContext = Boolean.TRUE.equals(args.get("includeContext"));

        StringBuilder logMsg = new StringBuilder();

        if (message != null) {
            logMsg.append(message).append(" | ");
        }

        logMsg.append("Action: ").append(context.getAction());
        logMsg.append(", Collection: ").append(context.getTargetCollection());
        logMsg.append(", User: ").append(context.getCurrentUser());

        if (context.getDocumentId() != null) {
            logMsg.append(", DocId: ").append(context.getDocumentId());
        }

        if (includeFormData && context.getFormData() != null) {
            logMsg.append(" | FormData: ").append(context.getFormData());
        }

        if (includeContext) {
            logMsg.append(" | Context: {formManagerId=").append(context.getFormManagerId())
                    .append(", formStepId=").append(context.getFormStepId())
                    .append(", userRoles=").append(context.getUserRoles())
                    .append("}");
        }

        String finalMessage = logMsg.toString();

        switch (level.toUpperCase()) {
            case "DEBUG" -> log.debug("[Interceptor] {}", finalMessage);
            case "WARN" -> log.warn("[Interceptor] {}", finalMessage);
            case "ERROR" -> log.error("[Interceptor] {}", finalMessage);
            default -> log.info("[Interceptor] {}", finalMessage);
        }
    }

    // ==================================================================================
    // PRIVATE HELPER METHODS
    // ==================================================================================

    /**
     * Extract document ID from result, formData, or context.
     */
    private String extractId(InterceptorContext.InterceptorResult result,
                             Map<String, Object> formData,
                             String contextDocumentId) {
        // Try context documentId first
        if (contextDocumentId != null && !contextDocumentId.isBlank()) {
            return contextDocumentId;
        }

        // Try result data
        if (result != null && result.getData() != null) {
            Object id = result.getData().get("id");
            if (id != null) return id.toString();

            id = result.getData().get("_id");
            if (id != null) return id.toString();
        }

        // Try form data
        if (formData != null) {
            Object id = formData.get("id");
            if (id != null) return id.toString();

            id = formData.get("_id");
            if (id != null) return id.toString();
        }

        return null;
    }

    /**
     * Remove role from all modules (for UPDATE action).
     */
    private void removeRoleFromModules(String roleId) {
        Query query = Query.query(Criteria.where("roles").is(roleId));
        Update update = new Update().pull("roles", roleId);
        var result = mongoTemplate.updateMulti(query, update, "modules");
        log.debug("Removed role {} from {} modules", roleId, result.getModifiedCount());
    }

    /**
     * Add role to modules based on permissions.
     */
    private void addRoleToModules(String roleId, List<String> permissionIds) {
        for (String permissionId : permissionIds) {
            Query query = Query.query(Criteria.where("permissions").is(permissionId));
            Update update = new Update().addToSet("roles", roleId);
            mongoTemplate.updateMulti(query, update, "modules");
        }
        log.debug("Added role {} to modules for {} permissions", roleId, permissionIds.size());
    }
}
