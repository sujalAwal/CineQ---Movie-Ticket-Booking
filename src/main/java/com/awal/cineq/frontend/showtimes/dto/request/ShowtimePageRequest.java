package com.awal.cineq.frontend.showtimes.dto.request;

import com.awal.cineq.frontend.common.dto.request.FrontendPageRequest;
import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.constraints.Pattern;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class ShowtimePageRequest extends FrontendPageRequest {

    @Pattern(regexp = "showDate|showTime|basePrice|movieId|theatreId|screenId|createdAt|updatedAt",
             message = "Sort by must be one of: showDate, showTime, basePrice, movieId, theatreId, screenId, createdAt, updatedAt")
    @JsonProperty("sortBy")
    private String sortBy = "showDate";

    @JsonProperty("movieId")
    private String movieId;

    @JsonProperty("theatreId")
    private String theatreId;

    @JsonProperty("screenId")
    private String screenId;

    @JsonProperty("statusCode")
    private String statusCode;

    public ShowtimePageRequest() {
        super();
        this.sortBy = "showDate";
    }
}
