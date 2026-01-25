package com.awal.cineq.artist.service;

import com.awal.cineq.artist.dto.ArtistDTO;
import com.awal.cineq.artist.dto.ArtistRequestDto;
import com.awal.cineq.artist.dto.BulkArtistStatusUpdateRequest;

import java.util.List;

/**
 * Artist Service Interface for MongoDB
 * Uses String ID (MongoDB ObjectId) instead of UUID
 */
public interface ArtistService {
    ArtistDTO createArtist(ArtistRequestDto requestDto);
    ArtistDTO updateArtist(String id, ArtistRequestDto requestDto);
    void deleteArtist(String id);
    ArtistDTO getArtist(String id);
    List<ArtistDTO> getAllArtists();
    List<ArtistDTO> getArtistsByActiveStatus(Boolean isActive);
    void bulkEnable(BulkArtistStatusUpdateRequest request);
    void bulkDisable(BulkArtistStatusUpdateRequest request);
}

