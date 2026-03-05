package com.awal.cineq.publicapi.settings.controller;

import com.awal.cineq.dto.ApiResponse;
import com.awal.cineq.publicapi.settings.dto.SettingResponse;
import com.awal.cineq.publicapi.settings.service.PublicSettingService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * Public Settings Controller
 *
 * Provides a completely open, no-auth endpoint for the customer portal
 * to fetch site-wide configuration (branding, contact info, SEO, etc.).
 *
 * Route: GET /public/settings
 *
 * WHY no @PreAuthorize?
 * SecurityConfig already has .requestMatchers("/public/**").permitAll()
 * so no JWT or role checks are applied to this controller at all.
 *
 * Response structure:
 * {
 *   "success": true,
 *   "message": "Settings fetched successfully",
 *   "data": [
 *     { "id": "...", "title": "Site Name", "slug": "site-name", "type": "text", "value": "CineQ", "group": "general" },
 *     ...
 *   ],
 *   "timestamp": "2026-03-05T10:00:00"
 * }
 */
@RestController
@RequestMapping("/public/settings")
@RequiredArgsConstructor
@Slf4j
public class PublicSettingController {

    private final PublicSettingService publicSettingService;

    /**
     * GET /public/settings
     *
     * Returns all active portal settings as a flat list, sorted by group then title.
     * Called by the frontend on initial page load to bootstrap site configuration.
     *
     * @return {@link ApiResponse} wrapping a list of {@link SettingResponse}
     */
    @GetMapping
    public ResponseEntity<ApiResponse<List<SettingResponse>>> getAllSettings() {
        log.info("GET /public/settings STARTED");

        try {
            List<SettingResponse> data = publicSettingService.getAllSettings();

            log.info("GET /public/settings END – {} settings returned", data.size());

            return ResponseEntity.ok(
                    ApiResponse.success("Settings fetched successfully", data)
            );

        } catch (Exception e) {
            log.error("GET /public/settings ERROR", e);
            return ResponseEntity.internalServerError().body(
                    ApiResponse.error("Failed to fetch settings: " + e.getMessage())
            );
        }
    }
}

