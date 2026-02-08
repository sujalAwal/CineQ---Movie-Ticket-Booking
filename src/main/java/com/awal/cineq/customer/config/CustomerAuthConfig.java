package com.awal.cineq.customer.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

@Configuration
@ConfigurationProperties(prefix = "app.customer")
@Data
public class CustomerAuthConfig {
    
    /**
     * If true, customers must verify email before login
     * Env: CUSTOMER_EMAIL_VERIFICATION (default: false)
     */
    private boolean emailVerificationRequired = false;
    
    /**
     * Maximum failed login attempts before lockout
     * Env: CUSTOMER_MAX_FAILED_ATTEMPTS (default: 10)
     */
    private int maxFailedLoginAttempts = 10;
    
    /**
     * Account lockout duration in minutes after max failed attempts
     * Env: CUSTOMER_LOCKOUT_MINUTES (default: 60)
     */
    private int lockoutDurationMinutes = 60;
    
    /**
     * Password reset token validity in hours
     * Env: CUSTOMER_RESET_TOKEN_EXPIRY (default: 24)
     */
    private int passwordResetTokenExpiryHours = 24;
    
    /**
     * Comma-separated list of admin emails for notifications
     * Env: ADMIN_NOTIFICATION_EMAILS (e.g., "admin1@example.com,admin2@example.com")
     */
    private String adminNotificationEmails = "";
    
    /**
     * Frontend application URL (for email links)
     * Env: APP_FRONTEND_URL
     */
    private String frontendUrl = "http://localhost:3000";
    
    /**
     * Admin panel URL (for email links)
     * Env: APP_ADMIN_URL  
     */
    private String adminUrl = "http://localhost:3000/admin";
    
    /**
     * Get admin emails as a list
     * @return List of admin email addresses
     */
    public List<String> getAdminEmailList() {
        if (adminNotificationEmails == null || adminNotificationEmails.trim().isEmpty()) {
            return new ArrayList<>();
        }
        return Arrays.stream(adminNotificationEmails.split(","))
                .map(String::trim)
                .filter(email -> !email.isEmpty())
                .toList();
    }
    
    /**
     * Check if admin notifications are enabled
     * @return true if at least one admin email is configured
     */
    public boolean isAdminNotificationEnabled() {
        return !getAdminEmailList().isEmpty();
    }
}
