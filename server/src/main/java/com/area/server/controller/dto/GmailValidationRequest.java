package com.area.server.controller.dto;

import jakarta.validation.constraints.NotNull;

public class GmailValidationRequest {

    @NotNull
    private Long connectionId;

    private String label;
    private String subjectContains;
    private String fromAddress;

    public Long getConnectionId() {
        return connectionId;
    }

    @SuppressWarnings("unused")
    public void setConnectionId(Long connectionId) {
        this.connectionId = connectionId;
    }

    public String getLabel() {
        return label;
    }

    @SuppressWarnings("unused")
    public void setLabel(String label) {
        this.label = label;
    }

    public String getSubjectContains() {
        return subjectContains;
    }

    @SuppressWarnings("unused")
    public void setSubjectContains(String subjectContains) {
        this.subjectContains = subjectContains;
    }

    public String getFromAddress() {
        return fromAddress;
    }

    @SuppressWarnings("unused")
    public void setFromAddress(String fromAddress) {
        this.fromAddress = fromAddress;
    }
}
