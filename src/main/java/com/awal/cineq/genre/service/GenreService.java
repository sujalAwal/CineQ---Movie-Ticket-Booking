package com.awal.cineq.genre.service;

import com.awal.cineq.genre.dto.GenreDTO;
import com.awal.cineq.genre.dto.request.GenreRequestDto;
import com.awal.cineq.genre.dto.request.GenrePageRequest;

import java.util.List;
import java.util.UUID;

public interface GenreService {
    List<GenreDTO> getGenre(GenrePageRequest genrePageRequest);
    GenreDTO createGenre(GenreRequestDto genreRequestDto);
    GenreDTO getGenreById(UUID id);
    GenreDTO updateGenre(UUID id, GenreRequestDto genreRequestDto);
    void deleteGenre(UUID id);
    void bulkEnableGenres(List<UUID> ids, boolean enabled);
}
