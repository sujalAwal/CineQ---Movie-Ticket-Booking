package com.awal.cineq.user.dto.request;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * Request DTO for bulk user status updates (enable/disable)
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class BulkUserStatusUpdateRequest {
    private List<String> ids;  // MongoDB ObjectIds as Strings
}
