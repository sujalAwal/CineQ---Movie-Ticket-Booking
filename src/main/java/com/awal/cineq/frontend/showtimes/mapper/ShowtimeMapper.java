package com.awal.cineq.frontend.showtimes.mapper;

import com.awal.cineq.frontend.showtimes.dto.ShowtimeDTO;
import org.springframework.stereotype.Component;
import java.util.Map;

@Component
public class ShowtimeMapper {

    public ShowtimeDTO toDTO(Map<String, Object> document) {
        if (document == null) {
            return null;
        }

        ShowtimeDTO dto = new ShowtimeDTO();
        dto.setId((String) document.get("_id"));
        dto.setMovieId((String) document.get("movieId"));
        dto.setScreenId((String) document.get("screenId"));
        dto.setTheatreId((String) document.get("theatreId"));
        dto.setShowDate((String) document.get("showDate"));
        dto.setShowTime((String) document.get("showTime"));
        dto.setLanguage((String) document.get("language"));
        dto.setFormat((String) document.get("format"));
        dto.setStatusCode((String) document.get("statusCode"));
        Object basePriceObj = document.get("basePrice");
        if (basePriceObj instanceof Number) {
            dto.setBasePrice(((Number) basePriceObj).doubleValue());
        }
        Object isActiveObj = document.get("isActive");
        if (isActiveObj instanceof Boolean) {
            dto.setActive((Boolean) isActiveObj);
        }
        Object createdAtObj = document.get("createdAt");
        if (createdAtObj instanceof Long) {
            dto.setCreatedAt((Long) createdAtObj);
        }
        Object updatedAtObj = document.get("updatedAt");
        if (updatedAtObj instanceof Long) {
            dto.setUpdatedAt((Long) updatedAtObj);
        }
        return dto;
    }
}
