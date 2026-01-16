package com.area.server.scheduler;

import com.area.server.dto.WorkflowData;
import com.area.server.model.*;
import com.area.server.repository.ServiceConnectionRepository;
import com.area.server.service.WorkflowTriggerStateService;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.util.Map;

/**
 * Adapter that makes a Workflow work with AutomationEntity-based executors.
 * This allows reuse of existing ActionExecutor and ReactionExecutor
 * implementations
 * that were designed for the Area entity.
 *
 * Key adaptations:
 * - Maps workflow JSON config to embedded config objects (GmailActionConfig,
 * GitHubActionConfig, etc.)
 * - Retrieves ServiceConnections from workflow's connection references
 * - Provides workflow trigger state via WorkflowTriggerStateService
 */
public class WorkflowWrapper implements AutomationEntity {

    private final Workflow workflow;
    private final WorkflowData workflowData;
    private final WorkflowTriggerStateService stateService;
    final ServiceConnectionRepository connectionRepository; // Package-private for ActionWrapper
    private final ObjectMapper objectMapper = new ObjectMapper();

    public WorkflowWrapper(Workflow workflow,
            WorkflowData workflowData,
            WorkflowTriggerStateService stateService,
            ServiceConnectionRepository connectionRepository) {
        this.workflow = workflow;
        this.workflowData = workflowData;
        this.stateService = stateService;
        this.connectionRepository = connectionRepository;
    }

    @Override
    public Long getId() {
        return workflow.getId();
    }

    public boolean isActive() {
        return workflow.isActive();
    }

    @Override
    public ServiceConnection getActionConnection() {
        if (workflow.getTriggerConnection() != null) {
            return workflow.getTriggerConnection();
        }

        // Fallback: try to get from trigger config
        WorkflowData.TriggerConfig trigger = workflowData.getTrigger();
        if (trigger.getConnectionId() != null) {
            return connectionRepository.findById(trigger.getConnectionId()).orElse(null);
        }

        return null;
    }

    @Override
    public ServiceConnection getReactionConnection() {
        if (workflow.getReactionConnection() != null) {
            return workflow.getReactionConnection();
        }

        // Fallback: try to get from first action config
        if (workflowData.getActions() != null && !workflowData.getActions().isEmpty()) {
            WorkflowData.ActionConfig firstAction = workflowData.getActions().get(0);
            if (firstAction.getConnectionId() != null) {
                return connectionRepository.findById(firstAction.getConnectionId()).orElse(null);
            }
        }

        return null;
    }

    @Override
    public GmailActionConfig getGmailConfig() {
        WorkflowData.TriggerConfig trigger = workflowData.getTrigger();
        if (!"gmail".equalsIgnoreCase(trigger.getService())) {
            return null;
        }

        Map<String, Object> config = trigger.getConfig();
        if (config == null) {
            return null;
        }

        GmailActionConfig gmailConfig = new GmailActionConfig();

        // Map JSON config to GmailActionConfig
        if (config.containsKey("label") || config.containsKey("labelName")) {
            String label = (String) (config.containsKey("label") ? config.get("label") : config.get("labelName"));
            gmailConfig.setLabel(label);
        }
        if (config.containsKey("subjectContains")) {
            gmailConfig.setSubjectContains((String) config.get("subjectContains"));
        }
        if (config.containsKey("fromAddress")) {
            gmailConfig.setFromAddress((String) config.get("fromAddress"));
        }

        return gmailConfig;
    }

    @Override
    public GitHubActionConfig getGithubActionConfig() {
        WorkflowData.TriggerConfig trigger = workflowData.getTrigger();
        if (!"github".equalsIgnoreCase(trigger.getService())) {
            return null;
        }

        Map<String, Object> config = trigger.getConfig();
        if (config == null) {
            return null;
        }

        GitHubActionConfig githubConfig = new GitHubActionConfig();
        String type = trigger.getType();
        if (type != null && type.startsWith("github.")) {
            type = type.substring(7);
        }
        githubConfig.setActionType(type);

        // [FIX] Support for combined repository field from JSON
        if (config.containsKey("repository")) {
            githubConfig.setRepository((String) config.get("repository"));
        } else if (config.containsKey("repositoryName") || config.containsKey("repositoryOwner")) {
            // Fallback to legacy fields
            if (config.containsKey("repositoryOwner")) {
                githubConfig.setRepositoryOwner((String) config.get("repositoryOwner"));
            }
            if (config.containsKey("repositoryName")) {
                githubConfig.setRepositoryName((String) config.get("repositoryName"));
            }
        }

        return githubConfig;
    }

    @Override
    public DiscordReactionConfig getDiscordConfig() {
        // This is used for reactions, not triggers
        // Will be handled by ActionWrapper or we can implement checking first action
        return null;
    }

    @Override
    public GitHubReactionConfig getGithubReactionConfig() {
        // This is used for reactions, not triggers
        // Will be handled by ActionWrapper
        return null;
    }

    /**
     * Get the underlying workflow entity.
     */
    public Workflow getWorkflow() {
        return workflow;
    }

    /**
     * Get the parsed workflow data.
     */
    public WorkflowData getWorkflowData() {
        return workflowData;
    }
}
