package com.awal.cineq.form.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * DTO for bulk status update responses
 * Returns summary and detailed results of bulk status updates
 *
 * Example:
 * {
 *   "updated": 2,
 *   "failed": 0,
 *   "results": [
 *     { "id": "507f1f77bcf86cd799439011", "isActive": true, ... },
 *     { "id": "507f1f77bcf86cd799439012", "isActive": true, ... }
 *   ]
 * }
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class BulkStatusUpdateResponse {

    private int updated;  // Number of successfully updated submissions
    private int failed;   // Number of failed updates
    private List<FormSubmissionResponse> results;  // Detailed results for each submission
}

