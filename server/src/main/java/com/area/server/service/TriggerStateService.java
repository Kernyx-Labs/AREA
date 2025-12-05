package com.area.server.service;

import com.area.server.dto.GmailMessage;
import com.area.server.model.Area;
import com.area.server.model.AreaTriggerState;
import com.area.server.repository.AreaTriggerStateRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;

@Service
public class TriggerStateService {

    private static final Logger logger = LoggerFactory.getLogger(TriggerStateService.class);
    private static final int MAX_CONSECUTIVE_FAILURES = 5;

    private final AreaTriggerStateRepository stateRepository;

    public TriggerStateService(AreaTriggerStateRepository stateRepository) {
        this.stateRepository = stateRepository;
    }

    @Transactional
    public AreaTriggerState getOrCreateState(Area area) {
        return stateRepository.findByAreaId(area.getId())
            .orElseGet(() -> {
                AreaTriggerState state = new AreaTriggerState();
                state.setArea(area);
                state.setLastUnreadCount(0);
                state.setConsecutiveFailures(0);
                logger.info("Created new trigger state for area {}", area.getId());
                return stateRepository.save(state);
            });
    }

    public boolean shouldTrigger(Area area, List<GmailMessage> newMessages) {
        if (newMessages == null || newMessages.isEmpty()) {
            return false;
        }

        AreaTriggerState state = getOrCreateState(area);

        // If we've never triggered, trigger on any unread messages
        if (state.getLastProcessedMessageId() == null) {
            logger.info("Area {} has never triggered before, will trigger on {} messages",
                       area.getId(), newMessages.size());
            return true;
        }

        // Check if there are messages newer than last processed
        String lastProcessedId = state.getLastProcessedMessageId();
        boolean hasNewMessages = newMessages.stream()
            .anyMatch(msg -> msg.getId().compareTo(lastProcessedId) > 0);

        if (hasNewMessages) {
            logger.info("Area {} has new messages since last trigger (last: {})",
                       area.getId(), lastProcessedId);
        }

        return hasNewMessages;
    }

    @Transactional
    public void updateStateAfterSuccess(Area area, GmailMessage latestMessage, int unreadCount) {
        AreaTriggerState state = getOrCreateState(area);
        state.setLastProcessedMessageId(latestMessage.getId());
        state.setLastUnreadCount(unreadCount);
        state.setLastCheckedAt(Instant.now());
        state.setLastTriggeredAt(Instant.now());
        state.setConsecutiveFailures(0);
        state.setLastErrorMessage(null);
        stateRepository.save(state);

        logger.info("Updated trigger state for area {} - last message: {}, count: {}",
                   area.getId(), latestMessage.getId(), unreadCount);
    }

    @Transactional
    public void updateCheckedTime(Area area) {
        AreaTriggerState state = getOrCreateState(area);
        state.setLastCheckedAt(Instant.now());
        stateRepository.save(state);
    }

    @Transactional
    public void recordFailure(Area area, String errorMessage) {
        AreaTriggerState state = getOrCreateState(area);
        state.setConsecutiveFailures(state.getConsecutiveFailures() + 1);
        state.setLastCheckedAt(Instant.now());
        state.setLastErrorMessage(
            errorMessage != null && errorMessage.length() > 1000
                ? errorMessage.substring(0, 1000)
                : errorMessage
        );
        stateRepository.save(state);

        logger.warn("Recorded failure for area {} (consecutive: {}): {}",
                   area.getId(), state.getConsecutiveFailures(), errorMessage);
    }

    public boolean shouldSkipDueToFailures(Area area) {
        AreaTriggerState state = getOrCreateState(area);
        boolean skip = state.getConsecutiveFailures() >= MAX_CONSECUTIVE_FAILURES;

        if (skip) {
            logger.warn("Circuit breaker OPEN for area {} - {} consecutive failures",
                       area.getId(), state.getConsecutiveFailures());
        }

        return skip;
    }

    @Transactional
    public void resetFailureCount(Area area) {
        AreaTriggerState state = getOrCreateState(area);
        state.setConsecutiveFailures(0);
        state.setLastErrorMessage(null);
        stateRepository.save(state);
        logger.info("Reset failure count for area {}", area.getId());
    }
}
