package com.awal.cineq.common.util;

import com.awal.cineq.common.enums.FieldType;
import com.awal.cineq.exception.ValidationException;
import lombok.extern.slf4j.Slf4j;

import java.util.Optional;

/**
 * TypeValidator Helper Class
 *
 * Provides type validation and conversion utilities for form fields.
 * This class works in conjunction with the FieldType enum to ensure
 * type safety and consistency across the application.
 *
 * Features:
 * - Type validation: Check if a value matches a specific type
 * - Type conversion: Convert strings to appropriate types
 * - String-based type validation: Convert type strings to FieldType enums first
 *
 * Usage Examples:
 * {@code
 * // Direct enum-based validation
 * boolean isValid = TypeValidator.validateType(FieldType.STRING, "hello");
 *
 * // String-based validation
 * boolean isValid = TypeValidator.validateTypeByString("42", "INTEGER");
 *
 * // Get type name
 * String name = TypeValidator.getTypeName(FieldType.STRING);  // "String"
 * }
 *
 * @author CineQ Development Team
 * @version 1.0
 */
@Slf4j
public class TypeValidator {

    private TypeValidator() {
        // Utility class - private constructor to prevent instantiation
    }

    /**
     * Validate if a value matches a specific FieldType
     *
     * @param fieldType The FieldType enum to validate against
     * @param value The value to validate
     * @return true if the value is of the specified type, false otherwise
     *
     * Example:
     * {@code
     * TypeValidator.validateType(FieldType.INTEGER, 42);      // true
     * TypeValidator.validateType(FieldType.INTEGER, "hello");  // false
     * }
     */
    public static boolean validateType(FieldType fieldType, Object value) {
        if (fieldType == null) {
            log.warn("FieldType cannot be null");
            return false;
        }

        if (value == null) {
            log.debug("Value is null, returning false");
            return false;
        }

        boolean isValid = fieldType.validate(value);

        if (!isValid) {
            log.debug("Value '{}' does not match type '{}' (expected: {})",
                     value, fieldType.getDisplayName(), fieldType.getJavaClass().getSimpleName());
        }

        return isValid;
    }

    /**
     * Validate a value by passing a string type name
     *
     * This method converts the string type to a FieldType enum and then validates.
     * If the type string is invalid, throws ValidationException.
     *
     * @param value The value to validate
     * @param typeString The type string (e.g., "STRING", "INTEGER", "BOOLEAN")
     * @return true if the value matches the specified type
     * @throws ValidationException if typeString is not a valid FieldType
     *
     * Example:
     * {@code
     * TypeValidator.validateTypeByString("hello", "STRING");     // true
     * TypeValidator.validateTypeByString("42", "INTEGER");       // true (string "42" is valid integer)
     * TypeValidator.validateTypeByString("hello", "INTEGER");    // false
     * }
     */
    public static boolean validateTypeByString(Object value, String typeString) {
        Optional<FieldType> fieldType = FieldType.fromString(typeString);

        if (fieldType.isEmpty()) {
            String errorMsg = "Unknown field type: '" + typeString + "'. Valid types are: " +
                             getValidTypeNames();
            log.error(errorMsg);
            throw new ValidationException(errorMsg);
        }

        return validateType(fieldType.get(), value);
    }

    /**
     * Get the human-readable display name for a FieldType
     *
     * @param fieldType The FieldType enum
     * @return The display name (e.g., "String", "Integer", "Boolean")
     *
     * Example:
     * {@code
     * String name = TypeValidator.getTypeName(FieldType.STRING);   // "String"
     * String name = TypeValidator.getTypeName(FieldType.INTEGER);  // "Integer"
     * }
     */
    public static String getTypeName(FieldType fieldType) {
        if (fieldType == null) {
            return "Unknown";
        }
        return fieldType.getDisplayName();
    }

    /**
     * Get the display name for a type string
     *
     * Converts string to FieldType enum and returns its display name.
     * If conversion fails, returns the original string.
     *
     * @param typeString The type string (e.g., "STRING", "INTEGER")
     * @return The display name, or the original string if type is unknown
     *
     * Example:
     * {@code
     * String name = TypeValidator.getTypeNameByString("STRING");    // "String"
     * String name = TypeValidator.getTypeNameByString("invalid");   // "invalid" (unknown, returns original)
     * }
     */
    public static String getTypeNameByString(String typeString) {
        Optional<FieldType> fieldType = FieldType.fromString(typeString);
        return fieldType.map(FieldType::getDisplayName).orElse(typeString);
    }

