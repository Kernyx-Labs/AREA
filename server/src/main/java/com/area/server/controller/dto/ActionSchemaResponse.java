package com.area.server.controller.dto;

import java.util.List;

public record ActionSchemaResponse(
        String id,
        String name,
        String description,
        List<FieldSchemaResponse> fields
) {}

