package com.awal.cineq.movie.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import com.awal.cineq.movie.model.Movie;

import java.time.LocalDate;
import java.util.List;

@Repository
public interface MovieRepository extends JpaRepository<Movie, Long> {
    
    List<Movie> findByIsActiveTrue();
    
    List<Movie> findByTitleContainingIgnoreCase(String title);
    
    List<Movie> findByReleaseDateBetween(LocalDate startDate, LocalDate endDate);
    
    List<Movie> findByLanguage(String language);
    
    List<Movie> findByRating(String rating);
    
    @Query("SELECT m FROM Movie m JOIN m.genres g WHERE g.name = :genreName AND m.isActive = true")
    List<Movie> findByGenreName(@Param("genreName") String genreName);
    
    @Query("SELECT m FROM Movie m WHERE m.releaseDate <= :currentDate AND m.isActive = true ORDER BY m.releaseDate DESC")
    List<Movie> findCurrentlyShowingMovies(@Param("currentDate") LocalDate currentDate);
    
    @Query("SELECT m FROM Movie m WHERE m.releaseDate > :currentDate AND m.isActive = true ORDER BY m.releaseDate ASC")
    List<Movie> findUpcomingMovies(@Param("currentDate") LocalDate currentDate);
    
    @Query("SELECT DISTINCT m.language FROM Movie m WHERE m.isActive = true ORDER BY m.language")
    List<String> findDistinctLanguages();
    
    @Query("SELECT DISTINCT m.rating FROM Movie m WHERE m.isActive = true ORDER BY m.rating")
    List<String> findDistinctRatings();
}