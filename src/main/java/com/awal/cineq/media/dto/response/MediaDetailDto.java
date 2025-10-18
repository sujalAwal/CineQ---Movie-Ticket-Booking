package com.awal.cineq.media.dto.response;

import com.awal.cineq.media.model.MediaType;
import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * DTO for media detail responses in GET operations.
 * Contains all media information except isActive and deletedAt fields.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MediaDetailDto {

    private UUID id;
    private String fileName;
    private String url;
    private MediaType type;
    private UUID parentId;
    private String filePath;
    private String fileUuid;

    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd'T'HH:mm:ss")
    private LocalDateTime createdAt;

    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd'T'HH:mm:ss")
    private LocalDateTime updatedAt;
}

