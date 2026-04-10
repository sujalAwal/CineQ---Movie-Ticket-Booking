package com.awal.cineq.publicapi.movie.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * Complete movie detail response for public API.
 * 
 * NOTE: starcast array may contain:
 *   - artistId (actual database)
 *   - artistTypeId (actual database)
 *   - characterName (actual database)
 * 
 * These replace the form-manager defined personId/crewRoleId.
 * The API returns the raw starcast object as stored in MongoDB.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@JsonInclude(JsonInclude.Include.NON_NULL)
public class PublicMovieDetailResponse {

    private String id;
    private String title;
    private String description;
    private String poster;
    private String banner;
    private String trailerUrl;
    private Integer duration;
    private String releaseDate;
    private List<String> language;
    private String country;
    private String certification;
    private Object formats;
    private String status;
    private String director;
    private Object starcast;
    private List<GenreInfo> genres;
    private Boolean isActive;
}
