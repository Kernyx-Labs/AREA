package com.area.server.exception;

/**
 * Exception thrown when a service connection cannot be found.
 * This occurs when trying to access a non-existent service connection by ID.
 *
 * Mapped to HTTP 404 Not Found status.
 */
public class ServiceConnectionNotFoundException extends RuntimeException {

    private final Long connectionId;
    private final String serviceType;

    public ServiceConnectionNotFoundException(Long connectionId) {
        super(String.format("Service connection not found with id: %d", connectionId));
        this.connectionId = connectionId;
        this.serviceType = null;
    }

    public ServiceConnectionNotFoundException(String serviceType, Long connectionId) {
        super(String.format("%s service connection not found with id: %d", serviceType, connectionId));
        this.connectionId = connectionId;
        this.serviceType = serviceType;
    }

    public ServiceConnectionNotFoundException(String message) {
        super(message);
        this.connectionId = null;
        this.serviceType = null;
    }

    public Long getConnectionId() {
        return connectionId;
    }

    public String getServiceType() {
        return serviceType;
    }
}
