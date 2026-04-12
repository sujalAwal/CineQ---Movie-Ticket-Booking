package com.awal.cineq.booking.controller;

import com.awal.cineq.booking.dto.BookingListDTO;
import com.awal.cineq.booking.dto.BookingListFilterRequest;
import com.awal.cineq.booking.dto.BookingPageResponse;
import com.awal.cineq.booking.service.BookingService;
import com.awal.cineq.dto.ApiResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

/**
 * API controller for managing bookings
 * Provides endpoints for listing, filtering, and viewing all bookings with customer details
 *
 * Access: SUPER_ADMIN, ADMIN roles only
 */
@RestController
@RequestMapping("/bookings")
@RequiredArgsConstructor
@Slf4j
public class BookingController {

    private final BookingService bookingService;

    /**
     * List bookings with optional filters and pagination
     * Request body allows for multiple filter fields
     *
     * Request Body:
     * - paymentStatus: Filter by payment status (INITIATED, PENDING, COMPLETED, FAILED, REFUNDED)
     * - paymentMethod: Filter by payment method (ESEWA, KHALTI, CONNECTIPS)
     * - seatStatusCode: Filter by seat status (1=Available, 2=Booked, 3=Reserved)
     * - bookingReference: Filter by booking reference (case-insensitive)
     * - customerId: Filter by specific customer
     * - showtimeId: Filter by specific showtime
     * - fromDate: Filter from date (ISO format: YYYY-MM-DD)
     * - toDate: Filter to date (ISO format: YYYY-MM-DD)
     * - page: Page number (0-indexed, default: 0)
     * - size: Page size (default: 10)
     * - sortBy: Sort field (default: updatedAt)
     * - sortDirection: Sort direction (asc/desc, default: desc)
     *
     * @param filterRequest Filter request with optional criteria parameters
     * @return Paginated list of bookings with customer details
     */
    @GetMapping("/list")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'ADMIN')")
    public ResponseEntity<ApiResponse<BookingPageResponse>> listBookings(
            @RequestBody(required = false) BookingListFilterRequest filterRequest) {

        log.info("listBookings — filters={}", filterRequest);

        // Use default filters if not provided
        if (filterRequest == null) {
            filterRequest = BookingListFilterRequest.builder().build();
        }

        BookingPageResponse response = bookingService.getBookingsWithFilters(filterRequest);

        return ResponseEntity.ok(
                ApiResponse.success("Bookings fetched successfully", response)
        );
    }

    /**
     * Get single booking details by ID
     *
     * @param bookingId The booking ID
     * @return Detailed booking information including customer and enriched data
     */
    @GetMapping("/{bookingId}")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'ADMIN')")
    public ResponseEntity<ApiResponse<BookingListDTO>> getBooking(@PathVariable String bookingId) {
        log.info("getBooking — bookingId={}", bookingId);

        // Fetch the specific booking
        BookingListFilterRequest filterRequest = BookingListFilterRequest.builder()
                .page(0)
                .size(1)
                .build();

        BookingPageResponse allBookings = bookingService.getBookingsWithFilters(filterRequest);

        BookingListDTO booking = allBookings.getBookings().stream()
                .filter(b -> b.getId().equals(bookingId))
                .findFirst()
                .orElse(null);

        if (booking == null) {
            return ResponseEntity.notFound().build();
        }

        return ResponseEntity.ok(
                ApiResponse.success("Booking detail fetched successfully", booking)
        );
    }
}
