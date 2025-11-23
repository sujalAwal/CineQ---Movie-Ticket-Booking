package com.awal.cineq.form.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Map;

/**
 * Dynamic Form Request DTO
 * Used for submitting form data to the universal form system
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class DynamicFormRequest {

    @NotBlank(message = "Form slug is required")
    private String formSlug;

    @NotBlank(message = "Step slug is required")
    private String stepSlug;

    @NotNull(message = "Form data is required")
    private Map<String, Object> formData;

    private Map<String, Object> metadata;
}

