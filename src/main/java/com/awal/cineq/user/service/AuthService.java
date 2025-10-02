package com.awal.cineq.user.service;

import com.awal.cineq.user.dto.LoginRequest;
import com.awal.cineq.user.dto.RegisterRequest;
import com.awal.cineq.user.dto.AuthResponse;

public interface AuthService {
    AuthResponse login(LoginRequest loginRequest);
    AuthResponse register(RegisterRequest registerRequest);
    void logout(String token);
}