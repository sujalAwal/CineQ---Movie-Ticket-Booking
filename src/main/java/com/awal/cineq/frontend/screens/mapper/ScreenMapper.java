package com.awal.cineq.frontend.screens.mapper;

import com.awal.cineq.frontend.screens.dto.ScreenDTO;
import org.springframework.stereotype.Component;
import java.util.Map;

@Component
public class ScreenMapper {

    public ScreenDTO toDTO(Map<String, Object> document) {
        if (document == null) {
            return null;
        }

        ScreenDTO dto = new ScreenDTO();
        dto.setId((String) document.get("_id"));
        dto.setScreenName((String) document.get("screenName"));
        dto.setTheatreId((String) document.get("theatreId"));
        Object rowsObj = document.get("rows");
        if (rowsObj instanceof Integer) {
            dto.setRows((Integer) rowsObj);
        } else if (rowsObj instanceof Number) {
            dto.setRows(((Number) rowsObj).intValue());
        }
        Object columnsObj = document.get("columns");
        if (columnsObj instanceof Integer) {
            dto.setColumns((Integer) columnsObj);
        } else if (columnsObj instanceof Number) {
            dto.setColumns(((Number) columnsObj).intValue());
        }
        dto.setScreenType((String) document.get("screenType"));
        dto.setSoundSystem((String) document.get("soundSystem"));
        Object breakTimeObj = document.get("breakTime");
        if (breakTimeObj instanceof Integer) {
            dto.setBreakTime((Integer) breakTimeObj);
        } else if (breakTimeObj instanceof Number) {
            dto.setBreakTime(((Number) breakTimeObj).intValue());
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
