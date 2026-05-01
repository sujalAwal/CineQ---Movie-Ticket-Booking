package com.awal.cineq.frontend.theatres.dto.request;

import com.awal.cineq.frontend.common.dto.request.FrontendPageRequest;
import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.constraints.Pattern;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class TheatrePageRequest extends FrontendPageRequest {

    @Pattern(regexp = "name|city|state|pincode|createdAt|updatedAt",
             message = "Sort by must be one of: name, city, state, pincode, createdAt, updatedAt")
    @JsonProperty("sortBy")
    private String sortBy = "name";

    @JsonProperty("city")
    private String city;

    @JsonProperty("state")
    private String state;

    public TheatrePageRequest() {
        super();
        this.sortBy = "name";
    }
}
