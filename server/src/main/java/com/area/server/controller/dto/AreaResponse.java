package com.area.server.controller.dto;

import java.time.Instant;

public class AreaResponse {

    private Long id;
    private Long actionConnectionId;
    private Long reactionConnectionId;
    private GmailConfigDto gmailConfig;
    private DiscordConfigDto discordConfig;
    private boolean active;
    private TriggerStateDto triggerState;

    public static class GmailConfigDto {
        private String label;
        private String subjectContains;
        private String fromAddress;

        public GmailConfigDto() {}

        public GmailConfigDto(String label, String subjectContains, String fromAddress) {
            this.label = label;
            this.subjectContains = subjectContains;
            this.fromAddress = fromAddress;
        }

        public String getLabel() {
            return label;
        }

        public void setLabel(String label) {
            this.label = label;
        }

        public String getSubjectContains() {
            return subjectContains;
        }

        public void setSubjectContains(String subjectContains) {
            this.subjectContains = subjectContains;
        }

        public String getFromAddress() {
            return fromAddress;
        }

        public void setFromAddress(String fromAddress) {
            this.fromAddress = fromAddress;
        }
    }

    public static class DiscordConfigDto {
        private String webhookUrl;
        private String channelName;
        private String messageTemplate;

        public DiscordConfigDto() {}

        public DiscordConfigDto(String webhookUrl, String channelName, String messageTemplate) {
            this.webhookUrl = webhookUrl;
            this.channelName = channelName;
            this.messageTemplate = messageTemplate;
        }

        public String getWebhookUrl() {
            return webhookUrl;
        }

        public void setWebhookUrl(String webhookUrl) {
            this.webhookUrl = webhookUrl;
        }

        public String getChannelName() {
            return channelName;
        }

        public void setChannelName(String channelName) {
            this.channelName = channelName;
        }

        public String getMessageTemplate() {
            return messageTemplate;
        }

        public void setMessageTemplate(String messageTemplate) {
            this.messageTemplate = messageTemplate;
        }
    }

    public static class TriggerStateDto {
        private Integer lastUnreadCount;
        private String lastProcessedMessageId;
        private Instant lastCheckedAt;
        private Instant lastTriggeredAt;
        private Integer consecutiveFailures;
        private String lastErrorMessage;

        public TriggerStateDto() {}

        public Integer getLastUnreadCount() {
            return lastUnreadCount;
        }

        public void setLastUnreadCount(Integer lastUnreadCount) {
            this.lastUnreadCount = lastUnreadCount;
        }

        public String getLastProcessedMessageId() {
            return lastProcessedMessageId;
        }

        public void setLastProcessedMessageId(String lastProcessedMessageId) {
            this.lastProcessedMessageId = lastProcessedMessageId;
        }

        public Instant getLastCheckedAt() {
            return lastCheckedAt;
        }

        public void setLastCheckedAt(Instant lastCheckedAt) {
            this.lastCheckedAt = lastCheckedAt;
        }

        public Instant getLastTriggeredAt() {
            return lastTriggeredAt;
        }

        public void setLastTriggeredAt(Instant lastTriggeredAt) {
            this.lastTriggeredAt = lastTriggeredAt;
        }

        public Integer getConsecutiveFailures() {
            return consecutiveFailures;
        }

        public void setConsecutiveFailures(Integer consecutiveFailures) {
            this.consecutiveFailures = consecutiveFailures;
        }

        public String getLastErrorMessage() {
            return lastErrorMessage;
        }

        public void setLastErrorMessage(String lastErrorMessage) {
            this.lastErrorMessage = lastErrorMessage;
        }
    }

    public AreaResponse() {}

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Long getActionConnectionId() {
        return actionConnectionId;
    }

    public void setActionConnectionId(Long actionConnectionId) {
        this.actionConnectionId = actionConnectionId;
    }

    public Long getReactionConnectionId() {
        return reactionConnectionId;
    }

    public void setReactionConnectionId(Long reactionConnectionId) {
        this.reactionConnectionId = reactionConnectionId;
    }

    public GmailConfigDto getGmailConfig() {
        return gmailConfig;
    }

    public void setGmailConfig(GmailConfigDto gmailConfig) {
        this.gmailConfig = gmailConfig;
    }

    public DiscordConfigDto getDiscordConfig() {
        return discordConfig;
    }

    public void setDiscordConfig(DiscordConfigDto discordConfig) {
        this.discordConfig = discordConfig;
    }

    public boolean isActive() {
        return active;
    }

    public void setActive(boolean active) {
        this.active = active;
    }

    public TriggerStateDto getTriggerState() {
        return triggerState;
    }

    public void setTriggerState(TriggerStateDto triggerState) {
        this.triggerState = triggerState;
    }
}
