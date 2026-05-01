package com.awal.cineq.artist.service.impl;

import com.awal.cineq.artist.dto.ArtistDTO;
import com.awal.cineq.artist.dto.ArtistRequestDto;
import com.awal.cineq.artist.dto.BulkArtistStatusUpdateRequest;
import com.awal.cineq.artist.model.Artist;
import com.awal.cineq.artist.repository.ArtistRepository;
import com.awal.cineq.artist.service.ArtistService;
import com.awal.cineq.exception.BusinessException;
import com.awal.cineq.exception.ResourceNotFoundException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Artist Service Implementation using MongoDB
 * Handles all business logic for artist management with soft-delete pattern
 */
@Service
@RequiredArgsConstructor
@Transactional
@Slf4j
public class ArtistServiceImpl implements ArtistService {
    private final ArtistRepository artistRepository;

    @Override
    @Transactional
    public ArtistDTO createArtist(ArtistRequestDto requestDto) {
        log.info("createArtist STARTED: name={}", requestDto.getName());
        try {
            Artist artist = Artist.builder()
                    .name(requestDto.getName())
                    .bio(requestDto.getBio())
                    .profilePicture(requestDto.getProfilePicture())
                    .isActive(requestDto.getIsActive())
                    .order(requestDto.getOrder())
                    .industry(requestDto.getIndustry())
                    .build();

            Artist saved = artistRepository.save(artist);
            ArtistDTO result = toDTO(saved);

            log.info("createArtist END: id={}", saved.getId());
            return result;
        } catch (Exception e) {
            log.error("createArtist ERROR", e);
            throw new BusinessException("Failed to create artist", e);
        }
    }

    @Override
    @Transactional
    public ArtistDTO updateArtist(String id, ArtistRequestDto requestDto) {
        log.info("updateArtist STARTED: id={}", id);
        try {
            Artist artist = artistRepository.findByIdActive(id)
                    .orElseThrow(() -> new ResourceNotFoundException("Artist not found with id: " + id));

            BeanUtils.copyProperties(requestDto, artist, "id", "createdAt", "updatedAt", "deletedAt");

            Artist updated = artistRepository.save(artist);
            ArtistDTO result = toDTO(updated);

            log.info("updateArtist END: id={}", id);
            return result;
        } catch (ResourceNotFoundException e) {
            log.error("updateArtist NOT FOUND: {}", e.getMessage());
            throw e;
        } catch (Exception e) {
            log.error("updateArtist ERROR", e);
            throw new BusinessException("Failed to update artist", e);
        }
    }

    @Override
    @Transactional
    public void deleteArtist(String id) {
        log.info("deleteArtist STARTED: id={}", id);
        try {
            Artist artist = artistRepository.findByIdActive(id)
                    .orElseThrow(() -> new ResourceNotFoundException("Artist not found with id: " + id));

            artist.setDeletedAt(LocalDateTime.now()); // Soft delete
            artistRepository.save(artist);

            log.info("deleteArtist END: id={}", id);
        } catch (ResourceNotFoundException e) {
            log.error("deleteArtist NOT FOUND: {}", e.getMessage());
            throw e;
        } catch (Exception e) {
            log.error("deleteArtist ERROR", e);
            throw new BusinessException("Failed to delete artist", e);
        }
    }

    @Override
    @Transactional(readOnly = true)
    public ArtistDTO getArtist(String id) {
        log.info("getArtist STARTED: id={}", id);
        try {
            Artist artist = artistRepository.findByIdActive(id)
                    .orElseThrow(() -> new ResourceNotFoundException("Artist not found with id: " + id));

            ArtistDTO result = toDTO(artist);
            log.info("getArtist END");
            return result;
        } catch (ResourceNotFoundException e) {
            log.error("getArtist NOT FOUND: {}", e.getMessage());
            throw e;
        } catch (Exception e) {
            log.error("getArtist ERROR", e);
            throw new BusinessException("Failed to fetch artist", e);
        }
    }

    @Override
    @Transactional(readOnly = true)
    public List<ArtistDTO> getAllArtists() {
        log.info("getAllArtists STARTED");
        try {
            List<ArtistDTO> result = artistRepository.findAllActive().stream()
                    .map(this::toDTO)
                    .collect(Collectors.toList());

            log.info("getAllArtists END: count={}", result.size());
            return result;
        } catch (Exception e) {
            log.error("getAllArtists ERROR", e);
            throw new BusinessException("Failed to fetch all artists", e);
        }
    }

    @Override
    @Transactional(readOnly = true)
    public List<ArtistDTO> getArtistsByActiveStatus(Boolean isActive) {
        log.info("getArtistsByActiveStatus STARTED: isActive={}", isActive);
        try {
            List<ArtistDTO> result = artistRepository.findByIsActive(isActive).stream()
                    .map(this::toDTO)
                    .collect(Collectors.toList());

            log.info("getArtistsByActiveStatus END: count={}", result.size());
            return result;
        } catch (Exception e) {
            log.error("getArtistsByActiveStatus ERROR", e);
            throw new BusinessException("Failed to fetch artists by active status", e);
        }
    }

    @Override
    @Transactional
    public void bulkEnable(BulkArtistStatusUpdateRequest request) {
        log.info("bulkEnable STARTED: count={}", request.getIds().size());
        try {
            List<Artist> artists = artistRepository.findAllById(request.getIds());

            // Filter out soft-deleted artists
            List<Artist> activeArtists = artists.stream()
                    .filter(a -> a.getDeletedAt() == null)
                    .toList();

            if (activeArtists.isEmpty()) {
                throw new ResourceNotFoundException("No active artists found for the provided IDs");
            }

            for (Artist artist : activeArtists) {
                artist.setIsActive(true);
            }

            artistRepository.saveAll(activeArtists);
            log.info("bulkEnable END: updated={}", activeArtists.size());
        } catch (Exception e) {
            log.error("bulkEnable ERROR", e);
            throw new BusinessException("Failed to bulk enable artists", e);
        }
    }

    @Override
    @Transactional
    public void bulkDisable(BulkArtistStatusUpdateRequest request) {
        log.info("bulkDisable STARTED: count={}", request.getIds().size());
        try {
            List<Artist> artists = artistRepository.findAllById(request.getIds());

            // Filter out soft-deleted artists
            List<Artist> activeArtists = artists.stream()
                    .filter(a -> a.getDeletedAt() == null)
                    .toList();

            if (activeArtists.isEmpty()) {
                throw new ResourceNotFoundException("No active artists found for the provided IDs");
            }

            for (Artist artist : activeArtists) {
                artist.setIsActive(false);
            }

            artistRepository.saveAll(activeArtists);
            log.info("bulkDisable END: updated={}", activeArtists.size());
        } catch (Exception e) {
            log.error("bulkDisable ERROR", e);
            throw new BusinessException("Failed to bulk disable artists", e);
        }
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
