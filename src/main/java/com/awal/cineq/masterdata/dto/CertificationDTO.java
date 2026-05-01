package com.awal.cineq.masterdata.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Certification DTO
 * Used in MasterDataResponse to provide certification/rating information to frontend
 *
 * Example:
 * {
 *   "id": "507f1f77bcf86cd799439011",
 *   "name": "Universal with Adult Supervision",
 *   "code": "UA",
 *   "description": "Suitable for all ages but parental guidance suggested for children under 12.",
 *   "isActive": true
 * }
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CertificationDTO {
    private String id;
    private String name;
    private String code;
    private String description;
    private Boolean isActive;
}
