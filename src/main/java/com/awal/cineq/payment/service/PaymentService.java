package com.awal.cineq.payment.service;

import com.awal.cineq.payment.dto.InitiatePaymentRequest;
import com.awal.cineq.payment.dto.InitiatePaymentResponse;

public interface PaymentService {

    /**
     * Initiate a payment for a booking.
     * Validates the booking, creates a Payment record, and delegates to
     * the appropriate gateway (eSewa / Khalti / ConnectIPS).
     *
     * @param customerId authenticated customer's ID
     * @param request    contains bookingId, amount, paymentMethod
     * @return gateway-specific initiation data (paymentUrl or eSewa form fields)
     */
    InitiatePaymentResponse initiatePayment(String customerId, InitiatePaymentRequest request);
}
