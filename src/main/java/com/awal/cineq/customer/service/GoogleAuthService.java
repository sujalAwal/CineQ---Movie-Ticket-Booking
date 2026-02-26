package com.awal.cineq.customer.service;

import com.awal.cineq.customer.dto.CustomerAuthResponse;
import com.awal.cineq.customer.dto.GoogleLoginRequest;
import jakarta.servlet.http.HttpServletResponse;

public interface GoogleAuthService {
    CustomerAuthResponse login(GoogleLoginRequest googleLoginRequest, HttpServletResponse response);
}