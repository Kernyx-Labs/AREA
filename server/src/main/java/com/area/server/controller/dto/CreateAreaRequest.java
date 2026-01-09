package com.area.server.controller.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public class CreateAreaRequest {

    @NotNull
    private Long actionConnectionId;

    @NotNull
    private Long reactionConnectionId;

    private String gmailLabel;

    private String gmailSubjectContains;

    private String gmailFromAddress;

    @NotBlank
    private String discordWebhookUrl;

    private String discordChannelName;

    private String discordMessageTemplate;

    public Long getActionConnectionId() {
        return actionConnectionId;
    }

    @SuppressWarnings("unused")
    public void setActionConnectionId(Long actionConnectionId) {
        this.actionConnectionId = actionConnectionId;
    }

    public Long getReactionConnectionId() {
        return reactionConnectionId;
    }

    @SuppressWarnings("unused")
    public void setReactionConnectionId(Long reactionConnectionId) {
        this.reactionConnectionId = reactionConnectionId;
    }

    public String getGmailLabel() {
        return gmailLabel;
    }

    @SuppressWarnings("unused")
    public void setGmailLabel(String gmailLabel) {
        this.gmailLabel = gmailLabel;
    }

    public String getGmailSubjectContains() {
        return gmailSubjectContains;
    }

    @SuppressWarnings("unused")
    public void setGmailSubjectContains(String gmailSubjectContains) {
        this.gmailSubjectContains = gmailSubjectContains;
    }

    public String getGmailFromAddress() {
        return gmailFromAddress;
    }

    @SuppressWarnings("unused")
    public void setGmailFromAddress(String gmailFromAddress) {
        this.gmailFromAddress = gmailFromAddress;
    }

    public String getDiscordWebhookUrl() {
        return discordWebhookUrl;
    }

    @SuppressWarnings("unused")
    public void setDiscordWebhookUrl(String discordWebhookUrl) {
        this.discordWebhookUrl = discordWebhookUrl;
    }

    public String getDiscordChannelName() {
        return discordChannelName;
    }

    @SuppressWarnings("unused")
    public void setDiscordChannelName(String discordChannelName) {
        this.discordChannelName = discordChannelName;
    }

    public String getDiscordMessageTemplate() {
        return discordMessageTemplate;
    }

    @SuppressWarnings("unused")
    public void setDiscordMessageTemplate(String discordMessageTemplate) {
        this.discordMessageTemplate = discordMessageTemplate;
    }
}
