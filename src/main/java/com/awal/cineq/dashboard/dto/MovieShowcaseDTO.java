package com.awal.cineq.dashboard.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class MovieShowcaseDTO {

    private String id;
    private String title;
    private String poster;
    private Integer duration;
    private String certification;
    private List<String> language;
    private String releaseDate;
}
