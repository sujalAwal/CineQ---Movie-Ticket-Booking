package com.awal.cineq.form.validation;

import com.awal.cineq.common.enums.FieldType;
import com.awal.cineq.common.util.TypeValidator;
import com.awal.cineq.exception.ValidationException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.bson.types.ObjectId;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.*;
import java.util.regex.Pattern;

@Slf4j
@Component
@RequiredArgsConstructor
public class JavaAnnotationValidator {

    private final MongoTemplate mongoTemplate;

    /**
     * Validate form data with action-aware override logic
     * @param data Form data to validate
     * @param validationRules Validation rules from FormStep
     * @param action Action being performed (create, update, delete)
     */
    public void validate(Map<String, Object> data, Map<String, Object> validationRules, String action) {
        validate(data, validationRules, action, null, null);
    }

    /**
     * Backward compatibility: validate without action parameter
     */
    public void validate(Map<String, Object> data, Map<String, Object> validationRules) {
        validate(data, validationRules, null, null, null);
    }

    /**
     * Validate form data with @Unique support (requires formManagerId and formStepId context)
     * @param data Form data to validate
     * @param validationRules Validation rules from FormStep
     * @param action Action being performed (create, update, delete)
     * @param formManagerId FormManager ID for @Unique checks
     * @param formStepId FormStep ID for @Unique checks
     */
    public void validate(Map<String, Object> data, Map<String, Object> validationRules,
                        String action, String formManagerId, String formStepId) {
        if (validationRules == null || validationRules.isEmpty()) {
            log.debug("Validation skipped: no validation rules provided");
            return;
        }

        // CRITICAL: Normalize action to lowercase to match JSON keys (update, create, delete)
        // This ensures case-insensitive action matching (UPDATE → update, Create → create)
        String normalizedAction = action != null ? action.toLowerCase() : null;

        log.info("Starting validation: action={} (normalized to '{}'), formManagerId={}, formStepId={}, data keys={}",
                 action, normalizedAction, formManagerId, formStepId, data.keySet());

        // Use LinkedHashMap to preserve field order for better error messages
        Map<String, String> fieldErrors = new java.util.LinkedHashMap<>();

        for (Map.Entry<String, Object> entry : validationRules.entrySet()) {
            String fieldName = entry.getKey();
            Object rulesConfig = entry.getValue();

            // CHECK FOR ARRAY NESTED FIELD PATTERN: fieldName[*].nestedField
            // Example: buttons[*].title → validate 'title' field in each element of 'buttons' array
            if (fieldName.contains("[*].")) {
                if (rulesConfig instanceof Map) {
                    @SuppressWarnings("unchecked")
                    Map<String, Object> fieldConfig = (Map<String, Object>) rulesConfig;
                    
                    // Parse: "buttons[*].title" → arrayFieldName="buttons", nestedFieldName="title"
                    int bracketIndex = fieldName.indexOf("[*].");
                    String arrayFieldName = fieldName.substring(0, bracketIndex);
                    String nestedFieldName = fieldName.substring(bracketIndex + 4); // Skip "[*]."

                    log.info("ARRAY VALIDATION: fieldName='{}' → array='{}', nested='{}'",
                             fieldName, arrayFieldName, nestedFieldName);

                    // Get the array from formData
                    Object arrayValue = data.get(arrayFieldName);

                    if (arrayValue instanceof List) {
                        @SuppressWarnings("unchecked")
                        List<Object> arrayList = (List<Object>) arrayValue;

                        // Validate each element in the array
                        for (int i = 0; i < arrayList.size(); i++) {
                            Object element = arrayList.get(i);

                            if (element instanceof Map) {
                                @SuppressWarnings("unchecked")
                                Map<String, Object> elementMap = (Map<String, Object>) element;
                                Object nestedValue = elementMap.get(nestedFieldName);

                                // Create display name for error messages: "buttons[0].title"
                                String displayFieldName = arrayFieldName + "[" + i + "]." + nestedFieldName;

                                log.info("ARRAY VALIDATION: Validating {}='{}' with config={}",
                                         displayFieldName, nestedValue, fieldConfig.keySet());

                                // Collect errors for this array element field
                                List<String> elementErrors = new ArrayList<>();
                                validateFieldWithOverride(displayFieldName, nestedValue, fieldConfig, elementMap,
                                                        normalizedAction, elementErrors, formManagerId, formStepId);

                                // If there are errors, add to fieldErrors map
                                if (!elementErrors.isEmpty()) {
                                    fieldErrors.put(displayFieldName, String.join(" | ", elementErrors));
                                }
                            }
                        }
                    } else if (arrayValue != null) {
                        // Array field exists but is not a List
                        log.warn("ARRAY VALIDATION: Field '{}' is not an array, got: {}",
                                 arrayFieldName, arrayValue.getClass().getSimpleName());
                    }
                    // If arrayValue is null, skip (let @NotEmpty on the array field handle it)
                }
                continue; // Skip regular validation for array pattern fields
            }

            // REGULAR FIELD VALIDATION (non-array fields)
            // CRITICAL: Check if field exists in formData
            // For missing keys: fieldValue will be null
            // Some validators (like @Exists on 'id' field for UPDATE/DELETE) need this behavior
            boolean fieldExistsInData = data.containsKey(fieldName);
            Object fieldValue = data.get(fieldName);

            log.info("VALIDATE LOOP: fieldName='{}', exists={}, value={}, rulesConfig class={}",
                     fieldName, fieldExistsInData, fieldValue, rulesConfig != null ? rulesConfig.getClass().getSimpleName() : "null");

           if (rulesConfig instanceof Map) {
                // NEW FORMAT: Enhanced with override logic
                @SuppressWarnings("unchecked")
                Map<String, Object> fieldConfig = (Map<String, Object>) rulesConfig;
                log.info("Calling validateFieldWithOverride for field='{}' with action='{}'", fieldName, normalizedAction);

                // Collect errors for this specific field
                List<String> fieldErrorList = new ArrayList<>();
                validateFieldWithOverride(fieldName, fieldValue, fieldConfig, data, normalizedAction, fieldErrorList,
                                        formManagerId, formStepId);

                // If there are errors for this field, add to fieldErrors map
                if (!fieldErrorList.isEmpty()) {
                    // Join multiple errors for the same field with " | "
                    fieldErrors.put(fieldName, String.join(" | ", fieldErrorList));
                }
            }
        }

        log.info("Validation complete. Total field errors: {}, errors={}", fieldErrors.size(), fieldErrors);

        if (!fieldErrors.isEmpty()) {
            // Build cleaner error message: {fieldName: errorMessage} instead of default toString
            StringBuilder errorMessage = new StringBuilder("Validation failed: {");
            int index = 0;
            for (Map.Entry<String, String> entry : fieldErrors.entrySet()) {
                if (index > 0) errorMessage.append(", ");
                errorMessage.append(entry.getKey()).append(": ").append(entry.getValue());
                index++;
            }
            errorMessage.append("}");

            log.error("Validation failed with {} field errors: {}", fieldErrors.size(), errorMessage);
            throw new ValidationException(errorMessage.toString(), fieldErrors);
        }

        log.info("Validation passed successfully");
    }

