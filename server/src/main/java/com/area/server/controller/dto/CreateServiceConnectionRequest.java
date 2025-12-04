package com.area.server.controller.dto;

import com.area.server.model.ServiceConnection;
import jakarta.validation.constraints.NotNull;

public class CreateServiceConnectionRequest {

    @NotNull
    private ServiceConnection.ServiceType type;

    @NotNull
    private String accessToken;

    private String refreshToken;

    private Long expiresInSeconds;

    private String metadata;

    public ServiceConnection.ServiceType getType() {
        return type;
    }

    @SuppressWarnings("unused")
    public void setType(ServiceConnection.ServiceType type) {
        this.type = type;
    }

    public String getAccessToken() {
        return accessToken;
    }

    @SuppressWarnings("unused")
    public void setAccessToken(String accessToken) {
        this.accessToken = accessToken;
    }

    public String getRefreshToken() {
        return refreshToken;
    }

    @SuppressWarnings("unused")
    public void setRefreshToken(String refreshToken) {
        this.refreshToken = refreshToken;
    }

    public Long getExpiresInSeconds() {
        return expiresInSeconds;
    }

    @SuppressWarnings("unused")
    public void setExpiresInSeconds(Long expiresInSeconds) {
        this.expiresInSeconds = expiresInSeconds;
    }

    public String getMetadata() {
        return metadata;
    }

    @SuppressWarnings("unused")
    public void setMetadata(String metadata) {
        this.metadata = metadata;
    }
}
