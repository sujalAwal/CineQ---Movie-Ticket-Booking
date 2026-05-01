package com.awal.cineq.media.model;

import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.Id;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.mongodb.core.mapping.Document;
import org.springframework.data.mongodb.core.mapping.Field;

import java.time.LocalDateTime;

/**
 * MongoDB Document for Media
 * Uses soft-delete pattern: deletedAt = null means active, not null means deleted
 */
@Document(collection = "medias")
@Data
@NoArgsConstructor
public class Media {

    @Id
    private String id;  // MongoDB ObjectId stored as String

    @Field("file_name")
    private String fileName;

    @Field("url")
    private String url;

    @Field("type")
    private MediaType type; // e.g., image, video

    @Field("is_active")
    private boolean isActive = true;

    @Field("parent_id")
    private String parentId; // e.g., movie or series ID (String for MongoDB reference)

    @Field("file_path")
    private String filePath;

    @Field("file_id")
    private String fileUuid;

    @Field("created_at")
    @CreatedDate
    private LocalDateTime createdAt;

    @Field("updated_at")
    @LastModifiedDate
    private LocalDateTime updatedAt;

    @Field("deleted_at")
    private LocalDateTime deletedAt;  // Soft-delete marker: null = active

}

