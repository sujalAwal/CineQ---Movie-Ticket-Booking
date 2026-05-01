package com.awal.cineq.theater.repository;

import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.data.mongodb.repository.Query;
import org.springframework.stereotype.Repository;

import com.awal.cineq.theater.model.Showtime;

import java.time.LocalDateTime;
import java.util.List;

/**
 * MongoDB Repository for Showtime
 * Includes soft-delete queries (deletedAt = null)
 */
@Repository
public interface ShowtimeRepository extends MongoRepository<Showtime, String> {

    @Query("{ 'isActive': true, 'deletedAt': null }")
    List<Showtime> findByIsActiveTrue();
    
    @Query("{ 'movieId': ?0, 'isActive': true, 'deletedAt': null }")
    List<Showtime> findByMovieIdAndIsActiveTrue(String movieId);

    @Query("{ 'theaterId': ?0, 'isActive': true, 'deletedAt': null }")
    List<Showtime> findByTheaterIdAndIsActiveTrue(String theaterId);

    @Query(value = "{ 'movieId': ?0, 'theaterId': ?1, 'isActive': true, 'deletedAt': null }", sort = "{ 'showDateTime': 1 }")
    List<Showtime> findByMovieAndTheater(String movieId, String theaterId);

    @Query(value = "{ 'showDateTime': { $gte: ?0, $lte: ?1 }, 'isActive': true, 'deletedAt': null }", sort = "{ 'showDateTime': 1 }")
    List<Showtime> findByShowDateTimeBetween(LocalDateTime startDateTime, LocalDateTime endDateTime);

    @Query(value = "{ 'movieId': ?0, 'showDateTime': { $gte: ?1 }, 'isActive': true, 'deletedAt': null }", sort = "{ 'showDateTime': 1 }")
    List<Showtime> findUpcomingShowtimesByMovie(String movieId, LocalDateTime currentDateTime);

    @Query(value = "{ 'theaterId': ?0, 'showDateTime': { $gte: ?1 }, 'isActive': true, 'deletedAt': null }", sort = "{ 'showDateTime': 1 }")
    List<Showtime> findUpcomingShowtimesByTheater(String theaterId, LocalDateTime currentDateTime);

    @Query(value = "{ 'availableSeats': { $gt: 0 }, 'showDateTime': { $gte: ?0 }, 'isActive': true, 'deletedAt': null }", sort = "{ 'showDateTime': 1 }")
    List<Showtime> findAvailableShowtimes(LocalDateTime currentDateTime);
}