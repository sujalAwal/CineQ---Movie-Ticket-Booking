package com.awal.cineq.frontend.theatres.mapper;

import com.awal.cineq.frontend.theatres.dto.TheatreDTO;
import org.springframework.stereotype.Component;
import java.util.Map;

@Component
public class TheatreMapper {

    public TheatreDTO toDTO(Map<String, Object> document) {
        if (document == null) {
            return null;
        }

        TheatreDTO dto = new TheatreDTO();
        dto.setId((String) document.get("_id"));
        dto.setName((String) document.get("name"));
        dto.setEmail((String) document.get("email"));
        dto.setPhone((String) document.get("phone"));
        dto.setChain((String) document.get("chain"));
        dto.setAddress((String) document.get("address"));
        dto.setCity((String) document.get("city"));
        dto.setState((String) document.get("state"));
        dto.setPincode((String) document.get("pincode"));
        Object latitudeObj = document.get("latitude");
        if (latitudeObj instanceof Number) {
            dto.setLatitude(((Number) latitudeObj).doubleValue());
        }
        Object longitudeObj = document.get("longitude");
        if (longitudeObj instanceof Number) {
            dto.setLongitude(((Number) longitudeObj).doubleValue());
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
