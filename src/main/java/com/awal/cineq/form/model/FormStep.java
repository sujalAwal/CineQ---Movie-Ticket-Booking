package com.awal.cineq.form.model;

import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.Id;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.Map;

/**
 * MongoDB Document for FormStep
 * Represents a single step in a multi-step form workflow
 * Contains validation rules, form schema, and workflow permissions
 */
@Document(collection = "form_steps")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class FormStep {

    @Id
    private String id;  // MongoDB ObjectId stored as String

    @Indexed
    private String formManagerId;  // Reference to parent FormManager

    private String stepTitle;

    private String stepSlug;

    private Integer stepOrder = 0;

    // Validation rules: { "fieldName": ["@NotBlank", "@Size(max=100)"] }
    private Map<String, Object> validationRules;

    // Form schema for UI rendering
    private Map<String, Object> formSchema;

    // UI-specific schema
    private Map<String, Object> uiSchema;

    // Additional metadata
    private Map<String, Object> metadata;

    // Workflow rules: permissions, next step, previous step, etc.
    // { "permissions": { "submit": ["USER", "MANAGER"] }, "nextStep": "step2" }
    private Map<String, Object> workflowRules;

    private Boolean isActive = true;

    @CreatedDate
    private LocalDateTime createdAt;

    @LastModifiedDate
    private LocalDateTime updatedAt;

    private LocalDateTime deletedAt;  // Soft-delete marker: null = active
}

