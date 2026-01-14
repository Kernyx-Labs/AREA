package com.area.server.controller.dto;

import java.time.Instant;

public class AreaResponse {

    private Long id;
    private Long actionConnectionId;
    private Long reactionConnectionId;
    private GmailConfigDto gmailConfig;
    private DiscordConfigDto discordConfig;
    private TimerConfigDto timerConfig;
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
        /**
         * @deprecated Use bot token from ServiceConnection instead.
         * This field is kept for backward compatibility.
         */
        @Deprecated(since = "2.0", forRemoval = true)
        private String webhookUrl;
        private String channelName;
        private String messageTemplate;

        public DiscordConfigDto() {}

        public DiscordConfigDto(String webhookUrl, String channelName, String messageTemplate) {
            this.webhookUrl = webhookUrl;
            this.channelName = channelName;
            this.messageTemplate = messageTemplate;
        }

        /**
         * @deprecated Use bot token from ServiceConnection instead.
         */
        @Deprecated(since = "2.0", forRemoval = true)
        public String getWebhookUrl() {
            return webhookUrl;
        }

        /**
         * @deprecated Use bot token from ServiceConnection instead.
         */
        @Deprecated(since = "2.0", forRemoval = true)
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

    public static class TimerConfigDto {
        private String timerType;
        private Integer intervalMinutes;
        private Integer daysCount;
        private String targetDay;

        public TimerConfigDto() {}

        public TimerConfigDto(String timerType, Integer intervalMinutes, Integer daysCount, String targetDay) {
            this.timerType = timerType;
            this.intervalMinutes = intervalMinutes;
            this.daysCount = daysCount;
            this.targetDay = targetDay;
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

    public TimerConfigDto getTimerConfig() {
        return timerConfig;
    }

    public void setTimerConfig(TimerConfigDto timerConfig) {
        this.timerConfig = timerConfig;
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
