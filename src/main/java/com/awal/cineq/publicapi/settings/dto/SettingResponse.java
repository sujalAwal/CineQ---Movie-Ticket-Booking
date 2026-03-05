package com.awal.cineq.publicapi.settings.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Public-facing response DTO for a single portal setting.
 *
 * Internal fields (formManagerId, formStepId, deletedAt, createdAt, updatedAt)
 * are intentionally excluded — only the data the frontend needs is exposed.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@JsonInclude(JsonInclude.Include.NON_NULL)
public class SettingResponse {

    /** MongoDB ObjectId as String */
    private String id;

    /** Human-readable label (e.g. "Site Name") */
    private String title;

    /** Machine-readable key (e.g. "site-name") */
    private String slug;

    /** Field type hint for the UI (text, media, checkbox, color, etc.) */
    private String type;

    /** The stored value for this setting */
    private String value;

    /**
     * Logical group (e.g. "general", "seo", "appearance", "contact", "integrations")
     * Allows the frontend to group and render settings by section.
     */
    private String group;
}

