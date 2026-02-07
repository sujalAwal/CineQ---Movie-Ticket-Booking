package com.awal.cineq.form.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * DTO for bulk soft-delete responses
 * Returns summary and detailed results of bulk soft-delete operations
 *
 * Example:
 * {
 *   "deleted": 2,
 *   "failed": 0,
 *   "results": [
 *     { "id": "507f1f77bcf86cd799439011", "success": true, "message": "Deleted successfully" },
 *     { "id": "507f1f77bcf86cd799439012", "success": true, "message": "Deleted successfully" }
 *   ]
 * }
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class BulkDeleteResponse {

    private int deleted;  // Number of successfully soft-deleted documents
    private int failed;   // Number of failed deletions
    private List<DeleteResult> results;  // Detailed results for each document

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class DeleteResult {
        private String id;       // Document ID
        private boolean success; // Whether deletion was successful
        private String message;  // Success or error message
    }
}

