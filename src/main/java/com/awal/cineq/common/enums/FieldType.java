package com.awal.cineq.common.enums;

import lombok.Getter;
import lombok.extern.slf4j.Slf4j;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Collection;
import java.util.Map;
import java.util.Optional;

/**
 * FieldType Enum - Represents all supported data types for dynamic forms
 *
 * This enum defines all available field types that can be used in form definitions.
 * Each type includes:
 * - Display name (human-readable)
 * - Description (what the type represents)
 * - Java class representation
 *
 * Usage Example:
 * {@code
 * FieldType type = FieldType.STRING;
 * String displayName = type.getDisplayName();  // "String"
 * boolean isValid = type.validate("hello");    // true
 * }
 *
 * Supported Types:
 * - STRING: Text values
 * - INTEGER: Whole numbers (-2,147,483,648 to 2,147,483,647)
 * - LONG: Large whole numbers (-9,223,372,036,854,775,808 to 9,223,372,036,854,775,807)
 * - DOUBLE: Decimal numbers (64-bit floating point)
 * - FLOAT: Decimal numbers (32-bit floating point)
 * - BOOLEAN: True/false values
 * - DECIMAL: Precise decimal numbers (BigDecimal)
 * - OBJECT: JSON objects or Map structures
 * - ARRAY: Lists, sets, or collections of items
 * - DATE: Date values (yyyy-MM-dd format)
 * - DATETIME: Date and time values (yyyy-MM-dd'T'HH:mm:ss format)
 *
 * @author CineQ Development Team
 * @version 1.0
 */
@Slf4j
@Getter
public enum FieldType {
    STRING(
        "String",
        "A text value",
        String.class,
        "hello"
    ),
    INTEGER(
        "Integer",
        "A whole number between -2,147,483,648 and 2,147,483,647",
        Integer.class,
        42
    ),
    LONG(
        "Long",
        "A large whole number",
        Long.class,
        9223372036854775807L
    ),
    DOUBLE(
        "Double",
        "A decimal number with up to 15 significant digits",
        Double.class,
        3.14159265359
    ),
    FLOAT(
        "Float",
        "A decimal number with up to 7 significant digits",
        Float.class,
        3.14f
    ),
    BOOLEAN(
        "Boolean",
        "A true or false value",
        Boolean.class,
        true
    ),
    DECIMAL(
        "Decimal",
        "A precise decimal number (e.g., for money)",
        BigDecimal.class,
        "99.99"
    ),
    OBJECT(
        "Object",
        "A JSON object or key-value map structure",
        Map.class,
        "{\"key\": \"value\"}"
    ),
    ARRAY(
        "Array",
        "A list or collection of items",
        Collection.class,
        "[1, 2, 3]"
    ),
    DATE(
        "Date",
        "A date value (format: yyyy-MM-dd)",
        LocalDate.class,
        "2025-01-11"
    ),
    DATETIME(
        "DateTime",
        "A date and time value (format: yyyy-MM-dd'T'HH:mm:ss)",
        LocalDateTime.class,
        "2025-01-11T14:30:00"
    );

    // Properties
    private final String displayName;
    private final String description;
    private final Class<?> javaClass;
    private final Object exampleValue;

    /**
     * Constructor for FieldType enum
     *
     * @param displayName Human-readable name
     * @param description Description of what this type represents
     * @param javaClass Java class that represents this type
     * @param exampleValue Example value of this type
     */
    FieldType(String displayName, String description, Class<?> javaClass, Object exampleValue) {
        this.displayName = displayName;
        this.description = description;
        this.javaClass = javaClass;
        this.exampleValue = exampleValue;
    }

    /**
     * Validate if a value matches this FieldType
     *
     * @param value The value to validate
     * @return true if value is of this type, false otherwise
     */
    public boolean validate(Object value) {
        if (value == null) {
            return false;  // null is not a valid value for any type
        }

        switch (this) {
            case STRING:
                return value instanceof String;

            case INTEGER:
                return value instanceof Integer ||
                       (value instanceof String && isValidInteger((String) value));

            case LONG:
                return value instanceof Long ||
                       (value instanceof Integer) ||
                       (value instanceof String && isValidLong((String) value));

            case DOUBLE:
                return value instanceof Double ||
                       (value instanceof Number && !(value instanceof Integer && !(value instanceof Long)));

            case FLOAT:
                return value instanceof Float ||
                       (value instanceof Number);

            case BOOLEAN:
                return value instanceof Boolean ||
                       (value instanceof String && isValidBoolean((String) value));

            case DECIMAL:
                return value instanceof BigDecimal ||
                       (value instanceof Number) ||
                       (value instanceof String && isValidBigDecimal((String) value));

            case OBJECT:
                return value instanceof Map;

            case ARRAY:
                return value instanceof Collection;

            case DATE:
                return value instanceof LocalDate ||
                       (value instanceof String && isValidDate((String) value));

            case DATETIME:
                return value instanceof LocalDateTime ||
                       (value instanceof String && isValidDateTime((String) value));

            default:
                log.warn("Unknown type: {}", this);
                return false;
        }
    }

