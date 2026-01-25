package com.awal.cineq.form.dto.request;

import com.awal.cineq.form.enums.FormAction;
import com.awal.cineq.form.validation.ConditionalValidation;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Map;

/**
 * Dynamic Form Request DTO
 * Used for submitting form data to the universal form system
 *
 * Validation Rules (enforced by @ConditionalValidation):
 * - CREATE: id is NOT required (creating new document)
 * - UPDATE: id IS required (must know which document to update)
 * - DELETE: id IS required (must know which document to delete)
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@ConditionalValidation
public class DynamicFormRequest {

    @NotBlank(message = "Step slug is required")
    private String stepSlug;

    @NotNull(message = "Action is required")
    private FormAction action;  // CREATE, READ, UPDATE, DELETE

    private String id;  // Required only for UPDATE/DELETE actions

    @NotNull(message = "Form data is required")
    private Map<String, Object> formData;

    private String submittedBy;

    private Map<String, Object> metadata;
}

