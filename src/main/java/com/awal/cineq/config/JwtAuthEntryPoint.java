package com.awal.cineq.config;

import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.stereotype.Component;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;

@Component
@Slf4j
public class JwtAuthEntryPoint implements AuthenticationEntryPoint {

    @Override
    public void commence(HttpServletRequest request, HttpServletResponse response,
                         AuthenticationException authException) throws IOException {
        
        log.error("Unauthorized error: {}", authException.getMessage());
        
        response.setContentType("application/json");
        response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
        
        String jsonResponse = "{"
                + "\"success\": false,"
                + "\"message\": \"Unauthorized: Please provide a valid authentication token\","
                + "\"error\": \"" + authException.getMessage() + "\","
                + "\"responseCode\": 401,"
                + "\"timestamp\": \"" + java.time.LocalDateTime.now() + "\""
                + "}";
        
        response.getWriter().write(jsonResponse);
    }
}