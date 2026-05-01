package com.awal.cineq.publicapi.banner.service;

import com.awal.cineq.publicapi.banner.dto.BannerResponse;

import java.util.List;

/**
 * Service interface for the public banners API.
 *
 * Provides read-only access to active banners for the customer portal.
 * No write operations — this is a public read API only.
 */
public interface PublicBannerService {

    /**
     * Returns all active, non-deleted banners sorted by {@code order} ascending.
     * Called by the frontend to populate the homepage carousel/hero section.
     *
     * @return list of {@link BannerResponse}, never null, may be empty
     */
    List<BannerResponse> getAllBanners();
}

