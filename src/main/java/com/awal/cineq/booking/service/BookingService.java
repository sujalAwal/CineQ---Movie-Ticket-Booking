package com.awal.cineq.booking.service;

import com.awal.cineq.booking.dto.BookingResponse;
import com.awal.cineq.booking.dto.ConfirmBookingRequest;
import com.awal.cineq.booking.dto.CreateBookingRequest;

import java.util.List;

public interface BookingService {

    BookingResponse createBooking(String customerId, CreateBookingRequest request);

    List<BookingResponse> getMyBookings(String customerId);

    BookingResponse getBookingByReference(String bookingReference, String customerId);

    BookingResponse confirmBooking(String bookingReference, String customerId, ConfirmBookingRequest request);

    BookingResponse cancelBooking(String bookingReference, String customerId);
}
