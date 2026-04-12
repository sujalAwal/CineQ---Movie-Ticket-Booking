package com.awal.cineq.booking.service;

import com.awal.cineq.booking.dto.BookingListDTO;
import com.awal.cineq.booking.dto.BookingListFilterRequest;
import com.awal.cineq.booking.dto.BookingPageResponse;
import com.awal.cineq.booking.dto.BookingResponse;

import java.util.List;

public interface BookingService {

    List<BookingResponse> getMyBookings(String customerId);

    BookingResponse getBookingByReference(String bookingReference, String customerId);

    BookingResponse cancelBooking(String bookingReference, String customerId);

    /**
     * Get bookings with optional filters, date range, and pagination
     * @param filterRequest Contains optional filters and pagination parameters
     * @return Paginated response with bookings and customer details
     */
    BookingPageResponse getBookingsWithFilters(BookingListFilterRequest filterRequest);
}
