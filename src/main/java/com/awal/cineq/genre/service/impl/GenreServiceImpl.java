package com.awal.cineq.genre.service.impl;

import com.awal.cineq.dto.PaginationResponse;
import com.awal.cineq.exception.BusinessException;
import com.awal.cineq.exception.ResourceNotFoundException;
import com.awal.cineq.genre.dto.GenreDTO;
import com.awal.cineq.genre.dto.request.GenrePageRequest;
import com.awal.cineq.genre.dto.request.GenreRequestDto;
import com.awal.cineq.genre.model.Genre;
import com.awal.cineq.genre.repository.GenreRepository;
import com.awal.cineq.genre.service.GenreService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.modelmapper.ModelMapper;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Genre Service Implementation using MongoDB
 * Handles all business logic for genre management
 */
@Service
@RequiredArgsConstructor
@Transactional
@Slf4j
public class GenreServiceImpl implements GenreService {

    private final GenreRepository genreRepository;
    private final ModelMapper modelMapper;

    @Override
    @Transactional(readOnly = true)
    public PaginationResponse<GenreDTO> getGenre(GenrePageRequest genreRequest) {
        log.info("getGenre STARTED: request={}", genreRequest);
        try {
            PageRequest pageRequest = genreRequest.toPageRequest();

            Page<Genre> genres = findGenres(genreRequest, pageRequest);
            Long total = genres.getTotalElements();
            log.debug("Total genres found: {}", total);

            List<GenreDTO> result = genres.stream()
                    .map(genre -> modelMapper.map(genre, GenreDTO.class))
                    .toList();

            log.debug("getGenre result count: {}", result.size());
            log.info("getGenre END");

            return PaginationResponse.success(
                    "Genres fetched successfully",
                    result,
                    genres.getNumber() + 1, // converting to 1-based page index
                    genres.getSize(),
                    genres.getTotalPages(),
                    genres.getTotalElements(),
                    genres.hasNext(),
                    genres.hasPrevious()
            );

        } catch (Exception e) {
            log.error("getGenre ERROR", e);
            throw new BusinessException("Failed to fetch genres", e);
        }
    }

    @Override
    public GenreDTO createGenre(GenreRequestDto genreRequestDto) {
        log.info("createGenre STARTED: name={}", genreRequestDto.getName());
        try {
            // Check if genre already exists
            if (genreRepository.existsByName(genreRequestDto.getName())) {
                throw new BusinessException("Genre with name '" + genreRequestDto.getName() + "' already exists");
            }

            Genre genre = new Genre();
            genre.setName(genreRequestDto.getName());
            genre.setDescription(genreRequestDto.getDescription());
            genre.setIsActive(genreRequestDto.is_active());
            
            Genre saved = genreRepository.save(genre);
            GenreDTO result = modelMapper.map(saved, GenreDTO.class);
            
            log.debug("createGenre result: {}", result);
            log.info("createGenre END: id={}", saved.getId());
            
            return result;
        } catch (BusinessException e) {
            log.error("createGenre BUSINESS ERROR", e);
            throw e;
        } catch (Exception e) {
            log.error("createGenre ERROR", e);
            throw new BusinessException("Failed to create genre", e);
        }
    }

    @Override
    @Transactional(readOnly = true)
    public GenreDTO getGenreById(String id) {
        log.info("getGenreById STARTED: id={}", id);
        try {
            Genre genre = genreRepository.findById(id)
                    .orElseThrow(() -> new ResourceNotFoundException("Genre not found with id: " + id));

            // Ensure genre is not soft-deleted
            if (genre.getDeletedAt() != null) {
                throw new ResourceNotFoundException("Genre not found with id: " + id);
            }

            GenreDTO result = modelMapper.map(genre, GenreDTO.class);
            log.debug("getGenreById result: {}", result);
            log.info("getGenreById END");
            
            return result;
        } catch (ResourceNotFoundException e) {
            log.error("getGenreById NOT FOUND: {}", e.getMessage());
            throw e;
        } catch (Exception e) {
            log.error("getGenreById ERROR", e);
            throw new BusinessException("Failed to fetch genre by id", e);
        }
    }

    @Override
    public GenreDTO updateGenre(String id, GenreRequestDto genreRequestDto) {
        log.info("updateGenre STARTED: id={}", id);
        try {
            Genre genre = genreRepository.findById(id)
                    .orElseThrow(() -> new ResourceNotFoundException("Genre not found with id: " + id));

            // Ensure genre is not soft-deleted
            if (genre.getDeletedAt() != null) {
                throw new ResourceNotFoundException("Genre not found with id: " + id);
            }

            genre.setName(genreRequestDto.getName());
            genre.setDescription(genreRequestDto.getDescription());
            genre.setIsActive(genreRequestDto.is_active());
            
            Genre updated = genreRepository.save(genre);
            GenreDTO result = modelMapper.map(updated, GenreDTO.class);
            
            log.debug("updateGenre result: {}", result);
            log.info("updateGenre END");
            
            return result;
        } catch (ResourceNotFoundException e) {
            log.error("updateGenre NOT FOUND: {}", e.getMessage());
            throw e;
        } catch (Exception e) {
            log.error("updateGenre ERROR", e);
            throw new BusinessException("Failed to update genre", e);
        }
    }

    @Override
    public void deleteGenre(String id) {
        log.info("deleteGenre STARTED: id={}", id);
        try {
            Genre genre = genreRepository.findById(id)
                    .orElseThrow(() -> new ResourceNotFoundException("Genre not found with id: " + id));

            // Ensure genre is not already soft-deleted
            if (genre.getDeletedAt() != null) {
                throw new ResourceNotFoundException("Genre not found with id: " + id);
            }

            genre.setDeletedAt(LocalDateTime.now()); // Soft delete
            genreRepository.save(genre);
            
            log.info("deleteGenre END: id={}", id);
        } catch (ResourceNotFoundException e) {
            log.error("deleteGenre NOT FOUND: {}", e.getMessage());
            throw e;
        } catch (Exception e) {
            log.error("deleteGenre ERROR", e);
            throw new BusinessException("Failed to delete genre", e);
        }
    }

    @Override
    @Transactional
    public void bulkEnableGenres(List<String> ids, boolean enabled) {
        log.info("bulkEnableGenres STARTED: count={}, enabled={}", ids.size(), enabled);
        try {
            List<Genre> genres = genreRepository.findAllById(ids);
            
            if (genres.size() != ids.size()) {
                throw new ResourceNotFoundException("Some genres not found for the provided IDs");
            }

            // Filter out soft-deleted genres
            List<Genre> activeGenres = genres.stream()
                    .filter(g -> g.getDeletedAt() == null)
                    .toList();

            if (activeGenres.isEmpty()) {
                throw new ResourceNotFoundException("No active genres found for the provided IDs");
            }

            for (Genre genre : activeGenres) {
                genre.setIsActive(enabled);
            }
            
            genreRepository.saveAll(activeGenres);
            log.info("bulkEnableGenres END: updated={}", activeGenres.size());
        } catch (Exception e) {
            log.error("bulkEnableGenres ERROR", e);
            throw new BusinessException("Failed to bulk update genres", e);
        }
    }

    private Page<Genre> findGenres(GenrePageRequest request, PageRequest pageRequest) {
        log.info("findGenres STARTED: hasSearch={}", request.hasSearch());
        
        if (request.hasSearch()) {
            return genreRepository.findByNameContainingIgnoreCase(request.getSearch(), pageRequest);
        } else {
            return genreRepository.findAll(pageRequest);
        }
    }

}
