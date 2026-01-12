package com.area.server.exception;

/**
 * Exception thrown when an error occurs during service integration operations
 * (e.g., OAuth flow, API calls to external services).
 */
public class ServiceIntegrationException extends RuntimeException {

    private final String serviceType;

    public ServiceIntegrationException(String message) {
        super(message);
        this.serviceType = null;
    }

    public ServiceIntegrationException(String serviceType, String message) {
        super(String.format("%s integration error: %s", serviceType, message));
        this.serviceType = serviceType;
    }

    public ServiceIntegrationException(String message, Throwable cause) {
        super(message, cause);
        this.serviceType = null;
    }

    public ServiceIntegrationException(String serviceType, String message, Throwable cause) {
        super(String.format("%s integration error: %s", serviceType, message), cause);
        this.serviceType = serviceType;
    }

    public String getServiceType() {
        return serviceType;
    }
}
