package com.awal.cineq.customer.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

/**
 * Configuration for HttpOnly authentication cookies
 * Provides centralized cookie settings
 */
@Configuration
@ConfigurationProperties(prefix = "app.security.cookie")
@Data
public class CookieConfig {
    
    /**
     * Cookie name for JWT authentication
     * Default: auth_token
     */
    private String name = "auth_token";
    
    /**
     * Enable secure flag (HTTPS only)
     * Should be true in production, false for local dev
     * Default: true
     */
    private boolean secure = true;
    
    /**
     * Enable HttpOnly flag (prevents JavaScript access)
     * Should always be true for security
     * Default: true
     */
    private boolean httpOnly = true;
    
    /**
     * SameSite policy for CSRF protection
     * Options: Strict, Lax, None
     * Default: Strict
     */
    private String sameSite = "Strict";
    
    /**
     * Cookie max age in seconds
     * Default: 86400 (24 hours)
     */
    private int maxAge = 86400;
    
    /**
     * Cookie path
     * Default: / (all paths)
     */
    private String path = "/";
}
