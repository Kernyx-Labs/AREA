package com.area.server.model;

/**
 * Interface that standardizes access to configuration for both Area and
 * Workflow entities.
 * This decouples the Execution logic from the specific database entity.
 */
public interface AutomationEntity {
    Long getId();

    ServiceConnection getActionConnection();

    ServiceConnection getReactionConnection();

    GitHubActionConfig getGithubActionConfig();

    GitHubReactionConfig getGithubReactionConfig();

    GmailActionConfig getGmailConfig();

    DiscordReactionConfig getDiscordConfig();
}
