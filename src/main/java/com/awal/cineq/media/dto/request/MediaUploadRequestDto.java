package com.awal.cineq.media.dto.request;

import org.springframework.web.multipart.MultipartFile;
import java.util.List;

/**
 * Media upload request DTO
 * MongoDB compatible: uses String parentId instead of UUID
 */
public class MediaUploadRequestDto {
    private List<MultipartFile> files;
    private String parentId;  // MongoDB ObjectId as String

    public List<MultipartFile> getFiles() {
        return files;
    }
    public void setFiles(List<MultipartFile> files) {
        this.files = files;
    }
    public String getParentId() {
        return parentId;
    }
    public void setParentId(String parentId) {
        this.parentId = parentId;
    }
}

