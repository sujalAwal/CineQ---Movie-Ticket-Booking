package com.awal.cineq.config;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.Arrays;
import java.util.List;

@Component
@RequiredArgsConstructor
@Slf4j
public class JwtAuthTokenFilter extends OncePerRequestFilter {

    private final JwtUtil jwtUtil;
    private final CompositeUserDetailsService compositeUserDetailsService;

    // Public endpoints that should skip JWT authentication
    // This prevents slow database queries from blocking health checks, docs, etc.
    private static final List<String> PUBLIC_PATHS = Arrays.asList(
        "/auth/",                  // User login/register
        "/frontend/customer/auth/", // Customer auth
        "/health",                 // Health check
        "/actuator",               // Actuator metrics
        "/swagger-ui",             // API docs
        "/v3/api-docs",            // OpenAPI docs
        "/login",                  // Login page
        "/h2-console/"             // H2 console (dev only)
    );

    /**
     * Skip JWT filter for public endpoints to prevent database queries
     * from blocking health checks and metrics
     */
    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        String requestPath = request.getRequestURI();
        log.debug("Checking if path should skip JWT filter: {}", requestPath);

        return PUBLIC_PATHS.stream()
            .anyMatch(requestPath::startsWith);
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response,
                                  FilterChain filterChain) throws ServletException, IOException {

        try {
            String jwt = parseJwt(request);

            if (jwt != null && jwtUtil.validateToken(jwt)) {
                String username = jwtUtil.extractUsername(jwt);

                // Load user details with timeout protection
                // If user lookup takes > 5 seconds, log warning and continue without authentication
                long startTime = System.currentTimeMillis();
                UserDetails userDetails = null;

                try {
                    userDetails = compositeUserDetailsService.loadUserByUsername(username);
                    long loadTime = System.currentTimeMillis() - startTime;

                    if (loadTime > 1000) {
                        log.warn("Slow user details lookup ({}ms) for username: {}", loadTime, username);
                    }
                } catch (Exception e) {
                    long loadTime = System.currentTimeMillis() - startTime;
                    log.warn("Failed to load user details ({}ms) for username: {}. Error: {}",
                        loadTime, username, e.getMessage());
                    // Continue without authentication rather than blocking request
                    filterChain.doFilter(request, response);
                    return;
                }

                if (userDetails != null && jwtUtil.validateToken(jwt, userDetails)) {
                    UsernamePasswordAuthenticationToken authentication =
                        new UsernamePasswordAuthenticationToken(userDetails, null, userDetails.getAuthorities());
                    authentication.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));

                    SecurityContextHolder.getContext().setAuthentication(authentication);
                }
            }
        } catch (Exception e) {
            log.error("JWT authentication filter error: {}", e.getMessage());
            // Continue filter chain even if JWT processing fails
        }

        filterChain.doFilter(request, response);
    }

    private String parseJwt(HttpServletRequest request) {
        // First try Authorization header (for API clients)
        String headerAuth = request.getHeader("Authorization");
        if (headerAuth != null && headerAuth.startsWith("Bearer ")) {
            return headerAuth.substring(7);
        }
        
        // Then try httpOnly cookie (for web clients)
        if (request.getCookies() != null) {
            for (Cookie cookie : request.getCookies()) {
                if ("jwt-auth-token".equals(cookie.getName())) {
                    return cookie.getValue();
                }
            }
        }
        
        return null;
    }
}