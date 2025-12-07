package com.area.server.model;

import jakarta.persistence.*;

@Entity
@Table(name = "service_connections")
public class ServiceConnection {

    public enum ServiceType {
        GMAIL,
        DISCORD
    }

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 32)
    private ServiceType type;

    @Column(length = 2048)
    private String accessToken;

    @Column(length = 2048)
    private String refreshToken;

    private Long expiresInSeconds;

    @Column(name = "token_expires_at")
    private java.time.Instant tokenExpiresAt;

    @Column(name = "last_refresh_attempt")
    private java.time.Instant lastRefreshAttempt;

    @Column(length = 4096)
    private String metadata;

    public Long getId() {
        return id;
    }

    public ServiceType getType() {
        return type;
    }

    public void setType(ServiceType type) {
        this.type = type;
    }

    public String getAccessToken() {
        return accessToken;
    }

    public void setAccessToken(String accessToken) {
        this.accessToken = accessToken;
    }

    @SuppressWarnings("unused")
    public String getRefreshToken() {
        return refreshToken;
    }

    public void setRefreshToken(String refreshToken) {
        this.refreshToken = refreshToken;
    }

    @SuppressWarnings("unused")
    public Long getExpiresInSeconds() {
        return expiresInSeconds;
    }

    public void setExpiresInSeconds(Long expiresInSeconds) {
        this.expiresInSeconds = expiresInSeconds;
    }

    @SuppressWarnings("unused")
    public String getMetadata() {
        return metadata;
    }

    public void setMetadata(String metadata) {
        this.metadata = metadata;
    }

    public java.time.Instant getTokenExpiresAt() {
        return tokenExpiresAt;
    }

    public void setTokenExpiresAt(java.time.Instant tokenExpiresAt) {
        this.tokenExpiresAt = tokenExpiresAt;
    }

    public java.time.Instant getLastRefreshAttempt() {
        return lastRefreshAttempt;
    }

    public void setLastRefreshAttempt(java.time.Instant lastRefreshAttempt) {
        this.lastRefreshAttempt = lastRefreshAttempt;
    }

    public boolean needsRefresh() {
        if (tokenExpiresAt == null) {
            return false;
        }
        return java.time.Instant.now().plusSeconds(300).isAfter(tokenExpiresAt);
    }
}
