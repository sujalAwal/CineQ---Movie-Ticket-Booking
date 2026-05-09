package com.awal.cineq.common.util;

import com.awal.cineq.config.ApplicationProperties;
import com.awal.cineq.config.service.BackendSettingsService;
import com.awal.cineq.email.dto.SmtpSettingsDTO;
import com.awal.cineq.email.service.SmtpSettingsService;
import com.awal.cineq.exception.ResourceNotFoundException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import jakarta.mail.*;
import jakarta.mail.internet.InternetAddress;
import jakarta.mail.internet.MimeMessage;
import java.util.List;
import java.util.Properties;

@Slf4j
@Component
@RequiredArgsConstructor
public class EmailHelper {

    private final ApplicationProperties applicationProperties;
    private final SmtpSettingsService smtpSettingsService;
    private final BackendSettingsService backendSettingsService;

    /**
     * Send email to multiple recipients
     *
     * @param recipients List of email addresses to send to
     * @param subject    Email subject
     * @param message    Email body (HTML supported)
     * @return true if email sent successfully, false otherwise
     */
    public boolean sendEmail(List<String> recipients, String subject, String message) {
        if (recipients == null || recipients.isEmpty()) {
            log.warn("No recipients provided for email");
            return false;
        }
        return sendEmail(recipients.toArray(new String[0]), subject, message);
    }

    /**
     * Send email to a single recipient
     *
     * @param recipient Single email address
     * @param subject   Email subject
     * @param message   Email body (HTML supported)
     * @return true if email sent successfully, false otherwise
     */
    public boolean sendEmail(String recipient, String subject, String message) {
        if (recipient == null || recipient.isBlank()) {
            log.warn("No recipient provided for email");
            return false;
        }
        return sendEmail(new String[]{recipient}, subject, message);
    }

    /**
     * Send email to multiple recipients (array)
     *
     * @param recipients Array of email addresses
     * @param subject    Email subject
     * @param message    Email body (HTML supported)
     * @return true if email sent successfully, false otherwise
     */
    public boolean sendEmail(String[] recipients, String subject, String message) {
        try {
            if (recipients == null || recipients.length == 0) {
                log.warn("No recipients provided for email");
                return false;
            }

            EmailConfig emailConfig = getSMTPConfig();

            if (emailConfig == null) {
                log.error("Failed to retrieve SMTP configuration");
                return false;
            }

            if (emailConfig.getSmtpUsername() == null || emailConfig.getSmtpPassword() == null) {
                log.error("❌ SMTP credentials not configured - username={}, password={}",
                    emailConfig.getSmtpUsername() != null ? "present" : "NULL",
                    emailConfig.getSmtpPassword() != null ? "present" : "NULL");
                return false;
            }

            if (emailConfig.getFromAddress() == null || emailConfig.getFromAddress().isBlank()) {
                log.error("SMTP from address not configured");
                return false;
            }

            log.info("📧 Preparing email - host={}, port={}, username={}, recipients={}",
                emailConfig.getSmtpHost(), emailConfig.getSmtpPort(),
                emailConfig.getSmtpUsername(), recipients.length);

            Properties props = new Properties();
            props.put("mail.smtp.host", emailConfig.getSmtpHost());
            props.put("mail.smtp.port", emailConfig.getSmtpPort());
            props.put("mail.smtp.auth", "true");
            props.put("mail.smtp.starttls.enable", emailConfig.getEnableTls());
            props.put("mail.smtp.starttls.required", emailConfig.getEnableTls());
            props.put("mail.smtp.ssl.enable", emailConfig.getEnableSsl());
            props.put("mail.smtp.connectiontimeout", 5000);
            props.put("mail.smtp.timeout", 5000);
            props.put("mail.smtp.writetimeout", 5000);

            Session session = Session.getInstance(props, new Authenticator() {
                @Override
                protected PasswordAuthentication getPasswordAuthentication() {
                    return new PasswordAuthentication(
                            emailConfig.getSmtpUsername(),
                            emailConfig.getSmtpPassword()
                    );
                }
            });

            MimeMessage mimeMessage = new MimeMessage(session);

            String fromName = emailConfig.getFromName() != null && !emailConfig.getFromName().isBlank()
                ? emailConfig.getFromName()
                : "CineQ";
            mimeMessage.setFrom(new InternetAddress(emailConfig.getFromAddress(), fromName));

            for (String recipient : recipients) {
                mimeMessage.addRecipient(Message.RecipientType.TO, new InternetAddress(recipient));
            }

            mimeMessage.setSubject(subject, "UTF-8");
            mimeMessage.setContent(message, "text/html; charset=UTF-8");

            Transport.send(mimeMessage);

            log.info("✅ Email sent successfully to {} recipient(s) with subject: {}", recipients.length, subject);
            return true;

        } catch (MessagingException e) {
            log.error("Failed to send email with subject: {}", subject, e);
            return false;
        } catch (Exception e) {
            log.error("Unexpected error while sending email", e);
            return false;
        }
    }

