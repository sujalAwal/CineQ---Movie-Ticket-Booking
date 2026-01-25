package com.awal.cineq.theater.model;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.Id;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;
import org.springframework.data.mongodb.core.mapping.Field;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * MongoDB Document for Showtime
 * Represents a movie showing at a specific theater and time
 * References Movie and Theater by ID (not embedded for flexibility)
 */
@Document(collection = "showtimes")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class Showtime {
    
    @Id
    private String id;  // MongoDB ObjectId stored as String

    @Field("movie_id")
    @Indexed
    private String movieId;  // Reference to Movie document

    @Field("theater_id")
    @Indexed
    private String theaterId;  // Reference to Theater document

    @Field("show_date_time")
    @Indexed
    private LocalDateTime showDateTime;
    
    @Field("price")
    private BigDecimal price;
    
    @Field("available_seats")
    private Integer availableSeats;
    
    @Field("total_seats")
    private Integer totalSeats;
    
    @Field("is_active")
    private Boolean isActive = true;
    
    @Field("created_at")
    @CreatedDate
    private LocalDateTime createdAt;
    
    @Field("updated_at")
    @LastModifiedDate
    private LocalDateTime updatedAt;
    
    @Field("deleted_at")
    private LocalDateTime deletedAt;  // Soft-delete marker: null = active
}