package com.awal.cineq.publicapi.banner.controller;

import com.awal.cineq.dto.ApiResponse;
import com.awal.cineq.publicapi.banner.dto.BannerResponse;
import com.awal.cineq.publicapi.banner.service.PublicBannerService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * Public Banner Controller
 *
 * Provides a completely open, no-auth endpoint for the customer portal
 * to fetch active banners for the homepage carousel / hero section.
 *
 * Route: GET /public/banners
 *
 * WHY no @PreAuthorize?
 * SecurityConfig already has .requestMatchers("/public/**").permitAll()
 * so no JWT or role checks are applied to this controller at all.
 *
 * Response structure:
 * {
 *   "success": true,
 *   "message": "Banners fetched successfully",
 *   "data": [
 *     {
 *       "id": "...",
 *       "title": "Jaari 2",
 *       "slug": "jaari-2",
 *       "description": "...",
 *       "imageUrl": "https://...",
 *       "imageAltText": "jaari2-image",
 *       "order": 1,
 *       "buttons": [
 *         { "title": "Watch Trailer", "redirectLink": "https://...", "buttonType": "primary", "openInNewTab": true }
 *       ]
 *     }
 *   ],
 *   "timestamp": "2026-03-05T10:00:00"
 * }
 */
@RestController
@RequestMapping("/public/banners")
@RequiredArgsConstructor
@Slf4j
public class PublicBannerController {

    private final PublicBannerService publicBannerService;

    /**
     * GET /public/banners
     *
     * Returns all active banners sorted by {@code order} ascending.
     * Called by the frontend on page load to render the homepage carousel.
     *
     * @return {@link ApiResponse} wrapping a list of {@link BannerResponse}
     */
    @GetMapping
    public ResponseEntity<ApiResponse<List<BannerResponse>>> getAllBanners() {
        log.info("GET /public/banners STARTED");

        try {
            List<BannerResponse> data = publicBannerService.getAllBanners();

            log.info("GET /public/banners END – {} banners returned", data.size());

            return ResponseEntity.ok(
                    ApiResponse.success("Banners fetched successfully", data)
            );

        } catch (Exception e) {
            log.error("GET /public/banners ERROR", e);
            return ResponseEntity.internalServerError().body(
                    ApiResponse.error("Failed to fetch banners: " + e.getMessage())
            );
        }
    }
}

