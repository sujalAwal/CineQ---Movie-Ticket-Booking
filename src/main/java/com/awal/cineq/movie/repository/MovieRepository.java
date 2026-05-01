package com.awal.cineq.movie.repository;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.data.mongodb.repository.Query;
import org.springframework.stereotype.Repository;

import com.awal.cineq.movie.model.Movie;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

/**
 * MongoDB Repository for Movie documents
 * All queries include soft-delete filter: { 'deleted_at': null }
 */
@Repository
public interface MovieRepository extends MongoRepository<Movie, String> {
    
    // Find active movies (not soft-deleted)
    @Query("{ 'is_active': true, 'deleted_at': null }")
    List<Movie> findByIsActiveTrue();
    
    // Find movies by title (case-insensitive, soft-delete excluded)
    @Query("{ 'title': { $regex: ?0, $options: 'i' }, 'deleted_at': null }")
    List<Movie> findByTitleContainingIgnoreCase(String title);
    
    // Find movies with pagination
    @Query("{ 'deleted_at': null }")
    Page<Movie> findAll(Pageable pageable);
    
    // Find movies by release date range (soft-delete excluded)
    @Query("{ 'release_date': { $gte: ?0, $lte: ?1 }, 'deleted_at': null }")
    List<Movie> findByReleaseDateBetween(LocalDate startDate, LocalDate endDate);
    
    // Find movies by language (soft-delete excluded)
    @Query("{ 'language': ?0, 'deleted_at': null }")
    List<Movie> findByLanguage(String language);
    
    // Find movies by rating (soft-delete excluded)
    @Query("{ 'rating': ?0, 'deleted_at': null }")
    List<Movie> findByRating(String rating);
    
    // Find movies by genre ID (soft-delete excluded)
    @Query("{ 'genres': ?0, 'is_active': true, 'deleted_at': null }")
    List<Movie> findByGenreId(String genreId);
    
    // Find currently showing movies (release_date <= today, soft-delete excluded)
    @Query("{ 'release_date': { $lte: ?0 }, 'is_active': true, 'deleted_at': null }")
    List<Movie> findCurrentlyShowingMovies(LocalDate currentDate);
    
    // Find upcoming movies (release_date > today, soft-delete excluded)
    @Query("{ 'release_date': { $gt: ?0 }, 'is_active': true, 'deleted_at': null }")
    List<Movie> findUpcomingMovies(LocalDate currentDate);
    
    // Find movie by ID (soft-delete excluded)
    @Query("{ '_id': ?0, 'deleted_at': null }")
    Optional<Movie> findById(String id);
}