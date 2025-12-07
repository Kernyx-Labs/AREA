package com.area.server.model;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;

@Embeddable
public class GmailActionConfig {

    @Column(name = "gmail_label")
    private String label;

    @Column(name = "gmail_subject_contains")
    private String subjectContains;

    @Column(name = "gmail_from_address")
    private String fromAddress;

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
