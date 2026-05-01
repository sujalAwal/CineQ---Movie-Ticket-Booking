package com.awal.cineq.genre.controller;

import com.awal.cineq.dto.ApiResponse;
import com.awal.cineq.dto.PaginationResponse;
import com.awal.cineq.genre.dto.GenreDTO;
import com.awal.cineq.genre.dto.request.GenrePageRequest;
import com.awal.cineq.genre.dto.request.GenreRequestDto;
import com.awal.cineq.genre.service.GenreService;
import com.awal.cineq.genre.dto.request.BulkGenreStatusUpdateRequest;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

/**
 * REST Controller for Genre endpoints
 * Provides CRUD operations for genres
 */
@RestController
@RequiredArgsConstructor
@RequestMapping("genre")
@Slf4j
public class GenreController {

    private final GenreService genreService;

    @GetMapping(path = {"", "/"})
    public PaginationResponse<GenreDTO> getAllGenres(@Valid GenrePageRequest genreRequest) {
        log.info("getAllGenres STARTED");
        try {
            PaginationResponse<GenreDTO> response = genreService.getGenre(genreRequest);
            log.info("getAllGenres END");
            return response;
        } catch (Exception e) {
            log.error("getAllGenres ERROR", e);
            throw e;
        }
    }

    @PostMapping(path = {"", "/"})
    public ResponseEntity<ApiResponse<GenreDTO>> createGenre(
            @RequestBody GenreRequestDto genreRequestDto,
            HttpServletRequest request) {
        log.info("createGenre STARTED");
        try {
            GenreDTO created = genreService.createGenre(genreRequestDto);
            ApiResponse<GenreDTO> response = ApiResponse.success("Genre created successfully", created);
            response.setPath(request.getRequestURI());
            log.info("createGenre END");
            return ResponseEntity.status(201).body(response);
        } catch (Exception e) {
            log.error("createGenre ERROR", e);
            throw e;
        }
    }

    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<GenreDTO>> getGenreById(
            @PathVariable String id,
            HttpServletRequest request) {
        log.info("getGenreById STARTED: id={}", id);
        try {
            GenreDTO genre = genreService.getGenreById(id);
            ApiResponse<GenreDTO> response = ApiResponse.success("Genre fetched successfully", genre);
            response.setPath(request.getRequestURI());
            log.info("getGenreById END");
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            log.error("getGenreById ERROR", e);
            throw e;
        }
    }

    @PutMapping("/{id}")
    public ResponseEntity<ApiResponse<GenreDTO>> updateGenre(
            @PathVariable String id,
            @RequestBody GenreRequestDto genreRequestDto,
            HttpServletRequest request) {
        log.info("updateGenre STARTED: id={}", id);
        try {
            GenreDTO updated = genreService.updateGenre(id, genreRequestDto);
            ApiResponse<GenreDTO> response = ApiResponse.success("Genre updated successfully", updated);
            response.setPath(request.getRequestURI());
            log.info("updateGenre END");
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            log.error("updateGenre ERROR", e);
            throw e;
        }
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<ApiResponse<Void>> deleteGenre(
            @PathVariable String id,
            HttpServletRequest request) {
        log.info("deleteGenre STARTED: id={}", id);
        try {
            genreService.deleteGenre(id);
            ApiResponse<Void> response = ApiResponse.success("Genre deleted successfully", null);
            response.setPath(request.getRequestURI());
            log.info("deleteGenre END");
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            log.error("deleteGenre ERROR", e);
            throw e;
        }
    }

    @PostMapping("/bulk-enable")
    public ResponseEntity<ApiResponse<Void>> bulkEnableGenres(
            @RequestBody BulkGenreStatusUpdateRequest bulkRequest,
            HttpServletRequest request) {
        log.info("bulkEnableGenres STARTED");
        try {
            genreService.bulkEnableGenres(bulkRequest.getIds(), true);
            ApiResponse<Void> response = ApiResponse.success("Genres enabled successfully", null);
            response.setPath(request.getRequestURI());
            log.info("bulkEnableGenres END");
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            log.error("bulkEnableGenres ERROR", e);
            throw e;
        }
    }

    @PostMapping("/bulk-disable")
    public ResponseEntity<ApiResponse<Void>> bulkDisableGenres(
            @RequestBody BulkGenreStatusUpdateRequest bulkRequest,
            HttpServletRequest request) {
        log.info("bulkDisableGenres STARTED");
        try {
            genreService.bulkEnableGenres(bulkRequest.getIds(), false);
            ApiResponse<Void> response = ApiResponse.success("Genres disabled successfully", null);
            response.setPath(request.getRequestURI());
            log.info("bulkDisableGenres END");
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            log.error("bulkDisableGenres ERROR", e);
            throw e;
        }
    }
}
