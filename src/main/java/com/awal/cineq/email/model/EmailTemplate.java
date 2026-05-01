package com.awal.cineq.email.model;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.Id;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;
import org.springframework.data.mongodb.core.mapping.Field;

import java.time.LocalDateTime;

/**
 * MongoDB Document for Email Templates
 * Stores email templates with placeholders that can be dynamically replaced
 */
@Document(collection = "email_templates")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class EmailTemplate {

    @Id
    private String id;

    @Indexed
    private String slug;  // Unique identifier (e.g., "email-verification")

    private String name;  // Human-readable name

    private String subject;  // Email subject line

    private String message;  // HTML email body with {{placeholders}}

    private String adminSubject;

    private String adminMessage;

    private Boolean isActive;

    private String formManagerId;

    private String formStepId;

    @CreatedDate
    private LocalDateTime createdAt;

    @LastModifiedDate
    private LocalDateTime updatedAt;

    private LocalDateTime deletedAt;
}
