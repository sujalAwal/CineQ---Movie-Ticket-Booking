package com.awal.cineq.frontend.showtimes.service.impl;

import com.awal.cineq.dto.PaginationResponse;
import com.awal.cineq.frontend.showtimes.dto.ShowtimeDTO;
import com.awal.cineq.frontend.showtimes.dto.request.ShowtimePageRequest;
import com.awal.cineq.frontend.showtimes.mapper.ShowtimeMapper;
import com.awal.cineq.frontend.showtimes.repository.FrontendShowtimeRepository;
import com.awal.cineq.frontend.showtimes.service.FrontendShowtimeService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class FrontendShowtimeServiceImpl implements FrontendShowtimeService {

  private final FrontendShowtimeRepository repository;
  private final ShowtimeMapper showtimeMapper;

  @Override
  public PaginationResponse<ShowtimeDTO> getAllShowtimes(ShowtimePageRequest pageRequest) {
    log.info("getAllShowtimes STARTED: {}", pageRequest);
    try {
      Page<Map> result;
      if (pageRequest.hasSearch()) {
        result = repository.searchByField(pageRequest.getSearch(), pageRequest.toPageRequest());
      } else {
        result = repository.findAllActive(pageRequest.toPageRequest());
      }

      List<ShowtimeDTO> showtimes = result.getContent().stream()
        .map(showtimeMapper::toDTO)
        .collect(Collectors.toList());

      PaginationResponse<ShowtimeDTO> response = PaginationResponse.success(
        "Showtimes retrieved successfully",
        showtimes,
        pageRequest.getPage(),
        pageRequest.getSize(),
        result.getTotalElements(),
        result.getTotalPages()
      );
      log.info("getAllShowtimes END");
      return response;
    } catch (Exception e) {
      log.error("getAllShowtimes ERROR", e);
      throw e;
    }
  }

  @Override
  public ShowtimeDTO getShowtimeById(String id) {
    log.info("getShowtimeById STARTED: id={}", id);
    try {
      Map showtime = repository.findByIdAndActive(id);
      ShowtimeDTO result = showtimeMapper.toDTO(showtime);
      log.info("getShowtimeById END");
      return result;
    } catch (Exception e) {
      log.error("getShowtimeById ERROR", e);
      throw e;
    }
  }

  @Override
  public PaginationResponse<ShowtimeDTO> getShowtimesByMovieId(String movieId, int page, int size) {
    log.info("getShowtimesByMovieId STARTED: movieId={}", movieId);
    try {
      Pageable pageable = PageRequest.of(page - 1, size, Sort.by(Sort.Direction.ASC, "showDate"));
      Page<Map> result = repository.findByMovieId(movieId, pageable);
      List<ShowtimeDTO> showtimes = result.getContent().stream()
        .map(showtimeMapper::toDTO)
        .collect(Collectors.toList());
      PaginationResponse<ShowtimeDTO> response = PaginationResponse.success(
        "Showtimes for movie retrieved successfully",
        showtimes,
        page,
        size,
        result.getTotalElements(),
        result.getTotalPages()
      );
      log.info("getShowtimesByMovieId END");
      return response;
    } catch (Exception e) {
      log.error("getShowtimesByMovieId ERROR", e);
      throw e;
    }
  }

  @Override
  public PaginationResponse<ShowtimeDTO> getShowtimesByTheatreId(String theatreId, int page, int size) {
    log.info("getShowtimesByTheatreId STARTED: theatreId={}", theatreId);
    try {
      Pageable pageable = PageRequest.of(page - 1, size, Sort.by(Sort.Direction.ASC, "showDate"));
      Page<Map> result = repository.findByTheatreId(theatreId, pageable);
      List<ShowtimeDTO> showtimes = result.getContent().stream()
        .map(showtimeMapper::toDTO)
        .collect(Collectors.toList());
      PaginationResponse<ShowtimeDTO> response = PaginationResponse.success(
        "Showtimes for theatre retrieved successfully",
        showtimes,
        page,
        size,
        result.getTotalElements(),
        result.getTotalPages()
      );
      log.info("getShowtimesByTheatreId END");
      return response;
    } catch (Exception e) {
      log.error("getShowtimesByTheatreId ERROR", e);
      throw e;
    }
  }

  @Override
  public PaginationResponse<ShowtimeDTO> getShowtimesByScreenId(String screenId, int page, int size) {
    log.info("getShowtimesByScreenId STARTED: screenId={}", screenId);
    try {
      Pageable pageable = PageRequest.of(page - 1, size, Sort.by(Sort.Direction.ASC, "showDate"));
      Page<Map> result = repository.findByScreenId(screenId, pageable);
      List<ShowtimeDTO> showtimes = result.getContent().stream()
        .map(showtimeMapper::toDTO)
        .collect(Collectors.toList());
      PaginationResponse<ShowtimeDTO> response = PaginationResponse.success(
        "Showtimes for screen retrieved successfully",
        showtimes,
        page,
        size,
        result.getTotalElements(),
        result.getTotalPages()
      );
      log.info("getShowtimesByScreenId END");
      return response;
    } catch (Exception e) {
      log.error("getShowtimesByScreenId ERROR", e);
      throw e;
    }
  }
}