    /**
     * Enhanced validation with override logic
     * Priority: TYPE VALIDATION -> actionRules > conditionalRules > baseValidation
     */
    private void validateFieldWithOverride(
            String fieldName,
            Object fieldValue,
            Map<String, Object> fieldConfig,
            Map<String, Object> formData,
            String action,
            List<String> errors,
            String formManagerId,
            String formStepId) {

        // TYPE VALIDATION: Check if 'type' key is present in field config
        Object typeConfig = fieldConfig.get("type");
        if (typeConfig != null && !typeConfig.toString().isEmpty()) {
            String typeString = typeConfig.toString().trim();

            // Validate that the field value matches the specified type
            try {
                if (fieldValue != null && !TypeValidator.validateTypeByString(fieldValue, typeString)) {
                    String errorMsg = fieldName + " must be of type " + TypeValidator.getTypeNameByString(typeString);
                    errors.add(errorMsg);
                    log.debug("Type validation failed for field '{}': expected type '{}' but got '{}'",
                             fieldName, typeString, fieldValue.getClass().getSimpleName());
                    return;  // Stop further validation if type check fails
                }
            } catch (ValidationException e) {
                // Invalid type string in configuration
                log.error("Invalid type configuration for field '{}': {}", fieldName, e.getMessage());
                errors.add(fieldName + " has invalid type configuration: " + typeString);
                return;  // Stop further validation if type is invalid
            }

            log.debug("Type validation passed for field '{}': type='{}'", fieldName, typeString);
        }

        boolean validationApplied = false;

        // PRIORITY 1: ACTION-SPECIFIC RULES (OVERRIDE BASE)
        if (action != null) {
            log.info("validateFieldWithOverride: action='{}', fieldConfig keys={}", action, fieldConfig.keySet());
            Object actionRules = fieldConfig.get("actionRules");
            log.info("validateFieldWithOverride: actionRules={}, class={}", actionRules, actionRules != null ? actionRules.getClass().getSimpleName() : "null");

            if (actionRules instanceof Map) {
                @SuppressWarnings("unchecked")
                Map<String, Object> actionRulesMap = (Map<String, Object>) actionRules;
                
                log.info("validateFieldWithOverride: actionRulesMap keys={}, containsKey('{}')={}",
                         actionRulesMap.keySet(), action, actionRulesMap.containsKey(action));

                if (actionRulesMap.containsKey(action)) {
                    Object actionSpecificRules = actionRulesMap.get(action);
                    
                    log.info("validateFieldWithOverride: Found actionSpecificRules for action='{}', rules={}",
                             action, actionSpecificRules);

                    if (actionSpecificRules instanceof List) {
                        @SuppressWarnings("unchecked")
                        List<String> rules = (List<String>) actionSpecificRules;
                        
                        log.info("validateFieldWithOverride: Calling validateField with {} rules: {}", rules.size(), rules);
                        validateField(fieldName, fieldValue, rules, errors, formData, formManagerId, formStepId, action);
                        validationApplied = true;
                        
                        log.debug("Applied actionRules[{}] for field '{}' (overriding baseValidation)", 
                                  action, fieldName);
                    }
                } else {
                    log.info("validateFieldWithOverride: Action '{}' NOT found in actionRulesMap. Available actions: {}",
                             action, actionRulesMap.keySet());
                }
            }
        }

        // PRIORITY 2: CONDITIONAL RULES (IF CONDITIONS MET, OVERRIDE BASE)
        if (!validationApplied) {
            Object conditionalRules = fieldConfig.get("conditionalRules");
            if (conditionalRules instanceof List) {
                @SuppressWarnings("unchecked")
                List<Map<String, Object>> conditions = (List<Map<String, Object>>) conditionalRules;
                
                for (Map<String, Object> conditionConfig : conditions) {
                    @SuppressWarnings("unchecked")
                    Map<String, Object> condition = (Map<String, Object>) conditionConfig.get("condition");
                    
                    if (condition != null && evaluateCondition(condition, formData)) {
                        Object validation = conditionConfig.get("validation");
                        if (validation instanceof List) {
                            @SuppressWarnings("unchecked")
                            List<String> rules = (List<String>) validation;
                            validateField(fieldName, fieldValue, rules, errors, formData, formManagerId, formStepId, action);
                            validationApplied = true;
                            
                            log.debug("Applied conditionalRules for field '{}' (overriding baseValidation)", 
                                      fieldName);
                        }
                        break; // Only apply first matching condition
                    }
                }
            }
        }

        // PRIORITY 3: BASE VALIDATION (FALLBACK ONLY)
        if (!validationApplied) {
            Object baseValidation = fieldConfig.get("baseValidation");
            if (baseValidation instanceof List) {
                @SuppressWarnings("unchecked")
                List<String> rules = (List<String>) baseValidation;
                validateField(fieldName, fieldValue, rules, errors, formData, formManagerId, formStepId, action);

                log.debug("Applied baseValidation for field '{}' (no override rules)", fieldName);
            }
        }

        // ALWAYS CHECK: DEPENDENCY RULES (not part of override logic)
        Object dependencyRules = fieldConfig.get("dependencyRules");
        if (dependencyRules instanceof Map) {
            @SuppressWarnings("unchecked")
            Map<String, Object> dependencies = (Map<String, Object>) dependencyRules;
            validateDependencyRules(fieldName, fieldValue, dependencies, formData, errors);
        }
    }

    /**
     * Evaluate condition expression
     * Delegates to shared ConditionEvaluator utility for reusability
     */
    private boolean evaluateCondition(Map<String, Object> condition, Map<String, Object> formData) {
        return com.awal.cineq.common.util.ConditionEvaluator.evaluate(condition, formData);
    }

