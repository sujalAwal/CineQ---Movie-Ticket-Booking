package com.awal.cineq.theater.repository;

import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.data.mongodb.repository.Query;
import org.springframework.stereotype.Repository;

import com.awal.cineq.theater.model.Seat;

import java.util.List;

/**
 * MongoDB Repository for Seat
 * Includes soft-delete queries (deletedAt = null)
 */
@Repository
public interface SeatRepository extends MongoRepository<Seat, String> {

    @Query("{ 'theaterId': ?0, 'isActive': true, 'deletedAt': null }")
    List<Seat> findByTheaterIdAndIsActiveTrue(String theaterId);

    @Query("{ 'theaterId': ?0, 'seatType': ?1, 'deletedAt': null }")
    List<Seat> findByTheaterIdAndSeatType(String theaterId, String seatType);

    @Query(value = "{ 'theaterId': ?0, 'rowNumber': ?1, 'isActive': true, 'deletedAt': null }", sort = "{ 'seatNumber': 1 }")
    List<Seat> findByTheaterAndRow(String theaterId, String rowNumber);

    @Query(value = "{ 'theaterId': ?0, 'isActive': true, 'deletedAt': null }", fields = "{ 'rowNumber': 1 }")
    List<Seat> findDistinctRowsByTheaterRaw(String theaterId);

    @Query(value = "{ 'theaterId': ?0, 'isActive': true, 'deletedAt': null }", count = true)
    Long countActiveSeatsInTheater(String theaterId);
}