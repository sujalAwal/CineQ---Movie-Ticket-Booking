package com.awal.cineq.user.controller;

import com.awal.cineq.dto.ApiResponse;
import com.awal.cineq.user.dto.AuthResponse;
import com.awal.cineq.user.dto.LoginRequest;
import com.awal.cineq.user.dto.RegisterRequest;
import com.awal.cineq.user.service.AuthService;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/auth")
@RequiredArgsConstructor
@Slf4j
@CrossOrigin(origins = "*", maxAge = 3600)
public class AuthController {

    private final AuthService authService;
    
    @Value("${app.jwt.expiration}")
    private long jwtExpiration;

    @PostMapping("/login")
    public ResponseEntity<ApiResponse<AuthResponse>> login(
            @Valid @RequestBody LoginRequest loginRequest,
            HttpServletResponse response) {
        
        log.info("Login attempt for email: {}", loginRequest.getEmail());
        
        AuthResponse authResponse = authService.login(loginRequest);
        
        // Create httpOnly cookie for secure web clients
        Cookie jwtCookie = new Cookie("jwt-auth-token", authResponse.getToken());
        jwtCookie.setHttpOnly(true);              // Prevents JavaScript access (XSS protection)
        jwtCookie.setSecure(true);               // Set to true in production with HTTPS
        jwtCookie.setPath("/");                   // Available for entire app
       jwtCookie.setMaxAge((int) (jwtExpiration / 1000)); // CORRECT - convert ms to seconds
        jwtCookie.setAttribute("SameSite", "None"); // CSRF protection
        
        response.addCookie(jwtCookie);
        
        // Return full response (token included for API clients, cookie for web clients)
        return ResponseEntity.ok(ApiResponse.success("Login successful", authResponse));
    }

    @PostMapping("/register")
    public ResponseEntity<ApiResponse<AuthResponse>> register(@Valid @RequestBody RegisterRequest registerRequest) {
        log.info("Registration attempt for email: {}", registerRequest.getEmail());
        
        AuthResponse authResponse = authService.register(registerRequest);
        
        return ResponseEntity.ok(ApiResponse.success("Registration successful", authResponse));
    }

    @PostMapping("/logout")
    public ResponseEntity<ApiResponse<String>> logout(
            @RequestHeader(value = "Authorization", required = false) String token,
            HttpServletResponse response) {
        
        log.info("Logout attempt");
        
        // Clear the JWT cookie
        Cookie jwtCookie = new Cookie("jwt-auth-token", null);
        jwtCookie.setHttpOnly(true);
        jwtCookie.setPath("/");
        jwtCookie.setMaxAge(0); // Expire immediately
        
        response.addCookie(jwtCookie);
        
        // Also handle token-based logout if Authorization header is provided
        if (token != null && token.startsWith("Bearer ")) {
            token = token.substring(7);
            authService.logout(token);
        }
        
        return ResponseEntity.ok(ApiResponse.success("Logout successful", "User logged out successfully"));
    }
}