package com.awal.cineq.media.dto.request;

import org.springframework.web.multipart.MultipartFile;
import java.util.List;
import java.util.UUID;

public class MediaUploadRequestDto {
    private List<MultipartFile> files;
    private UUID parentId;

    public List<MultipartFile> getFiles() {
        return files;
    }
    public void setFiles(List<MultipartFile> files) {
        this.files = files;
    }
    public UUID getParentId() {
        return parentId;
    }
    public void setParentId(UUID parentId) {
        this.parentId = parentId;
    }
}

