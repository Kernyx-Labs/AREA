package com.area.server.service.integration;

import com.area.server.model.ServiceConnection;
import java.util.List;

/**
 * Base interface for all service integrations in the AREA platform.
 * Each service (Gmail, Discord, Slack, etc.) implements this interface
 * to provide metadata about available actions and reactions.
 *
 * This interface follows the Strategy pattern, allowing services to be
 * registered and discovered dynamically.
 */
public interface ServiceIntegration {

    /**
     * Get the unique identifier for this service type
     * @return ServiceConnection.ServiceType enum value
     */
    ServiceConnection.ServiceType getType();

    /**
     * Get the human-readable name of this service
     * @return Display name (e.g., "Gmail", "Discord")
     */
    String getName();

    /**
     * Get a description of what this service provides
     * @return Service description
     */
    String getDescription();

    /**
     * Get all available actions (triggers) this service supports
     * @return List of action definitions, or empty list if service only provides reactions
     */
    List<ActionDefinition> getActions();

    /**
     * Get all available reactions this service supports
     * @return List of reaction definitions, or empty list if service only provides actions
     */
    List<ReactionDefinition> getReactions();

    /**
     * Check if this service requires authentication
     * @return true if service needs connection/authentication, false otherwise
     */
    default boolean requiresAuthentication() {
        return true;
    }
}
