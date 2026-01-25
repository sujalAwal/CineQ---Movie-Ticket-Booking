package com.awal.cineq.masterdata.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.List;
import java.util.Map;

/**
 * Master Data Response DTO
 * Contains all enum/reference data for the frontend
 *
 * Structure allows for adding more enums later without API change
 * Frontend can dynamically use whatever it needs
 *
 * Example response:
 * {
 *   "formActions": [
 *     { "code": 1, "charCode": "C", "actionName": "create", ... },
 *     { "code": 2, "charCode": "R", "actionName": "read", ... },
 *     ...
 *   ],
 *   "otherEnums": { }  // Added later
 * }
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class MasterDataResponse {
    private List<PermissionActionDTO> permission;        // FormAction enum values
    private List<RoleDTO> role;
    // Future enums can be added here without breaking API
    // private List<OtherEnumDTO> otherEnums;
    // private List<AnotherEnumDTO> anotherEnums;

    // Or use a generic map for flexibility
    private Map<String, Object> additionalData;    // For other enums added later
}

