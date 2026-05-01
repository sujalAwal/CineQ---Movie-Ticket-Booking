package com.awal.cineq.masterdata.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Movie Release Status DTO
 * Used in MasterDataResponse to provide movie release status information to frontend
 *
 * Example:
 * {
 *   "id": "507f1f77bcf86cd799439011",
 *   "name": "Coming Soon",
 *   "code": "COMING_SOON",
 *   "description": "Movie is confirmed and will be releasing in theatres soon.",
 *   "isActive": true
 * }
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class MovieReleaseStatusDTO {
    private String id;
    private String name;
    private String code;
    private String description;
    private Boolean isActive;
}
