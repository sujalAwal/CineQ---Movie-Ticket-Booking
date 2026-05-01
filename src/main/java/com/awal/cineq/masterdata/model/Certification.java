package com.awal.cineq.masterdata.model;

import lombok.*;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.Id;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;
import org.springframework.data.mongodb.core.mapping.Field;

import java.time.LocalDateTime;

/**
 * MongoDB Document for Certification (Rating)
 * Defines movie ratings/certifications (U, UA, A, R, etc.)
 * Uses soft-delete pattern: deletedAt = null means active
 */
@Document(collection = "certifications")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Certification {

    @Id
    private String id;  // MongoDB ObjectId stored as String

    @Field("name")
    @Indexed
    private String name;  // e.g., "Unrated", "Universal with Adult Supervision"

    @Field("code")
    @Indexed
    private String code;  // e.g., "U", "UA", "A", "R"

    @Field("description")
    private String description;

    @Field("isActive")
    private Boolean isActive = true;

    @Field("createdAt")
    @CreatedDate
    private LocalDateTime createdAt;

    @Field("updatedAt")
    @LastModifiedDate
    private LocalDateTime updatedAt;

    @Field("deletedAt")
    private LocalDateTime deletedAt;  // Soft delete: null means active
}
