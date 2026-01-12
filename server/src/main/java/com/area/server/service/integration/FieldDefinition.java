package com.area.server.service.integration;

/**
 * Defines a configuration field for an action or reaction.
 * Describes the field type, validation rules, and user-facing metadata.
 */
public class FieldDefinition {
    private String name;
    private String label;
    private String type; // "string", "number", "boolean", "text", "url", etc.
    private boolean required;
    private String description;
    private Object defaultValue;

    public FieldDefinition() {
    }

    public FieldDefinition(String name, String label, String type, boolean required, String description) {
        this.name = name;
        this.label = label;
        this.type = type;
        this.required = required;
        this.description = description;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getLabel() {
        return label;
    }

    public void setLabel(String label) {
        this.label = label;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public boolean isRequired() {
        return required;
    }

    public void setRequired(boolean required) {
        this.required = required;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public Object getDefaultValue() {
        return defaultValue;
    }

    public void setDefaultValue(Object defaultValue) {
        this.defaultValue = defaultValue;
    }
}
