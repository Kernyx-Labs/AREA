package com.area.server.controller.dto;

import jakarta.validation.constraints.NotNull;

public class CreateTimerAreaRequest {
    private Long timerConnectionId; // Optional for Timer (time-based service)
    
    @NotNull
    private Long reactionConnectionId;
    
    // Timer configuration
    private String timerType; // "current_date", "current_time", "days_until", "recurring"
    private Integer intervalMinutes;
    private Integer daysCount;
    private String targetDay;
    
    // Discord configuration
    private String discordWebhookUrl; // Optional - uses connection metadata
    private String discordChannelName;
    private String discordMessageTemplate;
    
    // Action/Reaction types
    private String actionType; // e.g., "timer.recurring"
    private String reactionType; // e.g., "discord.send_webhook"

    public Long getTimerConnectionId() {
        return timerConnectionId;
    }

    public void setTimerConnectionId(Long timerConnectionId) {
        this.timerConnectionId = timerConnectionId;
    }

    public Long getReactionConnectionId() {
        return reactionConnectionId;
    }

    public void setReactionConnectionId(Long reactionConnectionId) {
        this.reactionConnectionId = reactionConnectionId;
    }

    public String getTimerType() {
        return timerType;
    }

    public void setTimerType(String timerType) {
        this.timerType = timerType;
    }

    public Integer getIntervalMinutes() {
        return intervalMinutes;
    }

    public void setIntervalMinutes(Integer intervalMinutes) {
        this.intervalMinutes = intervalMinutes;
    }

    public Integer getDaysCount() {
        return daysCount;
    }

    public void setDaysCount(Integer daysCount) {
        this.daysCount = daysCount;
    }

    public String getTargetDay() {
        return targetDay;
    }

    public void setTargetDay(String targetDay) {
        this.targetDay = targetDay;
    }

    public String getDiscordWebhookUrl() {
        return discordWebhookUrl;
    }

    public void setDiscordWebhookUrl(String discordWebhookUrl) {
        this.discordWebhookUrl = discordWebhookUrl;
    }

    public String getDiscordChannelName() {
        return discordChannelName;
    }

    public void setDiscordChannelName(String discordChannelName) {
        this.discordChannelName = discordChannelName;
    }

    public String getDiscordMessageTemplate() {
        return discordMessageTemplate;
    }

    public void setDiscordMessageTemplate(String discordMessageTemplate) {
        this.discordMessageTemplate = discordMessageTemplate;
    }

    public String getActionType() {
        return actionType;
    }

    public void setActionType(String actionType) {
        this.actionType = actionType;
    }

    public String getReactionType() {
        return reactionType;
    }

    public void setReactionType(String reactionType) {
        this.reactionType = reactionType;
    }
}

