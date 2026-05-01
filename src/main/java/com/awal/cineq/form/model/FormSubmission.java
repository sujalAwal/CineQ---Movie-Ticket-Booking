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
 * MongoDB Document for FormSubmission
 * Represents user submissions for multi-step forms
 */
@Document(collection = "form_submissions")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class FormSubmission {

    @Id
    private String id;  // MongoDB ObjectId stored as String

    @Indexed
    private String formManagerId;  // Reference to parent FormManager

    @Indexed
    private String formStepId;  // Reference to FormStep (optional)

    // The actual form data submitted by user
    private Map<String, Object> formData;

    @Indexed
    private Boolean isActive = true;  // Default: true (active). Use for enabling/disabling submissions

    private String status;  // Status of submission (e.g., "SUBMITTED", "APPROVED", "REJECTED")

    @Indexed
    private String submittedBy;  // User who submitted the form

    // Additional metadata
    private Map<String, Object> metadata;

    @CreatedDate
    private LocalDateTime createdAt;

    @LastModifiedDate
    private LocalDateTime updatedAt;

    private LocalDateTime deletedAt;  // Soft-delete marker: null = active
}

