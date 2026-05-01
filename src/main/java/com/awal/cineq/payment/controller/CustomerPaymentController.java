package com.awal.cineq.payment.controller;

import com.awal.cineq.booking.dto.BookingResponse;
import com.awal.cineq.customer.model.Customer;
import com.awal.cineq.customer.repository.CustomerRepository;
import com.awal.cineq.dto.ApiResponse;
import com.awal.cineq.exception.ResourceNotFoundException;
import com.awal.cineq.payment.dto.EsewaVerifyRequest;
import com.awal.cineq.payment.dto.InitiatePaymentRequest;
import com.awal.cineq.payment.dto.InitiatePaymentResponse;
import com.awal.cineq.payment.dto.KhaltiVerifyRequest;
import com.awal.cineq.payment.service.PaymentService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/customer")
@RequiredArgsConstructor
@Slf4j
public class CustomerPaymentController {

    private final PaymentService paymentService;
    private final CustomerRepository customerRepository;

    // ─────────────────────────────────────────────────────────────────────────
    // POST /customer/initiate-payment
    // Body: { "showtimeId": "...", "seats": [...], "paymentMethod": "ESEWA" }
    //
    // Atomically: validates seats → reserves them → creates PENDING booking →
    // creates Payment → calls gateway → returns redirect data.
    // Seat prices are always resolved server-side.
    // ─────────────────────────────────────────────────────────────────────────

    @PostMapping("/initiate-payment")
    public ResponseEntity<ApiResponse<InitiatePaymentResponse>> initiatePayment(
            @Valid @RequestBody InitiatePaymentRequest request) {

        log.info("initiatePayment — showtimeId={}, seats={}, method={}",
                request.getShowtimeId(), request.getSeats().size(), request.getPaymentMethod());

        Customer customer = getCurrentCustomer();
        InitiatePaymentResponse response = paymentService.initiateBookingPayment(customer.getId(), request);
        return ResponseEntity.ok(ApiResponse.success("Payment initiated successfully", response));
    }

    // ─────────────────────────────────────────────────────────────────────────
    // POST /customer/payment/verify/esewa
    // Body: { "data": "<base64 from eSewa redirect ?data= param>" }
    //
    // Frontend calls this after eSewa redirects to success_url?data=<base64>.
    // Backend verifies HMAC signature and confirms the booking.
    // ─────────────────────────────────────────────────────────────────────────

    @PostMapping("/payment/verify/esewa")
    public ResponseEntity<ApiResponse<BookingResponse>> verifyEsewa(
            @Valid @RequestBody EsewaVerifyRequest request) {

        log.info("verifyEsewa called");
        Customer customer = getCurrentCustomer();
        BookingResponse booking = paymentService.verifyEsewaPayment(customer.getId(), request.getData());
        return ResponseEntity.ok(ApiResponse.success("Payment verified successfully", booking));
    }

    // ─────────────────────────────────────────────────────────────────────────
    // POST /customer/payment/verify/khalti
    // Body: { "pidx": "<pidx from Khalti redirect ?pidx= param>" }
    //
    // Frontend calls this after Khalti redirects to return_url?pidx=<token>.
    // Backend calls Khalti lookup API and confirms the booking.
    // ─────────────────────────────────────────────────────────────────────────

    @PostMapping("/payment/verify/khalti")
    public ResponseEntity<ApiResponse<BookingResponse>> verifyKhalti(
            @Valid @RequestBody KhaltiVerifyRequest request) {

        log.info("verifyKhalti — pidx={}", request.getPidx());
        Customer customer = getCurrentCustomer();
        BookingResponse booking = paymentService.verifyKhaltiPayment(customer.getId(), request.getPidx());
        return ResponseEntity.ok(ApiResponse.success("Payment verified successfully", booking));
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Helper
    // ─────────────────────────────────────────────────────────────────────────

    private Customer getCurrentCustomer() {
        String email = SecurityContextHolder.getContext().getAuthentication().getName();
        return customerRepository.findByEmailAndIsActiveTrue(email)
                .orElseThrow(() -> new ResourceNotFoundException("Customer not found"));
    }
}
