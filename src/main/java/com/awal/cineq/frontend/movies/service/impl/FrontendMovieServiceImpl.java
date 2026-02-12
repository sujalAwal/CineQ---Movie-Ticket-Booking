package com.awal.cineq.frontend.movies.service.impl;

import com.awal.cineq.dto.PaginationResponse;
import com.awal.cineq.frontend.movies.dto.MovieDTO;
import com.awal.cineq.frontend.movies.dto.request.MoviePageRequest;
import com.awal.cineq.frontend.movies.mapper.MovieMapper;
import com.awal.cineq.frontend.movies.repository.FrontendMovieRepository;
import com.awal.cineq.frontend.movies.service.FrontendMovieService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.stereotype.Service;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class FrontendMovieServiceImpl implements FrontendMovieService {

  private final FrontendMovieRepository repository;
  private final MovieMapper movieMapper;

  @Override
  public PaginationResponse<MovieDTO> getAllMovies(MoviePageRequest pageRequest) {
    log.info("getAllMovies STARTED: {}", pageRequest);
    try {
      Page<Map> result;
      if (pageRequest.hasSearch()) {
        result = repository.searchByTitleOrDescription(pageRequest.getSearch(), pageRequest.toPageRequest());
      } else {
        result = repository.findAllActive(pageRequest.toPageRequest());
      }

      List<MovieDTO> movies = result.getContent().stream()
        .map(movieMapper::toDTO)
        .collect(Collectors.toList());

      PaginationResponse<MovieDTO> response = PaginationResponse.success(
        "Movies retrieved successfully",
        movies,
        pageRequest.getPage(),
        pageRequest.getSize(),
        result.getTotalElements(),
        result.getTotalPages()
      );
      log.info("getAllMovies END");
      return response;
    } catch (Exception e) {
      log.error("getAllMovies ERROR", e);
      throw e;
    }
  }

  @Override
  public MovieDTO getMovieById(String id) {
    log.info("getMovieById STARTED: id={}", id);
    try {
      Map movie = repository.findByIdAndActive(id);
      MovieDTO result = movieMapper.toDTO(movie);
      log.info("getMovieById END");
      return result;
    } catch (Exception e) {
      log.error("getMovieById ERROR", e);
      throw e;
    }
  }
}
