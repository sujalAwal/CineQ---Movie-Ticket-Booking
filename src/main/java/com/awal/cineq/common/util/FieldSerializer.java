package com.awal.cineq.common.util;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;

import java.util.HashMap;
import java.util.Map;

/**
 * FieldSerializer
 *
 * Utility for serializing MongoDB documents to JSON-compatible format
 * Handles BSON → JSON conversion and applies output field mappings from validation rules
 *
 * WHY:
 * - Convert BSON objects (MongoDB format) to JSON (API response format)
 * - Apply field mapping transformations (database field → output field)
 * - Filter fields based on validation rules schema (select: true/false)
 * - Format dates from milliseconds to ISO-8601
 * - Provides consistent API response format
 *
 * HOW:
 * - Iterates through validationRules schema
 * - Checks "select" flag: true = include field, false = exclude field
 * - Maps collectionField → outputField (e.g., "_id" → "id")
 * - Formats values based on "format" specification (e.g., "iso8601" for dates)
 *
 * Used by: UniversalFormServiceImpl for document serialization
 */
@Slf4j
public class FieldSerializer {

    private static final ObjectMapper objectMapper = new ObjectMapper();
    private static final DateTimeFormatter ISO_FORMATTER =
        DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss'Z'")
            .withZone(ZoneId.of("UTC"));

    /**
     * Serialize a MongoDB document to JSON-compatible Map using validation rules schema
     *
     * Process:
     * 1. Iterate through validationRules (schema definition)
     * 2. Check if field should be included (serialization.select = true)
     * 3. Get collectionField (where data is stored in MongoDB)
     * 4. Get outputField (what the field should be called in API response)
     * 5. Format the value (dates, ObjectIds, etc.)
     * 6. Map field: collectionField → outputField
     *
     * @param document The raw MongoDB document to serialize
     * @param validationRules Map containing field definitions with serialization rules
     * @return Serialized document as Map (filtered, formatted, and renamed fields)
     *
     * Example:
     * Input document: {_id: ObjectId("..."), name: "Role", createdAt: 1770050044902}
     * ValidationRules: {id: {collectionField: "_id", serialization: {select: true, outputField: "id"}}, ...}
     * Output: {id: "123abc...", name: "Role", createdAt: "2025-12-03T10:34:04Z"}
     */
    public static Map<String, Object> serializeDocument(Map<String, Object> document, Map<String, Object> validationRules) {

        if (document == null) {
            log.debug("serializeDocument: Document is null, returning empty map");
            return new HashMap<>();
        }

        Map<String, Object> serialized = new HashMap<>();

        log.debug("serializeDocument: Starting serialization with {} rules",
            validationRules != null ? validationRules.size() : 0);

        if (validationRules != null && !validationRules.isEmpty()) {
            log.debug("serializeDocument: Validation rules keys: {}", validationRules.keySet());
        }

        // If no validation rules provided, return raw document with ObjectIds converted
        if (validationRules == null || validationRules.isEmpty()) {
            log.debug("No validation rules provided, returning raw document with converted ObjectIds");
            Map<String, Object> raw = new HashMap<>(document);
            raw.replaceAll((key, value) -> {
                if (value != null && value.getClass().getSimpleName().equals("ObjectId")) {
                    return value.toString();
                }
                return value;
            });
            return raw;
        }

        // Iterate through validation rules schema
        for (Map.Entry<String, Object> ruleEntry : validationRules.entrySet()) {
            String fieldKey = ruleEntry.getKey();
            Object ruleValue = ruleEntry.getValue();

            log.debug("serializeDocument: Processing field: {}", fieldKey);

            if (!(ruleValue instanceof Map)) {
                log.debug("Skipping field {} - rule value is not a map, is: {}", fieldKey,
                    ruleValue != null ? ruleValue.getClass().getSimpleName() : "null");
                continue;
            }

            @SuppressWarnings("unchecked")
            Map<String, Object> fieldRule = (Map<String, Object>) ruleValue;

            // Step 1: Check if field should be included in output
            @SuppressWarnings("unchecked")
            Map<String, Object> serialization =
                (Map<String, Object>) fieldRule.get("serialization");

            if (serialization == null) {
                log.debug("Skipping field {} - no serialization config", fieldKey);
                continue;
            }

            log.debug("Field {} serialization config: {}", fieldKey, serialization);

            // Step 2: Check select flag (true = include, false = exclude)
            Boolean select = (Boolean) serialization.get("select");
            if (select != null && !select) {
                log.debug("Excluding field: {} (select=false)", fieldKey);
                continue;
            }

            // Step 3: Get collection field name (where data is in MongoDB)
            String collectionField = (String) fieldRule.get("collectionField");
            if (collectionField == null) {
                collectionField = fieldKey;  // Fallback to rule key
            }

            // Step 4: Get output field name (what it should be called in API response)
            String outputField = (String) serialization.get("outputField");
            if (outputField == null) {
                outputField = fieldKey;  // Fallback to rule key
            }

            // Step 5: Get the raw value from document
            Object value = document.get(collectionField);

            if (value == null) {
                log.debug("Field not found in document: {}", collectionField);
                continue;
            }

            log.debug("Found {} in document with value type: {}, value: {}",
                collectionField, value.getClass().getSimpleName(), value);

            // Step 6: Format the value based on type and format specification
            String format = (String) serialization.get("format");
            log.debug("Field {} format specification: {}", fieldKey, format);

            Object formattedValue = formatValue(value, format);

            // Step 7: Add to serialized output using outputField name
            serialized.put(outputField, formattedValue);

            log.debug("Mapped {} → {} : {} (formatted from: {})", collectionField, outputField,
                formattedValue, value);
        }

        log.debug("serializeDocument: Serialization complete, output has {} fields", serialized.size());
        return serialized;
    }

