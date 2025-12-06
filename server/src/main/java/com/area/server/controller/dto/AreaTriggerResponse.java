package com.area.server.controller.dto;

public record AreaTriggerResponse(
        Long areaId,
        int unreadCount,
        String message
) {}
