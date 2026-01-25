package com.awal.cineq.form.validation;

import jakarta.validation.Constraint;
import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;
import jakarta.validation.Payload;
import com.awal.cineq.form.dto.request.DynamicFormRequest;
import com.awal.cineq.form.enums.FormAction;

import java.lang.annotation.*;

/**
 * SINGLE FILE - Contains both Annotation & Validator
 *
 * File Name: ConditionalValidation.java
 * File Path: src/main/java/com/awal/cineq/form/validation/ConditionalValidation.java
 *
 * Custom validation that enforces conditional rules based on FormAction:
 * - CREATE: id is NOT required
 * - UPDATE: id IS required
 * - DELETE: id IS required
 */

// ==========================================
// PART 1: ANNOTATION (Interface)
// ==========================================
@Target({ ElementType.TYPE })
@Retention(RetentionPolicy.RUNTIME)
@Constraint(validatedBy = ConditionalValidationValidator.class)
@Documented
public @interface ConditionalValidation {
    String message() default "Validation failed";
    Class<?>[] groups() default {};
    Class<? extends Payload>[] payload() default {};
}

// ==========================================
// PART 2: VALIDATOR (Implementation)
// ==========================================
/**
 * Validator Implementation
 *
 * Rules:
 * - CREATE: id is NOT required (creating new document)
 * - UPDATE: id IS required (must know which document to update)
 * - DELETE: id IS required (must know which document to delete)
 */
class ConditionalValidationValidator implements ConstraintValidator<ConditionalValidation, DynamicFormRequest> {

    @Override
    public void initialize(ConditionalValidation annotation) {
    }

    @Override
    public boolean isValid(DynamicFormRequest request, ConstraintValidatorContext context) {

        // If no action, let other validators handle it
        if (request.getAction() == null) {
            return true;
        }

        FormAction action = request.getAction();

        // ✅ Rule 1: CREATE - id is optional
        if (FormAction.CREATE.equals(action)) {
            return true;  // Valid - id is optional for CREATE
        }

        // ❌ Rule 2: UPDATE - id is required
        if (FormAction.UPDATE.equals(action)) {
            if (request.getId() == null || request.getId().trim().isEmpty()) {
                addConstraintViolation(context, "id is required for UPDATE action");
                return false;
            }
            return true;
        }

        // ❌ Rule 3: DELETE - id is required
        if (FormAction.DELETE.equals(action)) {
            if (request.getId() == null || request.getId().trim().isEmpty()) {
                addConstraintViolation(context, "id is required for DELETE action");
                return false;
            }
            return true;
        }

        return true;
    }

    /**
     * Helper method to add custom error message
     * Builds a constraint violation with a specific property and message
     */
    private void addConstraintViolation(ConstraintValidatorContext context, String message) {
        context.disableDefaultConstraintViolation();
        context.buildConstraintViolationWithTemplate(message)
                .addPropertyNode("id")
                .addConstraintViolation();
    }
}

