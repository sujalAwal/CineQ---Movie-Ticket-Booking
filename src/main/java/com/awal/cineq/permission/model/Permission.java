package com.awal.cineq.permission.model;

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
 * Permission Entity - Auto-generated when modules are created
 * Represents a specific action that can be performed on a module
 * 
 * Example: MOVIES_VIEW, GENRES_ADD, USERS_EDIT
 *
 * Index Strategy:
 * - Compound index on (code, deletedAt) allows soft-deleted codes to be reused
 * - Only active records (deletedAt=null) have unique permission codes
 */
@Document(collection = "permissions")
@CompoundIndexes({
    @CompoundIndex(name = "code_active_idx", def = "{'code': 1, 'deletedAt': 1}", unique = true)
})
@Data
@NoArgsConstructor
@AllArgsConstructor
public class Permission {

    @Id
    private String id;

    /**
     * Immutable permission code - Format: MODULE_CODE + "_" + ACTION_CODE
     * Example: "MOVIES_VIEW", "GENRES_ADD", "USERS_EDIT"
     * Used in role permission assignments
     * Note: Uniqueness enforced by compound index (code, deletedAt)
     */
    @Field("code")
    private String code;

    /**
     * Reference to module (denormalized for performance)
     */
    @Indexed
    @Field("module_id")
    private String moduleId;

    /**
     * Denormalized module name (display purposes)
     */
    @Field("module_name")
    private String moduleName;

    /**
     * Denormalized module code - Integer auto-increment value
     * For filtering/querying and FormManager associations
     */
    @Field("module_code")
    private Integer moduleCode;

    /**
     * Reference to action (denormalized for performance)
     */
    @Indexed
    @Field("action_id")
    private String actionId;

    /**
     * Denormalized action name (display purposes)
     */
    @Field("action_name")
    private String actionName;

    /**
     * Denormalized action code (for filtering/querying)
     */
    @Field("action_code")
    private String actionCode;

    /**
     * Enable/disable flag - Disabled permissions won't be assignable to roles
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
