package com.awal.cineq.form.dto.response;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * Form Manager Response DTO
 * Used for returning form manager data
 * IDs are MongoDB ObjectId stored as String
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class FormManagerResponse {

    private String id;  // MongoDB ObjectId as String

    private String title;

    private String slug;

    private String description;

    private String modelName;

    private Boolean isActive;

    /**
     * Module code for permission/role configuration
     * References Module.code (auto-increment integer: 1, 2, 3...)
     * null = not associated with any module
     * Unique: Only one FormManager per module code
     */
    private Integer moduleCode;

    private List<FormStepResponse> formSteps;
}

