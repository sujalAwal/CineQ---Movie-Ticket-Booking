package com.awal.cineq.frontend.movies.service;

import com.awal.cineq.dto.PaginationResponse;
import com.awal.cineq.frontend.movies.dto.MovieDTO;
import com.awal.cineq.frontend.movies.dto.request.MoviePageRequest;

public interface FrontendMovieService {
  PaginationResponse<MovieDTO> getAllMovies(MoviePageRequest pageRequest);
  MovieDTO getMovieById(String id);
}
