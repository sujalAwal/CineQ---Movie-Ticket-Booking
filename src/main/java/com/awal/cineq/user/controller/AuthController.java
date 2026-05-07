package com.awal.cineq.user.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.awal.cineq.config.ApplicationProperties;
import com.awal.cineq.dto.ApiResponse;
import com.awal.cineq.user.dto.AuthResponse;
import com.awal.cineq.user.dto.LoginRequest;
import com.awal.cineq.user.dto.ProfileResponse;
import com.awal.cineq.user.dto.RegisterRequest;

import com.awal.cineq.user.dto.ResendLinkRequest;
import com.awal.cineq.user.dto.SetPasswordRequest;
import com.awal.cineq.user.dto.ValidateTokenRequest;
import java.util.Map;

import com.awal.cineq.user.service.AuthService;

import io.swagger.v3.oas.annotations.security.SecurityRequirements;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@RestController
@RequestMapping("/auth")
@RequiredArgsConstructor
@Slf4j
@CrossOrigin(origins = "*", maxAge = 3600)
@Tag(name = "Authentication")
public class AuthController {

    private final AuthService authService;
    private final ApplicationProperties applicationProperties;
    

    @SecurityRequirements() // ← ONLY annotation needed for public endpoints
    @PostMapping("/login")
    public ResponseEntity<ApiResponse<AuthResponse>> login(
            @Valid @RequestBody LoginRequest loginRequest,
            HttpServletResponse response) {
        
        log.info("Login attempt for email: {}", loginRequest.getEmail());
        
        AuthResponse authResponse = authService.login(loginRequest);
        
        // Create httpOnly cookie for secure web clients
    
        Cookie authCookie = new Cookie(applicationProperties.getSecurity().getCookie().getName(), authResponse.getToken());
        authCookie.setHttpOnly(applicationProperties.getSecurity().getCookie().isHttpOnly());
        authCookie.setSecure(applicationProperties.getSecurity().getCookie().isSecure());
        authCookie.setPath(applicationProperties.getSecurity().getCookie().getPath());
        authCookie.setMaxAge(applicationProperties.getSecurity().getCookie().getMaxAge());
        authCookie.setAttribute("SameSite", applicationProperties.getSecurity().getCookie().getSameSite());
        
        response.addCookie(authCookie);
        
        // Return full response (token included for API clients, cookie for web clients)
        return ResponseEntity.ok(ApiResponse.success("Login successful", authResponse));
    }

    @SecurityRequirements() // ← ONLY annotation needed for public endpoints
    @PostMapping("/register")
    public ResponseEntity<ApiResponse<AuthResponse>> register(@Valid @RequestBody RegisterRequest registerRequest) {
        log.info("Registration attempt for email: {}", registerRequest.getEmail());

        AuthResponse authResponse = authService.register(registerRequest);

        return ResponseEntity.ok(ApiResponse.success("Registration successful", authResponse));
    }


    @SecurityRequirements()
    @PostMapping("/validate-token")
    public ResponseEntity<ApiResponse<Map<String, Object>>> validateToken(@Valid @RequestBody ValidateTokenRequest request) {
        Map<String, Object> result = authService.validateToken(request.getToken());
        return ResponseEntity.ok(ApiResponse.success("Token is valid", result));
    }

    @SecurityRequirements()
    @PostMapping("/set-password")
    public ResponseEntity<ApiResponse<String>> setPassword(@Valid @RequestBody SetPasswordRequest request) {
        authService.setPassword(request.getToken(), request.getPassword());
        return ResponseEntity.ok(ApiResponse.success("Password set successfully", null));
    }

    @SecurityRequirements()
    @PostMapping("/resend-link")
    public ResponseEntity<ApiResponse<String>> resendLink(@Valid @RequestBody ResendLinkRequest request) {
        authService.resendLink(request.getEmail());
        return ResponseEntity.ok(ApiResponse.success("If the email is valid and pending password setup, a new link has been sent.", null));
    }


    @PostMapping("/logout")
    public ResponseEntity<ApiResponse<String>> logout(
            @RequestHeader(value = "Authorization", required = false) String token,
            HttpServletResponse response) {
        
        log.info("Logout attempt");
        
        // Clear the JWT cookie
        Cookie jwtCookie = new Cookie(applicationProperties.getSecurity().getCookie().getName(), null);
        jwtCookie.setHttpOnly(applicationProperties.getSecurity().getCookie().isHttpOnly());
        jwtCookie.setSecure(applicationProperties.getSecurity().getCookie().isSecure());
        jwtCookie.setPath(applicationProperties.getSecurity().getCookie().getPath());
        jwtCookie.setMaxAge(0); // Expire immediately
        
        response.addCookie(jwtCookie);
        
        // Also handle token-based logout if Authorization header is provided
        if (token != null && token.startsWith("Bearer ")) {
            token = token.substring(7);
            authService.logout(token);
        }
        
        return ResponseEntity.ok(ApiResponse.success("Logout successful", "User logged out successfully"));
    }

    @GetMapping("/profile")
    public ResponseEntity<ApiResponse<ProfileResponse>> profile() {
        log.info("Profile request");

        // Get current user email from security context
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        String email = authentication.getName();

        log.debug("Fetching profile for user: {}", email);

        ProfileResponse profileResponse = authService.getProfile(email);

        return ResponseEntity.ok(ApiResponse.success("Profile fetched successfully", profileResponse));
    }
}