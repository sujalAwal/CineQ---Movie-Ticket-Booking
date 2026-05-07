package com.awal.cineq.movie.model;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.Id;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.mongodb.core.mapping.Document;
import org.springframework.data.mongodb.core.mapping.Field;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

/**
 * Movie document in MongoDB
 * Stores movie metadata with denormalized genre list
 * ID is MongoDB ObjectId stored as String
 */
@Document(collection = "movies")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class Movie {
    
    @Id
    private String id;
    
    @Field("title")
    private String title;
    
    @Field("description")
    private String description;
    
    @Field("duration_minutes")
    private Integer durationMinutes;
    
    @Field("release_date")
    private LocalDate releaseDate;
    
    @Field("rating")
    private String rating;
    
    @Field("language")
    private String language;
    
    @Field("poster_url")
    private String posterUrl;
    
    @Field("trailer_url")
    private String trailerUrl;
    
    @Field("director")
    private String director;
    
    @Field("cast_members")
    private String castMembers;
    
    @Field("price")
    private BigDecimal price;
    
    @Field("is_active")
    private Boolean isActive = true;
    
    @Field("status")
    private String status ; 


    @CreatedDate
    @Field("created_at")
    private LocalDateTime createdAt;
    
    @LastModifiedDate
    @Field("updated_at")
    private LocalDateTime updatedAt;
    
    @Field("deleted_at")
    private LocalDateTime deletedAt;
    
    @Field("genres")
    private List<String> genreIds;
}