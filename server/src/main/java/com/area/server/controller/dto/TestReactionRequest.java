package com.area.server.controller.dto;

import jakarta.validation.constraints.NotNull;

import java.util.Map;

/**
 * Request DTO for testing reaction executors.
 * Contains the service connection ID, reaction configuration,
 * and optional mock context data to simulate trigger output.
 */
public class TestReactionRequest {

    @NotNull(message = "Service connection ID is required")
    private Long serviceConnectionId;

    // Mock context data from trigger (optional)
    // Can be used to test template rendering with actual data
    private Map<String, Object> mockContextData;

    // Discord reaction configuration
    private String discordChannelId;
    private String discordMessageTemplate;

    // GitHub create_issue reaction configuration
    private String githubRepository;  // Format: "owner/repo"
    private String githubIssueTitle;
    private String githubIssueBody;
    private String githubIssueLabels;  // Comma-separated

    // GitHub create_pr reaction configuration
    private String githubPrTitle;
    private String githubPrBody;
    private String githubSourceBranch;
    private String githubTargetBranch;
    private String githubCommitMessage;
    private String githubFilePath;
    private String githubFileContent;

    public Long getServiceConnectionId() {
        return serviceConnectionId;
    }

    public void setServiceConnectionId(Long serviceConnectionId) {
        this.serviceConnectionId = serviceConnectionId;
    }

    public Map<String, Object> getMockContextData() {
        return mockContextData;
    }

    public void setMockContextData(Map<String, Object> mockContextData) {
        this.mockContextData = mockContextData;
    }

    public String getDiscordChannelId() {
        return discordChannelId;
    }

    public void setDiscordChannelId(String discordChannelId) {
        this.discordChannelId = discordChannelId;
    }

    public String getDiscordMessageTemplate() {
        return discordMessageTemplate;
    }

    public void setDiscordMessageTemplate(String discordMessageTemplate) {
        this.discordMessageTemplate = discordMessageTemplate;
    }

    public String getGithubRepository() {
        return githubRepository;
    }

    public void setGithubRepository(String githubRepository) {
        this.githubRepository = githubRepository;
    }

    public String getGithubIssueTitle() {
        return githubIssueTitle;
    }

    public void setGithubIssueTitle(String githubIssueTitle) {
        this.githubIssueTitle = githubIssueTitle;
    }

    public String getGithubIssueBody() {
        return githubIssueBody;
    }

    public void setGithubIssueBody(String githubIssueBody) {
        this.githubIssueBody = githubIssueBody;
    }

    public String getGithubIssueLabels() {
        return githubIssueLabels;
    }

    public void setGithubIssueLabels(String githubIssueLabels) {
        this.githubIssueLabels = githubIssueLabels;
    }

    public String getGithubPrTitle() {
        return githubPrTitle;
    }

    public void setGithubPrTitle(String githubPrTitle) {
        this.githubPrTitle = githubPrTitle;
    }

    public String getGithubPrBody() {
        return githubPrBody;
    }

    public void setGithubPrBody(String githubPrBody) {
        this.githubPrBody = githubPrBody;
    }

    public String getGithubSourceBranch() {
        return githubSourceBranch;
    }

    public void setGithubSourceBranch(String githubSourceBranch) {
        this.githubSourceBranch = githubSourceBranch;
    }

    public String getGithubTargetBranch() {
        return githubTargetBranch;
    }

    public void setGithubTargetBranch(String githubTargetBranch) {
        this.githubTargetBranch = githubTargetBranch;
    }

    public String getGithubCommitMessage() {
        return githubCommitMessage;
    }

    public void setGithubCommitMessage(String githubCommitMessage) {
        this.githubCommitMessage = githubCommitMessage;
    }

    public String getGithubFilePath() {
        return githubFilePath;
    }

    public void setGithubFilePath(String githubFilePath) {
        this.githubFilePath = githubFilePath;
    }

    public String getGithubFileContent() {
        return githubFileContent;
    }

    public void setGithubFileContent(String githubFileContent) {
        this.githubFileContent = githubFileContent;
    }
}
