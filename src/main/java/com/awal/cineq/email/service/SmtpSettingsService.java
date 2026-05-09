package com.awal.cineq.email.service;

import com.awal.cineq.email.dto.SmtpSettingsDTO;

import java.util.List;

public interface SmtpSettingsService {

    SmtpSettingsDTO getActiveSmtpSettings();

    SmtpSettingsDTO createSmtpSettings(SmtpSettingsDTO dto);

    SmtpSettingsDTO updateSmtpSettings(String id, SmtpSettingsDTO dto);

    void deactivateSmtpSettings(String id);

    List<SmtpSettingsDTO> getAllSmtpSettings();
}
