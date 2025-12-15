package com.area.server.controller;

import com.area.server.controller.dto.AreaResponse;
import com.area.server.controller.dto.CreateAreaRequest;
import com.area.server.controller.dto.ExecutionLogResponse;
import com.area.server.controller.dto.UpdateAreaStatusRequest;
import com.area.server.dto.response.ApiResponse;
import com.area.server.exception.ResourceNotFoundException;
import com.area.server.model.Area;
import com.area.server.model.AreaExecutionLog;
import com.area.server.model.AreaTriggerState;
import com.area.server.model.DiscordReactionConfig;
import com.area.server.model.GmailActionConfig;
import com.area.server.service.AreaService;
import jakarta.validation.Valid;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/areas")
public class AreaController {

    private static final Logger logger = LoggerFactory.getLogger(AreaController.class);

    private final AreaService areaService;

    public AreaController(AreaService areaService) {
        this.areaService = areaService;
    }

    /**
     * Create a new AREA (Action-REAction)
     * Uses GlobalExceptionHandler for error handling - no try-catch needed
     */
    @PostMapping
    public ResponseEntity<ApiResponse<AreaResponse>> createArea(@Valid @RequestBody CreateAreaRequest request) {
        logger.info("Creating new AREA: actionConnection={}, reactionConnection={}",
                   request.getActionConnectionId(), request.getReactionConnectionId());

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

        AreaResponse response = mapToAreaResponse(area);

        return ResponseEntity.status(HttpStatus.CREATED)
            .body(ApiResponse.success("AREA created successfully", response));
    }

