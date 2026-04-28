package com.awal.cineq.payment.service;

import com.awal.cineq.booking.dto.BookingResponse;
import com.awal.cineq.payment.dto.InitiatePaymentRequest;
import com.awal.cineq.payment.dto.InitiatePaymentResponse;
import com.awal.cineq.payment.dto.PaymentListFilterRequest;
import com.awal.cineq.payment.dto.PaymentPageResponse;

public interface PaymentService {

    /**
     * Single atomic operation: validate seats, reserve them, create a PENDING booking,
     * create a Payment record, and call the gateway to get redirect/form data.
     * Seat prices are always resolved server-side from showtime.seatLayout[].price.
     */
    InitiatePaymentResponse initiateBookingPayment(String customerId, InitiatePaymentRequest request);

    /**
     * Called by the frontend after eSewa redirects back with ?data=<base64>.
     * Decodes the data, verifies the HMAC signature, and confirms the booking.
     */
    BookingResponse verifyEsewaPayment(String customerId, String encodedData);

    /**
     * Called by the frontend after Khalti redirects back with ?pidx=<token>.
     * Calls the Khalti lookup API to confirm the payment, then confirms the booking.
     */
    BookingResponse verifyKhaltiPayment(String customerId, String pidx);

    /**
     * List payments with optional filters and pagination
     * Returns all payment fields with enriched user/customer details from customers collection
     * Admin only endpoint
     */
    PaymentPageResponse getPaymentsWithFilters(PaymentListFilterRequest filterRequest);
}
