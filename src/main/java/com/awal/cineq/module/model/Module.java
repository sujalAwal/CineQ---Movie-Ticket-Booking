package com.awal.cineq.module.model;

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
 * Module Entity - Represents system modules (MOVIES, GENRES, USERS, etc.)
 * When created, automatically generates permissions for all active actions
 * 
 * Example modules: MOVIES, GENRES, USERS, THEATERS, BOOKINGS
 *
 * Index Strategy:
 * - Compound index on (code, deletedAt) allows soft-deleted codes to be reused
 * - Only active records (deletedAt=null) have unique codes
 */
@Document(collection = "modules")
@CompoundIndexes({
    @CompoundIndex(name = "code_active_idx", def = "{'code': 1, 'deletedAt': 1}", unique = true)
})
@Data
@NoArgsConstructor
@AllArgsConstructor
public class Module {

    @Id
    private String id;

    /**
     * Immutable auto-increment code - Integer value starting from 1
     * Used for module identification, FormManager associations, and permission/role configuration
     * MongoDB doesn't support native auto-increment, so we use max(code) + 1 pattern
     * This value is immutable once set and must be unique (only for active records)
     * Note: Uniqueness enforced by compound index (code, deletedAt)
     */
    @Field("code")
    private Integer code;

    /**
     * Display name - Mutable, user-friendly label (e.g., "Movies", "Content Manager")
     * Can be changed without affecting system logic
     */
    @Field("name")
    private String name;

    /**
     * Extended display name for UI (e.g., "Movies Management", "Content Manager Dashboard")
     * More descriptive than 'name' field
     */
    @Field("display_name")
    private String displayName;

    /**
     * API endpoint path for this module (e.g., "/api/movies", "/api/genres")
     * Used by frontend for API integration
     */
    @Field("api")
    private String api;

    /**
     * Human-readable description of what this module manages
     */
    @Field("description")
    private String description;

    /**
     * Icon identifier - Nullable, used for UI representation (e.g., "mdi-movie", "fas fa-users")
     */
    @Field("icon")
    private String icon;

    /**
     * Enable/disable flag - Disabled modules won't be accessible
     */
    @Field("is_enabled")
    private Boolean isEnabled = true;

    /**
     * Parent module reference - null means top-level module
     * Allows hierarchical organization of modules
     */
    @Indexed
    @Field("parent_id")
    private String parentId;

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
