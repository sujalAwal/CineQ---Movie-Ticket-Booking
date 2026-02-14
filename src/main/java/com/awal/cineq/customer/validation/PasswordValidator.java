package com.awal.cineq.customer.validation;

import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;

/**
 * Validator implementation for @ValidPassword annotation
 * Uses PasswordStrengthChecker to enforce password complexity rules
 */
public class PasswordValidator implements ConstraintValidator<ValidPassword, String> {
    
    @Override
    public void initialize(ValidPassword constraintAnnotation) {
        // No initialization needed
    }
    
    @Override
    public boolean isValid(String password, ConstraintValidatorContext context) {
        if (password == null) {
            return false;
        }
        
        PasswordStrengthChecker.PasswordValidationResult result = 
            PasswordStrengthChecker.validate(password);
        
        if (!result.isValid()) {
            // Disable default message
            context.disableDefaultConstraintViolation();
            
            // Add custom message with specific errors
            context.buildConstraintViolationWithTemplate(result.getErrorMessage())
                   .addConstraintViolation();
            
            return false;
        }
        
        return true;
    }
}
