package com.awal.cineq.movie.controller;

import com.awal.cineq.dto.ApiResponse;
import com.awal.cineq.dto.PaginationResponse;
import com.awal.cineq.movie.dto.MovieDTO;
import com.awal.cineq.movie.dto.request.MovieRequest;
import com.awal.cineq.movie.dto.request.BulkMovieStatusRequest;
import com.awal.cineq.movie.service.MovieService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * REST Controller for Movie management
 * All IDs are MongoDB ObjectIds stored as String
 */
@Slf4j
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/movies")
public class MovieController {
    
    private final MovieService movieService;
    
    @GetMapping
    public PaginationResponse<MovieDTO> getAllMovies(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(defaultValue = "createdAt") String sortBy,
            @RequestParam(defaultValue = "desc") String sortDirection) {

        log.info("getAllMovies STARTED: page={}, size={}, sortBy={}, sortDirection={}",
                 page, size, sortBy, sortDirection);
        return movieService.getAllMovies(page, size, sortBy, sortDirection);
    }
    
    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<MovieDTO>> getMovieById(
            @PathVariable String id,
            HttpServletRequest request) {

        log.info("getMovieById STARTED: id={}", id);
        MovieDTO movie = movieService.getMovieById(id);
        ApiResponse<MovieDTO> response = ApiResponse.success("Movie fetched successfully", movie);
        response.setPath(request.getRequestURI());
        return ResponseEntity.ok(response);
    }
    
    @PostMapping
    public ResponseEntity<ApiResponse<MovieDTO>> createMovie(
            @Valid @RequestBody MovieRequest movieRequest,
            HttpServletRequest request) {

        log.info("createMovie STARTED: title={}", movieRequest.getTitle());
        MovieDTO created = movieService.createMovie(movieRequest);
        ApiResponse<MovieDTO> response = ApiResponse.success("Movie created successfully", created);
        response.setPath(request.getRequestURI());
        return ResponseEntity.status(201).body(response);
    }
    
    @PutMapping("/{id}")
    public ResponseEntity<ApiResponse<MovieDTO>> updateMovie(
            @PathVariable String id,
            @Valid @RequestBody MovieRequest movieRequest,
            HttpServletRequest request) {

        log.info("updateMovie STARTED: id={}, title={}", id, movieRequest.getTitle());
        MovieDTO updated = movieService.updateMovie(id, movieRequest);
        ApiResponse<MovieDTO> response = ApiResponse.success("Movie updated successfully", updated);
        response.setPath(request.getRequestURI());
        return ResponseEntity.ok(response);
    }
    
    @DeleteMapping("/{id}")
    public ResponseEntity<ApiResponse<Object>> deleteMovie(
            @PathVariable String id,
            HttpServletRequest request) {
        
        log.info("deleteMovie STARTED: id={}", id);
        movieService.deleteMovie(id);
        ApiResponse<Object> response = ApiResponse.success("Movie deleted successfully", null);
        response.setPath(request.getRequestURI());
        return ResponseEntity.ok(response);
    }
    
    @GetMapping("/search/{title}")
    public ResponseEntity<ApiResponse<List<MovieDTO>>> searchByTitle(
            @PathVariable String title,
            HttpServletRequest request) {
        
        log.info("searchByTitle STARTED: title={}", title);
        List<MovieDTO> results = movieService.searchByTitle(title);
        ApiResponse<List<MovieDTO>> response = ApiResponse.success("Movies found successfully", results);
        response.setPath(request.getRequestURI());
        return ResponseEntity.ok(response);
    }
    
    @GetMapping("/active/list")
    public ResponseEntity<ApiResponse<List<MovieDTO>>> getActiveMovies(HttpServletRequest request) {
        
        log.info("getActiveMovies STARTED");
        List<MovieDTO> movies = movieService.getActiveMovies();
        ApiResponse<List<MovieDTO>> response = ApiResponse.success("Active movies fetched successfully", movies);
        response.setPath(request.getRequestURI());
        return ResponseEntity.ok(response);
    }
    
