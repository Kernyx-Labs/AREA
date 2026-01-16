package com.area.server.controller.dto;

import com.area.server.service.integration.FieldDefinition;

import java.util.List;
import java.util.Map;

public record FieldSchemaResponse(
        String name,
        String label,
        String type,
        boolean required,
        String description,
        Object defaultValue,
        List<FieldDefinition.SelectOption> options,
        Map<String, Object> metadata
) {
    /**
     * Factory method to convert from FieldDefinition
     */
    public static FieldSchemaResponse from(FieldDefinition field) {
        return new FieldSchemaResponse(
            field.getName(),
            field.getLabel(),
            field.getType(),
            field.isRequired(),
            field.getDescription(),
            field.getDefaultValue(),
            field.getOptions(),
            field.getMetadata()
        );
    }
}

