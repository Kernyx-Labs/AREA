package com.area.server.controller;

import com.area.server.controller.dto.CreateAreaRequest;
import com.area.server.model.Area;
import com.area.server.model.DiscordReactionConfig;
import com.area.server.model.GmailActionConfig;
import com.area.server.service.AreaService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/areas")
public class AreaController {

    private final AreaService areaService;

    public AreaController(AreaService areaService) {
        this.areaService = areaService;
    }

    @PostMapping
    public ResponseEntity<Area> create(@Valid @RequestBody CreateAreaRequest request) {
        GmailActionConfig gmailConfig = new GmailActionConfig();
        gmailConfig.setLabel(request.getGmailLabel());
        gmailConfig.setSubjectContains(request.getGmailSubjectContains());
        gmailConfig.setFromAddress(request.getGmailFromAddress());

        DiscordReactionConfig discordConfig = new DiscordReactionConfig();
        discordConfig.setWebhookUrl(request.getDiscordWebhookUrl());
        discordConfig.setChannelName(request.getDiscordChannelName());
        discordConfig.setMessageTemplate(request.getDiscordMessageTemplate());

        Area area = areaService.createArea(
                request.getActionConnectionId(),
                request.getReactionConnectionId(),
                gmailConfig,
                discordConfig
        );
        return ResponseEntity.ok(area);
    }

    @GetMapping
    public Iterable<Area> list() {
        return areaService.listAreas();
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        areaService.deleteArea(id);
        return ResponseEntity.noContent().build();
    }
}

