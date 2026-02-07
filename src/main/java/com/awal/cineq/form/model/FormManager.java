package com.awal.cineq.form.model;

import com.fasterxml.jackson.annotation.JsonProperty;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.Id;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.mongodb.core.index.CompoundIndex;
import org.springframework.data.mongodb.core.index.CompoundIndexes;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.DBRef;
import org.springframework.data.mongodb.core.mapping.Document;
import org.springframework.data.mongodb.core.mapping.Field;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * MongoDB Document for FormManager
 * Represents a multi-step form workflow definition
 * Soft-delete pattern: deletedAt = null means active
 *
 * Index Strategy:
 * - Compound index on (slug, deletedAt) allows soft-deleted slugs to be reused
 * - Only active records (deletedAt=null) are unique
 */
@Document(collection = "form_managers")
@CompoundIndexes({
    @CompoundIndex(name = "slug_active_idx", def = "{'slug': 1, 'deletedAt': 1}", unique = true)
})
@Data
@NoArgsConstructor
@AllArgsConstructor
public class FormManager {


    @Id
    private String id;  // MongoDB ObjectId stored as String

    private String slug;  // "movie-registration", "ticket-booking"
    // Note: Uniqueness enforced by compound index (slug, deletedAt)

    private String title;

    private String description;

    private String modelName;

    @JsonProperty("is_enabled")
    private Boolean isActive = true;

    /**
     * Module code for permission and role configuration
     * References Module.code (auto-increment integer starting from 1)
     * Nullable: null if form is not associated with any module
     * Unique: At most ONE module code per FormManager (0..1 relationship)
     * Integer type for direct comparison with Module.code
     */
    @Indexed(unique = true, sparse = true)
    @Field("module_code")
    @JsonProperty("module_code")
    private Integer moduleCode;

    // Reference to FormSteps (use DBRef only if steps are stored separately)
    // For better performance, consider embedding FormStep data directly
    @DBRef
    private List<FormStep> formSteps = new ArrayList<>();

    @CreatedDate
    private LocalDateTime createdAt;

    @LastModifiedDate
    private LocalDateTime updatedAt;

    private LocalDateTime deletedAt;  // Soft-delete marker: null = active
}
