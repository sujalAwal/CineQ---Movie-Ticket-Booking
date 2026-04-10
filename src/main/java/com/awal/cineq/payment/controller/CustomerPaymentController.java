package com.awal.cineq.payment.controller;

import com.awal.cineq.customer.model.Customer;
import com.awal.cineq.customer.repository.CustomerRepository;
import com.awal.cineq.dto.ApiResponse;
import com.awal.cineq.exception.ResourceNotFoundException;
import com.awal.cineq.payment.dto.InitiatePaymentRequest;
import com.awal.cineq.payment.dto.InitiatePaymentResponse;
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
    // Body: { "bookingId": "...", "amount": 593, "paymentMethod": "esewa" }
    // ─────────────────────────────────────────────────────────────────────────

    @PostMapping("/initiate-payment")
    public ResponseEntity<ApiResponse<InitiatePaymentResponse>> initiatePayment(
            @Valid @RequestBody InitiatePaymentRequest request) {

        log.info("initiatePayment STARTED — bookingId={}, method={}", request.getBookingId(), request.getPaymentMethod());
        try {
            Customer customer = getCurrentCustomer();
            InitiatePaymentResponse response = paymentService.initiatePayment(customer.getId(), request);
            log.info("initiatePayment END — paymentId={}", response.getPaymentId());
            return ResponseEntity.ok(ApiResponse.success("Payment initiated successfully", response));
        } catch (Exception e) {
            log.error("initiatePayment ERROR — bookingId={}, {}", request.getBookingId(), e.getMessage(), e);
            throw e;
        }
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
