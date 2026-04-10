package com.awal.cineq.publicapi.artist.service;

import com.awal.cineq.dto.ApiResponse;
import com.awal.cineq.publicapi.artist.dto.PublicArtistResponse;

import java.util.List;

public interface PublicArtistService {
    ApiResponse<List<PublicArtistResponse>> getAllActiveArtists(String artistTypeId);
    ApiResponse<List<PublicArtistResponse>> getArtistsByIds(List<String> ids, String artistTypeId);
}