    /**
     * Validate dependency rules
     */
    private void validateDependencyRules(
            String fieldName,
            Object fieldValue,
            Map<String, Object> dependencies,
            Map<String, Object> formData,
            List<String> errors) {
        
        // requiredIf: field required if condition is true
        Object requiredIf = dependencies.get("requiredIf");
        if (requiredIf instanceof Map) {
            @SuppressWarnings("unchecked")
            Map<String, Object> config = (Map<String, Object>) requiredIf;
            
            if (evaluateCondition(config, formData)) {
                if (fieldValue == null || 
                    (fieldValue instanceof String && ((String) fieldValue).isEmpty())) {
                    String message = (String) config.getOrDefault("message", fieldName + " is required");
                    errors.add(message);
                }
            }
        }
        
        // requiredUnless: field required unless condition is true
        Object requiredUnless = dependencies.get("requiredUnless");
        if (requiredUnless instanceof Map) {
            @SuppressWarnings("unchecked")
            Map<String, Object> config = (Map<String, Object>) requiredUnless;
            
            if (!evaluateCondition(config, formData)) {
                if (fieldValue == null || 
                    (fieldValue instanceof String && ((String) fieldValue).isEmpty())) {
                    String message = (String) config.getOrDefault("message", fieldName + " is required");
                    errors.add(message);
                }
            }
        }
    }

    private int compareNumbers(Object actual, Object expected) {
        if (actual instanceof Number && expected instanceof Number) {
            double a = ((Number) actual).doubleValue();
            double e = ((Number) expected).doubleValue();
            return Double.compare(a, e);
        }
        return 0;
    }

    private void validateField(String fieldName, Object fieldValue, List<String> rules, List<String> errors) {
        validateField(fieldName, fieldValue, rules, errors, null, null, null, null);
    }

    private void validateField(String fieldName, Object fieldValue, List<String> rules, List<String> errors, Map<String, Object> formData) {
        validateField(fieldName, fieldValue, rules, errors, formData, null, null, null);
    }

    /**
     * Main validate field method with @Unique support
     * @param fieldName Field name
     * @param fieldValue Field value
     * @param rules List of validation rules
     * @param errors Error list to accumulate
     * @param formData Complete form data (for cross-field validation)
     * @param formManagerId FormManager ID (for @Unique checks)
     * @param formStepId FormStep ID (for @Unique checks)
     * @param action Action being performed (create, update, delete)
     */
    private void validateField(String fieldName, Object fieldValue, List<String> rules, List<String> errors,
                              Map<String, Object> formData, String formManagerId, String formStepId, String action) {
        for (String rule : rules) {
            rule = rule.trim();

            // @NotBlank
            if (rule.startsWith("@NotBlank")) {
                String message = extractMessage(rule, fieldName + " cannot be blank");
                if (fieldValue == null || fieldValue.toString().trim().isEmpty()) {
                    errors.add(message);
                }
            }

            // @NotNull
            else if (rule.startsWith("@NotNull")) {
                String message = extractMessage(rule, fieldName + " cannot be null");
                if (fieldValue == null) {
                    errors.add(message);
                }
            }

            // @NotEmpty
            else if (rule.startsWith("@NotEmpty")) {
                String message = extractMessage(rule, fieldName + " cannot be empty");
                if (fieldValue == null ||
                    (fieldValue instanceof String && ((String) fieldValue).isEmpty()) ||
                    (fieldValue instanceof Collection && ((Collection<?>) fieldValue).isEmpty()) ||
                    (fieldValue instanceof Map && ((Map<?, ?>) fieldValue).isEmpty())) {
                    errors.add(message);
                }
            }

            // @Size
            else if (rule.startsWith("@Size")) {
                validateSize(fieldName, fieldValue, rule, errors);
            }

            // @Min
            else if (rule.startsWith("@Min")) {
                validateMin(fieldName, fieldValue, rule, errors);
            }

            // @Max
            else if (rule.startsWith("@Max")) {
                validateMax(fieldName, fieldValue, rule, errors);
            }

            // @Email
            else if (rule.startsWith("@Email")) {
                String message = extractMessage(rule, fieldName + " must be a valid email");
                if (fieldValue != null && !isValidEmail(fieldValue.toString())) {
                    errors.add(message);
                }
            }

            // @Pattern
            else if (rule.startsWith("@Pattern")) {
                validatePattern(fieldName, fieldValue, rule, errors);
            }

            // @DateFormat - validates date string format
            else if (rule.startsWith("@DateFormat")) {
                validateDateFormat(fieldName, fieldValue, rule, errors);
            }

            // @FutureDate - must be in the future
            else if (rule.startsWith("@FutureDate") || rule.startsWith("@Future")) {
                validateFutureDate(fieldName, fieldValue, rule, errors);
            }

            // @PastDate - must be in the past
            else if (rule.startsWith("@PastDate") || rule.startsWith("@Past")) {
                validatePastDate(fieldName, fieldValue, rule, errors);
            }

            // @DateAfter - date must be after another field
            else if (rule.startsWith("@DateAfter")) {
                validateDateAfter(fieldName, fieldValue, rule, errors, formData);
            }

            // @DateBefore - date must be before another field
            else if (rule.startsWith("@DateBefore")) {
                validateDateBefore(fieldName, fieldValue, rule, errors, formData);
            }

            // @UniqueExcludingSelf MUST come BEFORE @Unique (since "@UniqueExcludingSelf".startsWith("@Unique") is true)
            else if (rule.startsWith("@UniqueExcludingSelf")) {
                validateUniqueExcludingSelf(fieldName, fieldValue, rule, errors, formData);
            }

            // @Unique - value must be unique in target collection
            else if (rule.startsWith("@Unique")) {
                validateUnique(fieldName, fieldValue, rule, errors, action, formData);
            }

            // @Exists - value must exist in specified collection
            else if (rule.startsWith("@Exists")) {
                validateExists(fieldName, fieldValue, rule, errors, formData);
            }

            // @MustMatchExisting - value must match existing document value (immutable field check)
            else if (rule.startsWith("@MustMatchExisting")) {
                validateMustMatchExisting(fieldName, fieldValue, rule, errors, formData);
            }

            //@DatabaseConstraint handled by their respective validators
            else if (rule.startsWith("@DatabaseConstraint")) {
              
            }

            else {
                log.warn("Unknown validation rule: {}", rule);
            }
        }
    }

