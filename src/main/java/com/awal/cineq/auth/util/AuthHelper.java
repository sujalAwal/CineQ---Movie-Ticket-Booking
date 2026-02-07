package com.awal.cineq.auth.util;

import com.awal.cineq.config.ApplicationProperties;
import com.awal.cineq.exception.BusinessException;
import com.awal.cineq.user.model.User;
import com.awal.cineq.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;

/**
 * AuthHelper - Laravel-style authentication helper
 * Usage: authHelper.user() returns current User
 *        authHelper.user().getRole() returns user's role
 * Similar to Laravel's Auth()->user()
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class AuthHelper {

    private final UserRepository userRepository;
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

            log.debug("User found: id={}, role={}", user.getId(), user.getRole());
            return user;

        } catch (BusinessException e) {
            throw e;
        } catch (Exception e) {
            log.error("Error getting current user", e);
            throw new BusinessException("Error getting current user", e);
        }
    }

    /**
     * Get current user's role
     * Similar to Laravel's Auth()->user()->role
     *
     * @return User's role name (e.g., "SUPERADMIN", "ADMIN", "USER")
     */
    public String role() {
        return user().getRole();
    }

    /**
     * Check if current user has a specific role
     * Similar to Laravel's Auth()->user()->hasRole('ADMIN')
     *
     * @param roleName Role name to check
     * @return true if user has the role
     */
    public boolean hasRole(String roleName) {
        if (roleName == null) return false;
        String userRole = role();
        return userRole != null && userRole.equalsIgnoreCase(roleName);
    }

    /**
     * Check if current user is a prominent user (SUPERADMIN, ADMIN)
     * Reads from app.security.prominent-role property
     * Similar to Laravel's Auth()->user()->isAdmin()
     *
     * @return true if user is prominent role
     */
    public boolean isProminentRole() {
        String userRole = role();
        if (userRole == null) return false;

        String prominentRoles = applicationProperties.getSecurity().getProminentRole();
        if (prominentRoles == null || prominentRoles.isBlank()) {
            return false;
        }

        // Split by comma to support multiple prominent roles (e.g., "SUPERADMIN,ADMIN")
        String[] roleArray = prominentRoles.split(",");
        for (String prominentRole : roleArray) {
            if (userRole.equalsIgnoreCase(prominentRole.trim())) {
                return true;
            }
        }

        return false;
    }

    /**
     * Get current user's ID
     * Similar to Laravel's Auth()->id()
     *
     * @return User's ID
     */
    public String userId() {
        return user().getId();
    }

    /**
     * Get current user's email
     * Similar to Laravel's Auth()->user()->email
     *
     * @return User's email
     */
    public String email() {
        return user().getEmail();
    }

    /**
     * Get current user's name
     * Similar to Laravel's Auth()->user()->name
     *
     * @return User's name
     */
    public String name() {
        return user().getName();
    }

    /**
     * Check if user is authenticated
     * Similar to Laravel's Auth()->check()
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
