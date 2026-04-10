package com.awal.cineq.publicapi.theatre.service.impl;

import com.awal.cineq.dto.ApiResponse;
import com.awal.cineq.dto.PaginationResponse;
import com.awal.cineq.exception.ResourceNotFoundException;
import com.awal.cineq.frontend.theatres.dto.TheatreDTO;
import com.awal.cineq.frontend.theatres.dto.request.TheatrePageRequest;
import com.awal.cineq.frontend.theatres.repository.FrontendTheatreRepository;
import com.awal.cineq.frontend.theatres.service.FrontendTheatreService;
import com.awal.cineq.publicapi.theatre.service.PublicTheatreService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;

import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class PublicTheatreServiceImpl implements PublicTheatreService {

    private final FrontendTheatreService frontendTheatreService;
    private final FrontendTheatreRepository theatreRepository;

    @Override
    public PaginationResponse<TheatreDTO> getAllTheatres(TheatrePageRequest pageRequest) {
        log.info("getAllTheatres STARTED: pageRequest={}", pageRequest);
        try {
            PaginationResponse<TheatreDTO> response = frontendTheatreService.getAllTheatres(pageRequest);
            log.info("getAllTheatres END – returned {} theatres", response.getData() != null ? response.getData().size() : 0);
            return response;
        } catch (Exception e) {
            log.error("getAllTheatres ERROR", e);
            throw e;
        }
    }

    @Override
    public ApiResponse<TheatreDTO> getTheatreById(String id) {
        log.info("getTheatreById STARTED: id={}", id);
        try {
            TheatreDTO theatre = frontendTheatreService.getTheatreById(id);
            if (theatre == null) {
                throw new ResourceNotFoundException("Theatre not found: " + id);
            }
            log.info("getTheatreById END: id={}", id);
            return ApiResponse.success("Theatre fetched successfully", theatre);
        } catch (ResourceNotFoundException e) {
            log.error("getTheatreById ERROR – not found: id={}", id);
            throw e;
        } catch (Exception e) {
            log.error("getTheatreById ERROR: id={}", id, e);
            throw e;
        }
    }

    @Override
    public PaginationResponse<TheatreDTO> getTheatresByCity(String city, int page, int size) {
        log.info("getTheatresByCity STARTED: city={}, page={}, size={}", city, page, size);
        try {
            PaginationResponse<TheatreDTO> response = frontendTheatreService.getTheatresByCity(city, page, size);
            log.info("getTheatresByCity END – city={}, returned {} theatres", city, response.getData() != null ? response.getData().size() : 0);
            return response;
        } catch (Exception e) {
            log.error("getTheatresByCity ERROR: city={}", city, e);
            throw e;
        }
    }

    @Override
    public PaginationResponse<TheatreDTO> getTheatresByState(String state, int page, int size) {
        log.info("getTheatresByState STARTED: state={}, page={}, size={}", state, page, size);
        try {
            PaginationResponse<TheatreDTO> response = frontendTheatreService.getTheatresByState(state, page, size);
            log.info("getTheatresByState END – state={}, returned {} theatres", state, response.getData() != null ? response.getData().size() : 0);
            return response;
        } catch (Exception e) {
            log.error("getTheatresByState ERROR: state={}", state, e);
            throw e;
        }
    }

    @Override
    public List<String> getCities() {
        log.info("getCities STARTED");
        try {
            Page<Map> allTheatres = theatreRepository.findAllActive(PageRequest.of(0, 1000));

            List<String> cities = allTheatres.getContent().stream()
                    .map(doc -> (String) doc.get("city"))
                    .filter(Objects::nonNull)
                    .filter(city -> !city.isBlank())
                    .distinct()
                    .sorted(Comparator.naturalOrder())
                    .collect(Collectors.toList());

            log.info("getCities END – returned {} distinct cities", cities.size());
            return cities;
        } catch (Exception e) {
            log.error("getCities ERROR", e);
            throw e;
        }
    }
}
