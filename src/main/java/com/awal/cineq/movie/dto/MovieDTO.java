package com.awal.cineq.movie.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import jakarta.validation.constraints.*;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

import com.awal.cineq.genre.dto.GenreDTO;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class MovieDTO {
    private Long id;
    
    @NotBlank(message = "Movie title is required")
    @Size(max = 200, message = "Title must not exceed 200 characters")
    private String title;
    
    @Size(max = 1000, message = "Description must not exceed 1000 characters")
    private String description;
    
    @NotNull(message = "Duration is required")
    @Positive(message = "Duration must be positive")
    private Integer durationMinutes;
    
    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd")
    private LocalDate releaseDate;
    
    @Size(max = 10, message = "Rating must not exceed 10 characters")
    private String rating;
    
    @Size(max = 50, message = "Language must not exceed 50 characters")
    private String language;
    
    @Size(max = 500, message = "Poster URL must not exceed 500 characters")
    private String posterUrl;
    
    @Size(max = 500, message = "Trailer URL must not exceed 500 characters")
    private String trailerUrl;
    
    @Size(max = 100, message = "Director name must not exceed 100 characters")
    private String director;
    
    @Size(max = 1000, message = "Cast members must not exceed 1000 characters")
    private String castMembers;
    
    @DecimalMin(value = "0.0", inclusive = false, message = "Price must be positive")
    @Digits(integer = 8, fraction = 2, message = "Price format is invalid")
    private BigDecimal price;
    
    private Boolean isActive;
    
    private List<GenreDTO> genres;
}