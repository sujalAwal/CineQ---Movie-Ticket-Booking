package com.awal.cineq.masterdata.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;
import org.springframework.data.mongodb.core.index.Indexed;
import java.time.LocalDateTime;

/**
 * Language Reference Data Model
 * 
 * Stores available languages for movies
 * Examples: English, Hindi, Nepali, Malayalam, Marathi
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Document(collection = "languages")
public class Language {

    @Id
    private String id;

    @Indexed(unique = true)
    private String code;  // e.g., 'ENG', 'HIN', 'NEP', 'MAL', 'MARA'

    private String name;  // e.g., 'English', 'Hindi', 'Nepali'

    @Builder.Default
    private Boolean isActive = true;

    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private LocalDateTime deletedAt;
}
