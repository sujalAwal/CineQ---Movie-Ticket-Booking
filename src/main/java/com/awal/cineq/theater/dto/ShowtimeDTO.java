package com.awal.cineq.theater.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import jakarta.validation.constraints.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ShowtimeDTO {
    private Long id;
    
    @NotNull(message = "Movie ID is required")
    private Long movieId;
    
    @NotNull(message = "Theater ID is required")
    private Long theaterId;
    
    @NotNull(message = "Show date and time is required")
    @Future(message = "Show date and time must be in the future")
    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd'T'HH:mm:ss")
    private LocalDateTime showDateTime;
    
    @NotNull(message = "Price is required")
    @DecimalMin(value = "0.0", inclusive = false, message = "Price must be positive")
    @Digits(integer = 8, fraction = 2, message = "Price format is invalid")
    private BigDecimal price;
    
    private Integer availableSeats;
    
    @NotNull(message = "Total seats is required")
    @Positive(message = "Total seats must be positive")
    private Integer totalSeats;
    
    private Boolean isActive;
    
    // Additional fields for response
    private String movieTitle;
    private String theaterName;
    private String theaterCity;
}