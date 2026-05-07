package com.awal.cineq.config;

import jakarta.validation.constraints.NotBlank;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;
import lombok.Data;

@Configuration
@ConfigurationProperties(prefix = "app")
@Data
public class ApplicationProperties {

    private Jwt jwt = new Jwt();
    private File file = new File();
    private Security security = new Security();
    private Email email = new Email();
    private App app = new App();
    private Customer customer = new Customer();
    private Payment payment = new Payment();
    private Token token = new Token();

    @Data
    public static class Token {
        private int expiryHours = 24;
        private int maxResendPerHour = 3;
    }

    @Data
    public static class Jwt {
        private String secret = "mySecretKey";
        private long expiration = 86400;
    }
    
    @Data
    public static class File {
        private String uploadDir = "uploads/";
        private long maxSize = 10485760;
        private String[] allowedTypes = {"image/jpeg", "image/png", "image/gif"};
    }

    @Data
    public static class Security {
        @NotBlank(message = "Prominent role must be specified")
        private String prominentRole ;

        private String authorizationProvider = "RBAC";
        private Cookie cookie = new Cookie();

        @Data
        public static class Cookie {
            // @Value cannot be used in nested static classes for ConfigurationProperties easily without setters
            // We'll rely on the getter of ApplicationProperties.Jwt to access jwt expiration
            private String name = "jwt-auth-token";
            private boolean secure = true;
            private boolean httpOnly = true;
            private String sameSite = "Strict";
            private int maxAge = 86400;
            private String path = "/";
        }
    }

    @Data
    public static class Email {
        private String smtpHost = "smtp.gmail.com";
        private int smtpPort = 587;
        private String smtpUsername;
        private String smtpPassword;
        private String fromAddress;
        private String fromName = "CineQ";  // Display name for sender
        private boolean enableTls = true;
        private boolean enableSsl = false;
    }

    @Data
    public static class App {
        private String url = "http://localhost:8080";  // Backend API URL
        private String googleClientId ;
        private String defaultUserPassword = "default@Password123"; // Default password for new users
    }

    @Data
    public static class Customer {
        private boolean emailVerificationRequired = false;
        private int maxFailedLoginAttempts = 10;
        private int lockoutDurationMinutes = 60;
        private int passwordResetTokenExpiryHours = 24;
        private String adminNotificationEmails = "";
        private String frontendUrl = "http://localhost:3000";
        private String adminUrl = "http://localhost:3000/admin";

        public java.util.List<String> getAdminEmailList() {
            if (adminNotificationEmails == null || adminNotificationEmails.trim().isEmpty()) {
                return new java.util.ArrayList<>();
            }
            return java.util.Arrays.stream(adminNotificationEmails.split(","))
                    .map(String::trim)
                    .filter(email -> !email.isEmpty())
                    .toList();
        }

        public boolean isAdminNotificationEnabled() {
            return !getAdminEmailList().isEmpty();
        }
    }

    @Data
    public static class Payment {
        private Esewa esewa = new Esewa();
        private Khalti khalti = new Khalti();
        private String successUrl = "http://localhost:3000/payment/success";
        private String failureUrl = "http://localhost:3000/payment/failure";
        private String websiteUrl = "http://localhost:3000";

        @Data
        public static class Esewa {
            private String merchantCode = "EPAYTEST";
            private String secretKey = "8gBm/:&EnhH.1/q";
            private String paymentUrl = "https://rc-epay.esewa.com.np/api/epay/main/v2/form";
        }

        @Data
        public static class Khalti {
            private String secretKey = "test_secret_key_placeholder";
            private String initiateUrl = "https://a.khalti.com/api/v2/epayment/initiate/";
        }
    }
}