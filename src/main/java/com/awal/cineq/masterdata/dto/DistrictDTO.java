package com.awal.cineq.masterdata.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * DTO for District reference data
 * 
 * Used in API responses to provide available district options
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class DistrictDTO {

    private String id;
    private String title;                  // e.g., 'Kathmandu'
    private String titleNp;                // e.g., 'काठमाडौं' (Nepali text)
    private String code;                   // e.g., 'KATHM', 'PANCH'
    private String provinceId;             // Province reference
    private Boolean isActive;
    private Boolean activeForCustomerPortal;  // Customer portal visibility flag
}
