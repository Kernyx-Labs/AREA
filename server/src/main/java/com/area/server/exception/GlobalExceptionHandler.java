package com.area.server.exception;

import com.area.server.dto.response.ApiResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.reactive.function.client.WebClientResponseException;

import java.util.HashMap;
import java.util.Map;

/**
 * Global exception handler that catches exceptions thrown by controllers
 * and converts them to standardized ApiResponse format.
 * This eliminates the need for try-catch blocks in controllers.
 */
@RestControllerAdvice
public class GlobalExceptionHandler {

    private static final Logger logger = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    /**
     * Handle validation errors from @Valid annotations
     */
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ApiResponse<?>> handleValidationErrors(MethodArgumentNotValidException ex) {
        Map<String, String> errors = new HashMap<>();
        ex.getBindingResult().getAllErrors().forEach(error -> {
            String fieldName = ((FieldError) error).getField();
            String errorMessage = error.getDefaultMessage();
            errors.put(fieldName, errorMessage);
        });

        logger.warn("Validation errors: {}", errors);
        return ResponseEntity.badRequest()
            .body(ApiResponse.error("Validation Error",
                "Invalid request parameters: " + errors.toString()));
    }

    /**
     * Handle IllegalArgumentException - typically from business logic validation
     */
    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<ApiResponse<?>> handleValidationError(IllegalArgumentException e) {
        logger.warn("Validation error: {}", e.getMessage());
        return ResponseEntity.badRequest()
            .body(ApiResponse.error("Validation Error", e.getMessage()));
    }

    /**
     * Handle ResourceNotFoundException - resource not found
     */
    @ExceptionHandler(ResourceNotFoundException.class)
    public ResponseEntity<ApiResponse<?>> handleNotFound(ResourceNotFoundException e) {
        logger.warn("Resource not found: {}", e.getMessage());
        return ResponseEntity.status(HttpStatus.NOT_FOUND)
            .body(ApiResponse.error("Not Found", e.getMessage()));
    }

    /**
     * Handle ServiceIntegrationException - external service errors
     */
    @ExceptionHandler(ServiceIntegrationException.class)
    public ResponseEntity<ApiResponse<?>> handleServiceIntegration(ServiceIntegrationException e) {
        logger.error("Service integration error: {}", e.getMessage(), e);
        return ResponseEntity.status(HttpStatus.BAD_GATEWAY)
            .body(ApiResponse.error("Service Integration Error", e.getMessage()));
    }

    /**
     * Handle WebClient exceptions from external API calls
     */
    @ExceptionHandler(WebClientResponseException.class)
    public ResponseEntity<ApiResponse<?>> handleWebClientError(WebClientResponseException e) {
        logger.error("External API error: {} - {}", e.getStatusCode(), e.getResponseBodyAsString());

        String message = String.format("External service returned error: %s", e.getStatusCode());
        return ResponseEntity.status(HttpStatus.BAD_GATEWAY)
            .body(ApiResponse.error("External Service Error", message));
    }

    /**
     * Handle IllegalStateException - invalid state for operation
     */
    @ExceptionHandler(IllegalStateException.class)
    public ResponseEntity<ApiResponse<?>> handleIllegalState(IllegalStateException e) {
        logger.warn("Illegal state: {}", e.getMessage());
        return ResponseEntity.status(HttpStatus.CONFLICT)
            .body(ApiResponse.error("Conflict", e.getMessage()));
    }

    /**
     * Handle ServiceConnectionNotFoundException - service connection not found
     */
    @ExceptionHandler(ServiceConnectionNotFoundException.class)
    public ResponseEntity<ApiResponse<?>> handleServiceConnectionNotFound(ServiceConnectionNotFoundException e) {
        logger.warn("Service connection not found: {}", e.getMessage());
        return ResponseEntity.status(HttpStatus.NOT_FOUND)
            .body(ApiResponse.error("Service Connection Not Found", e.getMessage()));
    }

    /**
     * Handle OAuthException - OAuth authentication and token errors
     */
    @ExceptionHandler(OAuthException.class)
    public ResponseEntity<ApiResponse<?>> handleOAuthError(OAuthException e) {
        logger.error("OAuth error: {}", e.getMessage(), e);
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
            .body(ApiResponse.error("OAuth Error", e.getMessage()));
    }

    /**
     * Handle ValidationException - business logic validation errors
     */
    @ExceptionHandler(ValidationException.class)
    public ResponseEntity<ApiResponse<?>> handleValidation(ValidationException e) {
        logger.warn("Business validation error: {}", e.getMessage());
        return ResponseEntity.badRequest()
            .body(ApiResponse.error("Validation Error", e.getMessage()));
    }

    /**
     * Handle AuthenticationException - authentication failures
     */
    @ExceptionHandler(AuthenticationException.class)
    public ResponseEntity<ApiResponse<?>> handleAuthentication(AuthenticationException e) {
        logger.warn("Authentication error: {}", e.getMessage());
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
            .body(ApiResponse.error("Authentication Error", e.getMessage()));
    }

    /**
     * Handle UserAlreadyExistsException - duplicate user registration
     */
    @ExceptionHandler(UserAlreadyExistsException.class)
    public ResponseEntity<ApiResponse<?>> handleUserAlreadyExists(UserAlreadyExistsException e) {
        logger.warn("User already exists: {}", e.getMessage());
        return ResponseEntity.status(HttpStatus.CONFLICT)
            .body(ApiResponse.error("User Already Exists", e.getMessage()));
    }

    /**
     * Handle InvalidTokenException - invalid or expired JWT tokens
     */
    @ExceptionHandler(InvalidTokenException.class)
    public ResponseEntity<ApiResponse<?>> handleInvalidToken(InvalidTokenException e) {
        logger.warn("Invalid token: {}", e.getMessage());
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
            .body(ApiResponse.error("Invalid Token", e.getMessage()));
    }

    /**
     * Catch-all handler for unexpected exceptions
     */
    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiResponse<?>> handleGenericError(Exception e) {
        logger.error("Unexpected error", e);

        // Don't expose internal error details in production
        String message = "An unexpected error occurred. Please try again later.";
        if (logger.isDebugEnabled()) {
            message = e.getMessage();
        }

        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
            .body(ApiResponse.error("Internal Server Error", message));
    }
}
