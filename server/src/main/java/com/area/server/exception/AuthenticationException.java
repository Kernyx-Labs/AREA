package com.area.server.exception;

/**
 * Exception thrown when authentication fails.
 * Used for invalid credentials, locked accounts, etc.
 */
public class AuthenticationException extends RuntimeException {

    public AuthenticationException(String message) {
        super(message);
    }

    public AuthenticationException(String message, Throwable cause) {
        super(message, cause);
    }
}
