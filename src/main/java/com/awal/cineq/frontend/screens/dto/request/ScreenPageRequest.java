package com.awal.cineq.frontend.screens.dto.request;

import com.awal.cineq.frontend.common.dto.request.FrontendPageRequest;
import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.constraints.Pattern;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class ScreenPageRequest extends FrontendPageRequest {

    @Pattern(regexp = "screenName|screenType|rows|columns|breakTime|theatreId|createdAt|updatedAt",
             message = "Sort by must be one of: screenName, screenType, rows, columns, breakTime, theatreId, createdAt, updatedAt")
    @JsonProperty("sortBy")
    private String sortBy = "screenName";

    @JsonProperty("theatreId")
    private String theatreId;

    @JsonProperty("screenType")
    private String screenType;

    public ScreenPageRequest() {
        super();
        this.sortBy = "screenName";
    }
}
