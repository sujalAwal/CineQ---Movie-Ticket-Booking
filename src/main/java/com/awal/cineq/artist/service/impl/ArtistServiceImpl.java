package com.awal.cineq.artist.service.impl;

import com.awal.cineq.artist.dto.ArtistDTO;
import com.awal.cineq.artist.dto.ArtistRequestDto;
import com.awal.cineq.artist.dto.BulkArtistStatusUpdateRequest;
import com.awal.cineq.artist.model.Artist;
import com.awal.cineq.artist.repository.ArtistRepository;
import com.awal.cineq.artist.service.ArtistService;
import com.awal.cineq.exception.ResourceNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class ArtistServiceImpl implements ArtistService {
    private final ArtistRepository artistRepository;

    @Override
    @Transactional
    public ArtistDTO createArtist(ArtistRequestDto requestDto) {
        Artist artist = Artist.builder()
                .name(requestDto.getName())
                .bio(requestDto.getBio())
                .profilePicture(requestDto.getProfilePicture())
                .isActive(requestDto.getIsActive())
                .order(requestDto.getOrder())
                .industry(requestDto.getIndustry())
                .createdAt(LocalDateTime.now())
                .build();
        artist = artistRepository.save(artist);
        return toDTO(artist);
    }

    @Override
    @Transactional
    public ArtistDTO updateArtist(UUID id, ArtistRequestDto requestDto) {
        Artist artist = artistRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Artist not found"));
        BeanUtils.copyProperties(requestDto, artist, "id", "createdAt", "updatedAt", "deletedAt");
        artist.setUpdatedAt(LocalDateTime.now());
        artist = artistRepository.save(artist);
        return toDTO(artist);
    }

    @Override
    @Transactional
    public void deleteArtist(UUID id) {
        Artist artist = artistRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Artist not found"));
        artist.setDeletedAt(LocalDateTime.now());
        artistRepository.save(artist);
    }

    @Override
    @Transactional(readOnly = true)
    public ArtistDTO getArtist(UUID id) {
        Artist artist = artistRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Artist not found"));
        return toDTO(artist);
    }

    @Override
    @Transactional(readOnly = true)
    public List<ArtistDTO> getAllArtists() {
        return artistRepository.findAll().stream().map(this::toDTO).collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public List<ArtistDTO> getArtistsByActiveStatus(Boolean isActive) {
        return artistRepository.findByIsActive(isActive).stream().map(this::toDTO).collect(Collectors.toList());
    }

    @Override
    @Transactional
    public void bulkEnable(BulkArtistStatusUpdateRequest request) {
        List<Artist> artists = artistRepository.findAllById(request.getIds());
        for (Artist artist : artists) {
            artist.setIsActive(true);
            artist.setUpdatedAt(LocalDateTime.now());
        }
        artistRepository.saveAll(artists);
    }

    @Override
    @Transactional
    public void bulkDisable(BulkArtistStatusUpdateRequest request) {
        List<Artist> artists = artistRepository.findAllById(request.getIds());
        for (Artist artist : artists) {
            artist.setIsActive(false);
            artist.setUpdatedAt(LocalDateTime.now());
        }
        artistRepository.saveAll(artists);
    }

    private ArtistDTO toDTO(Artist artist) {
        return ArtistDTO.builder()
                .id(artist.getId())
                .name(artist.getName())
                .bio(artist.getBio())
                .profilePicture(artist.getProfilePicture())
                .isActive(artist.getIsActive())
                .order(artist.getOrder())
                .industry(artist.getIndustry())
                .createdAt(artist.getCreatedAt())
                .build();
    }
}
