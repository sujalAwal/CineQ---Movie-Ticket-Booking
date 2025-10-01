package com.awal.cineq.genre.controller;

import com.awal.cineq.dto.ApiResponse;
import com.awal.cineq.genre.dto.GenreDTO;
import com.awal.cineq.genre.dto.GenreRequestDto;
import com.awal.cineq.genre.service.GenreService;
import com.awal.cineq.genre.dto.BulkGenreStatusUpdateRequest;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@RestController
@RequiredArgsConstructor
@RequestMapping("genre")
public class GenreController {

    private final GenreService genreService;
    private static final Logger log = LoggerFactory.getLogger(GenreController.class);

    @GetMapping(path = {"", "/"})
    public ResponseEntity<ApiResponse<List<GenreDTO>>> getAllGenres(HttpServletRequest request) {
        log.info("getAllGenres STARTED");
        try {
            ApiResponse<List<GenreDTO>> response = ApiResponse.success("Genres fetched successfully", genreService.getGenre());
            response.setPath(request.getRequestURI());
            log.debug("getAllGenres response: {}", response);
            log.info("getAllGenres END");
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            log.error("getAllGenres ERROR", e);
            throw e;
        }
    }

    @PostMapping(path = {"", "/"})
    public ResponseEntity<ApiResponse<GenreDTO>> createGenre(@RequestBody GenreRequestDto genreRequestDto, HttpServletRequest request) {
        log.info("createGenre STARTED");
        log.debug("createGenre input: {}", genreRequestDto);
        try {
            ApiResponse<GenreDTO> response = ApiResponse.success("Genre created successfully", genreService.createGenre(genreRequestDto));
            response.setPath(request.getRequestURI());
            log.debug("createGenre response: {}", response);
            log.info("createGenre END");
            return ResponseEntity.status(201).body(response);
        } catch (Exception e) {
            log.error("createGenre ERROR", e);
            throw e;
        }
    }

    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<GenreDTO>> getGenreById(@PathVariable UUID id, HttpServletRequest request) {
        log.info("getGenreById STARTED");
        log.debug("getGenreById input: {}", id);
        try {
            ApiResponse<GenreDTO> response = ApiResponse.success("Genre fetched successfully", genreService.getGenreById(id));
            response.setPath(request.getRequestURI());
            log.debug("getGenreById response: {}", response);
            log.info("getGenreById END");
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            log.error("getGenreById ERROR", e);
            throw e;
        }
    }

    @PutMapping("/{id}")
    public ResponseEntity<ApiResponse<GenreDTO>> updateGenre(@PathVariable UUID id, @RequestBody GenreRequestDto genreRequestDto, HttpServletRequest request) {
        log.info("updateGenre STARTED");
        log.debug("updateGenre input: id={}, genreRequestDto={}", id, genreRequestDto);
        try {
            ApiResponse<GenreDTO> response = ApiResponse.success("Genre updated successfully", genreService.updateGenre(id, genreRequestDto));
            response.setPath(request.getRequestURI());
            log.debug("updateGenre response: {}", response);
            log.info("updateGenre END");
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            log.error("updateGenre ERROR", e);
            throw e;
        }
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<ApiResponse<Void>> deleteGenre(@PathVariable UUID id, HttpServletRequest request) {
        log.info("deleteGenre STARTED");
        log.debug("deleteGenre input: {}", id);
        try {
            genreService.deleteGenre(id);
            ApiResponse<Void> response = ApiResponse.success("Genre deleted successfully", null);
            response.setPath(request.getRequestURI());
            log.debug("deleteGenre response: {}", response);
            log.info("deleteGenre END");
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            log.error("deleteGenre ERROR", e);
            throw e;
        }
    }

    @PostMapping("/bulk-enable")
    public ResponseEntity<ApiResponse<Void>> bulkEnableGenres(@RequestBody BulkGenreStatusUpdateRequest request, HttpServletRequest httpRequest) {
        log.info("bulkEnableGenres STARTED");
        log.debug("bulkEnableGenres input: {}", request);
        try {
            genreService.bulkEnableGenres(request.getIds(), true);
            ApiResponse<Void> response = ApiResponse.success("Genres enabled successfully", null);
            response.setPath(httpRequest.getRequestURI());
            log.debug("bulkEnableGenres response: {}", response);
            log.info("bulkEnableGenres END");
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            log.error("bulkEnableGenres ERROR", e);
            throw e;
        }
    }

    @PostMapping("/bulk-disable")
    public ResponseEntity<ApiResponse<Void>> bulkDisableGenres(@RequestBody BulkGenreStatusUpdateRequest request, HttpServletRequest httpRequest) {
        log.info("bulkDisableGenres STARTED");
        log.debug("bulkDisableGenres input: {}", request);
        try {
            genreService.bulkEnableGenres(request.getIds(), false);
            ApiResponse<Void> response = ApiResponse.success("Genres disabled successfully", null);
            response.setPath(httpRequest.getRequestURI());
            log.debug("bulkDisableGenres response: {}", response);
            log.info("bulkDisableGenres END");
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            log.error("bulkDisableGenres ERROR", e);
            throw e;
        }
    }
}
