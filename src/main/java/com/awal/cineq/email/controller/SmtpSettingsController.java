package com.awal.cineq.email.controller;

import com.awal.cineq.dto.ApiResponse;
import com.awal.cineq.email.dto.SmtpSettingsDTO;
import com.awal.cineq.email.service.SmtpSettingsService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/smtp-settings")
@RequiredArgsConstructor
@Slf4j
public class SmtpSettingsController {

    private final SmtpSettingsService smtpSettingsService;

    @GetMapping("/active")
    public ApiResponse<SmtpSettingsDTO> getActiveSettings() {
        SmtpSettingsDTO settings = smtpSettingsService.getActiveSmtpSettings();
        return ApiResponse.success("Active SMTP settings retrieved", settings);
    }

    @PostMapping
    public ApiResponse<SmtpSettingsDTO> createSettings(@RequestBody SmtpSettingsDTO dto) {
        SmtpSettingsDTO created = smtpSettingsService.createSmtpSettings(dto);
        return ApiResponse.success("SMTP settings created", created);
    }

    @PutMapping("/{id}")
    public ApiResponse<SmtpSettingsDTO> updateSettings(
        @PathVariable String id,
        @RequestBody SmtpSettingsDTO dto) {
        SmtpSettingsDTO updated = smtpSettingsService.updateSmtpSettings(id, dto);
        return ApiResponse.success("SMTP settings updated", updated);
    }

    @GetMapping
    public ApiResponse<List<SmtpSettingsDTO>> getAllSettings() {
        List<SmtpSettingsDTO> settings = smtpSettingsService.getAllSmtpSettings();
        return ApiResponse.success("All SMTP settings retrieved", settings);
    }

    @DeleteMapping("/{id}")
    public ApiResponse<String> deactivateSettings(@PathVariable String id) {
        smtpSettingsService.deactivateSmtpSettings(id);
        return ApiResponse.success("SMTP settings deactivated", "");
    }
}
