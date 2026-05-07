package com.awal.cineq.config;

import io.github.bucket4j.Bucket;
import io.github.bucket4j.ConsumptionProbe;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

/**
 * Servlet filter for rate limiting authentication endpoints
 * Uses Bucket4j token bucket algorithm to prevent brute force attacks
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class RateLimitingFilter extends OncePerRequestFilter {
    
    private final RateLimitService rateLimitService;
    
    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {
        
        // Skip if rate limiting is disabled
        if (!rateLimitService.isEnabled()) {
            filterChain.doFilter(request, response);
            return;
        }
        
        String path = request.getServletPath();
        String ip = getClientIP(request);
        
        Bucket bucket = resolveBucket(path, ip);
        
        // If no bucket (endpoint not rate-limited), continue
        if (bucket == null) {
            filterChain.doFilter(request, response);
            return;
        }
        
        // Try to consume a token
        ConsumptionProbe probe = bucket.tryConsumeAndReturnRemaining(1);
        
        if (probe.isConsumed()) {
            // Add rate limit headers
            response.addHeader("X-Rate-Limit-Remaining", String.valueOf(probe.getRemainingTokens()));
            filterChain.doFilter(request, response);
        } else {
            // Rate limit exceeded
            long waitForRefill = probe.getNanosToWaitForRefill() / 1_000_000_000;
            
            response.setStatus(HttpStatus.TOO_MANY_REQUESTS.value());
            response.setContentType(MediaType.APPLICATION_JSON_VALUE);
            response.addHeader("X-Rate-Limit-Retry-After-Seconds", String.valueOf(waitForRefill));
            response.addHeader("Retry-After", String.valueOf(waitForRefill));
            
            String jsonResponse = String.format(
                "{\"status\":\"error\",\"message\":\"Rate limit exceeded. Please try again in %d seconds.\",\"code\":429}",
                waitForRefill
            );
            
            response.getWriter().write(jsonResponse);
            
            log.warn("Rate limit exceeded for IP: {} on endpoint: {}", ip, path);
        }
    }
    
    /**
     * Resolve the appropriate bucket based on the request path
     * 
     * @param path Request path
     * @param ip Client IP address
     * @return Bucket for rate limiting, or null if path is not rate-limited
     */
    private Bucket resolveBucket(String path, String ip) {
        if (path.endsWith("/login")) {
            return rateLimitService.resolveLoginBucket(ip);
        } else if (path.endsWith("/register")) {
            return rateLimitService.resolveRegisterBucket(ip);
        } else if (path.endsWith("/forgot-password") || path.endsWith("/reset-password")) {
            return rateLimitService.resolvePasswordResetBucket(ip);
        } else if (path.endsWith("/verify-email") || path.endsWith("/resend-verification")) {
            return rateLimitService.resolveEmailVerificationBucket(ip);
        } else if (path.endsWith("/auth/resend-link")) {
            return rateLimitService.resolveResendLinkBucket(ip);
        }
        
        // Path not rate-limited
        return null;
    }
    
    /**
     * Extract client IP address from request
     * Handles X-Forwarded-For header for proxy/load balancer scenarios
     * 
     * @param request HTTP request
     * @return Client IP address
     */
    private String getClientIP(HttpServletRequest request) {
        String xForwardedFor = request.getHeader("X-Forwarded-For");
        if (xForwardedFor != null && !xForwardedFor.isEmpty()) {
            // X-Forwarded-For can contain multiple IPs, take the first one
            return xForwardedFor.split(",")[0].trim();
        }
        
        String xRealIP = request.getHeader("X-Real-IP");
        if (xRealIP != null && !xRealIP.isEmpty()) {
            return xRealIP;
        }
        
        return request.getRemoteAddr();
    }
}
