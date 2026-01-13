package com.area.server.model;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;

/**
 * Configuration for GitHub action triggers.
 * Supports two action types:
 * 1. issue_created - Triggers when a new issue is created in the repository
 * 2. pr_created - Triggers when a new pull request is created in the repository
 */
@Embeddable
public class GitHubActionConfig {

    @Column(name = "github_action_type", length = 32)
    private String actionType;

    @Column(name = "github_repository_owner")
    private String repositoryOwner;

    @Column(name = "github_repository_name")
    private String repositoryName;

    public String getActionType() {
        return actionType;
    }

    public void setActionType(String actionType) {
        this.actionType = actionType;
    }

    public String getRepositoryOwner() {
        return repositoryOwner;
    }

    public void setRepositoryOwner(String repositoryOwner) {
        this.repositoryOwner = repositoryOwner;
    }

    public String getRepositoryName() {
        return repositoryName;
    }

    public void setRepositoryName(String repositoryName) {
        this.repositoryName = repositoryName;
    }

    /**
     * Get the full repository identifier in "owner/repo" format
     */
    public String getFullRepositoryName() {
        if (repositoryOwner == null || repositoryName == null) {
            return null;
        }
        return repositoryOwner + "/" + repositoryName;
    }
}
