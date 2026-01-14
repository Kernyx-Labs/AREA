package com.area.server.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * Data Transfer Object for GitHub repository information.
 * Used for the repository list endpoint to provide a clean API response.
 */
public class GitHubRepositoryDTO {

    @JsonProperty("full_name")
    private String fullName;  // "owner/repo" format

    private String description;

    @JsonProperty("private")
    private boolean isPrivate;

    @JsonProperty("html_url")
    private String htmlUrl;

    public GitHubRepositoryDTO() {
    }

    public GitHubRepositoryDTO(String fullName, String description, boolean isPrivate, String htmlUrl) {
        this.fullName = fullName;
        this.description = description;
        this.isPrivate = isPrivate;
        this.htmlUrl = htmlUrl;
    }

    public String getFullName() {
        return fullName;
    }

    public void setFullName(String fullName) {
        this.fullName = fullName;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public boolean isPrivate() {
        return isPrivate;
    }

    public void setPrivate(boolean isPrivate) {
        this.isPrivate = isPrivate;
    }

    public String getHtmlUrl() {
        return htmlUrl;
    }

    public void setHtmlUrl(String htmlUrl) {
        this.htmlUrl = htmlUrl;
    }
}
