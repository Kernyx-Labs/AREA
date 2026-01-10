package com.area.server.exception;

/**
 * Exception thrown when a JWT token is invalid or expired.
 * Used for token validation failures.
 */
public class InvalidTokenException extends RuntimeException {

    public InvalidTokenException(String message) {
        super(message);
    }

    public InvalidTokenException(String message, Throwable cause) {
        super(message, cause);
    }
}
