package com.awal.cineq.payment.controller;

import com.awal.cineq.payment.dto.PaymentListDTO;
import com.awal.cineq.payment.dto.PaymentListFilterRequest;
import com.awal.cineq.payment.dto.PaymentPageResponse;
import com.awal.cineq.payment.service.PaymentService;
import com.awal.cineq.dto.ApiResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

/**
 * API controller for managing payments (Admin Portal)
 * Provides endpoints for listing and filtering all payments with customer details
 *
 * Access: SUPER_ADMIN, ADMIN roles only
 */
@RestController
@RequestMapping("/payments")
@RequiredArgsConstructor
@Slf4j
public class PaymentController {

    private final PaymentService paymentService;

    /**
     * List payments with optional filters and pagination
     * Query parameters allow for multiple filter fields
     *
     * Query Parameters:
     * - status: Filter by payment status (INITIATED, COMPLETED)
     * - paymentMethod: Filter by payment method (ESEWA, KHALTI)
     * - customerId: Filter by specific customer
     * - bookingId: Filter by specific booking
     * - amountMin: Filter by minimum amount
     * - amountMax: Filter by maximum amount
     * - startDate: Filter from date (ISO format: YYYY-MM-DD)
     * - endDate: Filter to date (ISO format: YYYY-MM-DD)
     * - page: Page number (0-indexed, default: 0)
     * - size: Page size (default: 10)
     * - sortBy: Sort field (default: updatedAt)
     * - sortDirection: Sort direction (asc/desc, default: desc)
     *
     * @param status Payment status filter (optional)
     * @param paymentMethod Payment method filter (optional)
     * @param customerId Customer ID filter (optional)
     * @param bookingId Booking ID filter (optional)
     * @param amountMin Minimum amount filter (optional)
     * @param amountMax Maximum amount filter (optional)
     * @param startDate Start date filter (optional)
     * @param endDate End date filter (optional)
     * @param page Page number (optional)
     * @param size Page size (optional)
     * @param sortBy Sort field (optional)
     * @param sortDirection Sort direction (optional)
     * @return Paginated list of payments with customer details
     */
    @GetMapping("/list")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'ADMIN')")
    public ResponseEntity<ApiResponse<PaymentPageResponse>> listPayments(
            @RequestParam(required = false) String status,
            @RequestParam(required = false) String paymentMethod,
            @RequestParam(required = false) String customerId,
            @RequestParam(required = false) String bookingId,
            @RequestParam(required = false) Double amountMin,
            @RequestParam(required = false) Double amountMax,
            @RequestParam(required = false) String startDate,
            @RequestParam(required = false) String endDate,
            @RequestParam(defaultValue = "0") Integer page,
            @RequestParam(defaultValue = "10") Integer size,
            @RequestParam(defaultValue = "updatedAt") String sortBy,
            @RequestParam(defaultValue = "desc") String sortDirection) {

        log.info("listPayments — filters: status={}, method={}, customerId={}, page={}", 
                status, paymentMethod, customerId, page);

        // Build filter request from query parameters
        PaymentListFilterRequest filterRequest = PaymentListFilterRequest.builder()
                .status(status != null ? com.awal.cineq.payment.enums.PaymentStatus.valueOf(status) : null)
                .paymentMethod(paymentMethod != null ? com.awal.cineq.payment.enums.PaymentMethod.valueOf(paymentMethod) : null)
                .customerId(customerId)
                .bookingId(bookingId)
                .amountMin(amountMin)
                .amountMax(amountMax)
                .startDate(startDate != null ? java.time.LocalDate.parse(startDate) : null)
                .endDate(endDate != null ? java.time.LocalDate.parse(endDate) : null)
                .page(page)
                .size(size)
                .sortBy(sortBy)
                .sortDirection(sortDirection)
                .build();

        PaymentPageResponse response = paymentService.getPaymentsWithFilters(filterRequest);

        return ResponseEntity.ok(
                ApiResponse.success("Payments fetched successfully", response)
        );
    }

    /**
     * Get single payment details by ID
     *
     * @param paymentId The payment ID
     * @return Detailed payment information including user details
     */
    @GetMapping("/{paymentId}")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'ADMIN')")
    public ResponseEntity<ApiResponse<PaymentListDTO>> getPayment(@PathVariable String paymentId) {
        log.info("getPayment — paymentId={}", paymentId);

        PaymentListFilterRequest filterRequest = PaymentListFilterRequest.builder()
                .page(0)
                .size(1)
                .build();

        PaymentPageResponse allPayments = paymentService.getPaymentsWithFilters(filterRequest);

        PaymentListDTO payment = allPayments.getPayments().stream()
                .filter(p -> p.getId().equals(paymentId))
                .findFirst()
                .orElse(null);

        if (payment == null) {
            return ResponseEntity.notFound().build();
        }

        return ResponseEntity.ok(
                ApiResponse.success("Payment detail fetched successfully", payment)
        );
    }
}
