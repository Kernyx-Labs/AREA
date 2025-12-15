package com.area.server.exception;

import java.util.HashMap;
import java.util.Map;

/**
 * Exception thrown when business logic validation fails.
 * This is used for domain-specific validation that goes beyond
 * simple field validation handled by @Valid annotations.
 *
 * Mapped to HTTP 400 Bad Request status.
 */
public class ValidationException extends RuntimeException {

    private final Map<String, String> validationErrors;
    private final String field;

    public ValidationException(String message) {
        super(message);
        this.validationErrors = new HashMap<>();
        this.field = null;
    }

    public ValidationException(String field, String message) {
        super(String.format("Validation failed for field '%s': %s", field, message));
        this.field = field;
        this.validationErrors = new HashMap<>();
        this.validationErrors.put(field, message);
    }

    public ValidationException(Map<String, String> validationErrors) {
        super("Validation failed: " + validationErrors.toString());
        this.validationErrors = new HashMap<>(validationErrors);
        this.field = null;
    }

    public ValidationException(String message, Throwable cause) {
        super(message, cause);
        this.validationErrors = new HashMap<>();
        this.field = null;
    }

    public Map<String, String> getValidationErrors() {
        return new HashMap<>(validationErrors);
    }

    public String getField() {
        return field;
    }

    public boolean hasMultipleErrors() {
        return validationErrors.size() > 1;
    }
}
