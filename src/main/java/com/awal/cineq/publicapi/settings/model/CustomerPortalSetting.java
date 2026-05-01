package com.awal.cineq.publicapi.settings.model;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.Id;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.LocalDateTime;

/**
 * CustomerPortalSetting Entity
 *
 * Maps the {@code customer_portal_settings} MongoDB collection.
 * Each document represents a single configurable setting for the customer portal
 * (e.g. site name, logo URL, primary color, contact email).
 *
 * Soft-delete pattern: {@code deletedAt = null} → active, non-null → deleted.
 */
@Document(collection = "customer_portal_settings")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class CustomerPortalSetting {

    @Id
    private String id;

    /** Reference to the FormManager that owns this setting */
    private String formManagerId;

    /** Reference to the FormStep that owns this setting */
    private String formStepId;

    /** Human-readable label (e.g. "Site Name") */
    private String title;

    /**
     * Unique machine-readable key (e.g. "site-name").
     * Indexed for fast slug-based lookups.
     */
    @Indexed
    private String slug;

    /**
     * Field type hint for the UI (text, media, checkbox, color, etc.)
     */
    private String type;

    /** The actual stored value for this setting */
    private String value;

    /**
     * Logical group for grouping settings together on the UI
     * (e.g. "general", "seo", "appearance", "contact", "integrations")
     */
    private String group;

    /** Whether this setting is currently active and should be served publicly */
    private Boolean isActive = true;

    /** Soft-delete marker – null means the record is active */
    private LocalDateTime deletedAt;

    @CreatedDate
    private LocalDateTime createdAt;

    @LastModifiedDate
    private LocalDateTime updatedAt;
}

