package com.awal.cineq.booking.repository;

import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.data.mongodb.repository.Query;
import org.springframework.stereotype.Repository;

import com.awal.cineq.booking.model.Booking;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

/**
 * MongoDB Repository for Booking
 * BookingDetails are embedded in Booking document (no separate repository needed)
 */
@Repository
public interface BookingRepository extends MongoRepository<Booking, String> {

    // Find by booking reference
    @Query("{ 'bookingReference': ?0 }")
    Optional<Booking> findByBookingReference(String bookingReference);

    // Find bookings by user ID
    @Query("{ 'userId': ?0 }")
    List<Booking> findByUserId(String userId);

    // Find bookings by customer ID
    @Query("{ 'customerId': ?0 }")
    List<Booking> findByCustomerId(String customerId);

    // Find bookings by showtime ID
    @Query("{ 'showtimeId': ?0 }")
    List<Booking> findByShowtimeId(String showtimeId);

    // Find confirmed bookings by showtime ID (for seat availability)
    @Query("{ 'showtimeId': ?0, 'bookingStatus': 'CONFIRMED' }")
    List<Booking> findConfirmedBookingsByShowtimeId(String showtimeId);

    // Find bookings by status
    @Query("{ 'bookingStatus': ?0 }")
    List<Booking> findByBookingStatus(String bookingStatus);

    // Find bookings by date range
    @Query("{ 'bookingDate': { $gte: ?0, $lte: ?1 } }")
    List<Booking> findByBookingDateBetween(LocalDateTime startDate, LocalDateTime endDate);
}