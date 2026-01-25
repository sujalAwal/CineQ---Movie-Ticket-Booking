package com.awal.cineq.media.dto.request;

import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Request DTO for creating a folder in the media module.
 * MongoDB compatible: uses String parentId instead of UUID
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class FolderCreateRequest {

    @NotBlank(message = "Folder name is required")
    private String name;

    private String parentId; // Nullable - if null, folder will be created at root level (MongoDB ObjectId as String)
}

