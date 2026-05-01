package com.awal.cineq.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.domain.AuditorAware;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;

import java.util.Optional;

// @Configuration // Disabled for MongoDB-first development
@EnableJpaAuditing(auditorAwareRef = "auditorProvider")
public class JpaConfig {

    // This bean is now provided by MongoConfig
    // @Bean
    // public AuditorAware<String> auditorProvider() {
    //     return new AuditorAwareImpl();
    // }

    // AuditorAwareImpl is also not needed here anymore as it's in MongoConfig
    /*
    public static class AuditorAwareImpl implements AuditorAware<String> {
        @Override
        public Optional<String> getCurrentAuditor() {
            // In a real application, you would get this from the security context
            // For now, return a default value
            return Optional.of("system");
        }
    }
    */
}