    @GetMapping
    public ResponseEntity<Map<String, Object>> listAreas(
            @RequestParam(required = false, defaultValue = "false") boolean activeOnly) {
        try {
            List<Area> areas = activeOnly ? areaService.listActiveAreas() : areaService.listAllAreas();

            List<AreaResponse> areaResponses = areas.stream()
                .map(this::mapToAreaResponse)
                .collect(Collectors.toList());

            return ResponseEntity.ok(Map.of(
                "success", true,
                "count", areaResponses.size(),
                "areas", areaResponses
            ));

        } catch (Exception e) {
            logger.error("Error listing AREAs", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(Map.of(
                "success", false,
                "error", "Failed to list AREAs",
                "message", e.getMessage()
            ));
        }
    }

    /**
     * Get a specific AREA by ID
     * Throws ResourceNotFoundException if not found (handled by GlobalExceptionHandler)
     */
    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<AreaResponse>> getArea(@PathVariable Long id) {
        Area area = areaService.findById(id);
        AreaResponse response = mapToAreaResponse(area);

        return ResponseEntity.ok(ApiResponse.success(response));
    }

    /**
     * Delete an AREA by ID
     * Throws ResourceNotFoundException if not found (handled by GlobalExceptionHandler)
     */
    @DeleteMapping("/{id}")
    public ResponseEntity<ApiResponse<Void>> deleteArea(@PathVariable Long id) {
        logger.info("Deleting AREA with ID: {}", id);
        areaService.deleteArea(id);

        return ResponseEntity.ok(ApiResponse.success("AREA deleted successfully"));
    }

    @PutMapping("/{id}/status")
    public ResponseEntity<Map<String, Object>> updateAreaStatus(
            @PathVariable Long id,
            @Valid @RequestBody UpdateAreaStatusRequest request) {
        try {
            logger.info("Updating AREA {} status to: {}", id, request.getActive());

            Area area = areaService.updateAreaStatus(id, request.getActive());
            AreaResponse response = mapToAreaResponse(area);

            return ResponseEntity.ok(Map.of(
                "success", true,
                "message", "AREA status updated successfully",
                "area", response
            ));

        } catch (IllegalArgumentException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(Map.of(
                "success", false,
                "error", "AREA not found",
                "message", e.getMessage()
            ));
        } catch (Exception e) {
            logger.error("Error updating AREA {} status", id, e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(Map.of(
                "success", false,
                "error", "Failed to update AREA status",
                "message", e.getMessage()
            ));
        }
    }

    @GetMapping("/{id}/logs")
    public ResponseEntity<Map<String, Object>> getExecutionLogs(
            @PathVariable Long id,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(defaultValue = "executedAt") String sortBy,
            @RequestParam(defaultValue = "DESC") String sortDirection) {
        try {
            // Validate and limit page size
            if (size > 100) {
                size = 100;
            }
            if (size < 1) {
                size = 20;
            }

            Sort.Direction direction = sortDirection.equalsIgnoreCase("ASC")
                ? Sort.Direction.ASC
                : Sort.Direction.DESC;

            Pageable pageable = PageRequest.of(page, size, Sort.by(direction, sortBy));

            Page<AreaExecutionLog> logsPage = areaService.getExecutionLogs(id, pageable);

            List<ExecutionLogResponse> logResponses = logsPage.getContent().stream()
                .map(this::mapToExecutionLogResponse)
                .collect(Collectors.toList());

            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("logs", logResponses);
            response.put("pagination", Map.of(
                "currentPage", logsPage.getNumber(),
                "totalPages", logsPage.getTotalPages(),
                "totalElements", logsPage.getTotalElements(),
                "pageSize", logsPage.getSize(),
                "hasNext", logsPage.hasNext(),
                "hasPrevious", logsPage.hasPrevious()
            ));

            return ResponseEntity.ok(response);

        } catch (IllegalArgumentException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(Map.of(
                "success", false,
                "error", "AREA not found",
                "message", e.getMessage()
            ));
        } catch (Exception e) {
            logger.error("Error getting execution logs for AREA {}", id, e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(Map.of(
                "success", false,
                "error", "Failed to get execution logs",
                "message", e.getMessage()
            ));
        }
    }

    @GetMapping("/{id}/state")
    public ResponseEntity<Map<String, Object>> getTriggerState(@PathVariable Long id) {
        try {
            AreaTriggerState state = areaService.getTriggerState(id);

            if (state == null) {
                return ResponseEntity.ok(Map.of(
                    "success", true,
                    "message", "No trigger state found (AREA has not been triggered yet)",
                    "state", Map.of()
                ));
            }

            AreaResponse.TriggerStateDto stateDto = new AreaResponse.TriggerStateDto();
            stateDto.setLastUnreadCount(state.getLastUnreadCount());
            stateDto.setLastProcessedMessageId(state.getLastProcessedMessageId());
            stateDto.setLastCheckedAt(state.getLastCheckedAt());
            stateDto.setLastTriggeredAt(state.getLastTriggeredAt());
            stateDto.setConsecutiveFailures(state.getConsecutiveFailures());
            stateDto.setLastErrorMessage(state.getLastErrorMessage());

            return ResponseEntity.ok(Map.of(
                "success", true,
                "state", stateDto
            ));

        } catch (IllegalArgumentException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(Map.of(
                "success", false,
                "error", "AREA not found",
                "message", e.getMessage()
            ));
        } catch (Exception e) {
            logger.error("Error getting trigger state for AREA {}", id, e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(Map.of(
                "success", false,
                "error", "Failed to get trigger state",
                "message", e.getMessage()
            ));
        }
    }

    private AreaResponse mapToAreaResponse(Area area) {
        AreaResponse response = new AreaResponse();
        response.setId(area.getId());
        response.setActionConnectionId(area.getActionConnection().getId());
        response.setReactionConnectionId(area.getReactionConnection().getId());
        response.setActive(area.isActive());

        // Map Gmail config
        if (area.getGmailConfig() != null) {
            AreaResponse.GmailConfigDto gmailDto = new AreaResponse.GmailConfigDto(
                area.getGmailConfig().getLabel(),
                area.getGmailConfig().getSubjectContains(),
                area.getGmailConfig().getFromAddress()
            );
            response.setGmailConfig(gmailDto);
        }

        // Map Discord config
        if (area.getDiscordConfig() != null) {
            AreaResponse.DiscordConfigDto discordDto = new AreaResponse.DiscordConfigDto(
                area.getDiscordConfig().getWebhookUrl(),
                area.getDiscordConfig().getChannelName(),
                area.getDiscordConfig().getMessageTemplate()
            );
            response.setDiscordConfig(discordDto);
        }

        // Map trigger state if available
        AreaTriggerState state = areaService.getTriggerState(area.getId());
        if (state != null) {
            AreaResponse.TriggerStateDto stateDto = new AreaResponse.TriggerStateDto();
            stateDto.setLastUnreadCount(state.getLastUnreadCount());
            stateDto.setLastProcessedMessageId(state.getLastProcessedMessageId());
            stateDto.setLastCheckedAt(state.getLastCheckedAt());
            stateDto.setLastTriggeredAt(state.getLastTriggeredAt());
            stateDto.setConsecutiveFailures(state.getConsecutiveFailures());
            stateDto.setLastErrorMessage(state.getLastErrorMessage());
            response.setTriggerState(stateDto);
        }

        return response;
    }

    private ExecutionLogResponse mapToExecutionLogResponse(AreaExecutionLog log) {
        ExecutionLogResponse response = new ExecutionLogResponse();
        response.setId(log.getId());
        response.setAreaId(log.getArea().getId());
        response.setExecutedAt(log.getExecutedAt());
        response.setStatus(log.getStatus().name());
        response.setUnreadCount(log.getUnreadCount());
        response.setMessageSent(log.getMessageSent());
        response.setErrorMessage(log.getErrorMessage());
        response.setExecutionTimeMs(log.getExecutionTimeMs());
        return response;
    }
}


