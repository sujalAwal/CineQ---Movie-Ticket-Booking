package com.awal.cineq.masterdata.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;
import org.springframework.data.mongodb.core.mapping.Field;
import org.springframework.data.mongodb.core.index.Indexed;
import java.time.LocalDateTime;

/**
 * District Reference Data Model
 * 
 * Stores Nepal's 77 districts linked to provinces
 * Used for theatre location assignment with customer portal visibility control
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Document(collection = "districts")
public class District {

    @Id
    private String id;

    private String title;      // e.g., 'Kathmandu', 'Panchthar'

    private String titleNp;    // e.g., 'काठमाडौं', 'पञ्चथर' (Nepali text)

    @Indexed(unique = true)
    private String code;       // CBS codes: 'KATHM', 'PANCH', 'MORAN', etc.

    @Field("province_id")
    private String provinceId; // Reference to Province collection (MongoDB field: province_id)

    @Builder.Default
    private Boolean isActive = true;

    @Builder.Default
    private Boolean activeForCustomerPortal = true;  // Controls visibility in customer portal

    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private LocalDateTime deletedAt;
}
