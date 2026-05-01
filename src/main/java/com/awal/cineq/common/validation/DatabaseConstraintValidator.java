package com.awal.cineq.common.validation;

import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;

/**
 * Validator for @DatabaseConstraint annotation
 * Handles both EXISTS and UNIQUE validation types
 */
@Slf4j
public class DatabaseConstraintValidator implements ConstraintValidator<DatabaseConstraint, String> {

    @Autowired
    private MongoTemplate mongoTemplate;

    private Class<?> entityClass;
    private String fieldName;
    private DatabaseConstraint.ValidationType validationType;
    private boolean excludeDeleted;
    private String excludeId;
    private String customMessage;

    @Override
    public void initialize(DatabaseConstraint annotation) {
        this.entityClass = annotation.entity();
        this.fieldName = annotation.field();
        this.validationType = annotation.type();
        this.excludeDeleted = annotation.excludeDeleted();
        this.excludeId = annotation.excludeId();
        this.customMessage = annotation.message();
    }

    @Override
    public boolean isValid(String value, ConstraintValidatorContext context) {
        if (value == null || value.trim().isEmpty()) {
            return true; // Use @NotBlank for null/empty validation
        }

        try {
            boolean isValid;

            if (validationType == DatabaseConstraint.ValidationType.EXISTS) {
                isValid = validateExists(value, context);
            } else {
                isValid = validateUnique(value, context);
            }

            return isValid;

        } catch (Exception e) {
            log.error("Error validating database constraint: entity={}, field={}, type={}, value={}",
                    entityClass.getSimpleName(), fieldName, validationType, value, e);
            return false;
        }
    }

    /**
     * Validate that value EXISTS in database
     */
    private boolean validateExists(String value, ConstraintValidatorContext context) {
        Query query = buildQuery(value, false);
        boolean exists = mongoTemplate.exists(query, entityClass);

        if (!exists) {
            String message = customMessage.isEmpty()
                    ? entityClass.getSimpleName() + " with " + fieldName + " '" + value + "' does not exist"
                    : customMessage;

            context.disableDefaultConstraintViolation();
            context.buildConstraintViolationWithTemplate(message).addConstraintViolation();
        }

        return exists;
    }

    /**
     * Validate that value is UNIQUE in database
     */
    private boolean validateUnique(String value, ConstraintValidatorContext context) {
        Query query = buildQuery(value, true);
        boolean exists = mongoTemplate.exists(query, entityClass);

        // For UNIQUE, we want exists to be FALSE (value should NOT exist)
        boolean isUnique = !exists;

        if (!isUnique) {
            String message = customMessage.isEmpty()
                    ?"Field : "+fieldName +" must be unique, '" + value + "' already exists"
                    : customMessage;

            context.disableDefaultConstraintViolation();
            context.buildConstraintViolationWithTemplate(message).addConstraintViolation();
        }

        return isUnique;
    }

    /**
     * Build MongoDB query based on validation type
     */
    private Query buildQuery(String value, boolean isUniqueCheck) {
        Query query = new Query();

        // Add field criteria
        if ("id".equals(fieldName) || "_id".equals(fieldName)) {
            query.addCriteria(Criteria.where("_id").is(value));
        } else {
            query.addCriteria(Criteria.where(fieldName).is(value));
        }

        // Exclude soft-deleted records
        if (excludeDeleted) {
            query.addCriteria(Criteria.where("deletedAt").is(null));
        }

        // For UNIQUE validation on UPDATE: exclude current record
        if (isUniqueCheck && excludeId != null && !excludeId.isEmpty()) {
            query.addCriteria(Criteria.where("_id").ne(excludeId));
        }

        return query;
    }
}