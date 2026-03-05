package com.awal.cineq.publicapi.settings.service;

import com.awal.cineq.publicapi.settings.dto.SettingResponse;

import java.util.List;

/**
 * Service interface for the public settings API.
 *
 * Provides read-only access to active customer portal settings.
 * No write operations are exposed — this is a public read API only.
 */
public interface PublicSettingService {

    /**
     * Returns all active, non-deleted portal settings as a flat list.
     * Ordered by group then by title for predictable frontend rendering.
     *
     * @return list of {@link SettingResponse}, never null, may be empty
     */
    List<SettingResponse> getAllSettings();
}

