package com.awal.cineq.module.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Module Request DTO - Used for creating and updating modules
 * Validation enforced at controller layer via @Valid annotation
 * Note: Code is auto-generated as auto-increment integer, not provided in request
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class ModuleRequestDTO {


    /**
     * Display name - Can be changed
     * Example: "Movies", "Content Manager"
     */
    @NotBlank(message = "Name is required")
    @Size(max = 100, message = "Name must not exceed 100 characters")
    private String name;

    /**
     * Extended display name for UI
     * Example: "Movies Management", "Content Manager Dashboard"
     */
    @Size(max = 200, message = "Display name must not exceed 200 characters")
    private String displayName;

    /**
     * API endpoint path for frontend integration
     * Example: "/api/movies", "/api/genres"
     */
    @Size(max = 200, message = "API path must not exceed 200 characters")
    private String api;

    /**
     * Human-readable description
     */
    @Size(max = 500, message = "Description must not exceed 500 characters")
    private String description;

    /**
     * Icon identifier - Nullable, used for UI representation
     * Example: "mdi-movie", "fas fa-users", "bi-film"
     */
    @Size(max = 100, message = "Icon must not exceed 100 characters")
    private String icon;

    /**
     * Enable/disable flag
     */
    @JsonProperty("is_enabled")
    private Boolean isEnabled = true;

    /**
     * Parent module ID - null for top-level modules
     * Allows hierarchical organization of modules
     */
    private String parentId;
}
