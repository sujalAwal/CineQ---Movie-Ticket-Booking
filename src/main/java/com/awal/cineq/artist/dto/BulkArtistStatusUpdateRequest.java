package com.awal.cineq.artist.dto;

import lombok.*;
import java.util.List;

/**
 * Bulk Artist Status Update Request for MongoDB
 * Uses String IDs (MongoDB ObjectId) instead of UUID
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class BulkArtistStatusUpdateRequest {
    private List<String> ids;  // MongoDB ObjectId as String
    private Boolean isActive;
}

