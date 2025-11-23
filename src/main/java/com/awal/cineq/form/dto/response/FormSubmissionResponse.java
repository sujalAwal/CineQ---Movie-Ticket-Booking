package com.awal.cineq.form.dto.response;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Map;
import java.util.UUID;

/**
 * Form Submission Response DTO
 * Used for returning form submission data from the universal form system
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class FormSubmissionResponse {

    private UUID id;

    private String formSlug;

    private String stepSlug;

    private Map<String, Object> submittedData;

    private String status;

    private String submittedBy;

    private Map<String, Object> metadata;
}

