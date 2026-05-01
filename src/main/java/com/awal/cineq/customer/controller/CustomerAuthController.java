package com.awal.cineq.customer.controller;

import com.awal.cineq.customer.dto.*;
import com.awal.cineq.customer.service.CustomerAuthService;
import com.awal.cineq.customer.service.GoogleAuthService;
import com.awal.cineq.dto.ApiResponse;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/customer/auth")
@RequiredArgsConstructor
@Slf4j
public class CustomerAuthController {

    private final CustomerAuthService customerAuthService;

    private final GoogleAuthService googleAuthService;

    @PostMapping("/login")
    public ResponseEntity<ApiResponse<CustomerAuthResponse>> login(
            @Valid @RequestBody CustomerLoginRequest loginRequest,
            HttpServletResponse response) {
        log.info("Customer login attempt for email: {}", loginRequest.getEmail());
        
        CustomerAuthResponse authResponse = customerAuthService.login(loginRequest, response);
        
        return ResponseEntity.ok(ApiResponse.success("Login successful", authResponse));
    }

    @PostMapping("/register")
    public ResponseEntity<ApiResponse<CustomerAuthResponse>> register(@Valid @RequestBody CustomerRegisterRequest registerRequest) {
        log.info("Customer registration attempt for email: {}", registerRequest.getEmail());
        
        CustomerAuthResponse authResponse = customerAuthService.register(registerRequest);
        
        String message = authResponse.getToken() == null 
                ? "Registration successful. Please verify your email before logging in."
                : "Registration successful";
        
        return ResponseEntity.ok(ApiResponse.success(message, authResponse));
    }

    @PostMapping("/logout")
    public ResponseEntity<ApiResponse<String>> logout(HttpServletRequest request, HttpServletResponse response) {
        log.info("Customer logout attempt");
        
        customerAuthService.logout(request, response);
        
        return ResponseEntity.ok(ApiResponse.success("Logout successful", "Customer logged out successfully"));
    }

    @PostMapping(value = "/profile-picture", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @PreAuthorize("hasRole('CUSTOMER')")
    public ResponseEntity<ApiResponse<Map<String, String>>> uploadProfilePicture(
            @RequestParam("file") MultipartFile file) {
        log.info("Customer profile picture upload attempt");

        String profilePictureUrl = customerAuthService.uploadProfilePicture(file);

        return ResponseEntity.ok(ApiResponse.success(
                "Profile picture uploaded successfully",
                Map.of("profilePicture", profilePictureUrl)
        ));
    }

    @DeleteMapping("/profile-picture")
    @PreAuthorize("hasRole('CUSTOMER')")
    public ResponseEntity<ApiResponse<Map<String, String>>> deleteProfilePicture() {
        log.info("Customer profile picture delete attempt");

        customerAuthService.deleteProfilePicture();

        Map<String, String> response = new HashMap<>();
        response.put("profilePicture", null);
        return ResponseEntity.ok(ApiResponse.success(
                "Profile picture deleted successfully",
                response
        ));
    }

    @PostMapping("/verify-email")
    public ResponseEntity<ApiResponse<String>> verifyEmail(@RequestParam String token) {
        log.info("Email verification attempt with token: {}", token);
        
        customerAuthService.verifyEmail(token);
        
        return ResponseEntity.ok(ApiResponse.success("Email verified successfully", "Your email has been verified. You can now log in."));
    }

    @PostMapping("/resend-verification")
    public ResponseEntity<ApiResponse<String>> resendVerificationEmail(@Valid @RequestBody ForgotPasswordRequest request) {
        log.info("Resend verification email request for: {}", request.getEmail());
        
        customerAuthService.resendVerificationEmail(request.getEmail());
        
        return ResponseEntity.ok(ApiResponse.success("Verification email sent", "Please check your email for verification instructions"));
    }

    @PostMapping("/forgot-password")
    public ResponseEntity<ApiResponse<String>> forgotPassword(@Valid @RequestBody ForgotPasswordRequest request) {
        log.info("Password reset request for email: {}", request.getEmail());
        
        customerAuthService.forgotPassword(request.getEmail());
        
        // Always return success to prevent email enumeration
        return ResponseEntity.ok(ApiResponse.success(
                "Password reset instructions sent", 
                "If an account exists with this email, you will receive password reset instructions."
        ));
    }

    @PostMapping("/reset-password")
    public ResponseEntity<ApiResponse<String>> resetPassword(@Valid @RequestBody ResetPasswordRequest request) {
        log.info("Password reset attempt with token");
        
        customerAuthService.resetPassword(request.getToken(), request.getNewPassword());
        
        return ResponseEntity.ok(ApiResponse.success(
                "Password reset successful", 
                "Your password has been reset. You can now log in with your new password."
        ));
    }

    @GetMapping("/validate-reset-token")
    public ResponseEntity<ApiResponse<Map<String, Boolean>>> validateResetToken(@RequestParam String token) {
        log.info("Validating password reset token");
        
        boolean isValid = customerAuthService.validateResetToken(token);
        
        return ResponseEntity.ok(ApiResponse.success(
                isValid ? "Token is valid" : "Token is invalid or expired",
                Map.of("valid", isValid)
        ));
    }

    @PostMapping("/login/google")
    public ResponseEntity<ApiResponse<CustomerAuthResponse>> loginWithGoogle(
            @Valid @RequestBody GoogleLoginRequest request,
            HttpServletResponse response) {
        log.info("Attempting Google login");

        CustomerAuthResponse authResponse = googleAuthService.login(request, response);

        return ResponseEntity.ok(ApiResponse.success("Google login successful", authResponse));
    }
}