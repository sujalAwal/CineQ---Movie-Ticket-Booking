package com.awal.cineq.publicapi.movie.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ArtistTypeInfo {
    private String id;
    private String name;
    private String icon;
    private String description;
}
