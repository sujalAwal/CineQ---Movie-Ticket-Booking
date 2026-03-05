package com.awal.cineq.publicapi.banner.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Represents a single CTA button embedded inside a banner response.
 *
 * Fields mirror the MongoDB sub-document:
 * { title, redirectLink, buttonType, openInNewTab }
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@JsonInclude(JsonInclude.Include.NON_NULL)
public class BannerButtonResponse {

    /** Button label text (e.g. "Watch Trailer") */
    private String title;

    /** URL the button points to */
    private String redirectLink;

    /**
     * Visual style of the button (e.g. "primary", "secondary", "outline").
     * Used by the frontend to apply the correct CSS class.
     */
    private String buttonType;

    /** Whether the link should open in a new browser tab */
    private Boolean openInNewTab;
}


