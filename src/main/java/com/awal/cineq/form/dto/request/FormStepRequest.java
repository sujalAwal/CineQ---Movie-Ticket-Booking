package com.awal.cineq.form.dto.request;

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Map;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class FormStepRequest {

    @NotBlank(message = "Step title is required")
    @JsonProperty("stepTitle")
    private String stepTitle;

    @NotBlank(message = "Step slug is required")
    @JsonProperty("stepSlug")
    private String stepSlug;

    @NotNull(message = "Step order is required")
    @JsonProperty("stepOrder")
    private Integer stepOrder;

    @JsonProperty("validationRule")
    private Map<String, Object> validationRules;

    @JsonProperty("formSchema")
    private Map<String, Object> formSchema;

    @JsonProperty("uiSchema")
    private Map<String, Object> uiSchema;

    @JsonProperty("metadata")
    private Map<String, Object> metadata;

    @JsonProperty("workflowRule")
    private Map<String, Object> workflowRules;

    @JsonProperty("isActive")
    private Boolean isActive = true;
}

