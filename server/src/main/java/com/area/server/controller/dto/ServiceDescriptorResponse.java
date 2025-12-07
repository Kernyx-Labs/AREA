package com.area.server.controller.dto;

import java.util.List;

public record ServiceDescriptorResponse(
        String id,
        String name,
        String description,
        List<ActionSchemaResponse> actions,
        List<ReactionSchemaResponse> reactions
) {}

