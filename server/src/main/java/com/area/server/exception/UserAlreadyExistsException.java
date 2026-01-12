package com.area.server.exception;

/**
 * Exception thrown when attempting to register a user that already exists.
 * Used when email or username is already taken.
 */
public class UserAlreadyExistsException extends RuntimeException {

    public UserAlreadyExistsException(String message) {
        super(message);
    }

    public UserAlreadyExistsException(String message, Throwable cause) {
        super(message, cause);
    }
}
