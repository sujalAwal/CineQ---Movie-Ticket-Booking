package com.awal.cineq.booking.controller;

import com.awal.cineq.booking.dto.BookingResponse;
import com.awal.cineq.booking.service.BookingService;
import com.awal.cineq.customer.model.Customer;
import com.awal.cineq.customer.repository.CustomerRepository;
import com.awal.cineq.dto.ApiResponse;
import com.awal.cineq.exception.ResourceNotFoundException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import java.util.List;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;


@RestController
@RequestMapping("/customer/bookings")
@RequiredArgsConstructor
@Slf4j
public class CustomerBookingController {

    private final BookingService bookingService;
    private final CustomerRepository customerRepository;

    // ─────────────────────────────────────────────────────────────
    // GET /customer/bookings — list all bookings for current customer
    // ─────────────────────────────────────────────────────────────

    @GetMapping
    public ResponseEntity<ApiResponse<List<BookingResponse>>> getMyBookings() {
        Customer customer = getCurrentCustomer();
        List<BookingResponse> bookings = bookingService.getMyBookings(customer.getId());
        return ResponseEntity.ok(ApiResponse.success("Bookings retrieved successfully", bookings));
    }

    // ─────────────────────────────────────────────────────────────
    // GET /customer/bookings/{bookingReference}
    // ─────────────────────────────────────────────────────────────

    @GetMapping("/{bookingReference}")
    public ResponseEntity<ApiResponse<BookingResponse>> getBooking(
            @PathVariable String bookingReference) {

        Customer customer = getCurrentCustomer();
        BookingResponse booking = bookingService.getBookingByReference(bookingReference, customer.getId());
        return ResponseEntity.ok(ApiResponse.success("Booking retrieved successfully", booking));
    }

    // ─────────────────────────────────────────────────────────────
    // DELETE /customer/bookings/{bookingReference} — cancel (PENDING only)
    // ─────────────────────────────────────────────────────────────

    @DeleteMapping("/{bookingReference}")
    public ResponseEntity<ApiResponse<BookingResponse>> cancelBooking(
            @PathVariable String bookingReference) {

        Customer customer = getCurrentCustomer();
        BookingResponse booking = bookingService.cancelBooking(bookingReference, customer.getId());
        return ResponseEntity.ok(ApiResponse.success("Booking cancelled successfully", booking));
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
