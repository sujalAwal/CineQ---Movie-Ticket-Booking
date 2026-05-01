package com.awal.cineq.form.interceptor;

import lombok.Builder;
import lombok.Data;

import java.util.List;
import java.util.Map;

/**
 * Context object passed to interceptor handlers.
 * Contains all information needed for handler execution.
 */
@Data
@Builder
public class InterceptorContext {

    /**
     * The submitted form data (field names → values)
     */
    private Map<String, Object> formData;

    /**
     * The action being performed: CREATE, UPDATE, DELETE
     */
    private String action;

    /**
     * Target MongoDB collection name (e.g., "roles", "banners")
     */
    private String targetCollection;

    /**
     * Current user's roles (e.g., ["ADMIN", "MANAGER"])
     */
    private List<String> userRoles;

    /**
     * Current user identifier (username or user ID)
     */
    private String currentUser;

    /**
     * Complete workflow rules from FormStep
     */
    private Map<String, Object> workflowRules;

    /**
     * Handler-specific arguments from JSON config
     * Example: {"requiredPermission": "ROLE_CREATE", "cacheNames": ["roles"]}
     */
    private Map<String, Object> args;

    /**
     * Result holder for action result.
     * Available in after/afterReturning/afterThrowing handlers.
     * For afterThrowing, contains the exception.
     */
    private InterceptorResult result;

    /**
     * Form Manager ID
     */
    private String formManagerId;

    /**
     * Form Step ID
     */
    private String formStepId;

    /**
     * Document ID (for UPDATE/DELETE actions)
     */
    private String documentId;

    /**
     * Result holder class
     */
    @Data
    @Builder
    public static class InterceptorResult {
        /**
         * Whether the action succeeded
         */
        private boolean success;

        /**
         * Result data from the action (e.g., created document ID)
         */
        private Map<String, Object> data;

        /**
         * Exception if action failed (for afterThrowing)
         */
        private Throwable exception;

        /**
         * Message from the action
         */
        private String message;
    }
}
