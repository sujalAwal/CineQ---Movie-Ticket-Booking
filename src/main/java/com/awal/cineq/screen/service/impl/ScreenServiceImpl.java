package com.awal.cineq.screen.service.impl;

import com.awal.cineq.dto.PaginationResponse;
import com.awal.cineq.exception.ResourceNotFoundException;
import com.awal.cineq.screen.dto.ScreenDTO;
import com.awal.cineq.screen.model.Screen;
import com.awal.cineq.screen.repository.ScreenRepository;
import com.awal.cineq.screen.service.ScreenService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.modelmapper.ModelMapper;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;

/**
 * Service implementation for Screen operations
 * Handles business logic for screen management
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class ScreenServiceImpl implements ScreenService {

    private final ScreenRepository screenRepository;
    private final ModelMapper modelMapper;

    @Override
    public List<ScreenDTO> getScreensByTheatreId(String theatreId) {
        log.info("getScreensByTheatreId STARTED: theatreId={}", theatreId);
        try {
            List<Screen> screens = screenRepository.findByTheatreIdActive(theatreId);
            log.info("Found {} screens for theatre: {}", screens.size(), theatreId);
            log.info("getScreensByTheatreId END");
            return screens.stream()
                    .map(screen -> modelMapper.map(screen, ScreenDTO.class))
                    .collect(Collectors.toList());
        } catch (Exception e) {
            log.error("getScreensByTheatreId ERROR", e);
            throw e;
        }
    }

    @Override
    public PaginationResponse<ScreenDTO> getScreensByTheatreIdPaginated(String theatreId, int page, int size) {
        log.info("getScreensByTheatreIdPaginated STARTED: theatreId={}, page={}, size={}", theatreId, page, size);
        try {
            Pageable pageable = PageRequest.of(page, size);
            Page<Screen> screenPage = screenRepository.findByTheatreIdActivePaginated(theatreId, pageable);

            List<ScreenDTO> screenDTOs = screenPage.getContent().stream()
                    .map(screen -> modelMapper.map(screen, ScreenDTO.class))
                    .collect(Collectors.toList());

            PaginationResponse<ScreenDTO> response = PaginationResponse.success(
                    "Screens retrieved successfully",
                    screenDTOs,
                    screenPage.getNumber(),
                    screenPage.getSize(),
                    screenPage.getTotalPages(),
                    screenPage.getTotalElements(),
                    screenPage.hasNext(),
                    screenPage.hasPrevious()
            );

            log.info("getScreensByTheatreIdPaginated END: found {} screens", screenDTOs.size());
            return response;
        } catch (Exception e) {
            log.error("getScreensByTheatreIdPaginated ERROR", e);
            throw e;
        }
    }

    @Override
    public ScreenDTO getScreenById(String id) {
        log.info("getScreenById STARTED: id={}", id);
        try {
            Screen screen = screenRepository.findByIdActive(id)
                    .orElseThrow(() -> new ResourceNotFoundException("Screen not found with id: " + id));
            log.info("getScreenById END");
            return modelMapper.map(screen, ScreenDTO.class);
        } catch (Exception e) {
            log.error("getScreenById ERROR", e);
            throw e;
        }
    }

    @Override
    public List<ScreenDTO> getAllScreens() {
        log.info("getAllScreens STARTED");
        try {
            List<Screen> screens = screenRepository.findAllActive();
            log.info("Found {} total screens", screens.size());
            log.info("getAllScreens END");
            return screens.stream()
                    .map(screen -> modelMapper.map(screen, ScreenDTO.class))
                    .collect(Collectors.toList());
        } catch (Exception e) {
            log.error("getAllScreens ERROR", e);
            throw e;
        }
    }

    @Override
    public PaginationResponse<ScreenDTO> getAllScreensPaginated(int page, int size) {
        log.info("getAllScreensPaginated STARTED: page={}, size={}", page, size);
        try {
            Pageable pageable = PageRequest.of(page, size);
            Page<Screen> screenPage = screenRepository.findAllActivePaginated(pageable);

            List<ScreenDTO> screenDTOs = screenPage.getContent().stream()
                    .map(screen -> modelMapper.map(screen, ScreenDTO.class))
                    .collect(Collectors.toList());

            PaginationResponse<ScreenDTO> response = PaginationResponse.success(
                    "Screens retrieved successfully",
                    screenDTOs,
                    screenPage.getNumber(),
                    screenPage.getSize(),
                    screenPage.getTotalPages(),
                    screenPage.getTotalElements(),
                    screenPage.hasNext(),
                    screenPage.hasPrevious()
            );

            log.info("getAllScreensPaginated END: found {} screens", screenDTOs.size());
            return response;
        } catch (Exception e) {
            log.error("getAllScreensPaginated ERROR", e);
            throw e;
        }
    }

    @Override
    public long getScreenCountByTheatre(String theatreId) {
        log.info("getScreenCountByTheatre STARTED: theatreId={}", theatreId);
        try {
            long count = screenRepository.countByTheatreIdActive(theatreId);
            log.info("Screen count for theatre {}: {}", theatreId, count);
            log.info("getScreenCountByTheatre END");
            return count;
        } catch (Exception e) {
            log.error("getScreenCountByTheatre ERROR", e);
            throw e;
        }
    }
}
