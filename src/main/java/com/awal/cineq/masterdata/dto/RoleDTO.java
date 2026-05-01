package com.awal.cineq.masterdata.dto;

import lombok.*;

/**
 * Role DTO
 * Used in MasterDataResponse to provide role information to frontend
 *
 * Example:
 * {
 *   "id": "507f1f77bcf86cd799439011",
 *   "name": "ADMIN-123",
 *   "isActive": true
 * }
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class RoleDTO {
    private String name;
    private Boolean isActive;

}