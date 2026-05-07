package com.awal.cineq.auth.util;

import com.awal.cineq.config.ApplicationProperties;
import com.awal.cineq.exception.BusinessException;
import com.awal.cineq.user.model.User;
import com.awal.cineq.user.model.UserHasRole;
import com.awal.cineq.user.repository.UserRepository;
import com.awal.cineq.user.repository.UserHasRoleRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.stream.Collectors;

@Component
@RequiredArgsConstructor
@Slf4j
public class AuthHelper {

    private final UserRepository userRepository;
    private final UserHasRoleRepository userHasRoleRepository;
    private final ApplicationProperties applicationProperties;

    /**
     * Get current authenticated user
     * Similar to Laravel's Auth()->user()
     *
     * @return Current User from database
     * @throws BusinessException if user not found or not authenticated
     */
    public User user() {
        try {
            Authentication authentication = SecurityContextHolder.getContext().getAuthentication();

            if (authentication == null || !authentication.isAuthenticated()) {
                throw new BusinessException("User not authenticated");
            }

            String username = authentication.getName();
            log.debug("Getting user from database: {}", username);

            User user = userRepository.findByEmail(username)
                    .orElseThrow(() -> new BusinessException("Authenticated user not found in database"));

            log.debug("User found: id={}", user.getId());
            return user;

        } catch (BusinessException e) {
            throw e;
        } catch (Exception e) {
            log.error("Error getting current user", e);
            throw new BusinessException("Error getting current user", e);
        }
    }

    /**
     * Get current user's roles
     * Returns list of all roles assigned to the user
     *
     * @return List of user's role names (e.g., ["ADMIN", "MANAGER"])
     */
    public List<String> getRoles() {
        User user = user();
        List<UserHasRole> userRoles = userHasRoleRepository.findByUserIdActive(user.getId());

        if (userRoles.isEmpty()) {
            return List.of("USER");
        }

        return userRoles.stream()
                .map(UserHasRole::getRoleName)
                .collect(Collectors.toList());
    }

    /**
     * Get current user's primary role (first role if multiple)
     * For backward compatibility
     *
     * @return User's first role name or "USER" if none
     */
    public String role() {
        List<String> roles = getRoles();
        return roles.isEmpty() ? "USER" : roles.get(0);
    }

    /**
     * Check if current user has a specific role
     * Checks against all user roles, not just one
     *
     * @param roleName Role name to check
     * @return true if user has the role
     */
    public boolean hasRole(String roleName) {
        if (roleName == null) return false;
        List<String> userRoles = getRoles();
        return userRoles.stream().anyMatch(r -> r.equalsIgnoreCase(roleName));
    }

    /**
     * Check if current user is a prominent user (matches configured prominent role)
     * Reads from app.security.prominent-role property
     *
     * @return true if user has prominent role
     */
    public boolean isProminentRole() {
        List<String> userRoles = getRoles();
        if (userRoles.isEmpty()) return false;

        String prominentRoles = applicationProperties.getSecurity().getProminentRole();
        if (prominentRoles == null || prominentRoles.isBlank()) {
            return false;
        }

        String[] roleArray = prominentRoles.split(",");
        for (String prominentRole : roleArray) {
            for (String userRole : userRoles) {
                if (userRole.equalsIgnoreCase(prominentRole.trim())) {
                    return true;
                }
            }
        }

        return false;
    }

    /**
     * Get current user's ID
     *
     * @return User's ID
     */
    public String userId() {
        return user().getId();
    }

    /**
     * Get current user's email
     *
     * @return User's email
     */
    public String email() {
        return user().getEmail();
    }

    /**
     * Get current user's name
     *
     * @return User's name
     */
    public String name() {
        return user().getName();
    }

    /**
     * Check if user is authenticated
     *
     * @return true if user is authenticated
     */
    public boolean check() {
        try {
            Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
            return authentication != null && authentication.isAuthenticated();
        } catch (Exception e) {
            return false;
        }
    }
}
