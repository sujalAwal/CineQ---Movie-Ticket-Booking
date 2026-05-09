package com.awal.cineq.email.model;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.Id;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.mongodb.core.mapping.Document;
import org.springframework.data.mongodb.core.mapping.Field;

import java.time.LocalDateTime;

@Document(collection = "smtp-settings")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class SmtpSettings {
    @Id
    private String id;

    @Field("smtp_host")
    private String smtpHost;

    @Field("smtp_port")
    private Integer smtpPort;

    @Field("smtp_username")
    private String smtpUsername;

    @Field("smtp_password")
    private String smtpPassword;

    @Field("from_address")
    private String fromAddress;

    @Field("from_name")
    private String fromName;

    @Field("enable_tls")
    private Boolean enableTls = true;

    @Field("enable_ssl")
    private Boolean enableSsl = false;

    @Field("is_active")
    private Boolean isActive = true;

    @Field("created_at")
    @CreatedDate
    private LocalDateTime createdAt;

    @Field("updated_at")
    @LastModifiedDate
    private LocalDateTime updatedAt;

    @Field("deleted_at")
    private LocalDateTime deletedAt;
}
