package com.awal.cineq.media.dto.response;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;
import java.util.Map;

/**
 * Media-specific response wrapper used as the payload inside the global ApiResponse<T>.
 * Contains an optional message and either "data" or "result" lists (or both).
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class MediaResponse {
    private List<Map<String, Object>> media;
}
