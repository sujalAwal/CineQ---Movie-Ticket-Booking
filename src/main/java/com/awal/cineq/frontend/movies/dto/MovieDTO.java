package com.awal.cineq.frontend.movies.dto;

import com.awal.cineq.publicapi.movie.dto.GenreInfo;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class MovieDTO {
    private String id;

    @JsonProperty("title")
    private String title;

    @JsonProperty("description")
    private String description;

    @JsonProperty("poster")
    private String poster;

    @JsonProperty("banner")
    private String banner;

    @JsonProperty("duration")
    private Integer duration;

    @JsonProperty("releaseDate")
    private String releaseDate;

    @JsonProperty("certification")
    private String certification;

    @JsonProperty("language")
    private String language;

    @JsonProperty("format")
    private String format;

    @JsonProperty("status")
    private String status;

    @JsonProperty("is_active")
    private boolean isActive;

    @JsonProperty("created_at")
    private Long createdAt;

    @JsonProperty("updated_at")
    private Long updatedAt;

    @JsonProperty("genres")
    private List<GenreInfo> genres;
}
