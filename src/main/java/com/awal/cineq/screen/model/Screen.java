package com.awal.cineq.screen.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

/**
 * Model representing a cinema screen
 * Contains screen configuration, seat layout, and theatre reference
 */
@Document(collection = "screens")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Screen {

    @Id
    private String id;

    private String screenName;

    private String theatreId;

    private Integer rows;

    private Integer columns;

    private String screenType;

    private List<Map<String, Object>> seatLayout;

    private String soundSystem;

    private Integer breakTime;

    private Boolean isActive;

    private LocalDateTime createdAt;

    private LocalDateTime updatedAt;

    private LocalDateTime deletedAt;

    private String formManagerId;

    private String formStepId;
}
