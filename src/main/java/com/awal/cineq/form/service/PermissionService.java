package com.awal.cineq.form.service;

import com.awal.cineq.exception.BusinessException;
import com.awal.cineq.config.ApplicationProperties;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;

@Slf4j
@Service
@RequiredArgsConstructor
public class PermissionService {

    private final ApplicationProperties applicationProperties;

    /**
     * Check if user has permission based on workflow_rules
     * IMPORTANT: Users with the prominent role (configured via app.security.prominent-role)
     * bypass all permission checks and are treated as superusers.
     *
     * @param workflowRules The workflow rules containing permissions
     * @param action The action to check (submit, view, approve, export, etc.)
     * @param userRoles The roles of the current user
     */
    public void checkPermission(Map<String, Object> workflowRules, String action, List<String> userRoles) {
        log.debug("checkPermission STARTED: action={}, userRoles={}", action, userRoles);

        // STEP 1: Check if user has the prominent role (superuser bypass)
        String prominentRole = applicationProperties.getSecurity().getProminentRole();
        if (userRoles != null && userRoles.contains(prominentRole)) {
            log.info("checkPermission: User has prominent role '{}', bypassing permission check for action '{}'",
                    prominentRole, action);
            return;  // Skip permission check entirely
        }

        // STEP 2: If no prominent role, check standard permissions
        if (workflowRules == null || !workflowRules.containsKey("permissions")) {
            // No permissions defined, allow by default
            log.debug("checkPermission: No workflow rules defined, allowing action '{}'", action);
            return;
        }

        @SuppressWarnings("unchecked")
        Map<String, Object> permissions = (Map<String, Object>) workflowRules.get("permissions");

        if (!permissions.containsKey(action)) {
            // Action not defined in permissions, allow by default
            log.debug("checkPermission: Action '{}' not defined in permissions, allowing", action);
            return;
        }

        @SuppressWarnings("unchecked")
        List<String> allowedRoles = (List<String>) permissions.get(action);

        if (allowedRoles == null || allowedRoles.isEmpty()) {
            // No roles specified for this action, allow by default
            log.debug("checkPermission: No roles specified for action '{}', allowing", action);
            return;
        }

        // STEP 3: Check if user has any of the allowed roles
        boolean hasPermission = userRoles != null && userRoles.stream()
                .anyMatch(allowedRoles::contains);

        if (!hasPermission) {
            String errorMsg = "Access denied: User does not have permission to " + action +
                             ". Required roles: " + allowedRoles + ", User roles: " + userRoles;
            log.warn("checkPermission: {}", errorMsg);
            throw new BusinessException(errorMsg, HttpStatus.FORBIDDEN);
        }

        log.debug("checkPermission: User has required role for action '{}', permission granted", action);

        // Check conditional permissions
        if (permissions.containsKey("conditions")) {
            evaluateConditions(permissions, userRoles);
        }
    }

    /**
     * Extract user roles from workflow_rules permissions for a specific action
     */
    public List<String> getAllowedRoles(Map<String, Object> workflowRules, String action) {
        if (workflowRules == null || !workflowRules.containsKey("permissions")) {
            return List.of(); // Empty list means all allowed
        }

        @SuppressWarnings("unchecked")
        Map<String, Object> permissions = (Map<String, Object>) workflowRules.get("permissions");

        if (!permissions.containsKey(action)) {
            return List.of(); // Empty list means all allowed
        }

        @SuppressWarnings("unchecked")
        List<String> allowedRoles = (List<String>) permissions.get(action);

        return allowedRoles != null ? allowedRoles : List.of();
    }

    /**
     * Evaluate conditional permissions
     */
    private void evaluateConditions(Map<String, Object> permissions, List<String> userRoles) {
        if (!permissions.containsKey("conditions")) {
            return;
        }

        @SuppressWarnings("unchecked")
        List<Map<String, Object>> conditions = (List<Map<String, Object>>) permissions.get("conditions");

        for (Map<String, Object> condition : conditions) {
            // This is a placeholder for complex condition evaluation
            // In a real implementation, you would evaluate the "if" condition
            // and check if "then.require_roles" are present in userRoles
            
            if (condition.containsKey("then")) {
                @SuppressWarnings("unchecked")
                Map<String, Object> then = (Map<String, Object>) condition.get("then");
                
                if (then.containsKey("require_roles")) {
                    @SuppressWarnings("unchecked")
                    List<String> requiredRoles = (List<String>) then.get("require_roles");
                    
                    boolean hasRequiredRole = userRoles.stream()
                            .anyMatch(requiredRoles::contains);
                    
                    if (!hasRequiredRole) {
                        log.warn("Conditional permission check failed for roles: {}", requiredRoles);
                        // Note: In production, you might want to evaluate the "if" condition first
                        // before failing the permission check
                    }
                }
            }
        }
    }

    /**
     * Check if workflow_rules contains any permissions
     */
    public boolean hasPermissionsDefined(Map<String, Object> workflowRules) {
        return workflowRules != null && 
               workflowRules.containsKey("permissions") && 
               workflowRules.get("permissions") instanceof Map;
    }
}
