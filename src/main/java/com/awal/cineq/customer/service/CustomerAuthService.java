package com.awal.cineq.customer.service;

import com.awal.cineq.customer.dto.*;

public interface CustomerAuthService {
    CustomerAuthResponse login(CustomerLoginRequest loginRequest);
    CustomerAuthResponse register(CustomerRegisterRequest registerRequest);
    void logout(String token);
    void verifyEmail(String token);
    void resendVerificationEmail(String email);
}