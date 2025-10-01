package com.awal.cineq.theater.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import com.awal.cineq.theater.model.Showtime;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface ShowtimeRepository extends JpaRepository<Showtime, Long> {
    
    List<Showtime> findByIsActiveTrue();
    
    List<Showtime> findByMovieIdAndIsActiveTrue(Long movieId);
    
    List<Showtime> findByTheaterIdAndIsActiveTrue(Long theaterId);
    
    @Query("SELECT s FROM Showtime s WHERE s.movie.id = :movieId AND s.theater.id = :theaterId AND s.isActive = true ORDER BY s.showDateTime")
    List<Showtime> findByMovieAndTheater(@Param("movieId") Long movieId, @Param("theaterId") Long theaterId);
    
    @Query("SELECT s FROM Showtime s WHERE s.showDateTime BETWEEN :startDateTime AND :endDateTime AND s.isActive = true ORDER BY s.showDateTime")
    List<Showtime> findByShowDateTimeBetween(@Param("startDateTime") LocalDateTime startDateTime, @Param("endDateTime") LocalDateTime endDateTime);
    
    @Query("SELECT s FROM Showtime s WHERE s.movie.id = :movieId AND s.showDateTime >= :currentDateTime AND s.isActive = true ORDER BY s.showDateTime")
    List<Showtime> findUpcomingShowtimesByMovie(@Param("movieId") Long movieId, @Param("currentDateTime") LocalDateTime currentDateTime);
    
    @Query("SELECT s FROM Showtime s WHERE s.theater.id = :theaterId AND s.showDateTime >= :currentDateTime AND s.isActive = true ORDER BY s.showDateTime")
    List<Showtime> findUpcomingShowtimesByTheater(@Param("theaterId") Long theaterId, @Param("currentDateTime") LocalDateTime currentDateTime);
    
    @Query("SELECT s FROM Showtime s WHERE s.availableSeats > 0 AND s.showDateTime >= :currentDateTime AND s.isActive = true ORDER BY s.showDateTime")
    List<Showtime> findAvailableShowtimes(@Param("currentDateTime") LocalDateTime currentDateTime);
}