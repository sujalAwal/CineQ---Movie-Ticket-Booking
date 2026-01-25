package com.awal.cineq.common.validation;

import jakarta.validation.Constraint;
import jakarta.validation.Payload;
import java.lang.annotation.*;

/**
 * Multi-purpose database validation annotation
 * Supports both EXISTS and UNIQUE checks
 *
 * Usage Examples:
 * - @DatabaseConstraint(entity = Action.class, field = "code", type = EXISTS)
 * - @DatabaseConstraint(entity = Module.class, field = "code", type = UNIQUE)
 * - @DatabaseConstraint(entity = User.class, field = "email", type = UNIQUE, excludeDeleted = true)
 *
 * Similar to Laravel's:
 * - 'exists:actions,code' → type = EXISTS
 * - 'unique:modules,code' → type = UNIQUE
 */
@Target({ElementType.FIELD, ElementType.PARAMETER})
@Retention(RetentionPolicy.RUNTIME)
@Constraint(validatedBy = DatabaseConstraintValidator.class)
@Documented
public @interface DatabaseConstraint {

    String message() default "";

    /**
     * Entity class to check against
     * Example: Action.class, Module.class, User.class
     */
    Class<?> entity();

    /**
     * Field name to check in the entity
     * Default: "id"
     * Example: "code", "email", "username"
     */
    String field() default "id";

    /**
     * Validation type: EXISTS or UNIQUE
     */
    ValidationType type();

    /**
     * For UNIQUE validation on UPDATE operations
     * Specify the ID field value to exclude from uniqueness check
     * Use with @Validated groups for create vs update
     */
    String excludeId() default "";

    Class<?>[] groups() default {};

    Class<? extends Payload>[] payload() default {};

    /**
     * Whether to exclude soft-deleted records
     * Default: true (only check non-deleted records)
     */
    boolean excludeDeleted() default true;

    /**
     * Validation types
     */
    enum ValidationType {
        EXISTS,   // Check if value exists
        UNIQUE    // Check if value is unique
    }
}