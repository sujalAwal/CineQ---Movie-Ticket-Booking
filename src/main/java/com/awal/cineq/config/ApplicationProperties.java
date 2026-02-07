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
}