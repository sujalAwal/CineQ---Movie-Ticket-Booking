package com.awal.cineq.artist.dto;

import lombok.*;
import java.time.LocalDateTime;
import java.util.UUID;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ArtistDTO {
    private UUID id;
    private String name;
    private String bio;
    private String profilePicture;
    private Boolean isActive;
    private Integer order;
    private String industry;
    private LocalDateTime createdAt;
}

