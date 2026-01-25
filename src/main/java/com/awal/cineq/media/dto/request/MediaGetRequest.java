package com.awal.cineq.media.dto.request;

import jakarta.validation.Valid;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.beans.factory.annotation.Value;

/**
 * Request DTO for getting media items by parent ID.
 * MongoDB compatible: uses String parentId instead of UUID
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class MediaGetRequest {

    private String parentId; // Nullable - if null, returns root level items (MongoDB ObjectId as String)
}

