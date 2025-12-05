package com.area.server.model;

import jakarta.persistence.*;
import java.time.Instant;

@Entity
@Table(name = "area_trigger_states")
public class AreaTriggerState {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @OneToOne
    @JoinColumn(name = "area_id", nullable = false, unique = true)
    private Area area;

    @Column(name = "last_unread_count")
    private Integer lastUnreadCount;

    @Column(name = "last_processed_message_id", length = 256)
    private String lastProcessedMessageId;

    @Column(name = "last_checked_at")
    private Instant lastCheckedAt;

    @Column(name = "last_triggered_at")
    private Instant lastTriggeredAt;

    @Column(name = "consecutive_failures")
    private Integer consecutiveFailures = 0;

    @Column(name = "last_error_message", length = 1024)
    private String lastErrorMessage;

    public Long getId() {
        return id;
    }

    public Area getArea() {
        return area;
    }

    public void setArea(Area area) {
        this.area = area;
    }

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
