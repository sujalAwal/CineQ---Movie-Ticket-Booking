package com.awal.cineq.masterdata.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * DTO for Format reference data
 * 
 * Used in API responses to provide available movie format options
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class FormatDTO {

    private String id;
    private String code;    // e.g., '2D', '3D', 'IMAX', '4DX'
    private String name;    // e.g., '2D', '3D', 'IMAX', '4DX'
    private Boolean isActive;
}
