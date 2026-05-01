package com.awal.cineq.publicapi.movie.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ArtistInfo {
    private String id;
    private String fullName;
    private String avatar;
    private Double rating;
    private String bio;
}
