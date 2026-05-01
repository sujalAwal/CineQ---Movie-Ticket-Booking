package com.awal.cineq.publicapi.banner.service.impl;

import com.awal.cineq.publicapi.banner.dto.BannerButtonResponse;
import com.awal.cineq.publicapi.banner.dto.BannerResponse;
import com.awal.cineq.publicapi.banner.dto.DisplayConfigResponse;
import com.awal.cineq.publicapi.banner.model.Banner;
import com.awal.cineq.publicapi.banner.repository.BannerRepository;
import com.awal.cineq.publicapi.banner.service.PublicBannerService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Implementation of {@link PublicBannerService}.
 *
 * WHY @Transactional(readOnly = true)?
 * - Pure read operation – no database writes.
 * - Signals MongoDB driver to use a read-only session for better performance.
 * - Prevents accidental state mutation inside this service.
 */
@Service
@Transactional(readOnly = true)
@RequiredArgsConstructor
@Slf4j
public class PublicBannerServiceImpl implements PublicBannerService {

    private final BannerRepository bannerRepository;

    @Override
    public List<BannerResponse> getAllBanners() {
        log.info("getAllBanners STARTED");

        List<Banner> banners = bannerRepository.findAllActiveBanners();

        List<BannerResponse> response = banners.stream()
                .sorted(Comparator.comparingInt(b -> b.getOrder() != null ? b.getOrder() : Integer.MAX_VALUE))
                .map(this::mapToBannerResponse)
                .collect(Collectors.toList());

        log.info("getAllBanners END – returning {} banners", response.size());
        return response;
    }

    // ──────────────────────────────────────────────────────────────────────────
    // Mappers
    // ──────────────────────────────────────────────────────────────────────────

    /**
     * Maps a {@link Banner} entity to a {@link BannerResponse} DTO.
     * Internal fields (formManagerId, formStepId, audit timestamps) are excluded.
     * Display configuration defaults to all true if not provided.
     */
    private BannerResponse mapToBannerResponse(Banner banner) {
        return BannerResponse.builder()
                .id(banner.getId())
                .title(banner.getTitle())
                .slug(banner.getSlug())
                .description(banner.getDescription())
                .imageUrl(banner.getImageUrl())
                .imageAltText(banner.getImageAltText())
                .order(banner.getOrder())
                .buttons(mapButtons(banner.getButtons()))
                .displayConfig(mapDisplayConfig(banner.getDisplayConfig()))
                .build();
    }

    /**
     * Converts the raw {@code Map<String, Object>} displayConfig stored in MongoDB
     * into a typed {@link DisplayConfigResponse} DTO.
     *
     * If displayConfig is null or missing fields, defaults all boolean values to true.
     * This ensures backward compatibility with old banners that don't have displayConfig.
     */
    private DisplayConfigResponse mapDisplayConfig(Map<String, Object> rawDisplayConfig) {
        // Default values for new banners or those without displayConfig
        Boolean showTitle = true;
        Boolean showDescription = true;
        Boolean showButtons = true;

        // If displayConfig exists, extract and override defaults
        if (rawDisplayConfig != null) {
            showTitle = getBooleanValue(rawDisplayConfig, "showTitle", true);
            showDescription = getBooleanValue(rawDisplayConfig, "showDescription", true);
            showButtons = getBooleanValue(rawDisplayConfig, "showButtons", true);
        }

        return DisplayConfigResponse.builder()
                .showTitle(showTitle)
                .showDescription(showDescription)
                .showButtons(showButtons)
                .build();
    }

    /**
     * Converts the raw {@code List<Map<String, Object>>} buttons stored in MongoDB
     * into typed {@link BannerButtonResponse} DTOs.
     *
     * WHY raw Map? The buttons sub-document is stored as a flexible Map in MongoDB
     * (no strict schema), so we do a safe key-by-key extraction here.
     */
    private List<BannerButtonResponse> mapButtons(List<Map<String, Object>> rawButtons) {
        if (rawButtons == null || rawButtons.isEmpty()) {
            return Collections.emptyList();
        }

        return rawButtons.stream()
                .map(btn -> BannerButtonResponse.builder()
                        .title(getStringValue(btn, "title"))
                        .redirectLink(getStringValue(btn, "redirectLink"))
                        .buttonType(getStringValue(btn, "buttonType"))
                        .openInNewTab(getBooleanValue(btn, "openInNewTab"))
                        .build())
                .collect(Collectors.toList());
    }

    private String getStringValue(Map<String, Object> map, String key) {
        Object value = map.get(key);
        return value != null ? value.toString() : null;
    }

    private Boolean getBooleanValue(Map<String, Object> map, String key) {
        Object value = map.get(key);
        if (value instanceof Boolean) {
            return (Boolean) value;
        }
        return value != null ? Boolean.parseBoolean(value.toString()) : null;
    }

    /**
     * Overloaded version with a default value for backward compatibility.
     * Used when the field might not exist in older documents.
     */
    private Boolean getBooleanValue(Map<String, Object> map, String key, Boolean defaultValue) {
        Object value = map.get(key);
        if (value == null) {
            return defaultValue;
        }
        if (value instanceof Boolean) {
            return (Boolean) value;
        }
        return Boolean.parseBoolean(value.toString());
    }
}

