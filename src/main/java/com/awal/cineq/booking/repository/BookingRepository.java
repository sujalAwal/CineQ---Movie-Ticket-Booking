package com.awal.cineq.booking.repository;

import com.awal.cineq.booking.model.Booking;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.data.mongodb.repository.Query;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface BookingRepository extends MongoRepository<Booking, String> {

    @Query("{ 'bookingReference': ?0, 'deletedAt': null }")
    Optional<Booking> findByBookingReference(String bookingReference);

    @Query("{ 'customerId': ?0, 'deletedAt': null }")
    List<Booking> findByCustomerId(String customerId);

    @Query("{ 'showtimeId': ?0, 'deletedAt': null }")
    List<Booking> findByShowtimeId(String showtimeId);

    /**
     * Seat conflict check: find active bookings for this showtime that already
     * hold any of the requested seat names in Reserved(3) or Booked(2) state.
     * Used as the application-level guard before the DB unique index catches races.
     */
    @Query("{ 'showtimeId': ?0, 'deletedAt': null, 'seatStatusCode': { $in: [2, 3] }, 'bookingDetails.seatName': { $in: ?1 } }")
    List<Booking> findConflictingBookings(String showtimeId, List<String> seatNames);

    /**
     * Find PENDING (Reserved=3) bookings whose expiry has passed — used by cleanup scheduler.
     */
    @Query("{ 'seatStatusCode': 3, 'deletedAt': null, 'expiresAt': { $lt: ?0 } }")
    List<Booking> findExpiredPendingBookings(LocalDateTime now);
}
