package com.awal.cineq.form.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class FormManagerResponse {

    private UUID id;
    private String title;
    private String slug;
    private String description;

    @JsonProperty("model_name")
    private String modelName;

    @JsonProperty("is_active")
    private Boolean isActive;

    @JsonProperty("module_code")
    private Integer moduleCode;

    @JsonProperty("form_steps")
    private List<FormStepResponse> formSteps;
}

