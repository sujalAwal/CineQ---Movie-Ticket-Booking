package com.awal.cineq.media.dto.request;


import jakarta.validation.constraints.NotNull;
import lombok.Data;
import org.springframework.beans.factory.annotation.Value;

import java.util.List;

/**
 * Media delete request DTO
 * MongoDB compatible: uses String mediaIds instead of UUID
 */
@Data
public class MediaDeleteRequestDto {

    @NotNull(message = "Media IDs are required")
    public List<String> mediaIds;  // MongoDB ObjectId as String
}