    private void validateSize(String fieldName, Object fieldValue, String rule, List<String> errors) {
        Integer min = extractIntParam(rule, "min");
        Integer max = extractIntParam(rule, "max");
        String message = extractMessage(rule, fieldName + " size is invalid");

        if (fieldValue == null) return;

        int size = 0;
        if (fieldValue instanceof String) {
            size = ((String) fieldValue).length();
        } else if (fieldValue instanceof Collection) {
            size = ((Collection<?>) fieldValue).size();
        } else if (fieldValue instanceof Map) {
            size = ((Map<?, ?>) fieldValue).size();
        }

        if (min != null && size < min) {
            errors.add(message.replace("{min}", String.valueOf(min)));
        }
        if (max != null && size > max) {
            errors.add(message.replace("{max}", String.valueOf(max)));
        }
    }

    private void validateMin(String fieldName, Object fieldValue, String rule, List<String> errors) {
        Integer minValue = extractIntParam(rule, "value");
        String message = extractMessage(rule, fieldName + " must be at least " + minValue);

        if (fieldValue == null) return;

        try {
            double value = Double.parseDouble(fieldValue.toString());
            if (minValue != null && value < minValue) {
                errors.add(message);
            }
        } catch (NumberFormatException e) {
            errors.add(fieldName + " must be a number");
        }
    }

    private void validateMax(String fieldName, Object fieldValue, String rule, List<String> errors) {
        Integer maxValue = extractIntParam(rule, "value");
        String message = extractMessage(rule, fieldName + " must be at most " + maxValue);

        if (fieldValue == null) return;

        try {
            double value = Double.parseDouble(fieldValue.toString());
            if (maxValue != null && value > maxValue) {
                errors.add(message);
            }
        } catch (NumberFormatException e) {
            errors.add(fieldName + " must be a number");
        }
    }

    private void validatePattern(String fieldName, Object fieldValue, String rule, List<String> errors) {
        String regexp = extractStringParam(rule, "regexp");
        String message = extractMessage(rule, fieldName + " format is invalid");

        if (fieldValue == null || regexp == null) return;

        if (!Pattern.matches(regexp, fieldValue.toString())) {
            errors.add(message);
        }
    }

    private boolean isValidEmail(String email) {
        String emailRegex = "^[A-Za-z0-9+_.-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}$";
        return Pattern.matches(emailRegex, email);
    }

    private String extractMessage(String rule, String defaultMessage) {
        int messageStart = rule.indexOf("message=");
        if (messageStart == -1) return defaultMessage;

        int quoteStart = rule.indexOf("'", messageStart);
        int quoteEnd = rule.indexOf("'", quoteStart + 1);
        if (quoteStart == -1 || quoteEnd == -1) {
            quoteStart = rule.indexOf("\"", messageStart);
            quoteEnd = rule.indexOf("\"", quoteStart + 1);
        }

        if (quoteStart != -1 && quoteEnd != -1) {
            return rule.substring(quoteStart + 1, quoteEnd);
        }

        return defaultMessage;
    }

    private Integer extractIntParam(String rule, String paramName) {
        int paramStart = rule.indexOf(paramName + "=");
        if (paramStart == -1) return null;

        int valueStart = paramStart + paramName.length() + 1;
        int valueEnd = rule.indexOf(",", valueStart);
        if (valueEnd == -1) valueEnd = rule.indexOf(")", valueStart);
        if (valueEnd == -1) return null;

        try {
            return Integer.parseInt(rule.substring(valueStart, valueEnd).trim());
        } catch (NumberFormatException e) {
            return null;
        }
    }

    private String extractStringParam(String rule, String paramName) {
        int paramStart = rule.indexOf(paramName + "=");
        if (paramStart == -1) return null;

        // Try double quotes first
        int quoteStart = rule.indexOf("\"", paramStart);
        int quoteEnd = rule.indexOf("\"", quoteStart + 1);

        // If no double quotes found, try single quotes
        if (quoteStart == -1 || quoteEnd == -1) {
            quoteStart = rule.indexOf("'", paramStart);
            quoteEnd = rule.indexOf("'", quoteStart + 1);
        }

        if (quoteStart == -1 || quoteEnd == -1) return null;

        return rule.substring(quoteStart + 1, quoteEnd);
    }

    /**
     * Validate date format
     * @DateFormat(format="yyyy-MM-dd", message="Invalid date format")
     */
    private void validateDateFormat(String fieldName, Object fieldValue, String rule, List<String> errors) {
        if (fieldValue == null) return;

        String format = extractStringParam(rule, "format");
        if (format == null) format = "yyyy-MM-dd"; // Default format

        String message = extractMessage(rule, fieldName + " must be in format " + format);

        try {
            DateTimeFormatter formatter = DateTimeFormatter.ofPattern(format);
            String dateStr = fieldValue.toString();
            
            // Try to parse as LocalDate or LocalDateTime based on format
            if (format.contains("HH") || format.contains("hh") || format.contains("'T'")) {
                LocalDateTime.parse(dateStr, formatter);
            } else {
                LocalDate.parse(dateStr, formatter);
            }
        } catch (DateTimeParseException | IllegalArgumentException e) {
            errors.add(message);
        }
    }

    /**
     * Validate future date
     * @FutureDate or @Future
     */
    private void validateFutureDate(String fieldName, Object fieldValue, String rule, List<String> errors) {
        if (fieldValue == null) return;

        String message = extractMessage(rule, fieldName + " must be a future date");
        
        try {
            LocalDateTime dateTime = parseDateTime(fieldValue.toString());
            if (!dateTime.isAfter(LocalDateTime.now())) {
                errors.add(message);
            }
        } catch (DateTimeParseException e) {
            errors.add(fieldName + " is not a valid date");
        }
    }

    /**
     * Validate past date
     * @PastDate or @Past
     */
    private void validatePastDate(String fieldName, Object fieldValue, String rule, List<String> errors) {
        if (fieldValue == null) return;

        String message = extractMessage(rule, fieldName + " must be a past date");
        
        try {
            LocalDateTime dateTime = parseDateTime(fieldValue.toString());
            if (!dateTime.isBefore(LocalDateTime.now())) {
                errors.add(message);
            }
        } catch (DateTimeParseException e) {
            errors.add(fieldName + " is not a valid date");
        }
    }

