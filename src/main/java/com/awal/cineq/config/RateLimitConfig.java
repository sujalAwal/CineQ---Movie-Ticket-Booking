package com.awal.cineq.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

/**
 * Configuration for rate limiting on authentication endpoints
 * Prevents brute force attacks and abuse
 */
@Configuration
@ConfigurationProperties(prefix = "app.security.ratelimit")
@Data
public class RateLimitConfig {
    
    /**
     * Enable or disable rate limiting
     * Default: true
     */
    private boolean enabled = true;
    
    // Login endpoint rate limits
    private int loginCapacity = 20;  // 20 attempts
    private int loginRefillMinutes = 15;  // per 15 minutes
    
    // Register endpoint rate limits
    private int registerCapacity = 10;  // 10 attempts
    private int registerRefillMinutes = 60;  // per hour
    
    // Password reset endpoint rate limits
    private int passwordResetCapacity = 3;  // 3 attempts
    private int passwordResetRefillMinutes = 60;  // per hour
    
    // Email verification endpoint rate limits
    private int emailVerificationCapacity = 10;  // 10 attempts
    private int emailVerificationRefillMinutes = 60;  // per hour
}
