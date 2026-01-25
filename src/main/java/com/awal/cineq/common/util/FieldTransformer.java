package com.awal.cineq.common.util;

import lombok.extern.slf4j.Slf4j;

/**
 * Global Field Transformer Utility Class
 * Provides string transformation capabilities for all modules
 *
 * Supports multiple transformation types:
 * - UPPERCASE: Convert to UPPERCASE
 * - lowercase: Convert to lowercase
 * - camelCase: Convert to camelCase
 * - PascalCase: Convert to PascalCase
 * - snake_case: Convert to snake_case
 * - CONSTANT_CASE: Convert to CONSTANT_CASE
 * - kebab-case: Convert to kebab-case
 *
 * Usage:
 * Object transformed = FieldTransformer.transformField("hello_world", "camelCase");
 * // Returns: "helloWorld"
 *
 * @author CineQ Development Team
 * @version 1.0
 */
@Slf4j
public class FieldTransformer {

    /**
     * Transform a field value based on the transformation type
     *
     * @param value The value to transform (can be any type, will be converted to String)
     * @param transformType The type of transformation (UPPERCASE, lowercase, camelCase, etc.)
     * @return Transformed value as String, or original value if null or transformation type is invalid
     */
    public static Object transformField(Object value, String transformType) {
        if (value == null) {
            return null;
        }

        // Convert to string for transformation
        String stringValue = value.toString().trim();

        if (stringValue.isEmpty()) {
            return value;  // Return original if empty
        }

        switch (transformType.toLowerCase()) {
            case "uppercase":
                return toUpperCase(stringValue);

            case "lowercase":
                return toLowerCase(stringValue);

            case "camelcase":
                return toCamelCase(stringValue);

            case "pascalcase":
                return toPascalCase(stringValue);

            case "snake_case":
                return toSnakeCase(stringValue);

            case "constant_case":
                return toConstantCase(stringValue);

            case "kebab-case":
            case "kebab_case":
                return toKebabCase(stringValue);

            default:
                log.warn("Unknown transformation type: {}, returning original value", transformType);
                return value;
        }
    }

    /**
     * Convert string to UPPERCASE
     * Example: "hello world" → "HELLO WORLD"
     *
     * @param value The string to transform
     * @return Uppercase version of the string
     */
    private static String toUpperCase(String value) {
        return value.toUpperCase();
    }

    /**
     * Convert string to lowercase
     * Example: "Hello World" → "hello world"
     *
     * @param value The string to transform
     * @return Lowercase version of the string
     */
    private static String toLowerCase(String value) {
        return value.toLowerCase();
    }

    /**
     * Convert string to camelCase
     * First word lowercase, subsequent words capitalized, no separators
     *
     * Examples:
     * - "hello_world" → "helloWorld"
     * - "hello-world" → "helloWorld"
     * - "Hello World" → "helloWorld"
     * - "HelloWorld" → "helloWorld"
     *
     * @param value The string to transform
     * @return camelCase version of the string
     */
    private static String toCamelCase(String value) {
        // First normalize to words
        String normalized = normalizeToWords(value);
        String[] words = normalized.split("[_\\-\\s]+");

        StringBuilder result = new StringBuilder();

        for (int i = 0; i < words.length; i++) {
            String word = words[i].toLowerCase();

            if (i == 0) {
                // First word stays lowercase
                result.append(word);
            } else {
                // Capitalize first letter of subsequent words
                result.append(capitalize(word));
            }
        }

        return result.toString();
    }

    /**
     * Convert string to PascalCase
     * All words capitalized, no separators
     *
     * Examples:
     * - "hello_world" → "HelloWorld"
     * - "hello-world" → "HelloWorld"
     * - "hello world" → "HelloWorld"
     *
     * @param value The string to transform
     * @return PascalCase version of the string
     */
    private static String toPascalCase(String value) {
        String normalized = normalizeToWords(value);
        String[] words = normalized.split("[_\\-\\s]+");

        StringBuilder result = new StringBuilder();

        for (String word : words) {
            result.append(capitalize(word.toLowerCase()));
        }

        return result.toString();
    }

