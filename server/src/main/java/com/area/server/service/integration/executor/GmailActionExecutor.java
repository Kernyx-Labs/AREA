package com.area.server.service.integration.executor;

import com.area.server.dto.GmailMessage;
import com.area.server.model.Area;
import com.area.server.model.AreaTriggerState;
import com.area.server.model.AutomationEntity;
import com.area.server.model.WorkflowTriggerState;
import com.area.server.scheduler.WorkflowWrapper;
import com.area.server.service.GmailService;
import com.area.server.service.TokenRefreshService;
import com.area.server.service.TriggerStateService;
import com.area.server.service.WorkflowTriggerStateService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;

import java.util.List;

/**
 * Executor for Gmail "email_received" action.
 * Checks for new emails matching configured filters.
 */
@Component
public class GmailActionExecutor implements ActionExecutor {

    private static final Logger logger = LoggerFactory.getLogger(GmailActionExecutor.class);

    private final GmailService gmailService;
    private final TriggerStateService areaStateService;
    private final TokenRefreshService tokenRefreshService;
    private final WorkflowTriggerStateService workflowStateService;

    public GmailActionExecutor(GmailService gmailService,
            TriggerStateService areaStateService,
            TokenRefreshService tokenRefreshService,
            WorkflowTriggerStateService workflowStateService) {
        this.gmailService = gmailService;
        this.areaStateService = areaStateService;
        this.tokenRefreshService = tokenRefreshService;
        this.workflowStateService = workflowStateService;
    }

    @Override
    public String getActionType() {
        return "gmail.email_received";
    }

    @Override
    public Mono<Boolean> isTriggered(AutomationEntity entity) {
        return getTriggerContext(entity)
                .map(context -> context.has("newMessages") &&
                        context.getInteger("messageCount") != null &&
                        context.getInteger("messageCount") > 0);
    }

    @Override
    public Mono<TriggerContext> getTriggerContext(AutomationEntity entity) {
        String lastProcessedId = getLastProcessedId(entity);

        return tokenRefreshService.refreshTokenIfNeeded(entity.getActionConnection())
                .flatMap(connection -> gmailService.fetchNewMessages(
                        connection,
                        entity.getGmailConfig(),
                        lastProcessedId))
                .map(newMessages -> {
                    TriggerContext context = new TriggerContext();
                    context.put("newMessages", newMessages);
                    context.put("messageCount", newMessages.size());

                    if (!newMessages.isEmpty()) {
                        GmailMessage latest = newMessages.get(0);
                        context.put("latestMessage", latest);
                        context.put("subject", latest.getSubject());
                        context.put("from", latest.getFrom());
                        context.put("snippet", latest.getSnippet());
                        context.put("messageId", latest.getId());

                        // Update state
                        updateState(entity, latest.getId());
                    }

                    return context;
                })
                .doOnSuccess(context -> {
                    if (context.getInteger("messageCount") != null && context.getInteger("messageCount") > 0) {
                        logger.info("Gmail action triggered for entity {} with {} new message(s)",
                                entity.getId(), context.getInteger("messageCount"));
                    }
                })
                .onErrorResume(error -> {
                    logger.error("Error executing Gmail action for entity {}: {}",
                            entity.getId(), error.getMessage());
                    return Mono.just(new TriggerContext());
                });
    }

    private String getLastProcessedId(AutomationEntity entity) {
        if (entity instanceof Area area) {
            AreaTriggerState state = areaStateService.getOrCreateState(area);
            return state.getLastProcessedMessageId();
        } else if (entity instanceof WorkflowWrapper wrapper) {
            WorkflowTriggerState state = workflowStateService.getOrCreateState(wrapper.getWorkflow());
            return state.getLastProcessedItemId();
        }
        return null;
    }

    private void updateState(AutomationEntity entity, String lastItemId) {
        if (entity instanceof Area area) {
            AreaTriggerState state = areaStateService.getOrCreateState(area);
            state.setLastProcessedMessageId(lastItemId);
            areaStateService.update(state);
        } else if (entity instanceof WorkflowWrapper wrapper) {
            WorkflowTriggerState state = workflowStateService.getOrCreateState(wrapper.getWorkflow());
            state.setLastProcessedItemId(lastItemId);
            workflowStateService.update(state);
        }
    }
}