    /**
     * Send email with comma-separated recipients string
     *
     * @param recipientsString Comma-separated email addresses
     * @param subject          Email subject
     * @param message          Email body (HTML supported)
     * @return true if email sent successfully, false otherwise
     */
    public boolean sendEmailFromString(String recipientsString, String subject, String message) {
        if (recipientsString == null || recipientsString.isBlank()) {
            log.warn("No recipients provided for email");
            return false;
        }

        String[] recipients = recipientsString.split(",");
        String[] trimmedRecipients = new String[recipients.length];
        for (int i = 0; i < recipients.length; i++) {
            trimmedRecipients[i] = recipients[i].trim();
        }

        return sendEmail(trimmedRecipients, subject, message);
    }

    private EmailConfig getSMTPConfig() {
        try {
            boolean useEnvConfig = backendSettingsService
                .getBooleanValue("enable-environment-smtp-config");

            log.info("SMTP Config Decision: useEnvConfig={}", useEnvConfig);

            if (useEnvConfig) {
                log.info("✅ Using ENVIRONMENT-based SMTP configuration");
                EmailConfig config = getEnvBasedConfig();
                if (config != null) {
                    log.info("SMTP Host (ENV): {}", config.getSmtpHost());
                }
                return config;
            } else {
                log.info("✅ Using DATABASE-based SMTP configuration");
                EmailConfig config = getDbBasedConfig();
                if (config != null) {
                    log.info("SMTP Host (DB): {}", config.getSmtpHost());
                }
                return config;
            }
        } catch (Exception e) {
            log.error("Error determining SMTP config source, falling back to environment", e);
            EmailConfig fallbackConfig = getEnvBasedConfig();
            if (fallbackConfig != null) {
                log.warn("FALLBACK SMTP Host (ENV): {}", fallbackConfig.getSmtpHost());
            }
            return fallbackConfig;
        }
    }

    private EmailConfig getEnvBasedConfig() {
        ApplicationProperties.Email envEmail = applicationProperties.getEmail();

        if (envEmail.getSmtpUsername() == null || envEmail.getSmtpPassword() == null) {
            log.error("SMTP credentials not configured in environment");
            return null;
        }

        log.debug("ENV SMTP: host={}, port={}, username={}",
            envEmail.getSmtpHost(), envEmail.getSmtpPort(), envEmail.getSmtpUsername());

        return EmailConfig.builder()
            .smtpHost(envEmail.getSmtpHost())
            .smtpPort(envEmail.getSmtpPort())
            .smtpUsername(envEmail.getSmtpUsername())
            .smtpPassword(envEmail.getSmtpPassword())
            .fromAddress(envEmail.getFromAddress())
            .fromName(envEmail.getFromName())
            .enableTls(envEmail.isEnableTls())
            .enableSsl(envEmail.isEnableSsl())
            .build();
    }

    private EmailConfig getDbBasedConfig() {
        try {
            SmtpSettingsDTO dbSettings = smtpSettingsService.getActiveSmtpSettings();

            log.debug("DB SMTP: host={}, port={}, username={}",
                dbSettings.getSmtpHost(), dbSettings.getSmtpPort(), dbSettings.getSmtpUsername());

            return EmailConfig.builder()
                .smtpHost(dbSettings.getSmtpHost())
                .smtpPort(dbSettings.getSmtpPort())
                .smtpUsername(dbSettings.getSmtpUsername())
                .smtpPassword(dbSettings.getSmtpPassword())
                .fromAddress(dbSettings.getFromAddress())
                .fromName(dbSettings.getFromName())
                .enableTls(dbSettings.getEnableTls())
                .enableSsl(dbSettings.getEnableSsl())
                .build();
        } catch (ResourceNotFoundException e) {
            log.error("No active SMTP settings found in database");
            return null;
        }
    }
}
