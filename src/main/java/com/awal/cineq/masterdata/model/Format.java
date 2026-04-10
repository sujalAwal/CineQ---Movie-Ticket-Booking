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
 * Format Reference Data Model
 * 
 * Stores available movie formats/viewing experiences
 * Examples: 2D, 3D, IMAX, 4DX
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Document(collection = "formats")
public class Format {

    @Id
    private String id;

    @Indexed(unique = true)
    private String code;  // e.g., '2D', '3D', 'IMAX', '4DX'

    private String name;  // e.g., '2D', '3D', 'IMAX', '4DX'

    @Builder.Default
    private Boolean isActive = true;

    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private LocalDateTime deletedAt;
}
