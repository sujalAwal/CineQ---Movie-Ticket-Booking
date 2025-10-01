package com.awal.cineq.theater.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import com.awal.cineq.theater.model.Seat;

import java.util.List;

@Repository
public interface SeatRepository extends JpaRepository<Seat, Long> {
    
    List<Seat> findByTheaterIdAndIsActiveTrue(Long theaterId);
    
    List<Seat> findByTheaterIdAndSeatType(Long theaterId, String seatType);
    
    @Query("SELECT s FROM Seat s WHERE s.theater.id = :theaterId AND s.rowNumber = :rowNumber AND s.isActive = true ORDER BY s.seatNumber")
    List<Seat> findByTheaterAndRow(@Param("theaterId") Long theaterId, @Param("rowNumber") String rowNumber);
    
    @Query("SELECT DISTINCT s.rowNumber FROM Seat s WHERE s.theater.id = :theaterId AND s.isActive = true ORDER BY s.rowNumber")
    List<String> findDistinctRowsByTheater(@Param("theaterId") Long theaterId);
    
    @Query("SELECT COUNT(s) FROM Seat s WHERE s.theater.id = :theaterId AND s.isActive = true")
    Long countActiveSeatsInTheater(@Param("theaterId") Long theaterId);
}