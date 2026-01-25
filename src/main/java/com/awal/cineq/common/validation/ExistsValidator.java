package com.awal.cineq.common.validation;

import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;

/**
 * Validator for @Exists annotation
 * Checks if a value exists in the specified entity/collection
 */
@Slf4j
public class ExistsValidator implements ConstraintValidator<Exists, String> {

    @Autowired
    private MongoTemplate mongoTemplate;

    private Class<?> entityClass;
    private String fieldName;
    private boolean excludeDeleted;

    @Override
    public void initialize(Exists annotation) {
        this.entityClass = annotation.entity();
        this.fieldName = annotation.field();
        this.excludeDeleted = annotation.excludeDeleted();
    }

    @Override
    public boolean isValid(String value, ConstraintValidatorContext context) {
        if (value == null || value.trim().isEmpty()) {
            return true; // Use @NotBlank for null/empty validation
        }

        try {
            // Build query
            Query query = new Query();

            // Add field criteria
            if ("id".equals(fieldName) || "_id".equals(fieldName)) {
                query.addCriteria(Criteria.where("_id").is(value));
            } else {
                query.addCriteria(Criteria.where(fieldName).is(value));
            }

            // Exclude soft-deleted records if needed
            if (excludeDeleted) {
                query.addCriteria(Criteria.where("deletedAt").is(null));
            }

            // Check existence
            boolean exists = mongoTemplate.exists(query, entityClass);

            if (!exists) {
                // Customize error message
                String entityName = entityClass.getSimpleName();
                context.disableDefaultConstraintViolation();
                context.buildConstraintViolationWithTemplate(
                        entityName + " with " + fieldName + " '" + value + "' does not exist"
                ).addConstraintViolation();
            }

            return exists;

        } catch (Exception e) {
            log.error("Error validating existence: entity={}, field={}, value={}",
                    entityClass.getSimpleName(), fieldName, value, e);
            return false;
        }
    }
}