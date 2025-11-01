package com.awal.cineq.customer.controller;

import com.awal.cineq.customer.dto.*;
import com.awal.cineq.customer.service.CustomerAuthService;
import com.awal.cineq.dto.ApiResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/frontend/customer/auth")
@RequiredArgsConstructor
@Slf4j
@CrossOrigin(origins = "*", maxAge = 3600)
public class CustomerAuthController {

    private final CustomerAuthService customerAuthService;

    @PostMapping("/login")
    public ResponseEntity<ApiResponse<CustomerAuthResponse>> login(@Valid @RequestBody CustomerLoginRequest loginRequest) {
        log.info("Customer login attempt for email: {}", loginRequest.getEmail());
        
        CustomerAuthResponse authResponse = customerAuthService.login(loginRequest);
        
        return ResponseEntity.ok(ApiResponse.success("Login successful", authResponse));
    }

    @PostMapping("/register")
    public ResponseEntity<ApiResponse<CustomerAuthResponse>> register(@Valid @RequestBody CustomerRegisterRequest registerRequest) {
        log.info("Customer registration attempt for email: {}", registerRequest.getEmail());
        
        CustomerAuthResponse authResponse = customerAuthService.register(registerRequest);
        
        return ResponseEntity.ok(ApiResponse.success("Registration successful. Please verify your email.", authResponse));
    }

    @PostMapping("/logout")
    public ResponseEntity<ApiResponse<String>> logout(@RequestHeader("Authorization") String token) {
        log.info("Customer logout attempt");
        
        // Remove "Bearer " prefix if present
        if (token.startsWith("Bearer ")) {
            token = token.substring(7);
        }
        
        customerAuthService.logout(token);
        
        return ResponseEntity.ok(ApiResponse.success("Logout successful", "Customer logged out successfully"));
    }

    @PostMapping("/verify-email")
    public ResponseEntity<ApiResponse<String>> verifyEmail(@RequestParam String token) {
        log.info("Email verification attempt with token: {}", token);
        
        customerAuthService.verifyEmail(token);
        
        return ResponseEntity.ok(ApiResponse.success("Email verified successfully", "Your email has been verified"));
    }

    @PostMapping("/resend-verification")
    public ResponseEntity<ApiResponse<String>> resendVerificationEmail(@RequestParam String email) {
        log.info("Resend verification email request for: {}", email);
        
        customerAuthService.resendVerificationEmail(email);
        
        return ResponseEntity.ok(ApiResponse.success("Verification email sent", "Please check your email for verification instructions"));
    }
}