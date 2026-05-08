package com.awal.cineq.common.util;

import lombok.extern.slf4j.Slf4j;
import org.bson.types.ObjectId;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;

import java.util.*;
import java.util.stream.Collectors;

/**
 * SearchResolver
 *
 * Utility for building schema-driven search criteria with cross-collection support
 * Constructs MongoDB $or criteria based on searchable field configurations
 *
 * WHY:
 * - Enables efficient filtering on form submissions based on searchable fields
 * - Supports searching both direct fields and fields in related collections
 * - Uses case-insensitive "like" pattern for intuitive search experience
 * - Eliminates client-side filtering, reducing bandwidth and improving UX
 * - Schema-driven: search config lives in workflowRules, no code changes needed per form
 *
 * HOW:
 * - Extracts search config from workflowRules (enabled, fields array)
 * - For each searchable field:
 *   - Direct fields: adds regex criteria directly
 *   - Cross-collection fields: queries related collection, collects matching IDs, adds $in criteria
 * - Combines all criteria into single $or clause for flexible matching
 * - Returns MongoDB Criteria to be AND-ed with existing query filters
 *
 * Used by: UniversalFormServiceImpl.getSubmissionsByFormSlug() for search filtering
 */
@Slf4j
public class SearchResolver {

    /**
     * Build search criteria based on searchable field configuration
     *
     * Process:
     * 1. Guard checks: search term blank? search config disabled/missing? return null
     * 2. Escape regex special characters in search term
     * 3. For each field in search config:
     *    - Direct field: add regex criteria
     *    - Cross-collection field: lookup related docs, collect IDs, add $in criteria
     * 4. Combine all criteria into $or clause
     * 5. Return combined Criteria (or null if no criteria built)
     *
     * @param searchTerm The user search string (e.g., "Inception")
     * @param searchConfig The search configuration from workflowRules
     * @param mongoTemplate MongoDB template for cross-collection queries
     * @return Combined $or Criteria for filtering, or null if no search should be applied
     *
     * Example searchConfig:
     * {
     *   "enabled": true,
     *   "fields": [
     *     { "field": "title" },
     *     { "field": "language" },
     *     { "field": "movieTitle", "collection": "movies", "foreignKey": "movieId", "lookupField": "_id" }
     *   ]
     * }
     */
    public static Criteria buildSearchCriteria(
            String searchTerm,
            Map<String, Object> searchConfig,
            MongoTemplate mongoTemplate) {

        log.debug("buildSearchCriteria: Starting search criteria building with term='{}'", searchTerm);

        // Guard: search term is empty or null
        if (searchTerm == null || searchTerm.trim().isEmpty()) {
            log.debug("buildSearchCriteria: Search term is null or empty, returning null");
            return null;
        }

        String trimmedSearchTerm = searchTerm.trim();

        // Guard: search config is null or empty
        if (searchConfig == null || searchConfig.isEmpty()) {
            log.debug("buildSearchCriteria: Search config is null or empty, returning null");
            return null;
        }

        // Guard: search is disabled
        Boolean enabled = (Boolean) searchConfig.get("enabled");
        if (enabled == null || !enabled) {
            log.debug("buildSearchCriteria: Search is disabled (enabled={}), returning null", enabled);
            return null;
        }

        // Guard: fields array is empty
        @SuppressWarnings("unchecked")
        List<Map<String, Object>> fields = (List<Map<String, Object>>) searchConfig.get("fields");
        if (fields == null || fields.isEmpty()) {
            log.debug("buildSearchCriteria: Fields array is null or empty, returning null");
            return null;
        }

        log.debug("buildSearchCriteria: Processing {} search fields", fields.size());

        // Escape regex special characters in search term for literal "like" matching
        String escapedSearchTerm = escapeRegexSpecialChars(trimmedSearchTerm);
        log.debug("buildSearchCriteria: Escaped search term: '{}' → '{}'", trimmedSearchTerm, escapedSearchTerm);

        // Build criteria list for $or
        List<Criteria> criteriaList = new ArrayList<>();

        // Process each searchable field
        for (int i = 0; i < fields.size(); i++) {
            Map<String, Object> fieldConfig = fields.get(i);
            String field = (String) fieldConfig.get("field");
            String collection = (String) fieldConfig.get("collection");

            if (field == null || field.isEmpty()) {
                log.warn("buildSearchCriteria: Field {} has no 'field' key, skipping", i);
                continue;
            }

            log.debug("buildSearchCriteria: Processing field[{}]: field='{}', collection='{}'", i, field, collection);

            try {
                if (collection == null || collection.isEmpty()) {
                    // Direct field: search in current collection
                    Criteria fieldCriteria = Criteria.where(field).regex(escapedSearchTerm, "i");
                    criteriaList.add(fieldCriteria);
                    log.debug("buildSearchCriteria: Added direct field criteria for field='{}'", field);
                } else {
                    // Cross-collection field: lookup related collection first
                    processCrossCollectionField(fieldConfig, escapedSearchTerm, mongoTemplate, criteriaList);
                }
            } catch (Exception e) {
                log.error("buildSearchCriteria: Error processing field config at index {}, continuing gracefully", i, e);
                // Graceful degradation: skip this field and continue with next
            }
        }

        // Combine all criteria into $or clause
        if (criteriaList.isEmpty()) {
            log.debug("buildSearchCriteria: No criteria branches generated, returning null");
            return null;
        }

        Criteria combinedCriteria = new Criteria().orOperator(criteriaList.toArray(new Criteria[0]));
        log.info("buildSearchCriteria: Built combined $or criteria with {} branches", criteriaList.size());
        return combinedCriteria;
    }

