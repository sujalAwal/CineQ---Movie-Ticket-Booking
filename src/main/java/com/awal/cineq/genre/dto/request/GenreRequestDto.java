package com.awal.cineq.genre.dto.request;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class GenreRequestDto {
    @NotBlank(message = "Genre name is required")
    @Size(max = 100, message = "Genre name must not exceed 100 characters")
    private String name;

    @Size(max = 500, message = "Description must not exceed 500 characters")
    private String description;

    @JsonProperty("is_active")
    private boolean isActive;

    // backwards-compat: keep the old getter name that GenreServiceImpl calls
    public boolean is_active() { return isActive; }
}

