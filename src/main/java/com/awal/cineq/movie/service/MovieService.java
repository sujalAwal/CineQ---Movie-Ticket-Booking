package com.awal.cineq.movie.service;

import com.awal.cineq.dto.PaginationResponse;
import com.awal.cineq.movie.dto.MovieDTO;
import com.awal.cineq.movie.dto.request.MovieRequest;

import java.time.LocalDate;
import java.util.List;

/**
 * Service interface for Movie operations
 * All methods use String IDs (MongoDB ObjectId)
 */
public interface MovieService {
    
    /**
     * Get all movies with pagination (excludes soft-deleted)
     */
    PaginationResponse<MovieDTO> getAllMovies(int page, int size, String sortBy, String sortDirection);
    
    /**
     * Get movie by ID (excludes soft-deleted)
     */
    MovieDTO getMovieById(String id);
    
    /**
     * Create new movie
     */
    MovieDTO createMovie(MovieRequest request);
    
    /**
     * Update existing movie
     */
    MovieDTO updateMovie(String id, MovieRequest request);
    
    /**
     * Soft delete movie (sets deletedAt timestamp)
     */
    void deleteMovie(String id);
    
    /**
     * Search movies by title (excludes soft-deleted)
     */
    List<MovieDTO> searchByTitle(String title);
    
    /**
     * Get active movies (excludes soft-deleted)
     */
    List<MovieDTO> getActiveMovies();
    
    /**
     * Get currently showing movies (release_date <= today, excludes soft-deleted)
     */
    List<MovieDTO> getCurrentlyShowingMovies();
    
    /**
     * Get upcoming movies (release_date > today, excludes soft-deleted)
     */
    List<MovieDTO> getUpcomingMovies();
    
    /**
     * Find movies by language (excludes soft-deleted)
     */
    List<MovieDTO> findByLanguage(String language);
    
    /**
     * Find movies by rating (excludes soft-deleted)
     */
    List<MovieDTO> findByRating(String rating);
    
    /**
     * Find movies by genre ID (excludes soft-deleted)
     */
    List<MovieDTO> findByGenreId(String genreId);
    
    /**
     * Toggle movie active status
     */
    MovieDTO toggleMovieStatus(String id, boolean isActive);
    
    /**
     * Bulk update movie active status
     */
    void bulkUpdateMovieStatus(List<String> ids, boolean isActive);
}
