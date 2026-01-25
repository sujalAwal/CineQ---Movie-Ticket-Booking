package com.awal.cineq.rolehasmodule.model;

import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.Id;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.mongodb.core.index.CompoundIndex;
import org.springframework.data.mongodb.core.index.CompoundIndexes;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * MongoDB Document for RoleHasModule
 * Represents the relationship between Role and Module with permissions
 * Uses soft-delete pattern: deletedAt = null means active, not null means deleted
 */
@Document(collection = "role_has_modules")
@CompoundIndexes({
    @CompoundIndex(name = "role_module_active_idx", def = "{'roleId': 1, 'moduleId': 1, 'deletedAt': 1}", unique = true)
})
@Data
@NoArgsConstructor
@AllArgsConstructor
public class RoleHasModule {

    @Id
    private String id;  // MongoDB ObjectId stored as String

    @Indexed
    private String roleId;  // Reference to Role

    @Indexed
    private String moduleId;  // Reference to Module

    private String role;  // Denormalized role name for convenience

    private String module;  // Denormalized module name for convenience

    private Boolean isActive = true;  // Whether this role-module permission is active

    @CreatedDate
    private LocalDateTime createdAt;

    @LastModifiedDate
    private LocalDateTime updatedAt;

    private LocalDateTime deletedAt;  // Soft-delete marker: null = active
}
