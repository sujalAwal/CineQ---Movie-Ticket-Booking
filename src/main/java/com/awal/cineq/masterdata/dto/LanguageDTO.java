package com.awal.cineq.masterdata.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * DTO for Language reference data
 * 
 * Used in API responses to provide available language options
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class LanguageDTO {

    private String id;
    private String code;    // e.g., 'ENG', 'HIN', 'NEP'
    private String name;    // e.g., 'English', 'Hindi', 'Nepali'
    private Boolean isActive;
}
