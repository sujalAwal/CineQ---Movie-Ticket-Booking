package com.awal.cineq.booking.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import com.awal.cineq.booking.model.BookingDetail;

import java.util.List;

@Repository
public interface BookingDetailRepository extends JpaRepository<BookingDetail, Long> {
    
    List<BookingDetail> findByBookingId(Long bookingId);
    
    @Query("SELECT bd FROM BookingDetail bd WHERE bd.booking.showtime.id = :showtimeId")
    List<BookingDetail> findByShowtimeId(@Param("showtimeId") Long showtimeId);
    
    @Query("SELECT bd.seat.id FROM BookingDetail bd WHERE bd.booking.showtime.id = :showtimeId AND bd.booking.bookingStatus = 'CONFIRMED'")
    List<Long> findBookedSeatIdsByShowtime(@Param("showtimeId") Long showtimeId);
    
    @Query("SELECT COUNT(bd) FROM BookingDetail bd WHERE bd.booking.showtime.id = :showtimeId AND bd.booking.bookingStatus = 'CONFIRMED'")
    Long countBookedSeatsByShowtime(@Param("showtimeId") Long showtimeId);
}