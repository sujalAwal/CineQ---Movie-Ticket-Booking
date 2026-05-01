package com.awal.cineq.publicapi.showtime.service;

import com.awal.cineq.dto.ApiResponse;
import com.awal.cineq.dto.PaginationResponse;
import com.awal.cineq.frontend.showtimes.dto.ShowtimeDTO;
import com.awal.cineq.frontend.showtimes.dto.request.ShowtimePageRequest;
import com.awal.cineq.publicapi.showtime.dto.SeatAvailabilityResponse;
import com.awal.cineq.publicapi.showtime.dto.ShowtimeListDTO;
import com.awal.cineq.publicapi.showtime.dto.BookingPublicRequest;
import com.awal.cineq.publicapi.showtime.dto.BookingPublicResponse;
import com.awal.cineq.publicapi.showtime.dto.SuggestSeatsRequest;
import com.awal.cineq.publicapi.showtime.dto.SuggestSeatsResponse;

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

    /**
     * Suggest best contiguous seats for a showtime using sliding window and greedy scoring algorithm
     * Returns top 3 seat suggestions based on availability and center positioning
     */
    ApiResponse<SuggestSeatsResponse> suggestSeats(SuggestSeatsRequest request);
}
