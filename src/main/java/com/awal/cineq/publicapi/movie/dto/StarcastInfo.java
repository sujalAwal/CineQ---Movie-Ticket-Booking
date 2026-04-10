package com.awal.cineq.publicapi.movie.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class StarcastInfo {
    private String characterName;
    private String artistId;
    private ArtistInfo artist;
    private String artistTypeId;
    private ArtistTypeInfo artistType;
}
