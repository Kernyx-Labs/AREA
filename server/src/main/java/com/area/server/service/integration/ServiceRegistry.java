package com.area.server.service.integration;

import com.area.server.exception.ResourceNotFoundException;
import com.area.server.model.ServiceConnection;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * Central registry for all service integrations in the AREA platform.
 * Auto-discovers and manages all ServiceIntegration implementations via dependency injection.
 *
 * This service provides a single source of truth for:
 * - Available service integrations
 * - Actions (triggers) each service provides
 * - Reactions each service provides
 *
 * Benefits:
 * - Services auto-register (no configuration needed)
 * - Dynamic service discovery
 * - Easy to add new services (just implement ServiceIntegration)
 * - Type-safe service lookup
 */
@Service
public class ServiceRegistry {

    private static final Logger logger = LoggerFactory.getLogger(ServiceRegistry.class);

    private final Map<ServiceConnection.ServiceType, ServiceIntegration> services;

    /**
     * Constructor with dependency injection.
     * Spring automatically injects all beans implementing ServiceIntegration.
     *
     * @param integrations List of all ServiceIntegration implementations
     */
    public ServiceRegistry(List<ServiceIntegration> integrations) {
        this.services = integrations.stream()
            .collect(Collectors.toMap(
                ServiceIntegration::getType,
                Function.identity()
            ));

        logger.info("ServiceRegistry initialized with {} service integrations: {}",
            services.size(),
            services.keySet());
    }

    /**
     * Get all registered service integrations.
     *
     * @return List of all service integrations
     */
    public List<ServiceIntegration> getAllServices() {
        return new ArrayList<>(services.values());
    }

    /**
     * Get a specific service integration by type.
     *
     * @param type ServiceType enum value
     * @return ServiceIntegration instance
     * @throws ResourceNotFoundException if service type is not registered
     */
    public ServiceIntegration getService(ServiceConnection.ServiceType type) {
        return Optional.ofNullable(services.get(type))
            .orElseThrow(() -> new ResourceNotFoundException(
                "Service",
                type.name()
            ));
    }

    /**
     * Get all actions (triggers) for a specific service.
     *
     * @param type ServiceType enum value
     * @return List of ActionDefinitions for the service
     * @throws ResourceNotFoundException if service type is not registered
     */
    public List<ActionDefinition> getActionsForService(ServiceConnection.ServiceType type) {
        return getService(type).getActions();
    }

    /**
     * Get all reactions for a specific service.
     *
     * @param type ServiceType enum value
     * @return List of ReactionDefinitions for the service
     * @throws ResourceNotFoundException if service type is not registered
     */
    public List<ReactionDefinition> getReactionsForService(ServiceConnection.ServiceType type) {
        return getService(type).getReactions();
    }

    /**
     * Check if a service type is registered.
     *
     * @param type ServiceType enum value
     * @return true if service is registered, false otherwise
     */
    public boolean isServiceRegistered(ServiceConnection.ServiceType type) {
        return services.containsKey(type);
    }

    /**
     * Get all service types that are currently registered.
     *
     * @return List of registered ServiceType enum values
     */
    public List<ServiceConnection.ServiceType> getRegisteredServiceTypes() {
        return new ArrayList<>(services.keySet());
    }

    /**
     * Get all services that provide actions (triggers).
     *
     * @return List of service integrations that have at least one action
     */
    public List<ServiceIntegration> getServicesWithActions() {
        return services.values().stream()
            .filter(service -> !service.getActions().isEmpty())
            .toList();
    }

    /**
     * Get all services that provide reactions.
     *
     * @return List of service integrations that have at least one reaction
     */
    public List<ServiceIntegration> getServicesWithReactions() {
        return services.values().stream()
            .filter(service -> !service.getReactions().isEmpty())
            .toList();
    }

    /**
     * Get the total number of registered services.
     *
     * @return Count of registered services
     */
    public int getServiceCount() {
        return services.size();
    }

    /**
     * Get statistics about the registry.
     *
     * @return Map containing registry statistics
     */
    public Map<String, Object> getStatistics() {
        int totalActions = services.values().stream()
            .mapToInt(service -> service.getActions().size())
            .sum();

        int totalReactions = services.values().stream()
            .mapToInt(service -> service.getReactions().size())
            .sum();

        long servicesRequiringAuth = services.values().stream()
            .filter(ServiceIntegration::requiresAuthentication)
            .count();

        return Map.of(
            "totalServices", services.size(),
            "totalActions", totalActions,
            "totalReactions", totalReactions,
            "servicesRequiringAuth", servicesRequiringAuth,
            "servicesWithActions", getServicesWithActions().size(),
            "servicesWithReactions", getServicesWithReactions().size()
        );
    }
}