    @GetMapping("/showing/list")
    public ResponseEntity<ApiResponse<List<MovieDTO>>> getCurrentlyShowingMovies(HttpServletRequest request) {
        
        log.info("getCurrentlyShowingMovies STARTED");
        List<MovieDTO> movies = movieService.getCurrentlyShowingMovies();
        ApiResponse<List<MovieDTO>> response = ApiResponse.success("Currently showing movies fetched successfully", movies);
        response.setPath(request.getRequestURI());
        return ResponseEntity.ok(response);
    }
    
    @GetMapping("/upcoming/list")
    public ResponseEntity<ApiResponse<List<MovieDTO>>> getUpcomingMovies(HttpServletRequest request) {
        
        log.info("getUpcomingMovies STARTED");
        List<MovieDTO> movies = movieService.getUpcomingMovies();
        ApiResponse<List<MovieDTO>> response = ApiResponse.success("Upcoming movies fetched successfully", movies);
        response.setPath(request.getRequestURI());
        return ResponseEntity.ok(response);
    }
    
    @GetMapping("/language/{language}")
    public ResponseEntity<ApiResponse<List<MovieDTO>>> findByLanguage(
            @PathVariable String language,
            HttpServletRequest request) {
        
        log.info("findByLanguage STARTED: language={}", language);
        List<MovieDTO> movies = movieService.findByLanguage(language);
        ApiResponse<List<MovieDTO>> response = ApiResponse.success("Movies found successfully", movies);
        response.setPath(request.getRequestURI());
        return ResponseEntity.ok(response);
    }
    
    @GetMapping("/rating/{rating}")
    public ResponseEntity<ApiResponse<List<MovieDTO>>> findByRating(
            @PathVariable String rating,
            HttpServletRequest request) {
        
        log.info("findByRating STARTED: rating={}", rating);
        List<MovieDTO> movies = movieService.findByRating(rating);
        ApiResponse<List<MovieDTO>> response = ApiResponse.success("Movies found successfully", movies);
        response.setPath(request.getRequestURI());
        return ResponseEntity.ok(response);
    }
    
    @GetMapping("/genre/{genreId}")
    public ResponseEntity<ApiResponse<List<MovieDTO>>> findByGenreId(
            @PathVariable String genreId,
            HttpServletRequest request) {
        
        log.info("findByGenreId STARTED: genreId={}", genreId);
        List<MovieDTO> movies = movieService.findByGenreId(genreId);
        ApiResponse<List<MovieDTO>> response = ApiResponse.success("Movies found successfully", movies);
        response.setPath(request.getRequestURI());
        return ResponseEntity.ok(response);
    }
    
    @PatchMapping("/{id}/status")
    public ResponseEntity<ApiResponse<MovieDTO>> toggleMovieStatus(
            @PathVariable String id,
            @RequestParam boolean isActive,
            HttpServletRequest request) {
        
        log.info("toggleMovieStatus STARTED: id={}, isActive={}", id, isActive);
        MovieDTO movie = movieService.toggleMovieStatus(id, isActive);
        ApiResponse<MovieDTO> response = ApiResponse.success("Movie status updated successfully", movie);
        response.setPath(request.getRequestURI());
        return ResponseEntity.ok(response);
    }
    
    @PostMapping("/bulk-status")
    public ResponseEntity<ApiResponse<Object>> bulkUpdateMovieStatus(
            @Valid @RequestBody BulkMovieStatusRequest request,
            HttpServletRequest httpRequest) {
        
        log.info("bulkUpdateMovieStatus STARTED: count={}, isActive={}", 
                 request.getIds().size(), request.isActive());
        movieService.bulkUpdateMovieStatus(request.getIds(), request.isActive());
        ApiResponse<Object> response = ApiResponse.success("Movies updated successfully", null);
        response.setPath(httpRequest.getRequestURI());
        return ResponseEntity.ok(response);
    }
}
