package com.awal.cineq.genre.service.impl;

import com.awal.cineq.exception.BusinessException;
import com.awal.cineq.exception.ResourceNotFoundException;
import com.awal.cineq.genre.dto.GenreDTO;
import com.awal.cineq.genre.dto.request.GenrePageRequest;
import com.awal.cineq.genre.dto.request.GenreRequestDto;
import com.awal.cineq.genre.model.Genre;
import com.awal.cineq.genre.repository.GenreRepository;
import com.awal.cineq.genre.service.GenreService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Transactional
public class GenreServiceImpl implements GenreService {

    private static final Logger log = LoggerFactory.getLogger(GenreServiceImpl.class);
    private final GenreRepository genreRepository;

    @Override
    @Transactional(readOnly = true)
    public List<GenreDTO> getGenre(GenrePageRequest genrePageRequest) {
        log.info("getGenre STARTED");
        try {
            List<Genre> genres = genreRepository.findAll();
            List<GenreDTO> result = genres.stream().map(genre1 -> new GenreDTO(
                    genre1.getId(),
                    genre1.getName(),
                    genre1.getDescription(),
                    genre1.getIsActive())
            ).toList();
            log.debug("getGenre result: {}", result);
            log.info("getGenre END");
            return result;
        } catch (Exception e) {
            log.error("getGenre ERROR", e);
            throw new BusinessException("Failed to fetch genres", e);
        }
    }

    @Override
    public GenreDTO createGenre(GenreRequestDto genreRequestDto) {
        log.info("createGenre STARTED");
        log.debug("createGenre input: {}", genreRequestDto);
        try {
            Genre genre = new Genre();
            genre.setName(genreRequestDto.getName());
            genre.setDescription(genreRequestDto.getDescription());
            genre.setIsActive(genreRequestDto.is_active());
            Genre saved = genreRepository.save(genre);
            GenreDTO result = new GenreDTO(saved.getId(), saved.getName(), saved.getDescription(), saved.getIsActive());
            log.debug("createGenre result: {}", result);
            log.info("createGenre END");
            return result;
        } catch (Exception e) {
            log.error("createGenre ERROR", e);
            throw new BusinessException("Failed to create genre", e);
        }
    }

    @Override
    @Transactional(readOnly = true)
    public GenreDTO getGenreById(UUID id) {
        log.info("getGenreById STARTED");
        log.debug("getGenreById input: {}", id);
        try {
            Genre genre = genreRepository.findById(id)
                    .orElseThrow(() -> new ResourceNotFoundException("Genre not found with id: " + id));
            GenreDTO result = new GenreDTO(genre.getId(), genre.getName(), genre.getDescription(), genre.getIsActive());
            log.debug("getGenreById result: {}", result);
            log.info("getGenreById END");
            return result;
        } catch (ResourceNotFoundException e) {
            log.error("getGenreById NOT FOUND", e);
            throw e;
        } catch (Exception e) {
            log.error("getGenreById ERROR", e);
            throw new BusinessException("Failed to fetch genre by id", e);
        }
    }

    @Override
    public GenreDTO updateGenre(UUID id, GenreRequestDto genreRequestDto) {
        log.info("updateGenre STARTED");
        log.debug("updateGenre input: id={}, genreRequestDto={}", id, genreRequestDto);
        try {
            Genre genre = genreRepository.findById(id)
                    .orElseThrow(() -> new ResourceNotFoundException("Genre not found with id: " + id));
            genre.setName(genreRequestDto.getName());
            genre.setDescription(genreRequestDto.getDescription());
            genre.setIsActive(genreRequestDto.is_active());
            Genre updated = genreRepository.save(genre);
            GenreDTO result = new GenreDTO(updated.getId(), updated.getName(), updated.getDescription(), updated.getIsActive());
            log.debug("updateGenre result: {}", result);
            log.info("updateGenre END");
            return result;
        } catch (ResourceNotFoundException e) {
            log.error("updateGenre NOT FOUND", e);
            throw e;
        } catch (Exception e) {
            log.error("updateGenre ERROR", e);
            throw new BusinessException("Failed to update genre", e);
        }
    }

    @Override
    public void deleteGenre(UUID id) {
        log.info("deleteGenre STARTED");
        log.debug("deleteGenre input: {}", id);
        try {
            Genre genre = genreRepository.findById(id)
                    .orElseThrow(() -> new ResourceNotFoundException("Genre not found with id: " + id));
            genreRepository.delete(genre);
            log.info("deleteGenre END");
        } catch (ResourceNotFoundException e) {
            log.error("deleteGenre NOT FOUND", e);
            throw e;
        } catch (Exception e) {
            log.error("deleteGenre ERROR", e);
            throw new BusinessException("Failed to delete genre", e);
        }
    }

    @Override
    @Transactional
    public void bulkEnableGenres(List<UUID> ids, boolean enabled) {
        log.info("bulkEnableGenres STARTED: ids={}, enabled={}", ids, enabled);
        try {
            List<Genre> genres = genreRepository.findAllById(ids);
            if (genres.size() != ids.size()) {
                throw new ResourceNotFoundException("Some genres not found for the provided IDs");
            }
            for (Genre genre : genres) {
                genre.setIsActive(enabled);
                genre.setUpdatedAt(java.time.LocalDateTime.now());
            }
            genreRepository.saveAll(genres);
            log.info("bulkEnableGenres END");
        } catch (Exception e) {
            log.error("bulkEnableGenres ERROR", e);
            throw new BusinessException("Failed to bulk update genres", e);
        }
    }

}
