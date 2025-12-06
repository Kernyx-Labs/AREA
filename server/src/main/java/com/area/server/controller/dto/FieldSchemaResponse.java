package com.area.server.controller.dto;

public record FieldSchemaResponse(
        String name,
        String label,
        String type,
        boolean required,
        String description
) {}