    /**
     * Convert a string representation to FieldType enum
     *
     * Example: "STRING" → FieldType.STRING
     *
     * @param typeString The string representation of the type (case-insensitive)
     * @return Optional containing the FieldType if found
     */
    public static Optional<FieldType> fromString(String typeString) {
        if (typeString == null || typeString.trim().isEmpty()) {
            return Optional.empty();
        }

        try {
            return Optional.of(FieldType.valueOf(typeString.trim().toUpperCase()));
        } catch (IllegalArgumentException e) {
            log.warn("Unknown field type: {}", typeString);
            return Optional.empty();
        }
    }

    /**
     * Check if a string can be parsed as an Integer
     *
     * @param value The string to check
     * @return true if string is a valid integer
     */
    private static boolean isValidInteger(String value) {
        try {
            Integer.parseInt(value.trim());
            return true;
        } catch (NumberFormatException e) {
            return false;
        }
    }

    /**
     * Check if a string can be parsed as a Long
     *
     * @param value The string to check
     * @return true if string is a valid long
     */
    private static boolean isValidLong(String value) {
        try {
            Long.parseLong(value.trim());
            return true;
        } catch (NumberFormatException e) {
            return false;
        }
    }

    /**
     * Check if a string can be parsed as a Boolean
     *
     * @param value The string to check
     * @return true if string is "true", "false", "yes", "no", "1", "0" (case-insensitive)
     */
    private static boolean isValidBoolean(String value) {
        String trimmed = value.trim().toLowerCase();
        return trimmed.equals("true") || trimmed.equals("false") ||
               trimmed.equals("yes") || trimmed.equals("no") ||
               trimmed.equals("1") || trimmed.equals("0");
    }

    /**
     * Check if a string can be parsed as BigDecimal
     *
     * @param value The string to check
     * @return true if string is a valid decimal number
     */
    private static boolean isValidBigDecimal(String value) {
        try {
            new BigDecimal(value.trim());
            return true;
        } catch (NumberFormatException e) {
            return false;
        }
    }

    /**
     * Check if a string is a valid date in yyyy-MM-dd format
     *
     * @param value The string to check
     * @return true if string matches date format
     */
    private static boolean isValidDate(String value) {
        try {
            LocalDate.parse(value.trim());
            return true;
        } catch (Exception e) {
            return false;
        }
    }

    /**
     * Check if a string is a valid datetime in yyyy-MM-dd'T'HH:mm:ss format
     *
     * @param value The string to check
     * @return true if string matches datetime format
     */
    private static boolean isValidDateTime(String value) {
        try {
            LocalDateTime.parse(value.trim());
            return true;
        } catch (Exception e) {
            return false;
        }
    }

    /**
     * Get the default value for this type
     *
     * @return Example/default value for this type
     */
    public Object getExampleValue() {
        return exampleValue;
    }

    /**
     * Check if this type represents a number
     *
     * @return true if this is a numeric type (INTEGER, LONG, DOUBLE, FLOAT, DECIMAL)
     */
    public boolean isNumeric() {
        return this == INTEGER || this == LONG || this == DOUBLE || this == FLOAT || this == DECIMAL;
    }

    /**
     * Check if this type represents a collection
     *
     * @return true if this is a collection type (ARRAY, OBJECT)
     */
    public boolean isCollection() {
        return this == ARRAY || this == OBJECT;
    }

    /**
     * Check if this type represents a temporal value
     *
     * @return true if this is a date/time type (DATE, DATETIME)
     */
    public boolean isTemporal() {
        return this == DATE || this == DATETIME;
    }

    /**
     * Get a string representation of this type
     *
     * @return The enum name (STRING, INTEGER, etc.)
     */
    @Override
    public String toString() {
        return this.name();
    }
}

