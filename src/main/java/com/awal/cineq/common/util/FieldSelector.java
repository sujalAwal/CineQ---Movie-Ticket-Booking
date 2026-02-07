package com.awal.cineq.common.util;

import lombok.extern.slf4j.Slf4j;
import org.springframework.data.mongodb.core.query.Query;

import java.util.HashMap;
import java.util.Map;

/**
 * FieldSelector
 *
 * Utility for building MongoDB projection queries based on validation rules
 * Handles field selection to optimize MongoDB queries (fetch only needed fields)
 *
 * WHY:
 * - Improves query performance by not fetching unnecessary fields
 * - Reduces network bandwidth
 * - Enables fine-grained control over what data is returned
 *
 * Used by: UniversalFormServiceImpl for query optimization
 */
@Slf4j
public class FieldSelector {

    /**
     * Build projection fields from validationRules
     *
     * Extracts field names from validationRules to determine which fields should be queried
     *
     * @param validationRules Map containing field validation definitions
     * @return Map with MongoDB projection format: {fieldName: 1} for inclusion, {fieldName: 0} for exclusion
     */
    public static Map<String, Integer> buildProjectionFields(Map<String, Object> validationRules) {
        Map<String, Integer> projection = new HashMap<>();

        // If no validation rules, include all fields (empty projection)
        if (validationRules == null) {
            log.debug("buildProjectionFields: No validation rules provided, returning empty projection");
            return projection;
        }

        // Include always-needed fields for document tracking
        projection.put("_id", 1);           // MongoDB ObjectId
        projection.put("createdAt", 1);     // Created timestamp
        projection.put("updatedAt", 1);     // Updated timestamp
        projection.put("deletedAt", 1);     // Soft-delete marker

        // Add fields defined in validation rules
        // Use collectionField if specified, otherwise use the key name
        for (Map.Entry<String, Object> entry : validationRules.entrySet()) {
            String fieldKey = entry.getKey();
            Object ruleValue = entry.getValue();
            
            // Skip array element rules (e.g., buttons[*].title)
            if (fieldKey.contains("[*]")) {
                continue;
            }
            
            // Determine the actual MongoDB field name
            String collectionField = fieldKey;  // Default to key name
            
            if (ruleValue instanceof Map) {
                @SuppressWarnings("unchecked")
                Map<String, Object> fieldRule = (Map<String, Object>) ruleValue;
                String specifiedCollectionField = (String) fieldRule.get("collectionField");
                if (specifiedCollectionField != null && !specifiedCollectionField.isEmpty()) {
                    collectionField = specifiedCollectionField;
                }
            }
            
            projection.put(collectionField, 1);   // 1 = include field
            log.debug("buildProjectionFields: Adding '{}' to projection (from key '{}')", 
                collectionField, fieldKey);
        }

        log.debug("buildProjectionFields: Created projection with {} fields", projection.size());
        return projection;
    }

    /**
     * Apply MongoDB projection to a query
     *
     * Modifies the query to only fetch specified fields (performance optimization)
     *
     * @param query The MongoDB query to modify
     * @param projectionFields The fields to include in the result set
     */
    public static void applyProjection(Query query, Map<String, Integer> projectionFields) {
        if (query == null || projectionFields == null || projectionFields.isEmpty()) {
            log.debug("applyProjection: No projection to apply");
            return;
        }

        // MongoDB projection: include specified fields
        for (Map.Entry<String, Integer> entry : projectionFields.entrySet()) {
            if (entry.getValue() == 1) {
                query.fields().include(entry.getKey());  // Include field
            } else {
                query.fields().exclude(entry.getKey());  // Exclude field
            }
        }

        log.debug("applyProjection: Applied {} field projections to query", projectionFields.size());
    }
}
