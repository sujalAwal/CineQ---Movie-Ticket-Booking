package com.awal.cineq.frontend.screens.service.impl;

import com.awal.cineq.dto.PaginationResponse;
import com.awal.cineq.frontend.screens.dto.ScreenDTO;
import com.awal.cineq.frontend.screens.dto.request.ScreenPageRequest;
import com.awal.cineq.frontend.screens.mapper.ScreenMapper;
import com.awal.cineq.frontend.screens.repository.FrontendScreenRepository;
import com.awal.cineq.frontend.screens.service.FrontendScreenService;
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
public class FrontendScreenServiceImpl implements FrontendScreenService {

  private final FrontendScreenRepository repository;
  private final ScreenMapper screenMapper;

  @Override
  public PaginationResponse<ScreenDTO> getAllScreens(ScreenPageRequest pageRequest) {
    log.info("getAllScreens STARTED: {}", pageRequest);
    try {
      Page<Map> result;
      if (pageRequest.hasSearch()) {
        result = repository.searchByNameOrType(pageRequest.getSearch(), pageRequest.toPageRequest());
      } else {
        result = repository.findAllActive(pageRequest.toPageRequest());
      }

      List<ScreenDTO> screens = result.getContent().stream()
        .map(screenMapper::toDTO)
        .collect(Collectors.toList());

      PaginationResponse<ScreenDTO> response = PaginationResponse.success(
        "Screens retrieved successfully",
        screens,
        pageRequest.getPage(),
        pageRequest.getSize(),
        result.getTotalPages(),
        result.getTotalElements(),
        result.hasNext(),
        result.hasPrevious()
      );
      log.info("getAllScreens END");
      return response;
    } catch (Exception e) {
      log.error("getAllScreens ERROR", e);
      throw e;
    }
  }

  @Override
  public ScreenDTO getScreenById(String id) {
    log.info("getScreenById STARTED: id={}", id);
    try {
      Map screen = repository.findByIdAndActive(id);
      ScreenDTO result = screenMapper.toDTO(screen);
      log.info("getScreenById END");
      return result;
    } catch (Exception e) {
      log.error("getScreenById ERROR", e);
      throw e;
    }
  }

  @Override
  public PaginationResponse<ScreenDTO> getScreensByTheatreId(String theatreId, int page, int size) {
    log.info("getScreensByTheatreId STARTED: theatreId={}", theatreId);
    try {
      Pageable pageable = PageRequest.of(page - 1, size, Sort.by(Sort.Direction.ASC, "screenName"));
      Page<Map> result = repository.findByTheatreId(theatreId, pageable);
      List<ScreenDTO> screens = result.getContent().stream()
        .map(screenMapper::toDTO)
        .collect(Collectors.toList());
      PaginationResponse<ScreenDTO> response = PaginationResponse.success(
        "Screens in theatre retrieved successfully",
        screens,
        page,
        size,
        result.getTotalPages(),
        result.getTotalElements(),
        result.hasNext(),
        result.hasPrevious()
      );
      log.info("getScreensByTheatreId END");
      return response;
    } catch (Exception e) {
      log.error("getScreensByTheatreId ERROR", e);
      throw e;
    }
  }

  @Override
  public PaginationResponse<ScreenDTO> getScreensByType(String screenType, int page, int size) {
    log.info("getScreensByType STARTED: screenType={}", screenType);
    try {
      Pageable pageable = PageRequest.of(page - 1, size, Sort.by(Sort.Direction.ASC, "screenName"));
      Page<Map> result = repository.findByScreenType(screenType, pageable);
      List<ScreenDTO> screens = result.getContent().stream()
        .map(screenMapper::toDTO)
        .collect(Collectors.toList());
      PaginationResponse<ScreenDTO> response = PaginationResponse.success(
        "Screens of type retrieved successfully",
        screens,
        page,
        size,
        result.getTotalPages(),
        result.getTotalElements(),
        result.hasNext(),
        result.hasPrevious()
      );
      log.info("getScreensByType END");
      return response;
    } catch (Exception e) {
      log.error("getScreensByType ERROR", e);
      throw e;
    }
  }
}
