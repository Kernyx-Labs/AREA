package com.area.server.model;

import jakarta.persistence.*;

@Entity
@Table(name = "areas")
public class Area {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(optional = false)
    private ServiceConnection actionConnection;

    @ManyToOne(optional = false)
    private ServiceConnection reactionConnection;

    @Embedded
    private GmailActionConfig gmailConfig;

    @Embedded
    private DiscordReactionConfig discordConfig;

    private boolean active = true;

    public Long getId() {
        return id;
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

    @SuppressWarnings("unused")
    public boolean isActive() {
        return active;
    }

    @SuppressWarnings("unused")
    public void setActive(boolean active) {
        this.active = active;
    }
}
