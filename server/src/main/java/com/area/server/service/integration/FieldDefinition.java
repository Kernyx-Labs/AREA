package com.area.server.service.integration;

import java.util.List;
import java.util.Map;

/**
 * Defines a configuration field for an action or reaction.
 * Describes the field type, validation rules, and user-facing metadata.
 */
public class FieldDefinition {
    private String name;
    private String label;
    private String type; // "string", "number", "boolean", "text", "url", "select", etc.
    private boolean required;
    private String description;
    private Object defaultValue;
    private List<SelectOption> options; // For select fields with static options
    private Map<String, Object> metadata; // For dynamic options and other field metadata

    public FieldDefinition() {
    }

    public FieldDefinition(String name, String label, String type, boolean required, String description) {
        this.name = name;
        this.label = label;
        this.type = type;
        this.required = required;
        this.description = description;
    }

    public FieldDefinition(String name, String label, String type, boolean required, String description, Map<String, Object> metadata) {
        this.name = name;
        this.label = label;
        this.type = type;
        this.required = required;
        this.description = description;
        this.metadata = metadata;
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

    public List<SelectOption> getOptions() {
        return options;
    }

    public void setOptions(List<SelectOption> options) {
        this.options = options;
    }

    public Map<String, Object> getMetadata() {
        return metadata;
    }

    public void setMetadata(Map<String, Object> metadata) {
        this.metadata = metadata;
    }

    /**
     * Represents a single option in a select field
     */
    public static class SelectOption {
        private String value;
        private String label;

        public SelectOption() {
        }

        public SelectOption(String value, String label) {
            this.value = value;
            this.label = label;
        }

        public String getValue() {
            return value;
        }

        public void setValue(String value) {
            this.value = value;
        }

        public String getLabel() {
            return label;
        }

        public void setLabel(String label) {
            this.label = label;
        }
    }
}
