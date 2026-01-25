package com.awal.cineq.form.enums;

import com.awal.cineq.exception.BusinessException;
import com.awal.cineq.exception.ValidationException;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;
import com.fasterxml.jackson.databind.JsonNode;
import lombok.Getter;

/**
 * Enum for Form Actions with easy-to-remember codes
 *
 * Numeric Codes: 1, 2, 3, 4 (sequential)
 * Character Codes: C, R, U, D (first letter mnemonic)
 *
 * Used consistently across form submission workflow
 */
@Getter
public enum FormAction {
    CREATE(1, "C", "create", "Create new document"),
    READ(2, "R", "read", "View document"),
    UPDATE(3, "U", "update", "Update existing document"),
    DELETE(4, "D", "delete", "Delete document");

    private final int code;
    private final String charCode;  // Single character code (C, R, U, D)
    private final String actionName;
    private final String description;

    FormAction(int code, String charCode, String actionName, String description) {
        this.code = code;
        this.charCode = charCode;
        this.actionName = actionName;
        this.description = description;
    }

    /**
     * Get FormAction by action name (case-insensitive)
     * @param actionName The action name (create, read, update, delete)
     * @return FormAction enum or null if not found
     */
    public static FormAction fromActionName(String actionName) {
        if (actionName == null || actionName.isEmpty()) {
            return null;
        }

        for (FormAction action : FormAction.values()) {
            if (action.actionName.equalsIgnoreCase(actionName)) {
                return action;
            }
        }
        return null;
    }

    /**
     * Get FormAction by numeric code
     * @param code The integer code (1, 2, 3, 4)
     * @return FormAction enum or null if not found
     */
    public static FormAction fromCode(int code) {
        for (FormAction action : FormAction.values()) {
            if (action.code == code) {
                return action;
            }
        }
        return null;
    }

    /**
     * Get FormAction by character code (easy to remember)
     * @param charCode The character code (C, R, U, D - case insensitive)
     * @return FormAction enum or null if not found
     *
     * Example:
     *   FormAction.fromCharCode("C") -> CREATE
     *   FormAction.fromCharCode("u") -> UPDATE
     */
    public static FormAction fromCharCode(String charCode) {
        if (charCode == null || charCode.isEmpty()) {
            return null;
        }

        String upperCode = charCode.toUpperCase();
        for (FormAction action : FormAction.values()) {
            if (action.charCode.equals(upperCode)) {
                return action;
            }
        }
        return null;
    }

    /**
     * Check if this is a read-only action
     */
    public boolean isReadOnly() {
        return this == READ;
    }

    /**
     * Check if this is a write action (create, update, delete)
     */
    public boolean isWriteAction() {
        return this == CREATE || this == UPDATE || this == DELETE;
    }

    /**
     * Check if this requires document ID
     * READ, UPDATE, DELETE require ID; CREATE does not
     */
    public boolean requiresId() {
        return this == READ || this == UPDATE || this == DELETE;
    }

    /**
     * Get all available codes for this action
     * Useful for API documentation
     * @return String like "1 (C)"
     */
    public String getAllCodes() {
        return code + " (" + charCode + ")";
    }
    @JsonCreator
    public static FormAction fromJson(JsonNode value) {

        if (value == null || value.isNull()) {
            return null;
        }

        String text = value.asText();

        // try actionName: "create"
        FormAction byName = fromActionName(text);
        if (byName != null) return byName;

        // try enum name: "CREATE"
        try {
            return FormAction.valueOf(text.toUpperCase());
        } catch (Exception ignored) {}

        throw new ValidationException(
                "Invalid action. "
        );
    }

    // Optional: controls how enum is SENT back in responses
    @JsonValue
    public String toJson() {
        return actionName; // returns "create", "read", etc.
    }
}

