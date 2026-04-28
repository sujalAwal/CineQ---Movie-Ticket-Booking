package com.awal.cineq.payment.dto;

import com.awal.cineq.payment.enums.PaymentMethod;
import com.awal.cineq.payment.enums.PaymentStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;

/**
 * Request DTO for payment list filtering and pagination
 * All fields are optional - if not provided, no filter is applied
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PaymentListFilterRequest {

    // ─────────────────────────────────────────────────────────
    // Payment filters
    // ─────────────────────────────────────────────────────────
    private PaymentStatus status;
    private PaymentMethod paymentMethod;

    // ─────────────────────────────────────────────────────────
    // Entity filters
    // ─────────────────────────────────────────────────────────
    private String customerId;
    private String bookingId;

    // ─────────────────────────────────────────────────────────
    // Amount range filter
    // ─────────────────────────────────────────────────────────
    private Double amountMin;
    private Double amountMax;

    // ─────────────────────────────────────────────────────────
    // Date range filter (queries updatedAt field)
    // ─────────────────────────────────────────────────────────
    private LocalDate startDate;  // Filter updatedAt >= startDate (00:00:00)
    private LocalDate endDate;    // Filter updatedAt <= endDate (23:59:59)

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
