package com.area.server.controller.dto;

import jakarta.validation.constraints.NotNull;

/**
 * Request DTO for testing trigger/action executors.
 * Contains the service connection ID and optional configuration parameters
 * specific to the trigger being tested.
 */
public class TestTriggerRequest {

    @NotNull(message = "Service connection ID is required")
    private Long serviceConnectionId;

    // Gmail-specific filters (optional)
    private String gmailLabel;
    private String gmailSubjectContains;
    private String gmailFromAddress;

    // GitHub-specific filters (optional)
    private String githubRepository;  // Format: "owner/repo"
    private String githubActionType;  // "issue_created" or "pr_created"

    public Long getServiceConnectionId() {
        return serviceConnectionId;
    }

    public void setServiceConnectionId(Long serviceConnectionId) {
        this.serviceConnectionId = serviceConnectionId;
    }

    public String getGmailLabel() {
        return gmailLabel;
    }

    public void setGmailLabel(String gmailLabel) {
        this.gmailLabel = gmailLabel;
    }

    public String getGmailSubjectContains() {
        return gmailSubjectContains;
    }

    public void setGmailSubjectContains(String gmailSubjectContains) {
        this.gmailSubjectContains = gmailSubjectContains;
    }

    public String getGmailFromAddress() {
        return gmailFromAddress;
    }

    public void setGmailFromAddress(String gmailFromAddress) {
        this.gmailFromAddress = gmailFromAddress;
    }

    public String getGithubRepository() {
        return githubRepository;
    }

    public void setGithubRepository(String githubRepository) {
        this.githubRepository = githubRepository;
    }

    public String getGithubActionType() {
        return githubActionType;
    }

    public void setGithubActionType(String githubActionType) {
        this.githubActionType = githubActionType;
    }
}
