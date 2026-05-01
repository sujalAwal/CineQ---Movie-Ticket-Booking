package com.awal.cineq.media.dto.response;

import com.awal.cineq.media.model.MediaType;
import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * DTO for media detail responses in GET operations.
 * Contains all media information except isActive and deletedAt fields.
 * MongoDB compatible: uses String ID instead of UUID
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MediaDetailDto {

    private String id;  // MongoDB ObjectId as String
    private String fileName;
    private String url;
    private MediaType type;
    private String parentId;  // MongoDB ObjectId as String
    private String filePath;
    private String fileUuid;

    private List<MediaDetailDto> children ;

    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd'T'HH:mm:ss")
    private LocalDateTime createdAt;

    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd'T'HH:mm:ss")
    private LocalDateTime updatedAt;

    public List<MediaDetailDto> getChildren() {
        if (children == null) {
            children = new ArrayList<>();
        }
        return children;
    }

}

