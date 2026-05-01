package com.awal.cineq.media.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MediaDTO {
    private String id;
    private String fileName;
    private String fileUrl;
    private String fileType;
    private Long fileSize;
    private String storageType; // LOCAL or SUPABASE
    private String uploadedBy;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
