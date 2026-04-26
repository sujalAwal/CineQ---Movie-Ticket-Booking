package com.awal.cineq.publicapi.banner.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * Public-facing response DTO for a single banner.
 *
 * Internal fields (formManagerId, formStepId, deletedAt, createdAt, updatedAt)
 * are intentionally excluded — only the data the frontend needs is exposed.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@JsonInclude(JsonInclude.Include.NON_NULL)
public class BannerResponse {

    /** MongoDB ObjectId as String */
    private String id;

    /** Banner headline (e.g. "Jaari 2") */
    private String title;

    /** URL-friendly slug (e.g. "jaari-2") */
    private String slug;

    /** Short description / subtitle shown on the banner */
    private String description;

    /** Full URL to the banner image */
    private String imageUrl;

    /** Alt text for accessibility / SEO */
    private String imageAltText;

    /**
     * Display order – frontend sorts ascending by this value
     * so banner with order=1 appears first in the carousel.
     */
    private Integer order;

    /** CTA buttons to render on the banner */
    private List<BannerButtonResponse> buttons;

    /** Display configuration controlling which banner elements are shown */
    private DisplayConfigResponse displayConfig;
}

