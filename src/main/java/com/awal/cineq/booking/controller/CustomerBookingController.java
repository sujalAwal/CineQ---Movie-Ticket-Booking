package com.awal.cineq.booking.controller;

import com.awal.cineq.booking.dto.BookingResponse;
import com.awal.cineq.booking.dto.ConfirmBookingRequest;
import com.awal.cineq.booking.dto.CreateBookingRequest;
import com.awal.cineq.booking.service.BookingService;
import com.awal.cineq.customer.model.Customer;
import com.awal.cineq.customer.repository.CustomerRepository;
import com.awal.cineq.dto.ApiResponse;
import com.awal.cineq.exception.ResourceNotFoundException;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/customer/bookings")
@RequiredArgsConstructor
@Slf4j
public class CustomerBookingController {

    private final BookingService bookingService;
    private final CustomerRepository customerRepository;

    // ─────────────────────────────────────────────────────────────
    // POST /customer/bookings — create a new booking
    // ─────────────────────────────────────────────────────────────

    @PostMapping
    public ResponseEntity<ApiResponse<BookingResponse>> createBooking(
            @Valid @RequestBody CreateBookingRequest request) {

        log.info("createBooking STARTED — showtimeId={}", request.getShowtimeId());
        try {
            Customer customer = getCurrentCustomer();
            BookingResponse response = bookingService.createBooking(customer.getId(), request);
            log.info("createBooking END — bookingReference={}", response.getBookingReference());
            return ResponseEntity.status(HttpStatus.CREATED)
                    .body(ApiResponse.success("Booking created successfully", response));
        } catch (Exception e) {
            log.error("createBooking ERROR — {}", e.getMessage(), e);
            throw e;
        }
    }

    // ─────────────────────────────────────────────────────────────
    // GET /customer/bookings — list all bookings for current customer
    // ─────────────────────────────────────────────────────────────

    @GetMapping
    public ResponseEntity<ApiResponse<List<BookingResponse>>> getMyBookings() {
        log.info("getMyBookings STARTED");
        try {
            Customer customer = getCurrentCustomer();
            List<BookingResponse> bookings = bookingService.getMyBookings(customer.getId());
            log.info("getMyBookings END — count={}", bookings.size());
            return ResponseEntity.ok(ApiResponse.success("Bookings retrieved successfully", bookings));
        } catch (Exception e) {
            log.error("getMyBookings ERROR — {}", e.getMessage(), e);
            throw e;
        }
    }

    // ─────────────────────────────────────────────────────────────
    // GET /customer/bookings/{bookingReference} — get single booking
    // ─────────────────────────────────────────────────────────────

    @GetMapping("/{bookingReference}")
    public ResponseEntity<ApiResponse<BookingResponse>> getBookingByReference(
            @PathVariable String bookingReference) {

        log.info("getBookingByReference STARTED — reference={}", bookingReference);
        try {
            Customer customer = getCurrentCustomer();
            BookingResponse booking = bookingService.getBookingByReference(bookingReference, customer.getId());
            log.info("getBookingByReference END — reference={}", bookingReference);
            return ResponseEntity.ok(ApiResponse.success("Booking retrieved successfully", booking));
        } catch (Exception e) {
            log.error("getBookingByReference ERROR — reference={}, {}", bookingReference, e.getMessage(), e);
            throw e;
        }
    }

    // ─────────────────────────────────────────────────────────────
    // POST /customer/bookings/{bookingReference}/confirm — confirm a booking
    // ─────────────────────────────────────────────────────────────

    @PostMapping("/{bookingReference}/confirm")
    public ResponseEntity<ApiResponse<BookingResponse>> confirmBooking(
            @PathVariable String bookingReference,
            @RequestBody(required = false) ConfirmBookingRequest request) {

        log.info("confirmBooking STARTED — reference={}", bookingReference);
        try {
            Customer customer = getCurrentCustomer();
            BookingResponse booking = bookingService.confirmBooking(bookingReference, customer.getId(), request);
            log.info("confirmBooking END — reference={}", bookingReference);
            return ResponseEntity.ok(ApiResponse.success("Booking confirmed successfully", booking));
        } catch (Exception e) {
            log.error("confirmBooking ERROR — reference={}, {}", bookingReference, e.getMessage(), e);
            throw e;
        }
    }

    // ─────────────────────────────────────────────────────────────
    // DELETE /customer/bookings/{bookingReference} — cancel a booking
    // ─────────────────────────────────────────────────────────────

    @DeleteMapping("/{bookingReference}")
    public ResponseEntity<ApiResponse<BookingResponse>> cancelBooking(
            @PathVariable String bookingReference) {

        log.info("cancelBooking STARTED — reference={}", bookingReference);
        try {
            Customer customer = getCurrentCustomer();
            BookingResponse booking = bookingService.cancelBooking(bookingReference, customer.getId());
            log.info("cancelBooking END — reference={}", bookingReference);
            return ResponseEntity.ok(ApiResponse.success("Booking cancelled successfully", booking));
        } catch (Exception e) {
            log.error("cancelBooking ERROR — reference={}, {}", bookingReference, e.getMessage(), e);
            throw e;
        }
    }

    // ─────────────────────────────────────────────────────────────
    // Helper
    // ─────────────────────────────────────────────────────────────

    private Customer getCurrentCustomer() {
        String email = SecurityContextHolder.getContext().getAuthentication().getName();
        return customerRepository.findByEmailAndIsActiveTrue(email)
                .orElseThrow(() -> new ResourceNotFoundException("Customer not found"));
    }
}
