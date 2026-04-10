package com.awal.cineq.frontend.movies.dto.request;

import com.awal.cineq.frontend.common.dto.request.FrontendPageRequest;
import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.constraints.Pattern;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class MoviePageRequest extends FrontendPageRequest {

    @Pattern(regexp = "title|releaseDate|status|certification|createdAt|updatedAt",
             message = "Sort by must be one of: title, releaseDate, status, certification, createdAt, updatedAt")
    @JsonProperty("sortBy")
    private String sortBy = "title";

    @JsonProperty("certification")
    private String certification;

    @JsonProperty("language")
    private String language;

    @JsonProperty("format")
    private String format;

    @JsonProperty("status")
    private String status;

    @JsonProperty("releaseStatus")
    private String releaseStatus;

    public MoviePageRequest() {
        super();
        this.sortBy = "title";
    }
}