    /**
     * Check if a type string is valid
     *
     * @param typeString The type string to check
     * @return true if it's a valid FieldType name
     *
     * Example:
     * {@code
     * boolean valid = TypeValidator.isValidType("STRING");   // true
     * boolean valid = TypeValidator.isValidType("invalid");  // false
     * }
     */
    public static boolean isValidType(String typeString) {
        return FieldType.fromString(typeString).isPresent();
    }

    /**
     * Get all valid type names as a comma-separated string
     *
     * @return Comma-separated list of valid type names
     *
     * Example:
     * {@code
     * String types = TypeValidator.getValidTypeNames();
     * // Returns: "STRING, INTEGER, LONG, DOUBLE, FLOAT, BOOLEAN, DECIMAL, OBJECT, ARRAY, DATE, DATETIME"
     * }
     */
    public static String getValidTypeNames() {
        StringBuilder sb = new StringBuilder();
        FieldType[] types = FieldType.values();

        for (int i = 0; i < types.length; i++) {
            sb.append(types[i].name());
            if (i < types.length - 1) {
                sb.append(", ");
            }
        }

        return sb.toString();
    }

    /**
     * Check if a type is numeric (INTEGER, LONG, DOUBLE, FLOAT, DECIMAL)
     *
     * @param fieldType The FieldType to check
     * @return true if this is a numeric type
     *
     * Example:
     * {@code
     * TypeValidator.isNumeric(FieldType.INTEGER);  // true
     * TypeValidator.isNumeric(FieldType.STRING);   // false
     * }
     */
    public static boolean isNumeric(FieldType fieldType) {
        return fieldType != null && fieldType.isNumeric();
    }

    /**
     * Check if a type is a collection (ARRAY, OBJECT)
     *
     * @param fieldType The FieldType to check
     * @return true if this is a collection type
     *
     * Example:
     * {@code
     * TypeValidator.isCollection(FieldType.ARRAY);   // true
     * TypeValidator.isCollection(FieldType.OBJECT);  // true
     * TypeValidator.isCollection(FieldType.STRING);  // false
     * }
     */
    public static boolean isCollection(FieldType fieldType) {
        return fieldType != null && fieldType.isCollection();
    }

    /**
     * Check if a type is temporal (DATE, DATETIME)
     *
     * @param fieldType The FieldType to check
     * @return true if this is a date/time type
     *
     * Example:
     * {@code
     * TypeValidator.isTemporal(FieldType.DATE);      // true
     * TypeValidator.isTemporal(FieldType.DATETIME);  // true
     * TypeValidator.isTemporal(FieldType.STRING);    // false
     * }
     */
    public static boolean isTemporal(FieldType fieldType) {
        return fieldType != null && fieldType.isTemporal();
    }

    /**
     * Get the Java class for a FieldType
     *
     * @param fieldType The FieldType enum
     * @return The Java class that represents this type
     *
     * Example:
     * {@code
     * Class<?> clazz = TypeValidator.getJavaClass(FieldType.STRING);
     * // Returns: String.class
     * }
     */
    public static Class<?> getJavaClass(FieldType fieldType) {
        if (fieldType == null) {
            return Object.class;
        }
        return fieldType.getJavaClass();
    }

    /**
     * Get the example value for a FieldType
     *
     * Useful for documentation or placeholder text in forms.
     *
     * @param fieldType The FieldType enum
     * @return An example value of this type
     *
     * Example:
     * {@code
     * Object example = TypeValidator.getExampleValue(FieldType.STRING);
     * // Returns: "hello"
     * Object example = TypeValidator.getExampleValue(FieldType.INTEGER);
     * // Returns: 42
     * }
     */
    public static Object getExampleValue(FieldType fieldType) {
        if (fieldType == null) {
            return null;
        }
        return fieldType.getExampleValue();
    }
}

