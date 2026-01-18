package com.area.server.model;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;

/**
 * Configuration for GitHub action triggers.
 * Supports two action types:
 * 1. issue_created - Triggers when a new issue is created in the repository
 * 2. pr_created - Triggers when a new pull request is created in the repository
 *
 * This class supports both legacy (split owner/name fields) and new (combined repository field)
 * formats for backward compatibility.
 */
@Embeddable
public class GitHubActionConfig {

    @Column(name = "github_action_type", length = 32)
    private String actionType;

    // NEW: Single repository field in "owner/repo" format
    @Column(name = "github_repository", length = 255)
    private String repository;

    // DEPRECATED: Keep old fields for backward compatibility
    @Column(name = "github_repository_owner")
    @Deprecated
    private String repositoryOwner;

    @Column(name = "github_repository_name")
    @Deprecated
    private String repositoryName;

    public String getActionType() {
        return actionType;
    }

    public void setActionType(String actionType) {
        this.actionType = actionType;
    }

    public String getRepository() {
        return repository;
    }

    public void setRepository(String repository) {
        this.repository = repository;
        // Auto-sync: parse and populate old fields for backward compatibility
        if (repository != null && repository.contains("/")) {
            String[] parts = repository.split("/", 2);
            this.repositoryOwner = parts[0];
            this.repositoryName = parts.length > 1 ? parts[1] : null;
        }
    }

    /**
     * Backward-compatible getter for repository owner
     * Returns parsed value from new field if available, otherwise returns old field
     */
    @Deprecated
    public String getRepositoryOwner() {
        if (repository != null && repository.contains("/")) {
            return repository.split("/")[0];
        }
        return repositoryOwner;
    }

    /**
     * Backward-compatible setter for repository owner
     * Updates both old field and new combined field
     */
    @Deprecated
    public void setRepositoryOwner(String repositoryOwner) {
        this.repositoryOwner = repositoryOwner;
        if (repositoryOwner != null && repositoryName != null) {
            this.repository = repositoryOwner + "/" + repositoryName;
        }
    }

    /**
     * Backward-compatible getter for repository name
     * Returns parsed value from new field if available, otherwise returns old field
     */
    @Deprecated
    public String getRepositoryName() {
        if (repository != null && repository.contains("/")) {
            String[] parts = repository.split("/", 2);
            return parts.length > 1 ? parts[1] : null;
        }
        return repositoryName;
    }

    /**
     * Backward-compatible setter for repository name
     * Updates both old field and new combined field
     */
    @Deprecated
    public void setRepositoryName(String repositoryName) {
        this.repositoryName = repositoryName;
        if (repositoryOwner != null && repositoryName != null) {
            this.repository = repositoryOwner + "/" + repositoryName;
        }
    }

    /**
     * Get the full repository identifier in "owner/repo" format
     * Supports both new and legacy field formats
     */
    public String getFullRepositoryName() {
        if (repository != null && !repository.isBlank()) {
            return repository;
        }
        if (repositoryOwner == null || repositoryName == null) {
            return null;
        }
        return repositoryOwner + "/" + repositoryName;
    }

    /**
     * Set the full repository identifier in "owner/repo" format
     * Automatically parses and populates both new and old fields
     */
    public void setFullRepositoryName(String fullName) {
        this.repository = fullName;
        if (fullName != null && fullName.contains("/")) {
            String[] parts = fullName.split("/", 2);
            this.repositoryOwner = parts[0];
            this.repositoryName = parts.length > 1 ? parts[1] : null;
        }
    }
}
