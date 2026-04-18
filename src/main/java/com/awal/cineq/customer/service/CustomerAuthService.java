package com.awal.cineq.customer.service;

import com.awal.cineq.customer.dto.*;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.web.multipart.MultipartFile;

public interface CustomerAuthService {
    CustomerAuthResponse login(CustomerLoginRequest loginRequest, HttpServletResponse response);
    CustomerAuthResponse register(CustomerRegisterRequest registerRequest);
    void logout(HttpServletRequest request, HttpServletResponse response);
    void verifyEmail(String token);
    void resendVerificationEmail(String email);
    
    // Password reset
    void forgotPassword(String email);
    void resetPassword(String token, String newPassword);
    boolean validateResetToken(String token);

    String uploadProfilePicture(MultipartFile file);
    void deleteProfilePicture();
}