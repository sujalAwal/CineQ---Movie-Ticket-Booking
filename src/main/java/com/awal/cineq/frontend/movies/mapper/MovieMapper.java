package com.awal.cineq.frontend.movies.mapper;

import com.awal.cineq.frontend.movies.dto.MovieDTO;
import org.springframework.stereotype.Component;
import java.util.Map;

@Component
public class MovieMapper {

    public MovieDTO toDTO(Map<String, Object> document) {
        if (document == null) {
            return null;
        }

        MovieDTO dto = new MovieDTO();
        dto.setId((String) document.get("_id"));
        dto.setTitle((String) document.get("title"));
        dto.setDescription((String) document.get("description"));
        dto.setPoster((String) document.get("poster"));
        dto.setBanner((String) document.get("banner"));
        Object durationObj = document.get("duration");
        if (durationObj instanceof Integer) {
            dto.setDuration((Integer) durationObj);
        } else if (durationObj instanceof Number) {
            dto.setDuration(((Number) durationObj).intValue());
        }
        dto.setReleaseDate((String) document.get("releaseDate"));
        dto.setCertification((String) document.get("certification"));
        dto.setLanguage((String) document.get("language"));
        dto.setFormat((String) document.get("format"));
        dto.setStatus((String) document.get("status"));
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