    /**
     * Format a value based on its type and format specification
     *
     * @param value Raw value from MongoDB
     * @param format Format specification (e.g., "iso8601", "string")
     * @return Formatted value (ObjectIds as String, timestamps as ISO-8601, etc.)
     */
    private static Object formatValue(Object value, String format) {
        if (value == null) {
            return null;
        }

        log.debug("formatValue: Formatting value of type={}, format={}, value={}",
            value.getClass().getSimpleName(), format, value);

        // Convert ObjectId to String representation
        if (value.getClass().getSimpleName().equals("ObjectId")) {
            log.debug("Converting ObjectId to string");
            return value.toString();
        }

        // Handle ISO-8601 date formatting (timestamps stored as various types in MongoDB)
        // Check format explicitly OR detect Long/Date types
        if ("iso8601".equalsIgnoreCase(format) || "date".equalsIgnoreCase(format)) {
            log.debug("Format is iso8601/date, processing value as date");

            // If value is java.util.Date (MongoDB stores dates as Date objects)
            if (value instanceof java.util.Date) {
                long millis = ((java.util.Date) value).getTime();
                String isoDate = Instant.ofEpochMilli(millis)
                    .atZone(ZoneId.of("UTC"))
                    .format(ISO_FORMATTER);
                log.debug("Converted java.util.Date {} to ISO-8601: {}", value, isoDate);
                return isoDate;
            }

            // If value is Long (milliseconds from MongoDB)
            if (value instanceof Long) {
                long millis = (Long) value;
                String isoDate = Instant.ofEpochMilli(millis)
                    .atZone(ZoneId.of("UTC"))
                    .format(ISO_FORMATTER);
                log.debug("Converted timestamp {} to ISO-8601: {}", millis, isoDate);
                return isoDate;
            }

            // If value is already a date string
            if (value instanceof String) {
                log.debug("Value is already String, returning as-is");
                return value;
            }
        }

        // Handle string format conversion
        if ("string".equalsIgnoreCase(format)) {
            log.debug("Format is string, converting to string");
            return value.toString();
        }

        // If no format specified AND value is Long, try to detect if it's a date
        // (fallback for when format is missing from schema)
        if (format == null && value instanceof Long) {
            long longValue = (Long) value;
            // Heuristic: if Long is > 1000000000000 (13 digits), it's likely a timestamp in millis
            // because that's December 2001, and CineQ started after 2020
            if (longValue > 1000000000000L && longValue < 9999999999999L) {
                log.debug("Detected Long value as potential timestamp (no format specified), converting to ISO-8601");
                String isoDate = Instant.ofEpochMilli(longValue)
                    .atZone(ZoneId.of("UTC"))
                    .format(ISO_FORMATTER);
                log.debug("Converted timestamp {} to ISO-8601: {}", longValue, isoDate);
                return isoDate;
            }
        }

        // Return as-is for other types (boolean, object, array, etc.)
        log.debug("No format match, returning value as-is");
        return value;
    }

    /**
     * Serialize a single value to JSON-compatible format
     *
     * Converts common MongoDB types to JSON-serializable formats
     *
     * @param value The value to serialize
     * @return Serialized value (ObjectIds as String, timestamps as ISO-8601, etc.)
     */
    public static Object serializeValue(Object value) {
        if (value == null) {
            return null;
        }

        // Convert ObjectId to String
        if (value.getClass().getSimpleName().equals("ObjectId")) {
            log.debug("Converting ObjectId to string");
            return value.toString();
        }

        // Convert java.util.Date to ISO-8601 format
        if (value instanceof java.util.Date) {
            long millis = ((java.util.Date) value).getTime();
            String isoDate = Instant.ofEpochMilli(millis)
                .atZone(ZoneId.of("UTC"))
                .format(ISO_FORMATTER);
            log.debug("Converted java.util.Date to ISO-8601: {}", isoDate);
            return isoDate;
        }

        // Convert Long milliseconds to ISO-8601 format
        if (value instanceof Long) {
            long millis = (Long) value;
            String isoDate = Instant.ofEpochMilli(millis)
                .atZone(ZoneId.of("UTC"))
                .format(ISO_FORMATTER);
            log.debug("Converted timestamp {} to ISO-8601: {}", millis, isoDate);
            return isoDate;
        }

        // Convert LocalDateTime to ISO format
        if (value instanceof java.time.LocalDateTime) {
            String isoDate = ((java.time.LocalDateTime) value).format(
                java.time.format.DateTimeFormatter.ISO_LOCAL_DATE_TIME
            );
            log.debug("Converted LocalDateTime to ISO format: {}", isoDate);
            return isoDate;
        }

        // Return as-is for other types (String, Boolean, Map, List, etc.)
        return value;
    }
}
