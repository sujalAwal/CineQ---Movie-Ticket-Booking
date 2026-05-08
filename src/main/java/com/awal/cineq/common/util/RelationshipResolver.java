package com.awal.cineq.common.util;

import lombok.extern.slf4j.Slf4j;
import org.bson.types.ObjectId;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;

import java.util.*;
import java.util.stream.Collectors;

/**
 * RelationshipResolver
 *
 * Utility for resolving foreign key relationships in MongoDB documents
 * Performs batched $in queries to populate related entity data
 *
 * WHY:
 * - Enriches documents with related entity information without storing denormalized data
 * - Uses efficient batched queries to prevent N+1 problem
 * - Handles soft-deleted documents (filters by deletedAt=null)
 * - Applies field serialization to related documents for JSON compatibility
 *
 * HOW:
 * - Extracts relationship config from workflowRules (collection, lookupField, returnFields)
 * - Collects unique sourceField values from documents
 * - Converts ObjectIds if needed
 * - Builds single $in query per relationship with optional projection
 * - Maps results back as sibling keys using collection name
 * - Serializes values using FieldSerializer for JSON compatibility
 *
 * Used by: UniversalFormServiceImpl GET methods for enriching form submission responses
 */
@Slf4j
public class RelationshipResolver {

    /**
     * Resolve relationships for multiple documents (batch operation)
     *
     * Process:
     * 1. For each relationship config in the relationships map:
     *    a. Collect unique sourceField values from all documents
     *    b. Convert to ObjectId if lookupField="_id"
     *    c. Query related collection with $in filter and deletedAt=null
     *    d. Apply projection if returnFields specified
     *    e. Build lookup map from results
     *    f. Map back to source documents as sibling keys (collection name)
     *
     * @param documents List of documents to enrich (mutated in-place)
     * @param relationships Map of relationship configs: {sourceField → {collection, lookupField, returnFields}}
     * @param mongoTemplate MongoDB template for queries
     * @return The same documents list (enriched in-place)
     *
     * Example relationships config:
     * {
     *   "movieId": {
     *     "collection": "movies",
     *     "lookupField": "_id",
     *     "returnFields": ["title", "duration", "posterUrl"]
     *   },
     *   "theatreId": {
     *     "collection": "theatres",
     *     "lookupField": "_id",
     *     "returnFields": ["name", "location", "city"]
     *   }
     * }
     */
    public static List<Map<String, Object>> resolveRelationships(
            List<Map<String, Object>> documents,
            Map<String, Object> relationships,
            MongoTemplate mongoTemplate) {

        log.debug("resolveRelationships: Starting resolution for {} documents with {} relationships",
                documents != null ? documents.size() : 0,
                relationships != null ? relationships.size() : 0);

        // Guard: if relationships is null or empty, return immediately
        if (relationships == null || relationships.isEmpty()) {
            log.debug("resolveRelationships: No relationships to resolve, returning documents as-is");
            return documents;
        }

        if (documents == null || documents.isEmpty()) {
            log.debug("resolveRelationships: No documents provided, returning empty list");
            return documents;
        }

        // Process each relationship configuration
        for (Map.Entry<String, Object> relationshipEntry : relationships.entrySet()) {
            String sourceField = relationshipEntry.getKey();
            Object relationshipConfig = relationshipEntry.getValue();

            log.debug("resolveRelationships: Processing relationship for sourceField='{}'", sourceField);

            if (!(relationshipConfig instanceof Map)) {
                log.warn("resolveRelationships: Relationship config for '{}' is not a map, skipping", sourceField);
                continue;
            }

            @SuppressWarnings("unchecked")
            Map<String, Object> config = (Map<String, Object>) relationshipConfig;

            // Extract relationship config
            String collection = (String) config.get("collection");
            String lookupField = (String) config.get("lookupField");
            @SuppressWarnings("unchecked")
            List<String> returnFields = (List<String>) config.get("returnFields");

            if (collection == null || collection.isEmpty() || lookupField == null || lookupField.isEmpty()) {
                log.warn("resolveRelationships: Invalid relationship config for '{}': missing collection or lookupField",
                        sourceField);
                continue;
            }

            log.debug("resolveRelationships: Relationship '{}' → collection='{}', lookupField='{}', returnFields={}",
                    sourceField, collection, lookupField, returnFields);

            try {
                // Collect unique non-null values from sourceField across all documents
                Set<Object> uniqueIds = new HashSet<>();
                for (Map<String, Object> doc : documents) {
                    Object value = doc.get(sourceField);
                    if (value != null) {
                        uniqueIds.add(value);
                    }
                }

                // Guard: if no values found, populate sibling keys with null and skip query
                if (uniqueIds.isEmpty()) {
                    log.debug("resolveRelationships: No values found for sourceField='{}', populating sibling keys with null", sourceField);
                    for (Map<String, Object> doc : documents) {
                        doc.put(collection, null);
                    }
                    log.debug("resolveRelationships: Populated {} sibling keys with null for collection='{}'", documents.size(), collection);
                    continue;
                }

                log.debug("resolveRelationships: Found {} unique values for sourceField='{}'", uniqueIds.size(), sourceField);

                // Convert to ObjectId if lookupField is "_id"
                Set<Object> lookupIds = new HashSet<>();
                if ("_id".equals(lookupField)) {
                    for (Object id : uniqueIds) {
                        try {
                            if (id instanceof String) {
                                lookupIds.add(new ObjectId((String) id));
                            } else {
                                lookupIds.add(id);
                            }
                        } catch (IllegalArgumentException e) {
                            log.warn("resolveRelationships: Invalid ObjectId format for value='{}', skipping", id);
                        }
                    }
                } else {
                    // Use string values as-is for non-_id fields
                    lookupIds.addAll(uniqueIds.stream()
                            .map(Object::toString)
                            .collect(Collectors.toSet()));
                }

                if (lookupIds.isEmpty()) {
                    log.debug("resolveRelationships: No valid lookup IDs after conversion for sourceField='{}', populating sibling keys with null", sourceField);
                    for (Map<String, Object> doc : documents) {
                        doc.put(collection, null);
                    }
                    log.debug("resolveRelationships: Populated {} sibling keys with null for collection='{}'", documents.size(), collection);
                    continue;
                }

                log.debug("resolveRelationships: Built lookup set with {} IDs for relationship='{}'", lookupIds.size(), sourceField);

                // Build query: {lookupField: {$in: lookupIds}, deletedAt: null}
                Query query = new Query(
                        Criteria.where(lookupField).in(lookupIds)
                                .and("deletedAt").is(null)
                );

                // Apply projection if returnFields specified
                if (returnFields != null && !returnFields.isEmpty()) {
                    query.fields().include("_id");  // Always include _id for output identity
                    // Include lookupField if it's not _id (needed for internal matching)
                    if (!"_id".equals(lookupField)) {
                        query.fields().include(lookupField);
                    }
                    for (String field : returnFields) {
                        query.fields().include(field);
                    }
                    log.debug("resolveRelationships: Applied projection with _id, lookupField='{}', and {} return fields", 
                            lookupField, returnFields.size());
                } else {
                    log.debug("resolveRelationships: No projection applied, returning all fields");
                }

                // Execute query
                @SuppressWarnings("unchecked")
                List<Map<String, Object>> relatedDocs = (List<Map<String, Object>>) (List<?>) 
                        mongoTemplate.find(query, Map.class, collection);

                log.debug("resolveRelationships: Found {} related documents from collection='{}'", relatedDocs.size(), collection);

                // Build lookup map: use lookupField as key for matching source values
                Map<String, Map<String, Object>> lookupMap = new HashMap<>();
                for (Map<String, Object> relatedDoc : relatedDocs) {
                    Object lookupKeyValue = relatedDoc.get(lookupField);
                    if (lookupKeyValue != null) {
                        // Convert lookupKeyValue to string key for matching
                        String lookupKey = (lookupKeyValue instanceof ObjectId) ? 
                                ((ObjectId) lookupKeyValue).toHexString() : 
                                lookupKeyValue.toString();

                        // Serialize all values in the related document
                        Map<String, Object> serializedDoc = new HashMap<>();
                        for (Map.Entry<String, Object> field : relatedDoc.entrySet()) {
                            Object fieldValue = FieldSerializer.serializeValue(field.getValue());
                            // Rename _id to id in the output
                            if ("_id".equals(field.getKey())) {
                                serializedDoc.put("id", fieldValue);
                            } else {
                                serializedDoc.put(field.getKey(), fieldValue);
                            }
                        }

                        lookupMap.put(lookupKey, serializedDoc);
                        log.debug("resolveRelationships: Added lookup entry for lookupField='{}' with value='{}'", lookupField, lookupKey);
                    }
                }

                // Map results back to source documents as sibling keys (using collection name as key)
                // Use same field as lookupField for consistent matching
                for (Map<String, Object> doc : documents) {
                    Object sourceValue = doc.get(sourceField);
                    if (sourceValue != null) {
                        String sourceKey = (sourceValue instanceof ObjectId) ? 
                                ((ObjectId) sourceValue).toHexString() : 
                                sourceValue.toString();

                        Map<String, Object> relatedData = lookupMap.get(sourceKey);
                        doc.put(collection, relatedData);  // Sibling key = collection name

                        if (relatedData != null) {
                            log.debug("resolveRelationships: Mapped sourceField='{}' with sourceKey='{}' to collection='{}' using lookupField='{}'",
                                    sourceField, sourceKey, collection, lookupField);
                        } else {
                            log.debug("resolveRelationships: No related data found for sourceField='{}' with sourceKey='{}' (lookupField='{}')",
                                    sourceField, sourceKey, lookupField);
                        }
                    } else {
                        // Source value is null, set sibling key to null
                        doc.put(collection, null);
                        log.debug("resolveRelationships: sourceField='{}' is null, setting collection='{}' to null", sourceField, collection);
                    }
                }

                log.info("resolveRelationships: Completed relationship='{}', enriched {} documents",
                        sourceField, documents.size());

            } catch (Exception e) {
                log.error("resolveRelationships: Error resolving relationship for sourceField='{}', continuing gracefully",
                        sourceField, e);
                // Graceful degradation: continue with next relationship if one fails
            }
        }

        log.info("resolveRelationships: Resolution complete for {} relationships",
                relationships.size());
        return documents;  // Return mutated list
    }

    /**
     * Resolve relationships for a single document (convenience wrapper)
     *
     * Wraps the document in a list, delegates to resolveRelationships, returns the single enriched document
     *
     * @param document Single document to enrich
     * @param relationships Map of relationship configs
     * @param mongoTemplate MongoDB template for queries
     * @return The enriched document (mutated)
     */
    public static Map<String, Object> resolveRelationshipsForSingle(
            Map<String, Object> document,
            Map<String, Object> relationships,
            MongoTemplate mongoTemplate) {

        log.debug("resolveRelationshipsForSingle: Starting resolution for single document");

        if (document == null) {
            log.debug("resolveRelationshipsForSingle: Document is null, returning null");
            return null;
        }

        // Wrap in list, delegate to batch method, extract result
        List<Map<String, Object>> list = new ArrayList<>();
        list.add(document);

        List<Map<String, Object>> enriched = resolveRelationships(list, relationships, mongoTemplate);

        log.debug("resolveRelationshipsForSingle: Resolution complete for single document");
        return enriched.isEmpty() ? null : enriched.get(0);
    }
}
