package com.awal.cineq.artist.service;

import com.awal.cineq.artist.dto.ArtistDTO;
import com.awal.cineq.artist.dto.ArtistRequestDto;
import com.awal.cineq.artist.dto.BulkArtistStatusUpdateRequest;

import java.util.List;
import java.util.UUID;

public interface ArtistService {
    ArtistDTO createArtist(ArtistRequestDto requestDto);
    ArtistDTO updateArtist(UUID id, ArtistRequestDto requestDto);
    void deleteArtist(UUID id);
    ArtistDTO getArtist(UUID id);
    List<ArtistDTO> getAllArtists();
    List<ArtistDTO> getArtistsByActiveStatus(Boolean isActive);
    void bulkEnable(BulkArtistStatusUpdateRequest request);
    void bulkDisable(BulkArtistStatusUpdateRequest request);
}

