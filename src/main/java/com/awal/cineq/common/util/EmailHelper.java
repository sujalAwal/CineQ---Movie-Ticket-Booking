package com.awal.cineq.common.util;

import com.awal.cineq.config.ApplicationProperties;
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

            ApplicationProperties.Email emailConfig = applicationProperties.getEmail();

            if (emailConfig.getSmtpUsername() == null || emailConfig.getSmtpPassword() == null) {
                log.error("SMTP credentials not configured");
                return false;
            }

            if (emailConfig.getFromAddress() == null || emailConfig.getFromAddress().isBlank()) {
                log.error("SMTP from address not configured");
                return false;
            }

            Properties props = new Properties();
            props.put("mail.smtp.host", emailConfig.getSmtpHost());
            props.put("mail.smtp.port", emailConfig.getSmtpPort());
            props.put("mail.smtp.auth", "true");
            props.put("mail.smtp.starttls.enable", emailConfig.isEnableTls());
            props.put("mail.smtp.starttls.required", emailConfig.isEnableTls());
            props.put("mail.smtp.ssl.enable", emailConfig.isEnableSsl());
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

            // Set from address with sender name
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

            log.info("Email sent successfully to {} recipient(s) with subject: {}", recipients.length, subject);
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
}
