package com.awal.cineq.config.service.impl;

import com.awal.cineq.config.service.BackendSettingsService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Map;

@Service
@RequiredArgsConstructor
@Slf4j
@Transactional(readOnly = true)
public class BackendSettingsServiceImpl implements BackendSettingsService {

    private final MongoTemplate mongoTemplate;

    @Override
    public boolean getBooleanValue(String slug) {
        try {
            Query query = new Query(Criteria.where("slug").is(slug)
                .and("isActive").is(true)
                .and("deletedAt").is(null));

            Map<String, Object> setting = mongoTemplate.findOne(query, Map.class, "backend_settings");

            if (setting == null) {
                log.warn("Backend setting not found for slug: {}", slug);
                return false;
            }

            Object value = setting.get("value");
            if (value == null) {
                return false;
            }

            return Boolean.parseBoolean(value.toString());
        } catch (Exception e) {
            log.error("Error retrieving boolean setting for slug: {}", slug, e);
            return false;
        }
    }

    @Override
    public String getStringValue(String slug) {
        try {
            Query query = new Query(Criteria.where("slug").is(slug)
                .and("isActive").is(true)
                .and("deletedAt").is(null));

            Map<String, Object> setting = mongoTemplate.findOne(query, Map.class, "backend_settings");

            if (setting == null) {
                log.warn("Backend setting not found for slug: {}", slug);
                return null;
            }

            Object value = setting.get("value");
            return value != null ? value.toString() : null;
        } catch (Exception e) {
            log.error("Error retrieving string setting for slug: {}", slug, e);
            return null;
        }
    }

    @Override
    public Object getValue(String slug) {
        try {
            Query query = new Query(Criteria.where("slug").is(slug)
                .and("isActive").is(true)
                .and("deletedAt").is(null));

            Map<String, Object> setting = mongoTemplate.findOne(query, Map.class, "backend_settings");

            if (setting == null) {
                log.warn("Backend setting not found for slug: {}", slug);
                return null;
            }

            return setting.get("value");
        } catch (Exception e) {
            log.error("Error retrieving setting for slug: {}", slug, e);
            return null;
        }
    }
}
