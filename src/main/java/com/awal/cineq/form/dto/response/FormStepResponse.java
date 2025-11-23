package com.awal.cineq.form.dto.response;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Map;
import java.util.UUID;

/**
 * Form Step Response DTO
 * Used for returning form step data
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class FormStepResponse {

    private UUID id;

    @JsonProperty("stepTitle")
    private String stepTitle;

    @JsonProperty("stepSlug")
    private String stepSlug;

    @JsonProperty("stepOrder")
    private Integer stepOrder;

    @JsonProperty("validationRules")
    private Map<String, Object> validationRules;

    @JsonProperty("formSchema")
    private Map<String, Object> formSchema;

    @JsonProperty("uiSchema")
    private Map<String, Object> uiSchema;

    private Map<String, Object> metadata;

    @JsonProperty("workflowRules")
    private Map<String, Object> workflowRules;

    @JsonProperty("isActive")
    private Boolean isActive;
}

