package com.area.server.model;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;

/**
 * Configuration for GitHub reaction actions.
 * Supports two reaction types:
 * 1. create_issue - Creates a new issue in the target repository
 * 2. create_pr - Creates a new pull request in the target repository
 */
@Embeddable
public class GitHubReactionConfig {

    @Column(name = "github_reaction_type", length = 32)
    private String reactionType;

    @Column(name = "github_reaction_repository_owner")
    private String repositoryOwner;

    @Column(name = "github_reaction_repository_name")
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
     */
    public String getFullRepositoryName() {
        if (repositoryOwner == null || repositoryName == null) {
            return null;
        }
        return repositoryOwner + "/" + repositoryName;
    }
}
