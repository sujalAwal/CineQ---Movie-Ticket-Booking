package com.awal.cineq.common.util;

import lombok.extern.slf4j.Slf4j;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * Utility class for evaluating condition expressions.
 * Used by interceptors and validators for conditional logic.
 *
 * Supported operators:
 * - "==", "===" : equals
 * - "!=", "!==" : not equals
 * - "in" : value in list
 * - "notIn" : value not in list
 * - "isEmpty" : value is null/empty
 * - "isNotEmpty" : value is not null/empty
 * - ">", ">=", "<", "<=" : numeric comparisons
 */
@Slf4j
public final class ConditionEvaluator {

    private ConditionEvaluator() {
        // Utility class, prevent instantiation
    }

    /**
     * Evaluate a condition against form data.
     *
     * @param condition Map containing: field, operator, value
     * @param formData Map of field names to values
     * @return true if condition is met, false otherwise
     */
    public static boolean evaluate(Map<String, Object> condition, Map<String, Object> formData) {
        if (condition == null || condition.isEmpty()) {
            return true; // No condition = always true
        }

        String field = (String) condition.get("field");
        String operator = (String) condition.get("operator");
        Object expectedValue = condition.get("value");

        if (field == null || operator == null) {
            log.warn("Invalid condition: missing field or operator. Condition: {}", condition);
            return false;
        }

        Object actualValue = formData != null ? formData.get(field) : null;

        return evaluateOperator(operator, actualValue, expectedValue);
    }

    /**
     * Evaluate an operator against actual and expected values.
     */
    private static boolean evaluateOperator(String operator, Object actualValue, Object expectedValue) {
        switch (operator) {
            case "===":
            case "==":
            case "equals":
                return Objects.equals(actualValue, expectedValue);

            case "!==":
            case "!=":
            case "notEquals":
                return !Objects.equals(actualValue, expectedValue);

            case "in":
                if (expectedValue instanceof List) {
                    return ((List<?>) expectedValue).contains(actualValue);
                }
                return false;

            case "notIn":
                if (expectedValue instanceof List) {
                    return !((List<?>) expectedValue).contains(actualValue);
                }
                return true;

            case "isEmpty":
                return isEmptyValue(actualValue);

            case "isNotEmpty":
                return !isEmptyValue(actualValue);

            case ">":
                return compareNumbers(actualValue, expectedValue) > 0;

            case ">=":
                return compareNumbers(actualValue, expectedValue) >= 0;

            case "<":
                return compareNumbers(actualValue, expectedValue) < 0;

            case "<=":
                return compareNumbers(actualValue, expectedValue) <= 0;

            case "contains":
                return containsValue(actualValue, expectedValue);

            case "startsWith":
                if (actualValue instanceof String && expectedValue instanceof String) {
                    return ((String) actualValue).startsWith((String) expectedValue);
                }
                return false;

            case "endsWith":
                if (actualValue instanceof String && expectedValue instanceof String) {
                    return ((String) actualValue).endsWith((String) expectedValue);
                }
                return false;

            default:
                log.warn("Unknown operator: {}", operator);
                return false;
        }
    }

    /**
     * Check if a value is empty (null, empty string, or empty collection).
     */
    private static boolean isEmptyValue(Object value) {
        if (value == null) {
            return true;
        }
        if (value instanceof String) {
            return ((String) value).isEmpty();
        }
        if (value instanceof Collection) {
            return ((Collection<?>) value).isEmpty();
        }
        if (value instanceof Map) {
            return ((Map<?, ?>) value).isEmpty();
        }
        return false;
    }

    /**
     * Compare two numeric values.
     */
    private static int compareNumbers(Object actual, Object expected) {
        if (actual instanceof Number && expected instanceof Number) {
            double a = ((Number) actual).doubleValue();
            double e = ((Number) expected).doubleValue();
            return Double.compare(a, e);
        }
        // Try to parse strings as numbers
        try {
            double a = parseNumber(actual);
            double e = parseNumber(expected);
            return Double.compare(a, e);
        } catch (NumberFormatException ex) {
            log.warn("Cannot compare non-numeric values: {} vs {}", actual, expected);
            return 0;
        }
    }

    /**
     * Parse a value as a number.
     */
    private static double parseNumber(Object value) {
        if (value instanceof Number) {
            return ((Number) value).doubleValue();
        }
        if (value instanceof String) {
            return Double.parseDouble((String) value);
        }
        throw new NumberFormatException("Cannot parse as number: " + value);
    }

    /**
     * Check if actualValue contains expectedValue.
     */
    private static boolean containsValue(Object actualValue, Object expectedValue) {
        if (actualValue instanceof String && expectedValue instanceof String) {
            return ((String) actualValue).contains((String) expectedValue);
        }
        if (actualValue instanceof Collection) {
            return ((Collection<?>) actualValue).contains(expectedValue);
        }
        return false;
    }
}
