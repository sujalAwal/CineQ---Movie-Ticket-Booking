package com.awal.cineq.security.service;

import com.awal.cineq.config.ApplicationProperties;
import com.awal.cineq.exception.BusinessException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * RBACPermissionService
 * 
 * Role-Based Access Control permission checking service
 * Verifies if a user with given roles has permission to perform an action on a module
 * 
 * WHY: Decouples permission logic from business logic
 * Used by: UniversalFormServiceImpl for checking form submission permissions
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class RBACPermissionService {

    private final ApplicationProperties applicationProperties;

    /**
     * Check if user roles have permission to perform actions on a module
     * 
     * @param userRoles The roles assigned to the user (e.g., ["USER", "MANAGER"])
     * @param moduleCode The module code (from FormManager or RoleHasModule)
     * @param actionCodes The action codes being requested (1=CREATE, 2=READ, 3=UPDATE, 4=DELETE)
     * @param requireAll Whether all action codes must be allowed (true) or just one (false)
     * @throws BusinessException if permission is denied
     */
    public void checkPermission(List<String> userRoles, Integer moduleCode, List<Integer> actionCodes, boolean requireAll) {
        log.debug("checkPermission: userRoles={}, moduleCode={}, actionCodes={}, requireAll={}",
                userRoles, moduleCode, actionCodes, requireAll);

        // Validate inputs
        if (userRoles == null || userRoles.isEmpty()) {
            throw new BusinessException("User has no roles assigned");
        }

        // 1. Check for prominent role (e.g., SUPER_ADMIN)
        String prominentRole = applicationProperties.getSecurity().getProminentRole();
        if (userRoles.contains(prominentRole)) {
            log.info("Permission granted: User has prominent role '{}'", prominentRole);
            return; // Bypass further checks
        }

        if (moduleCode == null) {
            throw new BusinessException("Module code is not set");
        }

        if (actionCodes == null || actionCodes.isEmpty()) {
            throw new BusinessException("No action codes specified");
        }

        // Log for debugging
        log.debug("Checking if user roles {} have permission for module {} and actions {}",
                userRoles, moduleCode, actionCodes);

        // TODO: In production, implement actual RBAC checking:
        // 1. Query RoleHasModule table with:
        //    - role IN userRoles
        //    - module = moduleCode
        //    - action IN actionCodes
        // 2. Verify permissions based on requireAll flag
        // 3. Throw BusinessException if denied
        
        // For now: Mock implementation that allows all (can be enhanced later)
        log.info("Permission granted for roles {} on module {}", userRoles, moduleCode);
    }
}
