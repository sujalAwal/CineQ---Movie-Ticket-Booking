package com.awal.cineq.config;

import io.github.bucket4j.Bandwidth;
import io.github.bucket4j.Bucket;
import io.github.bucket4j.Refill;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Service for managing rate limit buckets per IP address
 * Uses Bucket4j for token bucket algorithm implementation
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class RateLimitService {
    
    private final RateLimitConfig config;
    
    // Map of IP address + endpoint to bucket
    private final Map<String, Bucket> cache = new ConcurrentHashMap<>();
    
    /**
     * Resolve bucket for login endpoint
     * 
     * @param key IP address or user identifier
     * @return Bucket for rate limiting
     */
    public Bucket resolveLoginBucket(String key) {
        return cache.computeIfAbsent(key + ":login", k -> createBucket(
            config.getLoginCapacity(),
            Duration.ofMinutes(config.getLoginRefillMinutes())
        ));
    }
    
    /**
     * Resolve bucket for register endpoint
     * 
     * @param key IP address or user identifier
     * @return Bucket for rate limiting
     */
    public Bucket resolveRegisterBucket(String key) {
        return cache.computeIfAbsent(key + ":register", k -> createBucket(
            config.getRegisterCapacity(),
            Duration.ofMinutes(config.getRegisterRefillMinutes())
        ));
    }
    
    /**
     * Resolve bucket for password reset endpoint
     * 
     * @param key IP address or user identifier
     * @return Bucket for rate limiting
     */
    public Bucket resolvePasswordResetBucket(String key) {
        return cache.computeIfAbsent(key + ":password-reset", k -> createBucket(
            config.getPasswordResetCapacity(),
            Duration.ofMinutes(config.getPasswordResetRefillMinutes())
        ));
    }
    
    /**
     * Resolve bucket for email verification endpoint
     * 
     * @param key IP address or user identifier
     * @return Bucket for rate limiting
     */
    public Bucket resolveEmailVerificationBucket(String key) {
        return cache.computeIfAbsent(key + ":email-verification", k -> createBucket(
            config.getEmailVerificationCapacity(),
            Duration.ofMinutes(config.getEmailVerificationRefillMinutes())
        ));
    }
    
    /**
     * Create a new bucket with specified capacity and refill duration
     * 
     * @param capacity Maximum number of tokens
     * @param refillDuration Duration to refill tokens
     * @return New bucket instance
     */
    private Bucket createBucket(int capacity, Duration refillDuration) {
        Bandwidth limit = Bandwidth.classic(capacity, Refill.intervally(capacity, refillDuration));
        return Bucket.builder()
            .addLimit(limit)
            .build();
    }
    
    /**
     * Check if rate limiting is enabled
     * 
     * @return true if enabled, false otherwise
     */
    public boolean isEnabled() {
        return config.isEnabled();
    }
    
    /**
     * Clear all rate limit buckets (for testing/admin purposes)
     */
    public void clearAll() {
        cache.clear();
        log.info("All rate limit buckets cleared");
    }
    
    /**
     * Get number of cached buckets
     * 
     * @return Cache size
     */
    public int getCacheSize() {
        return cache.size();
    }
}
