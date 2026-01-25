package com.awal.cineq.theater.model;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.Id;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.mongodb.core.index.CompoundIndex;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;
import org.springframework.data.mongodb.core.mapping.Field;

import java.time.LocalDateTime;

/**
 * MongoDB Document for Seat
 * Represents a seat in a theater
 * Compound index on theater_id + seat_number for uniqueness
 */
@Document(collection = "seats")
@CompoundIndex(name = "theater_seat_idx", def = "{'theater_id': 1, 'seat_number': 1}", unique = true)
@Data
@NoArgsConstructor
@AllArgsConstructor
public class Seat {
    
    @Id
    private String id;  // MongoDB ObjectId stored as String

    @Field("theater_id")
    @Indexed
    private String theaterId;  // Reference to Theater document

    @Field("seat_number")
    private String seatNumber;
    
    @Field("row_number")
    private String rowNumber;
    
    @Field("seat_type")
    private String seatType; // REGULAR, VIP, PREMIUM
    
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