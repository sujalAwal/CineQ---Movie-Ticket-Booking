package com.awal.cineq.exception;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;
import java.util.Map;

@ResponseStatus(HttpStatus.BAD_REQUEST)
public class ValidationException extends RuntimeException {
    
    private Map<String, String> fieldErrors;

    public ValidationException(String message) {
        super(message);
        this.fieldErrors = null;
    }

    public ValidationException(String message, Map<String, String> fieldErrors) {
        super(message);
        this.fieldErrors = fieldErrors;
    }
    
    public ValidationException(String message, Throwable cause) {
        super(message, cause);
        this.fieldErrors = null;
    }

    public Map<String, String> getFieldErrors() {
        return fieldErrors;
    }
}