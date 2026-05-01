package com.awal.cineq.customer.service;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.concurrent.TimeUnit;

/**
 * Service for managing blacklisted JWT tokens
 * Uses Caffeine cache for in-memory token blacklist with automatic expiration
 * Prevents reuse of tokens after logout
 */
@Service
@Slf4j
public class TokenBlacklistService {
    
    private final Cache<String, Boolean> blacklistedTokens;
    
    public TokenBlacklistService() {
        // Initialize cache with 24-hour expiration (matching JWT token lifetime)
        // Maximum 10,000 blacklisted tokens in memory
        this.blacklistedTokens = Caffeine.newBuilder()
            .expireAfterWrite(24, TimeUnit.HOURS)
            .maximumSize(10000)
            .build();
        
        log.info("TokenBlacklistService initialized with 24-hour expiration and 10K max size");
    }
    
    /**
     * Add a token to the blacklist
     * 
     * @param token JWT token to blacklist
     */
    public void blacklistToken(String token) {
        if (token != null && !token.isEmpty()) {
            blacklistedTokens.put(token, Boolean.TRUE);
            log.debug("Token blacklisted successfully");
        }
    }
    
    /**
     * Check if a token is blacklisted
     * 
     * @param token JWT token to check
     * @return true if token is blacklisted, false otherwise
     */
    public boolean isBlacklisted(String token) {
        if (token == null || token.isEmpty()) {
            return false;
        }
        
        Boolean isBlacklisted = blacklistedTokens.getIfPresent(token);
        return isBlacklisted != null && isBlacklisted;
    }
    
    /**
     * Remove a token from the blacklist (for testing/admin purposes)
     * 
     * @param token JWT token to remove
     */
    public void removeFromBlacklist(String token) {
        if (token != null && !token.isEmpty()) {
            blacklistedTokens.invalidate(token);
            log.debug("Token removed from blacklist");
        }
    }
    
    /**
     * Get current blacklist size
     * 
     * @return Number of blacklisted tokens
     */
    public long getBlacklistSize() {
        return blacklistedTokens.estimatedSize();
    }
    
    /**
     * Clear all blacklisted tokens (for admin/testing purposes)
     */
    public void clearBlacklist() {
        blacklistedTokens.invalidateAll();
        log.info("Token blacklist cleared");
    }
}