    /**
     * Convert string to snake_case
     * Lowercase words separated by underscores
     *
     * Examples:
     * - "helloWorld" → "hello_world"
     * - "Hello World" → "hello_world"
     * - "hello-world" → "hello_world"
     *
     * @param value The string to transform
     * @return snake_case version of the string
     */
    private static String toSnakeCase(String value) {
        // Insert underscore before uppercase letters
        String result = value.replaceAll("([a-z])([A-Z])", "$1_$2")
                .replaceAll("([A-Z]+)([A-Z][a-z])", "$1_$2");

        // Replace spaces and hyphens with underscores
        result = result.replaceAll("[\\s\\-]+", "_");

        // Convert to lowercase
        return result.toLowerCase();
    }

    /**
     * Convert string to CONSTANT_CASE
     * Uppercase words separated by underscores
     *
     * Examples:
     * - "helloWorld" → "HELLO_WORLD"
     * - "hello-world" → "HELLO_WORLD"
     * - "hello world" → "HELLO_WORLD"
     *
     * @param value The string to transform
     * @return CONSTANT_CASE version of the string
     */
    private static String toConstantCase(String value) {
        return toSnakeCase(value).toUpperCase();
    }

    /**
     * Convert string to kebab-case
     * Lowercase words separated by hyphens
     *
     * Examples:
     * - "helloWorld" → "hello-world"
     * - "hello_world" → "hello-world"
     * - "Hello World" → "hello-world"
     *
     * @param value The string to transform
     * @return kebab-case version of the string
     */
    private static String toKebabCase(String value) {
        // Insert hyphen before uppercase letters
        String result = value.replaceAll("([a-z])([A-Z])", "$1-$2")
                .replaceAll("([A-Z]+)([A-Z][a-z])", "$1-$2");

        // Replace spaces and underscores with hyphens
        result = result.replaceAll("[\\s_]+", "-");

        // Convert to lowercase
        return result.toLowerCase();
    }

    /**
     * Normalize mixed formats to word boundaries
     * Handles: camelCase, PascalCase, snake_case, kebab-case, spaces
     *
     * @param value The string to normalize
     * @return Normalized string with spaces separating words
     */
    private static String normalizeToWords(String value) {
        // Insert space before uppercase letters (for camelCase/PascalCase)
        String result = value.replaceAll("([a-z])([A-Z])", "$1 $2");

        // Replace underscores and hyphens with spaces
        result = result.replaceAll("[_\\-]+", " ");

        // Replace multiple spaces with single space
        result = result.replaceAll("\\s+", " ");

        return result;
    }

    /**
     * Capitalize first letter of string
     * Example: "hello" → "Hello"
     *
     * @param value The string to capitalize
     * @return String with first letter capitalized
     */
    private static String capitalize(String value) {
        if (value == null || value.isEmpty()) {
            return value;
        }
        return value.substring(0, 1).toUpperCase() + value.substring(1);
    }

    /**
     * Check if transformation type is valid and supported
     *
     * @param transformType The transformation type to validate
     * @return true if transformation type is supported, false otherwise
     */
    public static boolean isValidTransformType(String transformType) {
        if (transformType == null) {
            return false;
        }

        switch (transformType.toLowerCase()) {
            case "uppercase":
            case "lowercase":
            case "camelcase":
            case "pascalcase":
            case "snake_case":
            case "constant_case":
            case "kebab-case":
            case "kebab_case":
                return true;
            default:
                return false;
        }
    }

    /**
     * Get list of all supported transformation types
     * Useful for documentation and validation
     *
     * @return Array of supported transformation type names
     */
    public static String[] getSupportedTypes() {
        return new String[]{
                "UPPERCASE",
                "lowercase",
                "camelCase",
                "PascalCase",
                "snake_case",
                "CONSTANT_CASE",
                "kebab-case"
        };
    }
}

