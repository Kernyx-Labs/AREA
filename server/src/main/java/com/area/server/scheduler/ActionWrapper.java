package com.area.server.scheduler;

import com.area.server.dto.WorkflowData;
import com.area.server.model.*;

import java.util.Map;

/**
 * Adapter that wraps a specific action/reaction from a workflow
 * to work with ReactionExecutor implementations.
 *
 * This allows reaction executors to get the specific configuration
 * for the action they're executing (e.g., Discord webhook URL, GitHub repo
 * details).
 */
public class ActionWrapper extends Area {

    private final WorkflowWrapper workflowWrapper;
    private final WorkflowData.ActionConfig actionConfig;

    public ActionWrapper(WorkflowWrapper workflowWrapper, WorkflowData.ActionConfig actionConfig) {
        this.workflowWrapper = workflowWrapper;
        this.actionConfig = actionConfig;
    }

    @Override
    public Long getId() {
        return workflowWrapper.getId();
    }

    @Override
    public boolean isActive() {
        return workflowWrapper.isActive();
    }

    @Override
    public ServiceConnection getActionConnection() {
        return workflowWrapper.getActionConnection();
    }

    @Override
    public ServiceConnection getReactionConnection() {
        // Try to get connection for this specific action
        if (actionConfig.getConnectionId() != null) {
            return workflowWrapper.connectionRepository.findById(actionConfig.getConnectionId()).orElse(null);
        }
        return workflowWrapper.getReactionConnection();
    }

    @Override
    public GmailActionConfig getGmailConfig() {
        return workflowWrapper.getGmailConfig();
    }

    @Override
    public DiscordReactionConfig getDiscordConfig() {
        if (!"discord".equalsIgnoreCase(actionConfig.getService())) {
            return null;
        }

        Map<String, Object> config = actionConfig.getConfig();
        if (config == null) {
            return null;
        }

        DiscordReactionConfig discordConfig = new DiscordReactionConfig();

        if (config.containsKey("webhookUrl")) {
            discordConfig.setWebhookUrl((String) config.get("webhookUrl"));
        }
        if (config.containsKey("channelName")) {
            discordConfig.setChannelName((String) config.get("channelName"));
        }
        if (config.containsKey("message") || config.containsKey("messageTemplate") ||
                config.containsKey("body") || config.containsKey("content") || config.containsKey("message_template")) {

            String message = null;
            if (config.containsKey("message"))
                message = (String) config.get("message");
            else if (config.containsKey("messageTemplate"))
                message = (String) config.get("messageTemplate");
            else if (config.containsKey("message_template"))
                message = (String) config.get("message_template");
            else if (config.containsKey("body"))
                message = (String) config.get("body");
            else if (config.containsKey("content"))
                message = (String) config.get("content");

            discordConfig.setMessageTemplate(message);
        }

        return discordConfig;
    }

    @Override
    public GitHubActionConfig getGithubActionConfig() {
        return workflowWrapper.getGithubActionConfig();
    }

    @Override
    public GitHubReactionConfig getGithubReactionConfig() {
        if (!"github".equalsIgnoreCase(actionConfig.getService())) {
            return null;
        }

        Map<String, Object> config = actionConfig.getConfig();
        if (config == null) {
            return null;
        }

        GitHubReactionConfig githubConfig = new GitHubReactionConfig();
        githubConfig.setReactionType(actionConfig.getType());

        // Handle repository configuration (supports both formats)
        if (config.containsKey("repository")) {
            githubConfig.setRepository((String) config.get("repository"));
        } else {
            if (config.containsKey("repositoryOwner")) {
                githubConfig.setRepositoryOwner((String) config.get("repositoryOwner"));
            }
            if (config.containsKey("repositoryName")) {
                githubConfig.setRepositoryName((String) config.get("repositoryName"));
            }
        }

        // For create_issue reaction
        if (config.containsKey("issueTitle")) {
            githubConfig.setIssueTitle((String) config.get("issueTitle"));
        }
        if (config.containsKey("issueBody")) {
            githubConfig.setIssueBody((String) config.get("issueBody"));
        }
        if (config.containsKey("labels")) {
            githubConfig.setLabels((String) config.get("labels"));
        }

        // For create_pr reaction
        if (config.containsKey("prTitle")) {
            githubConfig.setPrTitle((String) config.get("prTitle"));
        }
        if (config.containsKey("prBody")) {
            githubConfig.setPrBody((String) config.get("prBody"));
        }
        if (config.containsKey("sourceBranch")) {
            githubConfig.setSourceBranch((String) config.get("sourceBranch"));
        }
        if (config.containsKey("targetBranch")) {
            githubConfig.setTargetBranch((String) config.get("targetBranch"));
        }
        if (config.containsKey("commitMessage")) {
            githubConfig.setCommitMessage((String) config.get("commitMessage"));
        }
        if (config.containsKey("filePath")) {
            githubConfig.setFilePath((String) config.get("filePath"));
        }
        if (config.containsKey("fileContent")) {
            githubConfig.setFileContent((String) config.get("fileContent"));
        }

        return githubConfig;
    }

    /**
     * Get the specific action configuration being executed.
     */
    public WorkflowData.ActionConfig getActionConfig() {
        return actionConfig;
    }

    /**
     * Get the underlying workflow wrapper.
     */
    public WorkflowWrapper getWorkflowWrapper() {
        return workflowWrapper;
    }
}
