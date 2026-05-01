package com.awal.cineq.publicapi.settings.service.impl;

import com.awal.cineq.publicapi.settings.dto.SettingResponse;
import com.awal.cineq.publicapi.settings.model.CustomerPortalSetting;
import com.awal.cineq.publicapi.settings.repository.CustomerPortalSettingRepository;
import com.awal.cineq.publicapi.settings.service.PublicSettingService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Implementation of {@link PublicSettingService}.
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
public class PublicSettingServiceImpl implements PublicSettingService {

    private final CustomerPortalSettingRepository customerPortalSettingRepository;

    @Override
    public List<SettingResponse> getAllSettings() {
        log.info("getAllSettings STARTED");

        List<CustomerPortalSetting> settings = customerPortalSettingRepository.findAllActiveSettings();

        List<SettingResponse> response = settings.stream()
                .sorted(Comparator.comparing(CustomerPortalSetting::getGroup)
                        .thenComparing(CustomerPortalSetting::getTitle))
                .map(this::mapToSettingResponse)
                .collect(Collectors.toList());

        log.info("getAllSettings END – returning {} settings", response.size());
        return response;
    }

    // ──────────────────────────────────────────────────────────────────────────
    // Mapper
    // ──────────────────────────────────────────────────────────────────────────

    /**
     * Maps a {@link CustomerPortalSetting} entity to a {@link SettingResponse} DTO.
     * Only safe, public-facing fields are copied – internal IDs and audit
     * timestamps are intentionally left out.
     */
    private SettingResponse mapToSettingResponse(CustomerPortalSetting setting) {
        return SettingResponse.builder()
                .id(setting.getId())
                .title(setting.getTitle())
                .slug(setting.getSlug())
                .type(setting.getType())
                .value(setting.getValue())
                .group(setting.getGroup())
                .build();
    }
}

