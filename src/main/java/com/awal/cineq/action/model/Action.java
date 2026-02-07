package com.awal.cineq.action.model;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.Id;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.mongodb.core.index.CompoundIndex;
import org.springframework.data.mongodb.core.index.CompoundIndexes;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;
import org.springframework.data.mongodb.core.mapping.Field;

import java.time.LocalDateTime;

/**
 * Action Entity - Represents system actions (VIEW, ADD, EDIT, DELETE, etc.)
 * Used for generating permissions when modules are created
 * Uses soft-delete pattern: deletedAt = null means active, not null means deleted
 */
@Document(collection = "actions")
@CompoundIndexes({
    @CompoundIndex(name = "code_active_idx", def = "{'code': 1, 'deletedAt': 1}", unique = true)
})
@Data
@NoArgsConstructor
@AllArgsConstructor
public class Action {

    @Id
    private String id;

    /**
     * Immutable code field - UPPERCASE with underscores (e.g., "VIEW", "ADD", "EDIT")
     * Used in permission code generation (MODULE_CODE + "_" + ACTION_CODE)
     * Once set, this value should NEVER be changed
     * Note: Uniqueness enforced by compound index (code, deletedAt)
     */
    @Field("code")
    private String code;

    /**
     * Display name - Mutable, user-friendly label (e.g., "View", "Add", "Edit")
     * Can be changed without affecting system logic
     */
    @Field("name")
    private String name;

    /**
     * Human-readable description of what this action allows
     */
    @Field("description")
    private String description;

    /**
     * Enable/disable flag - Disabled actions won't generate permissions for new modules
     */
    @Field("is_enabled")
    private Boolean isEnabled = true;

    /**
     * Soft delete marker - null = active, timestamp = deleted
     */
    @Field("deleted_at")
    private LocalDateTime deletedAt;

    /**
     * Audit timestamp - Auto-populated by Spring Data MongoDB
     */
    @CreatedDate
    @Field("created_at")
    private LocalDateTime createdAt;

    /**
     * Last update timestamp - Auto-populated by Spring Data MongoDB
     */
    @LastModifiedDate
    @Field("updated_at")
    private LocalDateTime updatedAt;
}
