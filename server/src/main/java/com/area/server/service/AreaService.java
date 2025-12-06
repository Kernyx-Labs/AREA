package com.area.server.service;

import com.area.server.model.Area;
import com.area.server.model.AreaExecutionLog;
import com.area.server.model.AreaTriggerState;
import com.area.server.model.DiscordReactionConfig;
import com.area.server.model.GmailActionConfig;
import com.area.server.model.ServiceConnection;
import com.area.server.repository.AreaExecutionLogRepository;
import com.area.server.repository.AreaRepository;
import com.area.server.repository.AreaTriggerStateRepository;
import com.area.server.util.DiscordWebhookValidator;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;

@Service
public class AreaService {

    private static final Logger logger = LoggerFactory.getLogger(AreaService.class);

    private final AreaRepository areaRepository;
    private final ServiceConnectionService connectionService;
    private final AreaExecutionLogRepository executionLogRepository;
    private final AreaTriggerStateRepository triggerStateRepository;

    public AreaService(AreaRepository areaRepository,
                       ServiceConnectionService connectionService,
                       AreaExecutionLogRepository executionLogRepository,
                       AreaTriggerStateRepository triggerStateRepository) {
        this.areaRepository = areaRepository;
        this.connectionService = connectionService;
        this.executionLogRepository = executionLogRepository;
        this.triggerStateRepository = triggerStateRepository;
    }

    @Transactional
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

        // Validate Discord webhook URL
        DiscordWebhookValidator.validateWebhookUrl(discordConfig.getWebhookUrl());

        Area area = new Area();
        area.setActionConnection(actionConnection);
        area.setReactionConnection(reactionConnection);
        area.setGmailConfig(gmailConfig);
        area.setDiscordConfig(discordConfig);
        area.setActive(true);

        Area savedArea = areaRepository.save(area);
        logger.info("Created new AREA with ID: {}", savedArea.getId());

        return savedArea;
    }

    public List<Area> listAllAreas() {
        return areaRepository.findAll();
    }

    public List<Area> listActiveAreas() {
        return areaRepository.findByActiveTrue();
    }

    @Transactional
    public void deleteArea(Long id) {
        Area area = findById(id);

        // Delete associated trigger state
        triggerStateRepository.findByAreaId(id).ifPresent(triggerStateRepository::delete);

        // Delete associated execution logs
        executionLogRepository.deleteByAreaId(id);

        // Delete the area itself
        areaRepository.deleteById(id);

        logger.info("Deleted AREA with ID: {} and all associated data", id);
    }

    public Area findById(Long id) {
        return areaRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Area not found: " + id));
    }

    @Transactional
    public Area updateAreaStatus(Long id, boolean active) {
        Area area = findById(id);
        area.setActive(active);
        Area savedArea = areaRepository.save(area);

        logger.info("Updated AREA {} status to: {}", id, active ? "ACTIVE" : "INACTIVE");

        return savedArea;
    }

    public Page<AreaExecutionLog> getExecutionLogs(Long areaId, Pageable pageable) {
        // Verify area exists
        findById(areaId);
        return executionLogRepository.findByAreaId(areaId, pageable);
    }

    public AreaTriggerState getTriggerState(Long areaId) {
        // Verify area exists
        findById(areaId);
        return triggerStateRepository.findByAreaId(areaId).orElse(null);
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
