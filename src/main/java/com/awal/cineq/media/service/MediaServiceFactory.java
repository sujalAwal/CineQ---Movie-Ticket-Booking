package com.awal.cineq.media.service;


import com.awal.cineq.client.supabase.config.SupabaseConfig;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import com.awal.cineq.media.config.StorageType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Map;

@Component
public class MediaServiceFactory {
    private static final Logger logger = LoggerFactory.getLogger(MediaServiceFactory.class);

    private final SupabaseConfig storageConfig;
    private final Map<StorageType, MediaService> services;

    @Value("${app.file.storage.type:LOCAL}")
    private StorageType storageType;
    public MediaServiceFactory(
            SupabaseConfig storageConfig,
            @Qualifier("localMediaService") MediaService localMediaService,
            @Qualifier("supabaseMediaService") MediaService supabaseMediaService) {
        this.storageConfig = storageConfig;
        this.services = Map.of(
                StorageType.LOCAL, localMediaService,
                StorageType.SUPABASE, supabaseMediaService
        );
        logger.info("MediaServiceFactory initialized. Injected services: LOCAL={}, SUPABASE={}",
                localMediaService.getClass().getName(), supabaseMediaService.getClass().getName());
    }

    public MediaService getMediaService() {
        logger.info("getMediaService called. Requested storageType: {}", this.storageType);
        MediaService service = services.get(this.storageType);
        if (service == null) {
            logger.error("Unsupported storage type: {}. Available types: {}", this.storageType, services.keySet());
            throw new IllegalArgumentException(
                    "Unsupported storage type: " + this.storageType + ". Available types: " + services.keySet());
        }
        logger.info("Returning MediaService implementation: {} for storageType: {}", service.getClass().getName(), this.storageType);
        return service;
    }

    public MediaService getMediaService(StorageType storageType) {
        logger.info("getMediaService(storageType) called. Requested storageType: {}", storageType);
        MediaService service = services.get(storageType);
        if (service == null) {
            logger.error("Unsupported storage type: {}. Available types: {}", storageType, services.keySet());
            throw new IllegalArgumentException(
                    "Unsupported storage type: " + storageType + ". Available types: " + services.keySet());
        }
        logger.info("Returning MediaService implementation: {} for storageType: {}", service.getClass().getName(), storageType);
        return service;
    }

    public Map<StorageType, MediaService> getAvailableServices() {
        logger.info("getAvailableServices called. Available services: {}", services);
        return Map.copyOf(services);
    }
}
