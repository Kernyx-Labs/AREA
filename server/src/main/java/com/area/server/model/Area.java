package com.area.server.model;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;

@Entity
@Table(name = "areas")
public class Area implements AutomationEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @JsonIgnore
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id")
    private User user;

    @ManyToOne(optional = false)
    private ServiceConnection actionConnection;

    @ManyToOne(optional = false)
    private ServiceConnection reactionConnection;

    @Embedded
    private GmailActionConfig gmailConfig;

    @Embedded
    private DiscordReactionConfig discordConfig;

    @Embedded
    private GitHubActionConfig githubActionConfig;

    @Embedded
    private GitHubReactionConfig githubReactionConfig;

    private boolean active = true;

    public Long getId() {
        return id;
    }

    public User getUser() {
        return user;
    }

    public void setUser(User user) {
        this.user = user;
    }

    public ServiceConnection getActionConnection() {
        return actionConnection;
    }

    public void setActionConnection(ServiceConnection actionConnection) {
        this.actionConnection = actionConnection;
    }

    @SuppressWarnings("unused")
    public ServiceConnection getReactionConnection() {
        return reactionConnection;
    }

    @SuppressWarnings("unused")
    public void setReactionConnection(ServiceConnection reactionConnection) {
        this.reactionConnection = reactionConnection;
    }

    public GmailActionConfig getGmailConfig() {
        return gmailConfig;
    }

    public void setGmailConfig(GmailActionConfig gmailConfig) {
        this.gmailConfig = gmailConfig;
    }

    public DiscordReactionConfig getDiscordConfig() {
        return discordConfig;
    }

    public void setDiscordConfig(DiscordReactionConfig discordConfig) {
        this.discordConfig = discordConfig;
    }

    public GitHubActionConfig getGithubActionConfig() {
        return githubActionConfig;
    }

    public void setGithubActionConfig(GitHubActionConfig githubActionConfig) {
        this.githubActionConfig = githubActionConfig;
    }

    public GitHubReactionConfig getGithubReactionConfig() {
        return githubReactionConfig;
    }

    public void setGithubReactionConfig(GitHubReactionConfig githubReactionConfig) {
        this.githubReactionConfig = githubReactionConfig;
    }

    @SuppressWarnings("unused")
    public boolean isActive() {
        return active;
    }

    @SuppressWarnings("unused")
    public void setActive(boolean active) {
        this.active = active;
    }
}
