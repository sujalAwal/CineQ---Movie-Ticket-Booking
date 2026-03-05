package com.awal.cineq.publicapi.banner.repository;

import com.awal.cineq.publicapi.banner.model.Banner;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.data.mongodb.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;

/**
 * Repository for {@link Banner}.
 *
 * All read queries exclude soft-deleted records ({@code deletedAt != null})
 * and only return active banners ({@code isActive: true}).
 */
@Repository
public interface BannerRepository extends MongoRepository<Banner, String> {

    /**
     * Returns all active, non-deleted banners.
     * Used by the public API to serve carousel/hero slides.
     * Results are sorted in Java by {@code order} ascending.
     */
    @Query("{ 'isActive': true, 'deletedAt': null }")
    List<Banner> findAllActiveBanners();
}

