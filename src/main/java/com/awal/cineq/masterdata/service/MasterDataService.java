package com.awal.cineq.masterdata.service;

import com.awal.cineq.masterdata.dto.MasterDataResponse;

/**
 * Service interface for master data / reference data
 *
 * Provides enum and configuration data to frontend
 * for populating dropdowns, select fields, and form options
 *
 * WHY separate service?
 * - Centralized reference data management
 * - Easy to add new enums later
 * - Caching opportunity for frequently accessed data
 * - Consistent response format
 */
public interface MasterDataService {

    /**
     * Get all master data (enums and reference data)
     * Frontend calls this once on page load to populate all dropdowns
     *
     * @return MasterDataResponse containing all available enums
     */
    MasterDataResponse getAllMasterData();

    /**
     * Get only FormAction enum data
     * More lightweight if frontend only needs actions
     *
     * @return MasterDataResponse with formActions field populated
     */
    MasterDataResponse getFormActions();
}

