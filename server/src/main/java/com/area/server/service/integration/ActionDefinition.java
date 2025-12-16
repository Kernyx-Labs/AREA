package com.area.server.service.integration;

import java.util.List;

/**
 * Defines an action (trigger) that a service integration can provide.
 * Actions are events that can trigger workflows (e.g., "New email received").
 */
public class ActionDefinition {
    private String id;
    private String name;
    private String description;
    private List<FieldDefinition> configFields;

    public ActionDefinition() {
    }

    public ActionDefinition(String id, String name, String description, List<FieldDefinition> configFields) {
        this.id = id;
        this.name = name;
        this.description = description;
        this.configFields = configFields;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public List<FieldDefinition> getConfigFields() {
        return configFields;
    }

    public void setConfigFields(List<FieldDefinition> configFields) {
        this.configFields = configFields;
    }
}
