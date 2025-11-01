package com.awal.cineq.media.dto.request;

import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.UUID;

/**
 * Request DTO for creating a folder in the media module.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class FolderCreateRequest {

    @NotBlank(message = "Folder name is required")
    private String name;

    private UUID parentId; // Nullable - if null, folder will be created at root level
}

