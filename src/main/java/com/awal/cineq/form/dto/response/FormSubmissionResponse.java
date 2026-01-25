package com.awal.cineq.form.dto.response;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Map;

/**
 * Form Submission Response DTO
 * Used for returning form submission data from the universal form system
 * IDs are MongoDB ObjectIds stored as String
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class FormSubmissionResponse {

    private String id;  // MongoDB ObjectId

    private String formManagerId;  // Reference to FormManager

    private String formStepId;  // Reference to FormStep

    private Map<String, Object> formData;

    private String status;

    private String submittedBy;

    private Boolean isActive;

    private Map<String, Object> metadata;
}

