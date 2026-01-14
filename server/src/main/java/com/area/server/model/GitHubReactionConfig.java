package com.area.server.model;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;

/**
 * Configuration for GitHub reaction actions.
 * Supports two reaction types:
 * 1. create_issue - Creates a new issue in the target repository
 * 2. create_pr - Creates a new pull request in the target repository
 *
 * This class supports both legacy (split owner/name fields) and new (combined repository field)
 * formats for backward compatibility.
 */
@Embeddable
public class GitHubReactionConfig {

    @Column(name = "github_reaction_type", length = 32)
    private String reactionType;

    // NEW: Single repository field in "owner/repo" format
    @Column(name = "github_reaction_repository", length = 255)
    private String repository;

    // DEPRECATED: Keep old fields for backward compatibility
    @Column(name = "github_reaction_repository_owner")
    @Deprecated
    private String repositoryOwner;

    @Column(name = "github_reaction_repository_name")
    @Deprecated
    private String repositoryName;

    // Fields for create_issue reaction
    @Column(name = "github_issue_title", length = 512)
    private String issueTitle;

    @Column(name = "github_issue_body", length = 4096)
    private String issueBody;

    @Column(name = "github_issue_labels", length = 512)
    private String labels;

    // Fields for create_pr reaction
    @Column(name = "github_pr_title", length = 512)
    private String prTitle;

    @Column(name = "github_pr_body", length = 4096)
    private String prBody;

    @Column(name = "github_pr_source_branch")
    private String sourceBranch;

    @Column(name = "github_pr_target_branch")
    private String targetBranch;

    @Column(name = "github_pr_commit_message", length = 512)
    private String commitMessage;

    @Column(name = "github_pr_file_path")
    private String filePath;

    @Column(name = "github_pr_file_content", length = 4096)
    private String fileContent;

    public String getReactionType() {
        return reactionType;
    }

    public void setReactionType(String reactionType) {
        this.reactionType = reactionType;
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

    public String getIssueTitle() {
        return issueTitle;
    }

    public void setIssueTitle(String issueTitle) {
        this.issueTitle = issueTitle;
    }

    public String getIssueBody() {
        return issueBody;
    }

    public void setIssueBody(String issueBody) {
        this.issueBody = issueBody;
    }

    public String getLabels() {
        return labels;
    }

    public void setLabels(String labels) {
        this.labels = labels;
    }

    public String getPrTitle() {
        return prTitle;
    }

    public void setPrTitle(String prTitle) {
        this.prTitle = prTitle;
    }

    public String getPrBody() {
        return prBody;
    }

    public void setPrBody(String prBody) {
        this.prBody = prBody;
    }

    public String getSourceBranch() {
        return sourceBranch;
    }

    public void setSourceBranch(String sourceBranch) {
        this.sourceBranch = sourceBranch;
    }

    public String getTargetBranch() {
        return targetBranch;
    }

    public void setTargetBranch(String targetBranch) {
        this.targetBranch = targetBranch;
    }

    public String getCommitMessage() {
        return commitMessage;
    }

    public void setCommitMessage(String commitMessage) {
        this.commitMessage = commitMessage;
    }

    public String getFilePath() {
        return filePath;
    }

    public void setFilePath(String filePath) {
        this.filePath = filePath;
    }

    public String getFileContent() {
        return fileContent;
    }

    public void setFileContent(String fileContent) {
        this.fileContent = fileContent;
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
