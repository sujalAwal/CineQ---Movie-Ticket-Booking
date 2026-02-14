package com.awal.cineq.customer.validation;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.HashSet;
import java.util.Set;
import java.util.regex.Pattern;

/**
 * Utility class for checking password strength and complexity
 */
public class PasswordStrengthChecker {
    
    private static final int MIN_LENGTH = 8;
    private static final Pattern UPPERCASE_PATTERN = Pattern.compile("[A-Z]");
    private static final Pattern LOWERCASE_PATTERN = Pattern.compile("[a-z]");
    private static final Pattern DIGIT_PATTERN = Pattern.compile("[0-9]");
    private static final Pattern SPECIAL_CHAR_PATTERN = Pattern.compile("[^A-Za-z0-9]");
    
    private static final Set<String> COMMON_PASSWORDS = new HashSet<>();
    
    static {
        loadCommonPasswords();
    }
    
    /**
     * Private constructor to prevent instantiation
     */
    private PasswordStrengthChecker() {
        throw new UnsupportedOperationException("Utility class cannot be instantiated");
    }
    
    /**
     * Validate password against all complexity requirements
     * 
     * @param password The password to validate
     * @return Validation result with details
     */
    public static PasswordValidationResult validate(String password) {
        PasswordValidationResult result = new PasswordValidationResult();
        
        if (password == null || password.isEmpty()) {
            result.setValid(false);
            result.addError("Password cannot be empty");
            return result;
        }
        
        // Check minimum length
        if (password.length() < MIN_LENGTH) {
            result.setValid(false);
            result.addError("Password must be at least " + MIN_LENGTH + " characters long");
        }
        
        // Check for uppercase letter
        if (!UPPERCASE_PATTERN.matcher(password).find()) {
            result.setValid(false);
            result.addError("Password must contain at least one uppercase letter");
        }
        
        // Check for lowercase letter
        if (!LOWERCASE_PATTERN.matcher(password).find()) {
            result.setValid(false);
            result.addError("Password must contain at least one lowercase letter");
        }
        
        // Check for digit
        if (!DIGIT_PATTERN.matcher(password).find()) {
            result.setValid(false);
            result.addError("Password must contain at least one digit");
        }
        
        // Check for special character
        if (!SPECIAL_CHAR_PATTERN.matcher(password).find()) {
            result.setValid(false);
            result.addError("Password must contain at least one special character");
        }
        
        // Check against common passwords
        if (COMMON_PASSWORDS.contains(password.toLowerCase())) {
            result.setValid(false);
            result.addError("Password is too common. Please choose a more secure password");
        }
        
        return result;
    }
    
    /**
     * Load common passwords from resource file
     */
    private static void loadCommonPasswords() {
        try (InputStream is = PasswordStrengthChecker.class.getResourceAsStream("/common-passwords.txt")) {
            if (is != null) {
                try (BufferedReader reader = new BufferedReader(new InputStreamReader(is, StandardCharsets.UTF_8))) {
                    String line;
                    while ((line = reader.readLine()) != null) {
                        String trimmed = line.trim();
                        if (!trimmed.isEmpty() && !trimmed.startsWith("#")) {
                            COMMON_PASSWORDS.add(trimmed.toLowerCase());
                        }
                    }
                }
            } else {
                // If file doesn't exist, add some basic common passwords
                COMMON_PASSWORDS.add("password");
                COMMON_PASSWORDS.add("123456");
                COMMON_PASSWORDS.add("12345678");
                COMMON_PASSWORDS.add("qwerty");
                COMMON_PASSWORDS.add("abc123");
                COMMON_PASSWORDS.add("password123");
                COMMON_PASSWORDS.add("admin");
                COMMON_PASSWORDS.add("letmein");
                COMMON_PASSWORDS.add("welcome");
                COMMON_PASSWORDS.add("monkey");
            }
        } catch (Exception e) {
            // Log error but don't fail - just use basic list
            System.err.println("Warning: Could not load common passwords file: " + e.getMessage());
        }
    }
    
    /**
     * Result object for password validation
     */
    public static class PasswordValidationResult {
        private boolean valid = true;
        private final Set<String> errors = new HashSet<>();
        
        public boolean isValid() {
            return valid;
        }
        
        public void setValid(boolean valid) {
            this.valid = valid;
        }
        
        public void addError(String error) {
            this.errors.add(error);
        }
        
        public Set<String> getErrors() {
            return errors;
        }
        
        public String getErrorMessage() {
            return String.join("; ", errors);
        }
    }
}
