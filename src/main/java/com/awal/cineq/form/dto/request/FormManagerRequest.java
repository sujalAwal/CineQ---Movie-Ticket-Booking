package com.awal.cineq.form.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * Form Manager Request DTO
 * Used for creating and updating form managers
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class FormManagerRequest {

    @NotBlank(message = "Title is required")
    private String title;

    @NotBlank(message = "Slug is required")
    private String slug;

    private String description;

    private String modelName;

    private Boolean isActive;

    /**
     * Module code for permission/role configuration
     * Optional: null if form is not associated with any module
     * References Module.code (auto-increment integer: 1, 2, 3...)
     * Unique: Only one FormManager per module code
     */
    private Integer moduleCode;

    @NotEmpty(message = "At least one form step is required")
    private List<FormStepRequest> formSteps;
}

