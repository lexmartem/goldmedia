package com.goldmediatech.videometadata.exception;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.AuthenticationException;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.context.request.WebRequest;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Global exception handler for consistent error responses.
 * 
 * This class provides centralized exception handling for all controllers
 * and ensures consistent error response format across the application.
 */
@RestControllerAdvice
public class GlobalExceptionHandler {

    private static final Logger logger = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    /**
     * Handle validation errors.
     * 
     * @param ex the validation exception
     * @param request the web request
     * @return ResponseEntity with validation error details
     */
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ErrorResponse> handleValidationErrors(
            MethodArgumentNotValidException ex, WebRequest request) {
        
        List<String> details = ex.getBindingResult()
                .getFieldErrors()
                .stream()
                .map(FieldError::getDefaultMessage)
                .collect(Collectors.toList());

        ErrorResponse errorResponse = new ErrorResponse(
                "VALIDATION_ERROR",
                "Invalid request parameters",
                details,
                LocalDateTime.now(),
                request.getDescription(false)
        );

        logger.warn("Validation error: {}", details);
        return ResponseEntity.badRequest().body(errorResponse);
    }

    /**
     * Handle authentication errors.
     * 
     * @param ex the authentication exception
     * @param request the web request
     * @return ResponseEntity with authentication error
     */
    @ExceptionHandler(AuthenticationException.class)
    public ResponseEntity<ErrorResponse> handleAuthenticationError(
            AuthenticationException ex, WebRequest request) {
        
        ErrorResponse errorResponse = new ErrorResponse(
                "AUTHENTICATION_ERROR",
                "Authentication failed",
                List.of(ex.getMessage()),
                LocalDateTime.now(),
                request.getDescription(false)
        );

        logger.warn("Authentication error: {}", ex.getMessage());
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(errorResponse);
    }

    /**
     * Handle access denied errors.
     * 
     * @param ex the access denied exception
     * @param request the web request
     * @return ResponseEntity with access denied error
     */
    @ExceptionHandler(AccessDeniedException.class)
    public ResponseEntity<ErrorResponse> handleAccessDeniedError(
            AccessDeniedException ex, WebRequest request) {
        
        ErrorResponse errorResponse = new ErrorResponse(
                "ACCESS_DENIED",
                "Access denied - insufficient permissions",
                List.of(ex.getMessage()),
                LocalDateTime.now(),
                request.getDescription(false)
        );

        logger.warn("Access denied: {}", ex.getMessage());
        return ResponseEntity.status(HttpStatus.FORBIDDEN).body(errorResponse);
    }

    /**
     * Handle runtime exceptions.
     * 
     * @param ex the runtime exception
     * @param request the web request
     * @return ResponseEntity with runtime error
     */
    @ExceptionHandler(RuntimeException.class)
    public ResponseEntity<ErrorResponse> handleRuntimeError(
            RuntimeException ex, WebRequest request) {
        
        ErrorResponse errorResponse = new ErrorResponse(
                "RUNTIME_ERROR",
                "An unexpected error occurred",
                List.of(ex.getMessage()),
                LocalDateTime.now(),
                request.getDescription(false)
        );

        logger.error("Runtime error: {}", ex.getMessage(), ex);
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(errorResponse);
    }

    /**
     * Handle generic exceptions.
     * 
     * @param ex the exception
     * @param request the web request
     * @return ResponseEntity with generic error
     */
    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorResponse> handleGenericError(
            Exception ex, WebRequest request) {
        
        ErrorResponse errorResponse = new ErrorResponse(
                "INTERNAL_ERROR",
                "An internal server error occurred",
                List.of("Please try again later or contact support"),
                LocalDateTime.now(),
                request.getDescription(false)
        );

        logger.error("Unexpected error: {}", ex.getMessage(), ex);
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(errorResponse);
    }

    /**
     * Error response DTO.
     */
    public record ErrorResponse(
        String code,
        String message,
        List<String> details,
        LocalDateTime timestamp,
        String path
    ) {}
} 