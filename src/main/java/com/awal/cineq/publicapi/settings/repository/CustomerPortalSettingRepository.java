package com.awal.cineq.publicapi.settings.repository;

import com.awal.cineq.publicapi.settings.model.CustomerPortalSetting;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.data.mongodb.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;

/**
 * Repository for {@link CustomerPortalSetting}.
 *
 * All read queries exclude soft-deleted records ({@code deletedAt != null})
 * and only return active settings ({@code isActive: true}).
 */
@Repository
public interface CustomerPortalSettingRepository extends MongoRepository<CustomerPortalSetting, String> {

    /**
     * Returns all settings that are active and not soft-deleted.
     * Used by the public API endpoint to serve portal configuration.
     */
    @Query("{ 'isActive': true, 'deletedAt': null }")
    List<CustomerPortalSetting> findAllActiveSettings();

    /**
     * Returns active settings filtered by group (e.g. "general", "seo").
     * Useful for future group-scoped endpoints.
     */
    @Query("{ 'group': ?0, 'isActive': true, 'deletedAt': null }")
    List<CustomerPortalSetting> findAllActiveByGroup(String group);

    /**
     * Find a single active setting by its slug (e.g. "site-name").
     */
    @Query("{ 'slug': ?0, 'isActive': true, 'deletedAt': null }")
    java.util.Optional<CustomerPortalSetting> findActiveBySlug(String slug);
}

