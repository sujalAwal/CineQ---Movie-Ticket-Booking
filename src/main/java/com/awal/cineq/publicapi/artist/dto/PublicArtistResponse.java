package com.awal.cineq.publicapi.artist.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PublicArtistResponse {
    private String id;
    private String fullName;
    private String bio;
    private String avatar;
    private String birthDate;
    private Double rating;
    private String nationality;
    private Integer moviesCount;
    private String artistTypeId;
    private Boolean isActive;
}
