package com.awal.cineq.common.validation;

import jakarta.validation.Constraint;
import jakarta.validation.Payload;
import java.lang.annotation.*;

/**
 * Validates that a value exists in the database
 * Usage: @Exists(entity = Action.class, field = "code")
 *
 * Similar to Laravel's: 'code' => 'exists:actions,code'
 */
@Target({ElementType.FIELD, ElementType.PARAMETER})
@Retention(RetentionPolicy.RUNTIME)
@Constraint(validatedBy = ExistsValidator.class)
@Documented
public @interface Exists {

    String message() default "{value} does not exist";

    /**
     * Entity class to check against
     * Example: Action.class, Module.class
     */
    Class<?> entity();

    /**
     * Field name to check in the entity
     * Default: "id"
     * Example: "code", "email", "username"
     */
    String field() default "id";


    Class<?>[] groups() default {};
    /**
     * Whether to exclude soft-deleted records
     * Default: true (only check non-deleted records)
     */
    boolean excludeDeleted() default true;

    Class<? extends Payload>[] payload() default {};
}