    /**
     * Validate date is after another field
     * @DateAfter(field="startDate", message="End date must be after start date")
     */
    private void validateDateAfter(String fieldName, Object fieldValue, String rule, List<String> errors, Map<String, Object> formData) {
        if (fieldValue == null) return;

        String compareFieldName = extractStringParam(rule, "field");
        if (compareFieldName == null) {
            log.warn("@DateAfter missing 'field' parameter");
            return;
        }

        String message = extractMessage(rule, fieldName + " must be after " + compareFieldName);
        
        try {
            LocalDateTime currentDate = parseDateTime(fieldValue.toString());
            
            // If formData available, compare with other field
            if (formData != null && formData.containsKey(compareFieldName)) {
                Object compareValue = formData.get(compareFieldName);
                if (compareValue != null) {
                    LocalDateTime compareDate = parseDateTime(compareValue.toString());
                    if (!currentDate.isAfter(compareDate)) {
                        errors.add(message);
                    }
                }
            }
        } catch (DateTimeParseException e) {
            errors.add(fieldName + " is not a valid date");
        }
    }

    /**
     * Validate date is before another field
     * @DateBefore(field="endDate", message="Start date must be before end date")
     */
    private void validateDateBefore(String fieldName, Object fieldValue, String rule, List<String> errors, Map<String, Object> formData) {
        if (fieldValue == null) return;

        String compareFieldName = extractStringParam(rule, "field");
        if (compareFieldName == null) {
            log.warn("@DateBefore missing 'field' parameter");
            return;
        }

        String message = extractMessage(rule, fieldName + " must be before " + compareFieldName);
        
        try {
            LocalDateTime currentDate = parseDateTime(fieldValue.toString());
            
            // If formData available, compare with other field
            if (formData != null && formData.containsKey(compareFieldName)) {
                Object compareValue = formData.get(compareFieldName);
                if (compareValue != null) {
                    LocalDateTime compareDate = parseDateTime(compareValue.toString());
                    if (!currentDate.isBefore(compareDate)) {
                        errors.add(message);
                    }
                }
            }
        } catch (DateTimeParseException e) {
            errors.add(fieldName + " is not a valid date");
        }
    }

    /**
     * Parse date string to LocalDateTime
     * Supports multiple formats: ISO-8601, yyyy-MM-dd, yyyy-MM-dd HH:mm:ss
     */
    private LocalDateTime parseDateTime(String dateStr) throws DateTimeParseException {
        if (dateStr == null || dateStr.trim().isEmpty()) {
            throw new DateTimeParseException("Empty date string", dateStr, 0);
        }

        // Try common formats
        List<DateTimeFormatter> formatters = Arrays.asList(
            DateTimeFormatter.ISO_DATE_TIME,                    // 2025-12-07T10:00:00Z
            DateTimeFormatter.ISO_LOCAL_DATE_TIME,              // 2025-12-07T10:00:00
            DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"), // 2025-12-07 10:00:00
            DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'"), // ISO with millis
            DateTimeFormatter.ofPattern("yyyy-MM-dd")           // 2025-12-07 (date only)
        );

        for (DateTimeFormatter formatter : formatters) {
            try {
                // Try parsing as LocalDateTime first
                return LocalDateTime.parse(dateStr, formatter);
            } catch (DateTimeParseException e) {
                // Try parsing as LocalDate and convert to LocalDateTime
                try {
                    LocalDate date = LocalDate.parse(dateStr, formatter);
                    return date.atStartOfDay();
                } catch (DateTimeParseException ignored) {
                    // Continue to next formatter
                }
            }
        }

        throw new DateTimeParseException("Unparseable date: " + dateStr, dateStr, 0);
    }

    /**
     * Compare two dates
     * Returns: negative if date1 < date2, zero if equal, positive if date1 > date2
     */
    private int compareDates(String date1Str, String date2Str) throws DateTimeParseException {
        LocalDateTime date1 = parseDateTime(date1Str);
        LocalDateTime date2 = parseDateTime(date2Str);
        return date1.compareTo(date2);
    }

    /**
     * Validate @Unique constraint
     * Queries the target collection directly using mongoTemplate.exists() for efficiency.
     *
     * Format: @Unique(collection='crew_roles', field='name', message='Already exists', exclude='id')
     *
     * For CREATE: checks if any non-deleted document has the same field value
     * For UPDATE: same check but excludes the current document (via 'exclude' param, default 'id')
     *
     * @param fieldName Field name
     * @param fieldValue Field value to check
     * @param rule Rule string (e.g., "@Unique(collection='crew_roles', field='name', message='...')")
     * @param errors Error list
     * @param action Action being performed (create, update, delete)
     * @param formData Complete form data (needed to get document ID for UPDATE exclusion)
     */
    private void validateUnique(String fieldName, Object fieldValue, String rule, List<String> errors,
                               String action, Map<String, Object> formData) {

        // Skip if value is null or empty
        if (fieldValue == null || fieldValue.toString().trim().isEmpty()) {
            log.debug("@Unique validation skipped for field '{}': value is null/empty", fieldName);
            return;
        }

        // For non-write actions (delete, etc.), skip uniqueness validation
        if (action != null && !"create".equalsIgnoreCase(action) && !"update".equalsIgnoreCase(action)) {
            log.debug("@Unique validation skipped for field '{}': action '{}' does not require uniqueness check", fieldName, action);
            return;
        }

        String collection = extractStringParam(rule, "collection");
        String field = extractStringParam(rule, "field");
        String message = extractMessage(rule, fieldName + " already exists");

        if (collection == null || field == null) {
            log.error("@Unique validation failed: missing 'collection' or 'field' parameter in rule: {}", rule);
            errors.add("Invalid @Unique configuration for field: " + fieldName);
            return;
        }

        try {
            Query query = new Query();
            query.addCriteria(Criteria.where(field).is(fieldValue));
            query.addCriteria(Criteria.where("deletedAt").is(null));

            // For UPDATE: exclude the current document
            if ("update".equalsIgnoreCase(action)) {
                String excludeKey = extractExcludeKey(rule); // default: "id"

                Object currentId = formData != null ? formData.get(excludeKey) : null;
                // Fallback: if excludeKey is "id", also try "_id"
                if (currentId == null && "id".equals(excludeKey) && formData != null) {
                    currentId = formData.get("_id");
                }

                if (currentId == null) {
                    log.error("@Unique validation failed: UPDATE requires '{}' in formData for field '{}'", excludeKey, fieldName);
                    errors.add("Unable to validate uniqueness: missing '" + excludeKey + "' in form data for UPDATE");
                    return;
                }

                try {
                    ObjectId objectId = new ObjectId(currentId.toString());
                    query.addCriteria(Criteria.where("_id").ne(objectId));
                } catch (IllegalArgumentException e) {
                    query.addCriteria(Criteria.where("_id").ne(currentId));
                }
            }

            boolean exists = mongoTemplate.exists(query, collection);

            if (exists) {
                log.debug("@Unique validation failed for field '{}': value '{}' already exists in collection '{}' (action={})",
                         fieldName, fieldValue, collection, action);
                errors.add(message);
            } else {
                log.debug("@Unique validation passed for field '{}': value '{}' is unique in collection '{}'",
                         fieldName, fieldValue, collection);
            }

        } catch (Exception e) {
            log.error("Error during @Unique validation for field '{}': {}", fieldName, e.getMessage(), e);
            errors.add("Unable to validate uniqueness: " + e.getMessage());
        }
    }

