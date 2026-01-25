package com.awal.cineq.masterdata.controller;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.awal.cineq.dto.ApiResponse;
import com.awal.cineq.masterdata.dto.MasterDataResponse;
import com.awal.cineq.masterdata.service.MasterDataService;

/**
 * Master Data Controller
 *
 * Provides endpoints for frontend to fetch enum and reference data
 * Used to populate dropdowns, select fields, and form options
 *
 * REST API:
 * GET master-data              - Get all master data
 * GET master-data/form-actions - Get only FormAction enum
 *
 * WHY REST architecture?
 * - Stateless (easier for frontend caching)
 * - Cacheable (CDN can cache these responses)
 * - Standard HTTP (easy to use with fetch/axios)
 *
 * Frontend Usage:
 * 1. On page load, fetch master-data
 * 2. Store formActions in frontend state/context
 * 3. When submitting form, use the action codes/values
 *
 * Example form submission:
 * {
 *   "action": "C",  // or use code: 1
 *   "formData": { ... },
 *   "stepId": "..."
 * }
 */
@RestController
@RequestMapping("master-data")
@RequiredArgsConstructor
@Slf4j
public class MasterDataController {

    private final MasterDataService masterDataService;

    /**
     * Get all master data (enums and reference data)
     *
     * Frontend calls this on initial page load
     * Caches the response in Redux/Context/Local Storage
     *
     * @return ApiResponse containing MasterDataResponse with all enums
     *
     * Example response:
     * {
     *   "success": true,
     *   "message": "Master data fetched successfully",
     *   "data": {
     *     "formActions": [
     *       {
     *         "code": 1,
     *         "charCode": "C",
     *         "actionName": "create",
     *         "description": "Create new document",
     *         "readOnly": false,
     *         "writeAction": true,
     *         "requiresId": false,
     *         "allCodes": "1 (C)"
     *       },
     *       ...more actions...
     *     ]
     *   },
     *   "timestamp": "2025-01-10T10:30:00"
     * }
     */
    @GetMapping
    public ResponseEntity<ApiResponse<MasterDataResponse>> getAllMasterData() {
        log.info("GET master-data STARTED");

        try {
            MasterDataResponse data = masterDataService.getAllMasterData();

            log.info("GET master-data END");

            ApiResponse<MasterDataResponse> response = ApiResponse.success(
                    "Master data fetched successfully",
                    data
            );
            return ResponseEntity.ok(response);

        } catch (Exception e) {
            log.error("GET master-data ERROR", e);
            ApiResponse<MasterDataResponse> response = ApiResponse.error(
                    "Failed to fetch master data: " + e.getMessage()
            );
            return ResponseEntity.badRequest().body(response);
        }
    }

    /**
     * Get only FormAction enum data
     *
     * Lightweight endpoint if frontend only needs FormAction
     * Useful for specific forms that only care about actions
     *
     * @return ApiResponse containing MasterDataResponse with formActions
     *
     * Example usage:
     * Frontend can call either:
     * 1. GET master-data (get all)
     * 2. GET master-data/form-actions (get only actions)
     */
    @GetMapping("/form-actions")
    public ResponseEntity<ApiResponse<MasterDataResponse>> getFormActions() {
        log.info("GET master-data/form-actions STARTED");

        try {
            MasterDataResponse data = masterDataService.getFormActions();

            log.info("GET master-data/form-actions END");

            ApiResponse<MasterDataResponse> response = ApiResponse.success(
                    "Form actions fetched successfully",
                    data
            );
            return ResponseEntity.ok(response);

        } catch (Exception e) {
            log.error("GET master-data/form-actions ERROR", e);
            ApiResponse<MasterDataResponse> response = ApiResponse.error(
                    "Failed to fetch form actions: " + e.getMessage()
            );
            return ResponseEntity.badRequest().body(response);
        }
    }
}

