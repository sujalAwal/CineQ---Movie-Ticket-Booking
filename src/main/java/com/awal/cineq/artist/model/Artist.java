package com.awal.cineq.artist.model;

import lombok.*;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.Id;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;
import org.springframework.data.mongodb.core.mapping.Field;

import java.time.LocalDateTime;

/**
 * MongoDB Document for Artist
 * Uses soft-delete pattern: deletedAt = null means active, not null means deleted
 */
@Document(collection = "artists")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Artist {

    @Id
    private String id;  // MongoDB ObjectId stored as String

    @Field("name")
    @Indexed
    private String name;

    @Field("bio")
    private String bio;

    @Field("profile_picture")
    private String profilePicture;

    @Field("is_active")
    private Boolean isActive = true;

    @Field("order")
    private Integer order;

    @Field("industry")
    private String industry;

    @Field("created_at")
    @CreatedDate
    private LocalDateTime createdAt;

    @Field("updated_at")
    @LastModifiedDate
    private LocalDateTime updatedAt;

    @Field("deleted_at")
    private LocalDateTime deletedAt;  // Soft-delete marker: null = active
}

