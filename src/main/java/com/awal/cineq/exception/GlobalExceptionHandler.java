package com.awal.cineq.exception;

import com.awal.cineq.dto.ApiResponse;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.env.Environment;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
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
            HttpStatus.BAD_REQUEST.value()
        );
        response.setPath(request.getRequestURI());
        
        return new ResponseEntity<>(response, HttpStatus.BAD_REQUEST);
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