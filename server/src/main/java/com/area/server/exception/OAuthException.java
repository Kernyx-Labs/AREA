package com.area.server.exception;

/**
 * Exception thrown when OAuth authentication or token operations fail.
 * This typically indicates issues with token refresh, invalid credentials,
 * or expired OAuth sessions.
 *
 * Mapped to HTTP 401 Unauthorized status.
 */
public class OAuthException extends RuntimeException {

    private final String provider;
    private final String operation;

    public OAuthException(String message) {
        super(message);
        this.provider = null;
        this.operation = null;
    }

    public OAuthException(String provider, String operation, String message) {
        super(String.format("OAuth error for %s during %s: %s", provider, operation, message));
        this.provider = provider;
        this.operation = operation;
    }

    public OAuthException(String message, Throwable cause) {
        super(message, cause);
        this.provider = null;
        this.operation = null;
    }

    public OAuthException(String provider, String operation, Throwable cause) {
        super(String.format("OAuth error for %s during %s", provider, operation), cause);
        this.provider = provider;
        this.operation = operation;
    }

    public String getProvider() {
        return provider;
    }

    public String getOperation() {
        return operation;
    }
}
