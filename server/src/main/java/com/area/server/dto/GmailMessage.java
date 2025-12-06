package com.area.server.dto;

import java.time.Instant;

public class GmailMessage {

    private String id;
    private String threadId;
    private String subject;
    private String from;
    private String snippet;
    private Instant receivedAt;

    public GmailMessage() {
    }

    public GmailMessage(String id, String subject, String from, String snippet, Instant receivedAt) {
        this.id = id;
        this.subject = subject;
        this.from = from;
        this.snippet = snippet;
        this.receivedAt = receivedAt;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getThreadId() {
        return threadId;
    }

    public void setThreadId(String threadId) {
        this.threadId = threadId;
    }

    public String getSubject() {
        return subject;
    }

    public void setSubject(String subject) {
        this.subject = subject;
    }

    public String getFrom() {
        return from;
    }

    public void setFrom(String from) {
        this.from = from;
    }

    public String getSnippet() {
        return snippet;
    }

    public void setSnippet(String snippet) {
        this.snippet = snippet;
    }

    public Instant getReceivedAt() {
        return receivedAt;
    }

    public void setReceivedAt(Instant receivedAt) {
        this.receivedAt = receivedAt;
    }
}
