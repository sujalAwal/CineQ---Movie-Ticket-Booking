package com.awal.cineq.frontend.theatres.service.impl;

import com.awal.cineq.dto.PaginationResponse;
import com.awal.cineq.frontend.theatres.dto.TheatreDTO;
import com.awal.cineq.frontend.theatres.dto.request.TheatrePageRequest;
import com.awal.cineq.frontend.theatres.mapper.TheatreMapper;
import com.awal.cineq.frontend.theatres.repository.FrontendTheatreRepository;
import com.awal.cineq.frontend.theatres.service.FrontendTheatreService;
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
public class FrontendTheatreServiceImpl implements FrontendTheatreService {

  private final FrontendTheatreRepository repository;
  private final TheatreMapper theatreMapper;

  @Override
  public PaginationResponse<TheatreDTO> getAllTheatres(TheatrePageRequest pageRequest) {
    log.info("getAllTheatres STARTED: {}", pageRequest);
    try {
      Page<Map> result;
      if (pageRequest.hasSearch()) {
        result = repository.searchByNameOrCityOrAddress(pageRequest.getSearch(), pageRequest.toPageRequest());
      } else {
        result = repository.findAllActive(pageRequest.toPageRequest());
      }

      List<TheatreDTO> theatres = result.getContent().stream()
        .map(theatreMapper::toDTO)
        .collect(Collectors.toList());

      PaginationResponse<TheatreDTO> response = PaginationResponse.success(
        "Theatres retrieved successfully",
        theatres,
        pageRequest.getPage(),
        pageRequest.getSize(),
        result.getTotalPages(),
        result.getTotalElements(),
        result.hasNext(),
        result.hasPrevious()
      );
      log.info("getAllTheatres END");
      return response;
    } catch (Exception e) {
      log.error("getAllTheatres ERROR", e);
      throw e;
    }
  }

  @Override
  public TheatreDTO getTheatreById(String id) {
    log.info("getTheatreById STARTED: id={}", id);
    try {
      Map theatre = repository.findByIdAndActive(id);
      TheatreDTO result = theatreMapper.toDTO(theatre);
      log.info("getTheatreById END");
      return result;
    } catch (Exception e) {
      log.error("getTheatreById ERROR", e);
      throw e;
    }
  }

  @Override
  public PaginationResponse<TheatreDTO> getTheatresByCity(String city, int page, int size) {
    log.info("getTheatresByCity STARTED: city={}", city);
    try {
      Pageable pageable = PageRequest.of(page - 1, size, Sort.by(Sort.Direction.ASC, "name"));
      Page<Map> result = repository.findByCity(city, pageable);
      List<TheatreDTO> theatres = result.getContent().stream()
        .map(theatreMapper::toDTO)
        .collect(Collectors.toList());
      PaginationResponse<TheatreDTO> response = PaginationResponse.success(
        "Theatres in city retrieved successfully",
        theatres,
        page,
        size,
        result.getTotalPages(),
        result.getTotalElements(),
        result.hasNext(),
        result.hasPrevious()
      );
      log.info("getTheatresByCity END");
      return response;
    } catch (Exception e) {
      log.error("getTheatresByCity ERROR", e);
      throw e;
    }
  }

  @Override
  public PaginationResponse<TheatreDTO> getTheatresByState(String state, int page, int size) {
    log.info("getTheatresByState STARTED: state={}", state);
    try {
      Pageable pageable = PageRequest.of(page - 1, size, Sort.by(Sort.Direction.ASC, "city"));
      Page<Map> result = repository.findByState(state, pageable);
      List<TheatreDTO> theatres = result.getContent().stream()
        .map(theatreMapper::toDTO)
        .collect(Collectors.toList());
      PaginationResponse<TheatreDTO> response = PaginationResponse.success(
        "Theatres in state retrieved successfully",
        theatres,
        page,
        size,
        result.getTotalPages(),
        result.getTotalElements(),
        result.hasNext(),
        result.hasPrevious()
      );
      log.info("getTheatresByState END");
      return response;
    } catch (Exception e) {
      log.error("getTheatresByState ERROR", e);
      throw e;
    }
  }
}
