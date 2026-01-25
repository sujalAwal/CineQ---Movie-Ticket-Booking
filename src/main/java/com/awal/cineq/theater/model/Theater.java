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

import java.time.LocalDateTime;

/**
 * MongoDB Document for Theater
 * Represents a movie theater with seat configuration
 * Showtimes and Seats are stored in separate collections with theater_id reference
 */
@Document(collection = "theaters")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class Theater {
    
    @Id
    private String id;  // MongoDB ObjectId stored as String

    @Field("name")
    @Indexed
    private String name;
    
    @Field("address")
    private String address;
    
    @Field("city")
    @Indexed
    private String city;
    
    @Field("state")
    private String state;
    
    @Field("postal_code")
    private String postalCode;
    
    @Field("phone_number")
    private String phoneNumber;
    
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