package com.awal.cineq.common.util;

import java.security.SecureRandom;
import java.util.Base64;

/**
 * Utility class for generating cryptographically secure random tokens
 * Replaces UUID.randomUUID() which is not cryptographically secure
 */
public class SecureTokenGenerator {
    
    private static final SecureRandom secureRandom = new SecureRandom();
    private static final int TOKEN_LENGTH = 32; // 256 bits
    
    /**
     * Private constructor to prevent instantiation
     */
    private SecureTokenGenerator() {
        throw new UnsupportedOperationException("Utility class cannot be instantiated");
    }
    
    /**
     * Generate a cryptographically secure random token
     * 
     * @return URL-safe Base64 encoded token string (256 bits)
     */
    public static String generateToken() {
        byte[] tokenBytes = new byte[TOKEN_LENGTH];
        secureRandom.nextBytes(tokenBytes);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(tokenBytes);
    }
    
    /**
     * Generate a cryptographically secure random token with custom length
     * 
     * @param length Number of random bytes (not Base64 length)
     * @return URL-safe Base64 encoded token string
     */
    public static String generateToken(int length) {
        if (length <= 0) {
            throw new IllegalArgumentException("Token length must be positive");
        }
        byte[] tokenBytes = new byte[length];
        secureRandom.nextBytes(tokenBytes);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(tokenBytes);
    }
}
