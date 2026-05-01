package com.awal.cineq.publicapi.banner.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Display configuration response for controlling which elements are shown on the banner.
 *
 * Fields:
 * - showTitle: Whether to display the banner title
 * - showDescription: Whether to display the banner description
 * - showButtons: Whether to display the action buttons
 *
 * All fields default to true if not provided.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@JsonInclude(JsonInclude.Include.NON_NULL)
public class DisplayConfigResponse {

    /** Whether to display the banner title */
    @Builder.Default
    private Boolean showTitle = true;

    /** Whether to display the banner description */
    @Builder.Default
    private Boolean showDescription = true;

    /** Whether to display the action buttons */
    @Builder.Default
    private Boolean showButtons = true;
}
