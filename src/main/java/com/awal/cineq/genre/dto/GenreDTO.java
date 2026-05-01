package com.awal.cineq.genre.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * DTO for Genre entity
 * Used for API responses and requests
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class GenreDTO {
    private String id;  // MongoDB ObjectId stored as String

    @NotBlank(message = "Genre name is required")
    @Size(max = 100, message = "Genre name must not exceed 100 characters")
    private String name;
    
    @Size(max = 500, message = "Description must not exceed 500 characters")
    private String description;

    @JsonProperty("is_active")
    private boolean isActive;
}