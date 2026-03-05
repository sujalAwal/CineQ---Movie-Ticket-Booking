package com.awal.cineq.publicapi.banner.model;

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
import java.util.List;
import java.util.Map;

/**
 * Banner Entity
 *
 * Maps the {@code banners} MongoDB collection.
 * Each banner represents a hero/carousel slide on the customer portal
 * with an image, description, and a list of CTA buttons.
 *
 * Soft-delete pattern: {@code deletedAt = null} → active, non-null → deleted.
 */
@Document(collection = "banners")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class Banner {

    @Id
    private String id;

    /** Human-readable title (e.g. "Jaari 2") */
    private String title;

    /**
     * URL-friendly slug (e.g. "jaari-2").
     * Indexed for fast slug-based lookups.
     */
    @Indexed
    private String slug;

    /** Short description / subtitle shown on the banner */
    private String description;

    /** Full URL to the banner image (hosted on Supabase or local storage) */
    @Field("bannerImage")
    private String imageUrl;

    /** Alt text for accessibility / SEO */
    private String imageAltText;

    /**
     * Display order – lower value appears first.
     * Banners are sorted ascending by this field.
     */
    private Integer order;

    /**
     * CTA buttons embedded in the banner.
     * Each button has: title, redirectLink, buttonType, openInNewTab.
     * Stored as a list of maps to stay flexible with future button fields.
     */
    private List<Map<String, Object>> buttons;

    /** Reference to the FormManager that owns this banner */
    private String formManagerId;

    /** Reference to the FormStep that owns this banner */
    private String formStepId;

    /** Whether this banner is currently active and should be served publicly */
    private Boolean isActive = true;

    /** Soft-delete marker – null means the record is active */
    private LocalDateTime deletedAt;

    @CreatedDate
    private LocalDateTime createdAt;

    @LastModifiedDate
    private LocalDateTime updatedAt;
}

