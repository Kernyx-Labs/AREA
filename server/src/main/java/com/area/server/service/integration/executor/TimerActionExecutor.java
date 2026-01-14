package com.area.server.service.integration.executor;

import com.area.server.model.Area;
import com.area.server.model.AreaTriggerState;
import com.area.server.model.TimerActionConfig;
import com.area.server.service.TriggerStateService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;

import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.time.format.TextStyle;
import java.time.temporal.ChronoUnit;
import java.util.Locale;

/**
 * Executor for Timer actions.
 * Provides time-based triggers including current date, current time, and days until calculations.
 */
@Component
public class TimerActionExecutor implements ActionExecutor {

    private static final Logger logger = LoggerFactory.getLogger(TimerActionExecutor.class);
    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("dd/MM");
    private static final DateTimeFormatter TIME_FORMATTER = DateTimeFormatter.ofPattern("HH:mm");
    
    private final TriggerStateService stateService;

    public TimerActionExecutor(TriggerStateService stateService) {
        this.stateService = stateService;
    }

    @Override
    public String getActionType() {
        return "timer.*"; // Matches all timer actions
    }

    @Override
    public Mono<Boolean> isTriggered(Area area) {
        TimerActionConfig config = area.getTimerConfig();
        if (config == null) {
            logger.warn("Timer config is null for area {}", area.getId());
            return Mono.just(false);
        }

        AreaTriggerState state = stateService.getOrCreateState(area);
        Instant lastChecked = state.getLastCheckedAt();
        Instant now = Instant.now();
        
        // Default interval is 60 minutes if not specified
        int intervalMinutes = config.getIntervalMinutes() != null ? config.getIntervalMinutes() : 60;
        
        // For days_until type, default to 24 hours (1440 minutes) if not specified
        String timerType = config.getTimerType();
        if ("days_until".equals(timerType) && config.getIntervalMinutes() == null) {
            intervalMinutes = 1440; // 24 hours
        }
        
        // Check if enough time has passed since last check
        if (lastChecked != null) {
            long minutesSinceLastCheck = ChronoUnit.MINUTES.between(lastChecked, now);
            if (minutesSinceLastCheck < intervalMinutes) {
                logger.debug("Timer for area {} not triggered - only {} minutes since last check (interval: {})",
                    area.getId(), minutesSinceLastCheck, intervalMinutes);
                return Mono.just(false);
            }
        }
        
        logger.info("Timer triggered for area {} (type: {}, interval: {} minutes)", 
            area.getId(), timerType, intervalMinutes);
        return Mono.just(true);
    }

    @Override
    public Mono<TriggerContext> getTriggerContext(Area area) {
        TimerActionConfig config = area.getTimerConfig();
        if (config == null) {
            return Mono.just(new TriggerContext());
        }

        TriggerContext context = new TriggerContext();
        LocalDate today = LocalDate.now();
        LocalTime currentTime = LocalTime.now();
        Instant now = Instant.now();

        // Add current date and time to context
        String currentDate = today.format(DATE_FORMATTER);
        String currentTimeStr = currentTime.format(TIME_FORMATTER);
        
        context.put("date", currentDate);
        context.put("time", currentTimeStr);
        context.put("timestamp", now.toEpochMilli());
        context.put("triggered", true);

        // Add day of week
        String dayOfWeek = today.getDayOfWeek().getDisplayName(TextStyle.FULL, Locale.ENGLISH);
        context.put("dayOfWeek", dayOfWeek);

        // Add type-specific information
        String timerType = config.getTimerType();
        context.put("timerType", timerType != null ? timerType : "recurring");

        // For days_until type, calculate future date
        if ("days_until".equals(timerType) && config.getDaysCount() != null) {
            int daysCount = config.getDaysCount();
            LocalDate futureDate = today.plusDays(daysCount);
            String futureDayName = futureDate.getDayOfWeek().getDisplayName(TextStyle.FULL, Locale.ENGLISH);
            String futureFormattedDate = futureDate.format(DATE_FORMATTER);
            
            String message = String.format("In %d day%s, it will be %s (%s)", 
                daysCount, 
                daysCount == 1 ? "" : "s",
                futureDayName,
                futureFormattedDate);
            
            context.put("daysUntilMessage", message);
            context.put("daysCount", daysCount);
            context.put("futureDay", futureDayName);
            context.put("futureDate", futureFormattedDate);
        }

        // Add interval information
        if (config.getIntervalMinutes() != null) {
            context.put("intervalMinutes", config.getIntervalMinutes());
        }

        logger.debug("Timer context for area {}: date={}, time={}, type={}", 
            area.getId(), currentDate, currentTimeStr, timerType);

        return Mono.just(context);
    }

    /**
     * Check if this executor handles the given action type
     */
    public boolean handles(String actionType) {
        return actionType != null && actionType.startsWith("timer.");
    }
}

