package com.area.server.service.integration.executor;

import com.area.server.dto.GmailMessage;
import com.area.server.model.Area;
import com.area.server.model.AreaTriggerState;
import com.area.server.service.GmailService;
import com.area.server.service.TokenRefreshService;
import com.area.server.service.TriggerStateService;
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
    private final TriggerStateService stateService;
    private final TokenRefreshService tokenRefreshService;

    public GmailActionExecutor(GmailService gmailService,
                               TriggerStateService stateService,
                               TokenRefreshService tokenRefreshService) {
        this.gmailService = gmailService;
        this.stateService = stateService;
        this.tokenRefreshService = tokenRefreshService;
    }

    @Override
    public String getActionType() {
        return "gmail.email_received";
    }

    @Override
    public Mono<Boolean> isTriggered(Area area) {
        return getTriggerContext(area)
            .map(context -> context.has("newMessages") && 
                           context.getInteger("messageCount") != null && 
                           context.getInteger("messageCount") > 0);
    }

    @Override
    public Mono<TriggerContext> getTriggerContext(Area area) {
        AreaTriggerState state = stateService.getOrCreateState(area);

        return tokenRefreshService.refreshTokenIfNeeded(area.getActionConnection())
            .flatMap(connection -> gmailService.fetchNewMessages(
                connection,
                area.getGmailConfig(),
                state.getLastProcessedMessageId()
            ))
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
                }
                
                return context;
            })
            .doOnSuccess(context -> {
                if (context.getInteger("messageCount") > 0) {
                    logger.info("Gmail action triggered for area {} with {} new message(s)",
                               area.getId(), context.getInteger("messageCount"));
                }
            })
            .onErrorResume(error -> {
                logger.error("Error executing Gmail action for area {}: {}",
                           area.getId(), error.getMessage());
                return Mono.just(new TriggerContext());
            });
    }
}