    /**
     * Extract exclude key from @Unique rule
     * Format: @Unique(message='...', exclude='fieldName')
     * Default: 'id'
     */
    private String extractExcludeKey(String rule) {
        // Look for exclude='...' or exclude="..."
        int excludeStart = rule.indexOf("exclude=");
        if (excludeStart == -1) {
            return "id";  // Default exclude key
        }

        int quoteStart = rule.indexOf("'", excludeStart);
        int quoteEnd = rule.indexOf("'", quoteStart + 1);

        if (quoteStart == -1 || quoteEnd == -1) {
            quoteStart = rule.indexOf("\"", excludeStart);
            quoteEnd = rule.indexOf("\"", quoteStart + 1);
        }

        if (quoteStart != -1 && quoteEnd != -1) {
            return rule.substring(quoteStart + 1, quoteEnd);
        }

        return "id";  // Default if parsing fails
    }

    /**
     * Validate @Exists constraint
     * Checks if the value exists in the specified MongoDB collection
     *
     * Format: @Exists(collection='roles', field='_id', message='Role not found with this ID')
     *
     * IMPORTANT: This validator handles NULL/missing values gracefully:
     * - If fieldValue is null AND @NotBlank/@NotNull is present in the same field rules, skip @Exists check
     * - This prevents "document not found" errors when the real issue is "field is required"
     *
     * @param fieldName Field name
     * @param fieldValue Field value to check (can be null)
     * @param rule Rule string (e.g., "@Exists(collection='roles', field='_id', message='...')")
     * @param errors Error list
     * @param formData Complete form data (used to get 'id' field for lookups if needed)
     */
    private void validateExists(String fieldName, Object fieldValue, String rule, List<String> errors, Map<String, Object> formData) {
        // CRITICAL: Skip if value is null or empty
        // Let @NotBlank/@NotNull handle missing value validation
        if (fieldValue == null || fieldValue.toString().trim().isEmpty()) {
            log.debug("@Exists validation skipped for field '{}': value is null/empty (use @NotBlank/@NotNull for required check)", fieldName);
            return;
        }

        String collection = extractStringParam(rule, "collection");
        String field = extractStringParam(rule, "field");
        String message = extractMessage(rule, fieldName + " does not exist in " + collection);

        if (collection == null || field == null) {
            log.error("@Exists validation failed: missing collection or field parameter in rule: {}", rule);
            errors.add("Invalid @Exists configuration for field: " + fieldName);
            return;
        }

        try {
            // Build MongoDB query
            Query query = new Query();

            // Handle ObjectId fields (_id)
            if ("_id".equals(field) || "id".equals(field)) {
                try {
                    ObjectId objectId = new ObjectId(fieldValue.toString());
                    query.addCriteria(Criteria.where("_id").is(objectId));
                } catch (IllegalArgumentException e) {
                    log.debug("@Exists validation failed for field '{}': invalid ObjectId format: {}", fieldName, fieldValue);
                    errors.add(message);
                    return;
                }
            } else {
                query.addCriteria(Criteria.where(field).is(fieldValue));
            }

            // Always exclude soft-deleted documents
            query.addCriteria(Criteria.where("deletedAt").is(null));

            // Check if document exists
            boolean exists = mongoTemplate.exists(query, collection);

            if (!exists) {
                log.debug("@Exists validation failed for field '{}': value '{}' not found in collection '{}' field '{}'",
                         fieldName, fieldValue, collection, field);
                errors.add(message);
            } else {
                log.debug("@Exists validation passed for field '{}': value '{}' found in collection '{}'",
                         fieldName, fieldValue, collection);
            }

        } catch (Exception e) {
            log.error("Error during @Exists validation for field '{}': {}", fieldName, e.getMessage(), e);
            errors.add("Unable to validate existence: " + e.getMessage());
        }
    }

    /**
     * Validate @UniqueExcludingSelf constraint
     * Checks if value is unique in the collection EXCLUDING the current document being updated
     *
     * Format: @UniqueExcludingSelf(collection='roles', field='name', idField='_id', message='...')
     *
     * USE CASE: UPDATE operations where you want to ensure uniqueness but allow keeping the same value
     *
     * Example:
     * - Updating role with id=123, name="ADMIN"
     * - User keeps name="ADMIN" → ALLOWED (same document)
     * - User changes name="SUPER_ADMIN" → CHECK if any OTHER role has "SUPER_ADMIN"
     *
     * @param fieldName Field name
     * @param fieldValue Field value to check
     * @param rule Rule string
     * @param errors Error list
     * @param formData Complete form data (must contain idField value)
     */
    private void validateUniqueExcludingSelf(String fieldName, Object fieldValue, String rule, List<String> errors, Map<String, Object> formData) {
        log.debug("@UniqueExcludingSelf ENTRY: fieldName='{}', fieldValue='{}', rule='{}'", fieldName, fieldValue, rule);

        // Skip if value is null or empty
        if (fieldValue == null || fieldValue.toString().trim().isEmpty()) {
            log.debug("@UniqueExcludingSelf validation skipped for field '{}': value is null/empty", fieldName);
            return;
        }

        String collection = extractStringParam(rule, "collection");
        String field = extractStringParam(rule, "field");
        String idField = extractStringParam(rule, "idField");
        String message = extractMessage(rule, fieldName + " already exists");

        if (collection == null || field == null) {
            log.error("@UniqueExcludingSelf validation failed: missing collection or field parameter in rule: {}", rule);
            errors.add("Invalid @UniqueExcludingSelf configuration for field: " + fieldName);
            return;
        }

        // Default idField to '_id' if not specified
        if (idField == null) {
            idField = "_id";
        }

        // Get current document ID from formData
        // Try the specified idField first, then fallback to 'id' if '_id' not found
        Object currentId = formData != null ? formData.get(idField) : null;
        if (currentId == null && "_id".equals(idField) && formData != null) {
            currentId = formData.get("id");  // Fallback to 'id' if '_id' not found
        }

        if (currentId == null) {
            log.error("@UniqueExcludingSelf validation failed: missing '{}' in formData for field '{}' (also checked 'id' field)", idField, fieldName);
            errors.add("Unable to validate uniqueness: missing document ID");
            return;
        }

        try {
            // Build MongoDB query: find documents with same field value
            Query query = new Query();
            query.addCriteria(Criteria.where(field).is(fieldValue));

            // Exclude soft-deleted documents
            query.addCriteria(Criteria.where("deletedAt").is(null));

            // Exclude current document by ID
            try {
                ObjectId objectId = new ObjectId(currentId.toString());
                query.addCriteria(Criteria.where("_id").ne(objectId));
            } catch (IllegalArgumentException e) {
                // If not ObjectId, use as-is
                query.addCriteria(Criteria.where("_id").ne(currentId));
            }

            // Check if any OTHER document has this value
            boolean existsInOtherDoc = mongoTemplate.exists(query, collection);

            if (existsInOtherDoc) {
                log.debug("@UniqueExcludingSelf validation failed for field '{}': value '{}' exists in another document in collection '{}'",
                         fieldName, fieldValue, collection);
                errors.add(message);
            } else {
                log.debug("@UniqueExcludingSelf validation passed for field '{}': value '{}' is unique (excluding current document)",
                         fieldName, fieldValue);
            }

        } catch (Exception e) {
            log.error("Error during @UniqueExcludingSelf validation for field '{}': {}", fieldName, e.getMessage(), e);
            errors.add("Unable to validate uniqueness: " + e.getMessage());
        }
    }

