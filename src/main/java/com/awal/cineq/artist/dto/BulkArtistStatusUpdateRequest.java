package com.awal.cineq.artist.dto;

import lombok.*;
import java.util.List;
import java.util.UUID;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class BulkArtistStatusUpdateRequest {
    private List<UUID> ids;
    private Boolean isActive;
}

