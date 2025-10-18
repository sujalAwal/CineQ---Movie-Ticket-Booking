package com.awal.cineq.media.dto.response;

import com.awal.cineq.media.model.MediaType;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * DTO representing a media item (file or folder) in responses.
 * Used for upload operations and folder creation responses.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MediaItemDto {

    private UUID id;
    private String title;
    private String name;
    private String url;
    private MediaType type;
    private String originalFileName;
    private UUID parentId;
    private String filePath;
    private Boolean isActive;
    private LocalDateTime createdAt;

    /**
     * Converts this DTO to a Map representation for backward compatibility.
     * @deprecated Use the DTO directly instead of Map
     */
    @Deprecated
    public java.util.Map<String, Object> toMap() {
        java.util.Map<String, Object> map = new java.util.HashMap<>();
        if (id != null) map.put("id", id);
        if (title != null) map.put("title", title);
        if (name != null) map.put("name", name);
        if (url != null) map.put("url", url);
        if (type != null) map.put("type", type);
        if (originalFileName != null) map.put("originalFileName", originalFileName);
        if (parentId != null) map.put("parentId", parentId);
        if (filePath != null) map.put("filePath", filePath);
        if (isActive != null) map.put("isActive", isActive);
        if (createdAt != null) map.put("createdAt", createdAt);
        return map;
    }
}

