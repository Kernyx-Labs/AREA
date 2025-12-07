package com.area.server.controller.dto;

import jakarta.validation.constraints.NotNull;

public class UpdateAreaStatusRequest {

    @NotNull
    private Boolean active;

    public UpdateAreaStatusRequest() {}

    public Boolean getActive() {
        return active;
    }

    public void setActive(Boolean active) {
        this.active = active;
    }
}
