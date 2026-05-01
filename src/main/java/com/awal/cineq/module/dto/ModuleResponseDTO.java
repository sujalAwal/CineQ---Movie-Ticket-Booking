package com.awal.cineq.module.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * Module Response DTO - Used for API responses
 * Includes permission count for convenience
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class ModuleResponseDTO {

    private String id;

    /**
     * Auto-increment code - Used for module identification and FormManager associations
     */
    private Integer code;

    /**
     * Display name
     */
    private String name;

    /**
     * Extended display name
     */
    private String displayName;

    /**
     * API endpoint path for frontend integration
     */
    private String api;

    /**
     * Description
     */
    private String description;

    /**
     * Icon identifier - Used for UI representation
     * Example: "mdi-movie", "fas fa-users", "bi-film"
     */
    private String icon;

    /**
     * Enable/disable status
     */
    @JsonProperty("is_enabled")
    private Boolean isEnabled;

    /**
     * Parent module ID - null for top-level modules
     * Allows hierarchical organization of modules
     */
    private String parentId;

    /**
     * Count of permissions generated for this module
     * Useful for displaying module information
     */
    private Long permissionCount;

    /**
     * Creation timestamp
     */
    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd'T'HH:mm:ss")
    private LocalDateTime createdAt;

    /**
     * Last update timestamp
     */
    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd'T'HH:mm:ss")
    private LocalDateTime updatedAt;
}
