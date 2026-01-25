package com.awal.cineq.user.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

/**
 * Profile response DTO containing user info, role, permissions, and accessible modules.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ProfileResponse {

    // ==================== USER INFO ====================

    private String id;
    private String name;
    private String email;
    private String phoneNumber;
    private String roleName;
    private Boolean isActive;

    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd'T'HH:mm:ss")
    private LocalDateTime createdAt;

    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd'T'HH:mm:ss")
    private LocalDateTime updatedAt;

    // ==================== ROLE INFO ====================

    /**
     * Full role object from roles collection
     */
    private RoleInfo role;

    // ==================== MODULES ====================

    /**
     * List of modules the user has access to (from role_has_modules)
     */
    private List<ModuleInfo> modules;

    // ==================== NESTED DTOs ====================

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class RoleInfo {
        private String id;
        private String name;
        private String slug;
        private Map<String, Object> permissions;
        private Boolean isActive;

        @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd'T'HH:mm:ss")
        private LocalDateTime createdAt;

        @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd'T'HH:mm:ss")
        private LocalDateTime updatedAt;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ModuleInfo {
        private String id;
        private Integer code;
        private String name;
        private String displayName;
        private String api;
        private String description;
        private Boolean isEnabled;

        /**
         * Permission IDs for this module (from role_has_modules.permissionIds)
         */
        private List<Object> permissionIds;
    }
}
