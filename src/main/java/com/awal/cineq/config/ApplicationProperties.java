package com.awal.cineq.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;
import lombok.Data;

@Configuration
@ConfigurationProperties(prefix = "app")
@Data
public class ApplicationProperties {
    
    private Jwt jwt = new Jwt();
    private File file = new File();
    
    @Data
    public static class Jwt {
        private String secret = "mySecretKey";
        private long expiration = 86400; // 24 hours in seconds
    }
    
    @Data
    public static class File {
        private String uploadDir = "uploads/";
        private long maxSize = 10485760; // 10MB in bytes
        private String[] allowedTypes = {"image/jpeg", "image/png", "image/gif"};
    }
}