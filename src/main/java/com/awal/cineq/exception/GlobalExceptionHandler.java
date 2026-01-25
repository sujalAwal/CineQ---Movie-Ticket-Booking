package com.awal.cineq.exception;

import com.awal.cineq.dto.ApiResponse;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.env.Environment;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.util.HashMap;
import java.util.Map;

@RestControllerAdvice
public class GlobalExceptionHandler {

    @Autowired
    private Environment environment;

    @ExceptionHandler(ResourceNotFoundException.class)
    public ResponseEntity<ApiResponse<Object>> handleResourceNotFoundException(
            ResourceNotFoundException ex, HttpServletRequest request) {
        
        ApiResponse<Object> response = ApiResponse.error(
            ex.getMessage(), 
            HttpStatus.NOT_FOUND.value()
        );
        response.setPath(request.getRequestURI());
        
        return new ResponseEntity<>(response, HttpStatus.NOT_FOUND);
    }

    @ExceptionHandler(DuplicateResourceException.class)
    public ResponseEntity<ApiResponse<Object>> handleDuplicateResourceException(
            DuplicateResourceException ex, HttpServletRequest request) {
        
        ApiResponse<Object> response = ApiResponse.error(
            ex.getMessage(), 
            HttpStatus.CONFLICT.value()
        );
        response.setPath(request.getRequestURI());
        
        return new ResponseEntity<>(response, HttpStatus.CONFLICT);
    }

    @ExceptionHandler(BadRequestException.class)
    public ResponseEntity<ApiResponse<Object>> handleBadRequestException(
            BadRequestException ex, HttpServletRequest request) {
        
        ApiResponse<Object> response = ApiResponse.error(
            ex.getMessage(),
            HttpStatus.BAD_REQUEST.value()
        );
        response.setPath(request.getRequestURI());
        
        return new ResponseEntity<>(response, HttpStatus.BAD_REQUEST);
    }

    @ExceptionHandler(BusinessException.class)
    public ResponseEntity<ApiResponse<Object>> handleBusinessException(
            BusinessException ex, HttpServletRequest request) {
        // If the exception message is provided and non-blank, return it to the client. Otherwise use a clearer default.
        String errorMessage = (ex != null && ex.getMessage() != null && !ex.getMessage().trim().isEmpty())
                ? ex.getMessage().trim()
                : "An error occurred while processing your request.";

        ApiResponse<Object> response = ApiResponse.error(
            errorMessage,
            ex.getStatus().value()
        );
        response.setPath(request.getRequestURI());
        
        return new ResponseEntity<>(response, ex.getStatus());
    }

    // Authentication-related exceptions
    @ExceptionHandler(BadCredentialsException.class)
    public ResponseEntity<ApiResponse<Object>> handleBadCredentialsException(
            BadCredentialsException ex, HttpServletRequest request) {
        
        ApiResponse<Object> response = ApiResponse.error(
            "Invalid email or password",
            HttpStatus.UNAUTHORIZED.value()
        );
        response.setPath(request.getRequestURI());
        
        return new ResponseEntity<>(response, HttpStatus.UNAUTHORIZED);
    }

    @ExceptionHandler(UsernameNotFoundException.class)
    public ResponseEntity<ApiResponse<Object>> handleUsernameNotFoundException(
            UsernameNotFoundException ex, HttpServletRequest request) {
        
        ApiResponse<Object> response = ApiResponse.error(
            "User not found",
            HttpStatus.UNAUTHORIZED.value()
        );
        response.setPath(request.getRequestURI());
        
        return new ResponseEntity<>(response, HttpStatus.UNAUTHORIZED);
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ApiResponse<Map<String, String>>> handleValidationExceptions(
            MethodArgumentNotValidException ex, HttpServletRequest request) {
        
        Map<String, String> errors = new HashMap<>();
        ex.getBindingResult().getAllErrors().forEach((error) -> {
            String fieldName = ((FieldError) error).getField();
            String errorMessage = error.getDefaultMessage();
            errors.put(fieldName, errorMessage);
        });

        ApiResponse<Map<String, String>> response = ApiResponse.error(
            "Validation failed",
            errors
        );
        response.setPath(request.getRequestURI());

        return new ResponseEntity<>(response, HttpStatus.BAD_REQUEST);
    }

    /**
     * Handle JSON parsing errors (e.g., invalid enum values, malformed JSON)
     * This catches errors from Jackson deserialization
     *
     * Jackson wraps custom exceptions (like ValidationException) inside HttpMessageNotReadableException
     * We extract the inner exception message to show the actual validation error to the user
     */
    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<ApiResponse<Object>> handleHttpMessageNotReadableException(
            HttpMessageNotReadableException ex, HttpServletRequest request) {

        String errorMessage = "Invalid request format. Please check your request body.";

        // Try to extract the actual error message from the cause
        Throwable cause = ex.getCause();

        if (cause != null) {
            // Check if the cause is a ValidationException (thrown by our enum @JsonCreator)
            if (cause instanceof ValidationException) {
                // Use the actual validation error message from our code
                String validationMessage = cause.getMessage();
                if (validationMessage != null && !validationMessage.isEmpty()) {
                    errorMessage = validationMessage;
                }
            } else {
                // For other causes, try to infer the error type
                String causeString = cause.toString();

                if (causeString.contains("FormAction") || causeString.contains("enum")) {
                    errorMessage = "Invalid action value. ";
                } else if (causeString.contains("JSON") || causeString.contains("JsonMappingException")) {
                    errorMessage = "Invalid JSON format in request body.";
                }
            }
        }

        ApiResponse<Object> response = ApiResponse.error(errorMessage);
        response.setPath(request.getRequestURI());

        return new ResponseEntity<>(response, HttpStatus.BAD_REQUEST);
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiResponse<Object>> handleGlobalException(
            Exception ex, HttpServletRequest request) {
        // If the exception message is provided and non-blank, return it; otherwise use a default.
        String provided = (ex != null && ex.getMessage() != null && !ex.getMessage().trim().isEmpty())
                ? ex.getMessage().trim()
                : null;

        String errorMessage = provided != null ? provided : "An unexpected error occurred.";

        ApiResponse<Object> response = ApiResponse.error(
            errorMessage,
            HttpStatus.INTERNAL_SERVER_ERROR.value()
        );
        response.setPath(request.getRequestURI());
        
        return new ResponseEntity<>(response, HttpStatus.INTERNAL_SERVER_ERROR);
    }

    @ExceptionHandler(ValidationException.class)
    public ResponseEntity<ApiResponse<Object>> handleValidationException(
            ValidationException ex, HttpServletRequest request) {

        ApiResponse<Object> response = ApiResponse.error(
            ex.getMessage(),
            HttpStatus.BAD_REQUEST.value()
        );
        response.setPath(request.getRequestURI());

        return new ResponseEntity<>(response, HttpStatus.BAD_REQUEST);
    }

    @ExceptionHandler(FileStorageException.class)
    public ResponseEntity<ApiResponse<Object>> handleFileStorageException(
            FileStorageException ex, HttpServletRequest request) {

        ApiResponse<Object> response = ApiResponse.error(
            ex.getMessage(),
            HttpStatus.BAD_REQUEST.value()
        );
        response.setPath(request.getRequestURI());

        return new ResponseEntity<>(response, HttpStatus.BAD_REQUEST);
    }
}