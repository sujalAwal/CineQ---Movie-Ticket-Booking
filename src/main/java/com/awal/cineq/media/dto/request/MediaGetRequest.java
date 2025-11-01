package com.awal.cineq.media.dto.request;

import jakarta.validation.Valid;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.beans.factory.annotation.Value;

import java.util.UUID;

/**
 * Request DTO for getting media items by parent ID.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class MediaGetRequest {

    private UUID parentId; // Nullable - if null, returns root level items
}

