package com.awal.cineq.artist.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.*;
import java.time.LocalDateTime;

/**
 * Artist DTO for MongoDB
 * Uses String ID (MongoDB ObjectId) instead of UUID
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ArtistDTO {
    private String id;  // MongoDB ObjectId as String
    private String name;
    private String bio;
    private String profilePicture;
    private Boolean isActive;
    private Integer order;
    private String industry;
    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd'T'HH:mm:ss")
    private LocalDateTime createdAt;
}