    /**
     * Validate @MustMatchExisting constraint
     * Ensures the submitted value EXACTLY MATCHES the existing value in the database
     *
     * Format: @MustMatchExisting(collection='roles', field='name', idField='_id', message='...')
     *
     * USE CASE: IMMUTABLE FIELDS that cannot be changed after creation
     *
     * Example:
     * - Role with id=123, name="ADMIN", slug="admin"
     * - User tries to update with name="SUPER_ADMIN" → REJECTED (name is immutable)
     * - User keeps name="ADMIN" → ALLOWED (matches existing)
     *
     * WHY THIS IS IMPORTANT:
     * - Protects referential integrity (other collections reference by name/slug)
     * - Prevents breaking existing permissions/workflows that depend on immutable identifiers
     *
     * @param fieldName Field name
     * @param fieldValue Field value to check
     * @param rule Rule string
     * @param errors Error list
     * @param formData Complete form data (must contain idField value)
     */
    private void validateMustMatchExisting(String fieldName, Object fieldValue, String rule, List<String> errors, Map<String, Object> formData) {
        // Skip if value is null or empty
        if (fieldValue == null || fieldValue.toString().trim().isEmpty()) {
            log.debug("@MustMatchExisting validation skipped for field '{}': value is null/empty", fieldName);
            return;
        }

        String collection = extractStringParam(rule, "collection");
        String field = extractStringParam(rule, "field");
        String idField = extractStringParam(rule, "idField");
        String message = extractMessage(rule, fieldName + " cannot be changed");

        if (collection == null || field == null) {
            log.error("@MustMatchExisting validation failed: missing collection or field parameter in rule: {}", rule);
            errors.add("Invalid @MustMatchExisting configuration for field: " + fieldName);
            return;
        }

        // Default idField to '_id' if not specified
        if (idField == null) {
            idField = "_id";
        }

        // Get current document ID from formData
        // Try the specified idField first, then fallback to 'id' if '_id' not found
        Object currentId = formData != null ? formData.get(idField) : null;
        if (currentId == null && "_id".equals(idField) && formData != null) {
            currentId = formData.get("id");  // Fallback to 'id' if '_id' not found
        }

        if (currentId == null) {
            log.error("@MustMatchExisting validation failed: missing '{}' in formData for field '{}' (also checked 'id' field)", idField, fieldName);
            errors.add("Unable to validate field immutability: missing document ID");
            return;
        }

        try {
            // Fetch existing document from MongoDB
            Query query = new Query();

            try {
                ObjectId objectId = new ObjectId(currentId.toString());
                query.addCriteria(Criteria.where("_id").is(objectId));
            } catch (IllegalArgumentException e) {
                query.addCriteria(Criteria.where("_id").is(currentId));
            }

            // Exclude soft-deleted documents
            query.addCriteria(Criteria.where("deletedAt").is(null));

            // Fetch the document
            Map existingDoc = mongoTemplate.findOne(query, Map.class, collection);

            if (existingDoc == null) {
                log.error("@MustMatchExisting validation failed: document not found with id '{}' in collection '{}'",
                         currentId, collection);
                errors.add("Document not found for validation");
                return;
            }

            // Get existing field value
            Object existingValue = existingDoc.get(field);

            // Compare submitted value with existing value
            if (existingValue == null) {
                // If existing value is null, submitted value must also be null/empty
                if (fieldValue != null && !fieldValue.toString().trim().isEmpty()) {
                    log.debug("@MustMatchExisting validation failed for field '{}': existing value is null but submitted value is '{}'",
                             fieldName, fieldValue);
                    errors.add(message);
                }
            } else {
                // Compare values (case-sensitive)
                if (!existingValue.toString().equals(fieldValue.toString())) {
                    log.debug("@MustMatchExisting validation failed for field '{}': submitted value '{}' does not match existing value '{}'",
                             fieldName, fieldValue, existingValue);
                    errors.add(message);
                } else {
                    log.debug("@MustMatchExisting validation passed for field '{}': value '{}' matches existing value",
                             fieldName, fieldValue);
                }
            }

        } catch (Exception e) {
            log.error("Error during @MustMatchExisting validation for field '{}': {}", fieldName, e.getMessage(), e);
            errors.add("Unable to validate field immutability: " + e.getMessage());
        }
    }

