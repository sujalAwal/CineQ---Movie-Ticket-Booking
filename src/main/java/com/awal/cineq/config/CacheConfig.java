package com.awal.cineq.config;

import org.springframework.cache.CacheManager;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.cache.concurrent.ConcurrentMapCacheManager;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
@EnableCaching
public class CacheConfig {

    @Bean
    public CacheManager cacheManager() {
        ConcurrentMapCacheManager cacheManager = new ConcurrentMapCacheManager(
                "sidebarFolders",      // Existing cache
                "formManager",          // FormConfigCacheService - FormManager by slug/id
                "formStep",             // FormConfigCacheService - FormStep by id/managerId+slug
                "formStepList"          // FormConfigCacheService - List of FormSteps by managerId
        );
        return cacheManager;
    }
}
