package com.awal.cineq.media.dto.response;

import com.fasterxml.jackson.annotation.JsonAlias;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

@JsonIgnoreProperties(ignoreUnknown = true)
@Data
public class SupabaseResponse {
    @JsonProperty("id")
    @JsonAlias({"Id", "ID"})
    private String id;

    @JsonProperty("key")
    @JsonAlias({"Key", "KEY"})
    private String key;
}