    /**
     * OPTIMIZED: Validate, map, and transform fields in a SINGLE LOOP
     *
     * This method combines validation, field mapping, and transformation
     * into one efficient pass through validationRules.
     *
     * WHITELIST FILTERING via collectionField:
     * - If "collectionField" is present: Include field in result (use mapped name)
     * - If "collectionField" is absent: EXCLUDE field entirely (security whitelist)
     *
     * TRANSFORMATION (Optional):
     * - If "fieldTransformer" is present: Apply AFTER all validation passes
     * - If "fieldTransformer" is absent: Use original value
     *
     * PERFORMANCE: Single loop instead of separate validation + mapping loops
     *
     * @param formData Form data submitted by user
     * @param validationRules Validation rules from FormStep (includes collectionField + fieldTransformer)
     * @param action Action being performed (create, update, delete)
     * @param formManagerId FormManager ID for @Unique checks
     * @param formStepId FormStep ID for @Unique checks
     * @return Map with whitelisted, validated, mapped, and transformed fields
     * @throws ValidationException If validation fails
     */
    public Map<String, Object> validateAndMapFields(
            Map<String, Object> formData,
            Map<String, Object> validationRules,
            String action,
            String formManagerId,
            String formStepId) {

        log.info("validateAndMapFields STARTED: action={}, totalFields={}",
                action, validationRules != null ? validationRules.size() : 0);

        Map<String, Object> mappedData = new java.util.HashMap<>();
        List<String> errors = new ArrayList<>();

        if (validationRules == null || validationRules.isEmpty()) {
            log.debug("validateAndMapFields: No validation rules defined");
            return mappedData;
        }

        // SINGLE LOOP: Validate, map, and transform each field
        for (Map.Entry<String, Object> entry : validationRules.entrySet()) {
            String formFieldName = entry.getKey();
            Object rulesConfigObj = entry.getValue();

            if (!(rulesConfigObj instanceof Map)) {
                continue;
            }

            @SuppressWarnings("unchecked")
            Map<String, Object> fieldConfig = (Map<String, Object>) rulesConfigObj;

            // ARRAY NESTED FIELD VALIDATION: fieldName[*].nestedField
            // Example: buttons[*].title → validate 'title' field in each element of 'buttons' array
            // NOTE: Array nested fields are VALIDATION-ONLY, no mapping (parent array handles mapping)
            if (formFieldName.contains("[*].")) {
                // Parse: "buttons[*].title" → arrayFieldName="buttons", nestedFieldName="title"
                int bracketIndex = formFieldName.indexOf("[*].");
                String arrayFieldName = formFieldName.substring(0, bracketIndex);
                String nestedFieldName = formFieldName.substring(bracketIndex + 4); // Skip "[*]."

                log.info("validateAndMapFields ARRAY: field='{}' → array='{}', nested='{}'",
                         formFieldName, arrayFieldName, nestedFieldName);

                // Get the array from formData
                Object arrayValue = formData.get(arrayFieldName);

                if (arrayValue instanceof List) {
                    @SuppressWarnings("unchecked")
                    List<Object> arrayList = (List<Object>) arrayValue;

                    // Validate each element in the array
                    for (int i = 0; i < arrayList.size(); i++) {
                        Object element = arrayList.get(i);

                        if (element instanceof Map) {
                            @SuppressWarnings("unchecked")
                            Map<String, Object> elementMap = (Map<String, Object>) element;
                            Object nestedValue = elementMap.get(nestedFieldName);

                            // Create display name for error messages: "buttons[0].title"
                            String displayFieldName = arrayFieldName + "[" + i + "]." + nestedFieldName;

                            // Collect errors for this array element field
                            List<String> elementErrors = new ArrayList<>();
                            validateFieldWithOverride(displayFieldName, nestedValue, fieldConfig, elementMap,
                                                    action, elementErrors, formManagerId, formStepId);

                            // If there are errors, add to errors list
                            if (!elementErrors.isEmpty()) {
                                errors.addAll(elementErrors);
                            }
                        }
                    }
                }
                continue; // Skip mapping for array pattern fields (parent array handles it)
            }

            // REGULAR FIELD: Check if collectionField exists (WHITELIST - required)
            if (!fieldConfig.containsKey("collectionField")) {
                log.debug("validateAndMapFields: Field '{}' has no collectionField - EXCLUDING from result",
                        formFieldName);
                continue;  // Skip this field entirely (don't include in result)
            }

            String collectionFieldName = (String) fieldConfig.get("collectionField");
            log.debug("validateAndMapFields: Processing field '{}' (maps to '{}')",
                    formFieldName, collectionFieldName);

            // STEP 2: Get field value from submitted data
            Object fieldValue = formData.get(formFieldName);

            // STEP 3: Validate field value (all validations)
            List<String> fieldErrors = new ArrayList<>();

            if (fieldConfig instanceof Map) {
                validateFieldWithOverride(
                    formFieldName,
                    fieldValue,
                    fieldConfig,
                    formData,
                    action,
                    fieldErrors,
                    formManagerId,
                    formStepId
                );
            }

            // If validation failed, add errors and skip this field
            if (!fieldErrors.isEmpty()) {
                log.debug("validateAndMapFields: Validation failed for field '{}': {}",
                        formFieldName, fieldErrors);
                errors.addAll(fieldErrors);
                continue;  // Don't add field to result
            }

            // STEP 4: Validation passed - now apply transformation (LAST STEP)
            Object transformedValue = fieldValue;

            // Check if fieldTransformer exists (OPTIONAL)
            if (fieldConfig.containsKey("fieldTransformer") &&
                fieldConfig.get("fieldTransformer") != null) {

                String transformer = fieldConfig.get("fieldTransformer").toString().trim();

                if (!transformer.isEmpty()) {
                    try {
                        transformedValue = com.awal.cineq.common.util.FieldTransformer.transformField(
                            fieldValue,
                            transformer
                        );
                        log.debug("validateAndMapFields: Applied transformer '{}' to field '{}': {} → {}",
                                transformer, formFieldName, fieldValue, transformedValue);
                    } catch (Exception e) {
                        log.warn("validateAndMapFields: Failed to apply transformer '{}' to field '{}': {}",
                                transformer, formFieldName, e.getMessage());
                        transformedValue = fieldValue;  // Use original value if transformation fails
                    }
                }
            }

            // STEP 5: Add to result with mapped field name
            mappedData.put(collectionFieldName, transformedValue);
            log.debug("validateAndMapFields: Added '{}': {} (transformed from '{}')",
                    collectionFieldName, transformedValue, formFieldName);
        }

        // Throw if any validation errors occurred
        if (!errors.isEmpty()) {
            log.error("validateAndMapFields: Validation failed with {} errors", errors.size());
            throw new ValidationException("Validation failed: " + String.join(", ", errors));
        }

        log.info("validateAndMapFields END: Processed {} whitelisted fields", mappedData.size());
        return mappedData;
    }

}





