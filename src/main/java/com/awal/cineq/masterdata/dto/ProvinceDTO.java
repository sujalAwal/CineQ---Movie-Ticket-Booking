package com.awal.cineq.masterdata.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * DTO for Province reference data
 * 
 * Used in API responses to provide available province options
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ProvinceDTO {

    private String id;
    private String title;      // e.g., 'Bagmati Pradesh'
    private String titleNp;    // e.g., 'बाग्मती' (Nepali text)
    private String code;       // e.g., 'PROV1', 'PROV2'
    private Integer order;     // Sort order
    private Boolean isActive;
}
