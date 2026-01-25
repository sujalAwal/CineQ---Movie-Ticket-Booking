package com.awal.cineq.genre.model;

import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.Id;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.mongodb.core.index.CompoundIndex;
import org.springframework.data.mongodb.core.index.CompoundIndexes;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * MongoDB Document for Genre
 * Uses soft-delete pattern: deletedAt = null means active, not null means deleted
 * Unique constraint only applies to active records via compound index
 */
@Document(collection = "genres")
@CompoundIndexes({
    @CompoundIndex(name = "name_active_idx", def = "{'name': 1, 'deletedAt': 1}", unique = true)
})
@Data
@NoArgsConstructor
@AllArgsConstructor
public class Genre {

    @Id
    private String id;  // MongoDB ObjectId stored as String

    private String name;  // Uniqueness enforced by compound index (name, deletedAt)

    private String description;

    private Boolean isActive = true;

    @CreatedDate
    private LocalDateTime createdAt;

    @LastModifiedDate
    private LocalDateTime updatedAt;

    private LocalDateTime deletedAt;  // Soft-delete marker: null = active
}
