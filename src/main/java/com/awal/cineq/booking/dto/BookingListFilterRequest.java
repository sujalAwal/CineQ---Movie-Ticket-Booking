package com.awal.cineq.booking.dto;

import com.awal.cineq.payment.enums.PaymentMethod;
import com.awal.cineq.payment.enums.PaymentStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;

/**
 * Request DTO for booking list filtering and pagination
 * All fields are optional - if not provided, no filter is applied
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class BookingListFilterRequest {

    // ─────────────────────────────────────────────────────────
    // Payment filters
    // ─────────────────────────────────────────────────────────
    private PaymentStatus paymentStatus;
    private PaymentMethod paymentMethod;

    // ─────────────────────────────────────────────────────────
    // Booking filters
    // ─────────────────────────────────────────────────────────
    private Integer seatStatusCode;
    private String bookingReference;
    private String customerId;
    private String showtimeId;

    // ─────────────────────────────────────────────────────────
    // Date range filter (queries updatedAt field)
    // ─────────────────────────────────────────────────────────
    private LocalDate fromDate;  // Filter updatedAt >= fromDate (00:00:00)
    private LocalDate toDate;    // Filter updatedAt <= toDate (23:59:59)

    // ─────────────────────────────────────────────────────────
    // Pagination
    // ─────────────────────────────────────────────────────────
    @Builder.Default
    private Integer page = 0;

    @Builder.Default
    private Integer size = 10;

    // ─────────────────────────────────────────────────────────
    // Sorting
    // ─────────────────────────────────────────────────────────
    @Builder.Default
    private String sortBy = "updatedAt";

    @Builder.Default
    private String sortDirection = "desc";
}
