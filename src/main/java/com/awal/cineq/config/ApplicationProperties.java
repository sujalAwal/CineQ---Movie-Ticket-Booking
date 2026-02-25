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
    }
}