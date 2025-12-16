package com.area.server.controller;

import com.area.server.dto.response.ApiResponse;
import com.area.server.model.ServiceConnection;
import com.area.server.service.integration.*;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * REST API controller for service discovery and metadata.
 * Provides endpoints to query available services, actions, and reactions.
 *
 * All endpoints return data in the standardized ApiResponse format.
 */
@RestController
@RequestMapping("/api/services")
public class ServiceRegistryController {

    private final ServiceRegistry serviceRegistry;

    public ServiceRegistryController(ServiceRegistry serviceRegistry) {
        this.serviceRegistry = serviceRegistry;
    }

    /**
     * Get all available service integrations.
     * Returns metadata about each service including name, description, and capabilities.
     *
     * @return List of service descriptors
     */
    @GetMapping
    public ResponseEntity<ApiResponse<List<ServiceDescriptorResponse>>> listServices() {
        List<ServiceDescriptorResponse> services = serviceRegistry.getAllServices()
            .stream()
            .map(this::toDescriptorResponse)
            .collect(Collectors.toList());

        return ResponseEntity.ok(ApiResponse.success(services));
    }

    /**
     * Get metadata for a specific service.
     *
     * @param type Service type (GMAIL, DISCORD, etc.)
     * @return Service descriptor with actions and reactions
     */
    @GetMapping("/{type}")
    public ResponseEntity<ApiResponse<ServiceDescriptorResponse>> getService(
            @PathVariable ServiceConnection.ServiceType type) {

        ServiceIntegration service = serviceRegistry.getService(type);
        ServiceDescriptorResponse descriptor = toDescriptorResponse(service);

        return ResponseEntity.ok(ApiResponse.success(descriptor));
    }

    /**
     * Get all available actions (triggers) for a specific service.
     *
     * @param type Service type (GMAIL, DISCORD, etc.)
     * @return List of action definitions
     */
    @GetMapping("/{type}/actions")
    public ResponseEntity<ApiResponse<List<ActionDefinition>>> getActions(
            @PathVariable ServiceConnection.ServiceType type) {

        List<ActionDefinition> actions = serviceRegistry.getActionsForService(type);
        return ResponseEntity.ok(ApiResponse.success(actions));
    }

    /**
     * Get all available reactions for a specific service.
     *
     * @param type Service type (GMAIL, DISCORD, etc.)
     * @return List of reaction definitions
     */
    @GetMapping("/{type}/reactions")
    public ResponseEntity<ApiResponse<List<ReactionDefinition>>> getReactions(
            @PathVariable ServiceConnection.ServiceType type) {

        List<ReactionDefinition> reactions = serviceRegistry.getReactionsForService(type);
        return ResponseEntity.ok(ApiResponse.success(reactions));
    }

    /**
     * Get all services that provide actions (triggers).
     *
     * @return List of services with at least one action
     */
    @GetMapping("/with-actions")
    public ResponseEntity<ApiResponse<List<ServiceDescriptorResponse>>> getServicesWithActions() {
        List<ServiceDescriptorResponse> services = serviceRegistry.getServicesWithActions()
            .stream()
            .map(this::toDescriptorResponse)
            .collect(Collectors.toList());

        return ResponseEntity.ok(ApiResponse.success(services));
    }

    /**
     * Get all services that provide reactions.
     *
     * @return List of services with at least one reaction
     */
    @GetMapping("/with-reactions")
    public ResponseEntity<ApiResponse<List<ServiceDescriptorResponse>>> getServicesWithReactions() {
        List<ServiceDescriptorResponse> services = serviceRegistry.getServicesWithReactions()
            .stream()
            .map(this::toDescriptorResponse)
            .collect(Collectors.toList());

        return ResponseEntity.ok(ApiResponse.success(services));
    }

    /**
     * Get registry statistics.
     *
     * @return Statistics about registered services, actions, and reactions
     */
    @GetMapping("/stats")
    public ResponseEntity<ApiResponse<Map<String, Object>>> getStatistics() {
        Map<String, Object> stats = serviceRegistry.getStatistics();
        return ResponseEntity.ok(ApiResponse.success(stats));
    }

    /**
     * Convert ServiceIntegration to ServiceDescriptorResponse DTO.
     */
    private ServiceDescriptorResponse toDescriptorResponse(ServiceIntegration service) {
        ServiceDescriptorResponse response = new ServiceDescriptorResponse();
        response.setType(service.getType().name());
        response.setName(service.getName());
        response.setDescription(service.getDescription());
        response.setRequiresAuthentication(service.requiresAuthentication());
        response.setActionCount(service.getActions().size());
        response.setReactionCount(service.getReactions().size());
        response.setActions(service.getActions());
        response.setReactions(service.getReactions());

        return response;
    }

    /**
     * DTO class for service descriptor responses.
     */
    public static class ServiceDescriptorResponse {
        private String type;
        private String name;
        private String description;
        private boolean requiresAuthentication;
        private int actionCount;
        private int reactionCount;
        private List<ActionDefinition> actions;
        private List<ReactionDefinition> reactions;

        public String getType() {
            return type;
        }

        public void setType(String type) {
            this.type = type;
        }

        public String getName() {
            return name;
        }

        public void setName(String name) {
            this.name = name;
        }

        public String getDescription() {
            return description;
        }

        public void setDescription(String description) {
            this.description = description;
        }

        public boolean isRequiresAuthentication() {
            return requiresAuthentication;
        }

        public void setRequiresAuthentication(boolean requiresAuthentication) {
            this.requiresAuthentication = requiresAuthentication;
        }

        public int getActionCount() {
            return actionCount;
        }

        public void setActionCount(int actionCount) {
            this.actionCount = actionCount;
        }

        public int getReactionCount() {
            return reactionCount;
        }

        public void setReactionCount(int reactionCount) {
            this.reactionCount = reactionCount;
        }

        public List<ActionDefinition> getActions() {
            return actions;
        }

        public void setActions(List<ActionDefinition> actions) {
            this.actions = actions;
        }

        public List<ReactionDefinition> getReactions() {
            return reactions;
        }

        public void setReactions(List<ReactionDefinition> reactions) {
            this.reactions = reactions;
        }
    }
}
