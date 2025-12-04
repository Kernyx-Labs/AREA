package com.area.server.service;

import com.area.server.model.Area;
import com.area.server.model.DiscordReactionConfig;
import com.area.server.model.GmailActionConfig;
import com.area.server.model.ServiceConnection;
import com.area.server.repository.AreaRepository;
import org.springframework.stereotype.Service;

import java.time.Instant;

@Service
public class AreaService {

    private final AreaRepository areaRepository;
    private final ServiceConnectionService connectionService;

    public AreaService(AreaRepository areaRepository, ServiceConnectionService connectionService) {
        this.areaRepository = areaRepository;
        this.connectionService = connectionService;
    }

    public Area createArea(Long actionConnectionId,
                           Long reactionConnectionId,
                           GmailActionConfig gmailConfig,
                           DiscordReactionConfig discordConfig) {
        ServiceConnection actionConnection = connectionService.findById(actionConnectionId);
        ServiceConnection reactionConnection = connectionService.findById(reactionConnectionId);

        if (actionConnection.getType() != ServiceConnection.ServiceType.GMAIL) {
            throw new IllegalArgumentException("Action connection must be of type GMAIL");
        }
        if (reactionConnection.getType() != ServiceConnection.ServiceType.DISCORD) {
            throw new IllegalArgumentException("Reaction connection must be of type DISCORD");
        }

        Area area = new Area();
        area.setActionConnection(actionConnection);
        area.setReactionConnection(reactionConnection);
        area.setGmailConfig(gmailConfig);
        area.setDiscordConfig(discordConfig);
        return areaRepository.save(area);
    }

    public Iterable<Area> listAreas() {
        return areaRepository.findAll();
    }

    public void deleteArea(Long id) {
        areaRepository.deleteById(id);
    }

    public Area findById(Long id) {
        return areaRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Area not found: " + id));
    }

    public String formatDiscordMessage(Area area, int unreadCount) {
        String template = area.getDiscordConfig().getMessageTemplate();
        if (template == null || template.isBlank()) {
            return "You have " + unreadCount + " unread Gmail messages matching the AREA filters.";
        }
        return template
                .replace("{{unreadCount}}", String.valueOf(unreadCount))
                .replace("{{timestamp}}", Instant.now().toString());
    }
}
