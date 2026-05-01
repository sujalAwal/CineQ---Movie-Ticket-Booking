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
 * Province Reference Data Model
 * 
 * Stores Nepal's 7 administrative provinces
 * Used for theatre location grouping and hierarchy
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Document(collection = "provinces")
public class Province {

    @Id
    private String id;

    private String title;      // e.g., 'Bagmati Pradesh', 'Lumbini Pradesh'

    private String titleNp;    // e.g., 'बाग्मती', 'लुम्बिनी' (Nepali text)

    @Indexed(unique = true)
    private String code;       // e.g., 'PROV1', 'PROV2', 'PROV3'

    private Integer order;     // Sort order for UI display (1-7)

    @Builder.Default
    private Boolean isActive = true;

    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private LocalDateTime deletedAt;
}
