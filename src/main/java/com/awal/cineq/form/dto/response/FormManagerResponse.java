package com.awal.cineq.form.dto.response;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;
import java.util.UUID;

/**
 * Form Manager Response DTO
 * Used for returning form manager data
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class FormManagerResponse {

    private UUID id;

    private String title;

    private String slug;

    private String description;

    private String modelName;

    private Boolean isActive;

    private List<FormStepResponse> formSteps;
}

