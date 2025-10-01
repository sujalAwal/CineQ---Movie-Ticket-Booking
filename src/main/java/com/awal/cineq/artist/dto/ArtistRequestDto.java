package com.awal.cineq.artist.dto;

import lombok.*;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ArtistRequestDto {
    private String name;
    private String bio;
    private String profilePicture;
    private Boolean isActive;
    private Integer order;
    private String industry;
}

