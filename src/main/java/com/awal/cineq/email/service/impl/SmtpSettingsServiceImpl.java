package com.awal.cineq.email.service.impl;

import com.awal.cineq.common.util.EncryptionUtil;
import com.awal.cineq.email.dto.SmtpSettingsDTO;
import com.awal.cineq.email.model.SmtpSettings;
import com.awal.cineq.email.repository.SmtpSettingsRepository;
import com.awal.cineq.email.service.SmtpSettingsService;
import com.awal.cineq.exception.ResourceNotFoundException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
@Transactional
public class SmtpSettingsServiceImpl implements SmtpSettingsService {

    private final SmtpSettingsRepository smtpSettingsRepository;
    private final EncryptionUtil encryptionUtil;

    @Override
    @Transactional(readOnly = true)
    public SmtpSettingsDTO getActiveSmtpSettings() {
        SmtpSettings settings = smtpSettingsRepository.findActiveSmtpSettings()
            .orElseThrow(() -> new ResourceNotFoundException(
                "No active SMTP settings found in database"));

        return mapToDTO(settings);
    }

    @Override
    public SmtpSettingsDTO createSmtpSettings(SmtpSettingsDTO dto) {
        log.info("Creating new SMTP settings for host: {}", dto.getSmtpHost());

        List<SmtpSettings> existing = smtpSettingsRepository.findAllActiveSmtpSettings();
        existing.forEach(s -> {
            s.setIsActive(false);
            s.setUpdatedAt(LocalDateTime.now());
            log.info("Deactivated previous SMTP settings: {}", s.getId());
        });
        smtpSettingsRepository.saveAll(existing);

        SmtpSettings settings = new SmtpSettings();
        settings.setSmtpHost(dto.getSmtpHost());
        settings.setSmtpPort(dto.getSmtpPort());
        settings.setSmtpUsername(dto.getSmtpUsername());

        // Encrypt plain password for storage
        String encryptedPassword = encryptionUtil.encrypt(dto.getSmtpPassword());
        settings.setSmtpPassword(encryptedPassword);
        log.debug("SMTP password encrypted and stored");

        settings.setFromAddress(dto.getFromAddress());
        settings.setFromName(dto.getFromName());
        settings.setEnableTls(dto.getEnableTls());
        settings.setEnableSsl(dto.getEnableSsl());
        settings.setIsActive(true);
        settings.setCreatedAt(LocalDateTime.now());
        settings.setUpdatedAt(LocalDateTime.now());
        settings.setDeletedAt(null);

        SmtpSettings saved = smtpSettingsRepository.save(settings);
        log.info("New SMTP settings created with ID: {} for host: {}", saved.getId(), saved.getSmtpHost());

        return mapToDTO(saved);
    }

    @Override
    public SmtpSettingsDTO updateSmtpSettings(String id, SmtpSettingsDTO dto) {
        log.info("Updating SMTP settings: {}", id);

        SmtpSettings settings = smtpSettingsRepository.findById(id)
            .orElseThrow(() -> new ResourceNotFoundException("SMTP settings not found with id: " + id));

        settings.setSmtpHost(dto.getSmtpHost());
        settings.setSmtpPort(dto.getSmtpPort());
        settings.setSmtpUsername(dto.getSmtpUsername());

        if (dto.getSmtpPassword() != null && !dto.getSmtpPassword().isEmpty()) {
            String encryptedPassword = encryptionUtil.encrypt(dto.getSmtpPassword());
            settings.setSmtpPassword(encryptedPassword);
            log.debug("SMTP password updated and encrypted");
        }

        settings.setFromAddress(dto.getFromAddress());
        settings.setFromName(dto.getFromName());
        settings.setEnableTls(dto.getEnableTls());
        settings.setEnableSsl(dto.getEnableSsl());
        settings.setUpdatedAt(LocalDateTime.now());

        SmtpSettings updated = smtpSettingsRepository.save(settings);
        log.info("SMTP settings updated: {}", id);

        return mapToDTO(updated);
    }

    @Override
    public void deactivateSmtpSettings(String id) {
        log.info("Deactivating SMTP settings: {}", id);

        SmtpSettings settings = smtpSettingsRepository.findById(id)
            .orElseThrow(() -> new ResourceNotFoundException("SMTP settings not found with id: " + id));

        settings.setIsActive(false);
        settings.setUpdatedAt(LocalDateTime.now());
        smtpSettingsRepository.save(settings);

        log.info("SMTP settings deactivated: {}", id);
    }

    @Override
    @Transactional(readOnly = true)
    public List<SmtpSettingsDTO> getAllSmtpSettings() {
        return smtpSettingsRepository.findAll()
            .stream()
            .filter(s -> s.getDeletedAt() == null)
            .map(this::mapToDTO)
            .collect(Collectors.toList());
    }

    private SmtpSettingsDTO mapToDTO(SmtpSettings settings) {
        String decryptedPassword = null;
        try {
            if (settings.getSmtpPassword() != null && !settings.getSmtpPassword().isBlank()) {
                decryptedPassword = encryptionUtil.decrypt(settings.getSmtpPassword());
                log.debug("SMTP password decrypted successfully");
            }
        } catch (Exception e) {
            log.error("Error decrypting SMTP password for settings: {}", settings.getId(), e);
            // If decryption fails, we set it to null - password won't be available for email sending
            decryptedPassword = null;
        }

        return SmtpSettingsDTO.builder()
            .id(settings.getId())
            .smtpHost(settings.getSmtpHost())
            .smtpPort(settings.getSmtpPort())
            .smtpUsername(settings.getSmtpUsername())
            .smtpPassword(decryptedPassword)  // ← NOW DECRYPTED!
            .smtpPasswordHash(settings.getSmtpPassword())  // ← Encrypted version (for reference)
            .fromAddress(settings.getFromAddress())
            .fromName(settings.getFromName())
            .enableTls(settings.getEnableTls())
            .enableSsl(settings.getEnableSsl())
            .isActive(settings.getIsActive())
            .createdAt(settings.getCreatedAt())
            .updatedAt(settings.getUpdatedAt())
            .build();
    }
}

