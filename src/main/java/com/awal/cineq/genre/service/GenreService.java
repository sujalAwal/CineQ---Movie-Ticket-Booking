package com.awal.cineq.genre.service;

import com.awal.cineq.dto.PaginationResponse;
import com.awal.cineq.genre.dto.GenreDTO;
import com.awal.cineq.genre.dto.request.GenreRequestDto;
import com.awal.cineq.genre.dto.request.GenrePageRequest;

import java.util.List;

public interface GenreService {
    PaginationResponse<GenreDTO> getGenre(GenrePageRequest genrePageRequest);
    GenreDTO createGenre(GenreRequestDto genreRequestDto);
    GenreDTO getGenreById(String id);
    GenreDTO updateGenre(String id, GenreRequestDto genreRequestDto);
    void deleteGenre(String id);
    void bulkEnableGenres(List<String> ids, boolean enabled);
}