    /**
     * Process a cross-collection search field
     *
     * Queries the related collection for matching documents, collects their IDs,
     * then adds an $in criteria on the foreignKey field in the main collection
     *
     * @param fieldConfig The field configuration containing collection, foreignKey, lookupField
     * @param escapedSearchTerm The escaped search term (regex-safe)
     * @param mongoTemplate MongoDB template for queries
     * @param criteriaList The criteria list to append to (mutated)
     */
    private static void processCrossCollectionField(
            Map<String, Object> fieldConfig,
            String escapedSearchTerm,
            MongoTemplate mongoTemplate,
            List<Criteria> criteriaList) {

        String field = (String) fieldConfig.get("field");
        String collection = (String) fieldConfig.get("collection");
        String foreignKey = (String) fieldConfig.get("foreignKey");
        String lookupField = (String) fieldConfig.get("lookupField");

        log.debug("processCrossCollectionField: field='{}', collection='{}', foreignKey='{}', lookupField='{}'",
                field, collection, foreignKey, lookupField);

        // Validate cross-collection config
        if (collection == null || collection.isEmpty() || foreignKey == null || foreignKey.isEmpty()) {
            log.warn("processCrossCollectionField: Invalid config - missing collection or foreignKey");
            return;
        }

        // Default lookupField to _id if not specified
        if (lookupField == null || lookupField.isEmpty()) {
            lookupField = "_id";
        }

        log.debug("processCrossCollectionField: Querying collection='{}' for field='{}' matching '{}'",
                collection, field, escapedSearchTerm);

        try {
            // Build query on related collection: {field: /searchTerm/i, deletedAt: null}
            Query relatedQuery = new Query(
                    Criteria.where(field).regex(escapedSearchTerm, "i")
                            .and("deletedAt").is(null)
            );

            // Project only the lookupField (typically _id)
            relatedQuery.fields().include(lookupField);

            log.debug("processCrossCollectionField: Executing query on collection='{}' with projection='{}'",
                    collection, lookupField);

            // Execute query
            @SuppressWarnings("unchecked")
            List<Map<String, Object>> matchingDocs = (List<Map<String, Object>>) (List<?>) 
                    mongoTemplate.find(relatedQuery, Map.class, collection);

            log.debug("processCrossCollectionField: Found {} matching documents in collection='{}'",
                    matchingDocs.size(), collection);

            // Collect IDs from matching documents
            Set<Object> matchingIds = new HashSet<>();
            for (Map<String, Object> doc : matchingDocs) {
                Object idValue = doc.get(lookupField);
                if (idValue != null) {
                    // Convert ObjectId to string for consistent keying
                    String idString = (idValue instanceof ObjectId) ? 
                            ((ObjectId) idValue).toHexString() : 
                            idValue.toString();
                    matchingIds.add(idString);
                }
            }

            log.debug("processCrossCollectionField: Collected {} unique IDs from lookupField='{}'",
                    matchingIds.size(), lookupField);

            // If any matches found, add $in criteria on foreignKey
            if (!matchingIds.isEmpty()) {
                // Convert string IDs back to ObjectId if lookupField is _id
                Set<Object> idsForCriteria = new HashSet<>();
                if ("_id".equals(lookupField)) {
                    for (Object id : matchingIds) {
                        try {
                            idsForCriteria.add(new ObjectId((String) id));
                        } catch (IllegalArgumentException e) {
                            log.warn("processCrossCollectionField: Invalid ObjectId format, skipping: {}", id);
                        }
                    }
                } else {
                    idsForCriteria.addAll(matchingIds);
                }

                if (!idsForCriteria.isEmpty()) {
                    Criteria inCriteria = Criteria.where(foreignKey).in(idsForCriteria);
                    criteriaList.add(inCriteria);
                    log.debug("processCrossCollectionField: Added $in criteria on foreignKey='{}' with {} IDs",
                            foreignKey, idsForCriteria.size());
                }
            } else {
                log.debug("processCrossCollectionField: No matches found in collection='{}', skipping this search branch",
                        collection);
            }

        } catch (Exception e) {
            log.error("processCrossCollectionField: Error querying collection='{}', skipping this field",
                    collection, e);
            // Graceful degradation: skip this field, continue with others
        }
    }

    /**
     * Escape regex special characters in search term for literal matching
     *
     * Escapes: . ^ $ * + ? { } [ ] \ | ( )
     * Ensures user input is treated as literal string, not regex pattern
     *
     * @param term Raw search term from user
     * @return Escaped string safe for use in MongoDB regex
     */
    private static String escapeRegexSpecialChars(String term) {
        if (term == null) {
            return "";
        }

        // Regex special characters that need escaping in MongoDB regex
        String[] specialChars = {"\\", "^", "$", ".", "*", "+", "?", "{", "}", "[", "]", "|", "("};
        String[] escapeReplacements = {"\\\\", "\\^", "\\$", "\\.", "\\*", "\\+", "\\?", "\\{", "\\}", "\\[", "\\]", "\\|", "\\("};

        String result = term;
        for (int i = 0; i < specialChars.length; i++) {
            result = result.replace(specialChars[i], escapeReplacements[i]);
        }

        // Close parenthesis needs special handling
        result = result.replace(")", "\\)");

        log.debug("escapeRegexSpecialChars: '{}' → '{}'", term, result);
        return result;
    }
}
