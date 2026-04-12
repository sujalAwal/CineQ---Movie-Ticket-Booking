package com.awal.cineq.publicapi.showtime.service;

import com.awal.cineq.dto.ApiResponse;
import com.awal.cineq.dto.PaginationResponse;
import com.awal.cineq.frontend.showtimes.dto.ShowtimeDTO;
import com.awal.cineq.frontend.showtimes.dto.request.ShowtimePageRequest;
import com.awal.cineq.publicapi.showtime.dto.SeatAvailabilityResponse;
import com.awal.cineq.publicapi.showtime.dto.ShowtimeListDTO;
import com.awal.cineq.publicapi.showtime.dto.BookingPublicRequest;
import com.awal.cineq.publicapi.showtime.dto.BookingPublicResponse;

import java.util.List;

public interface PublicShowtimeService {

    PaginationResponse<ShowtimeListDTO> getAllShowtimes(ShowtimePageRequest pageRequest);

    ApiResponse<Object> getShowtimeById(String id);

    PaginationResponse<ShowtimeListDTO> getShowtimesByMovieId(String movieId, int page, int size);

    PaginationResponse<ShowtimeListDTO> getShowtimesByTheatreId(String theatreId, int page, int size);

    ApiResponse<SeatAvailabilityResponse> getSeatAvailability(String showtimeId);

    /**
     * Get public booking details by showtime ID
     * Returns only paymentStatus, bookingDetails, and createdAt for bookings with COMPLETED or INITIATED payment status
     */
    List<BookingPublicResponse> getPublicBookingsByShowtime(BookingPublicRequest request);
}
