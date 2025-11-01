package com.awal.cineq.media.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * Response wrapper for media list operations.
 * Used for GET endpoints that return multiple media items.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MediaListResponse {
    private List<MediaDetailDto> media;
    private Integer totalCount;
}

