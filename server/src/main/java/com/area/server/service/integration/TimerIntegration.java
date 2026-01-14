package com.area.server.service.integration;

import com.area.server.model.ServiceConnection;
import org.springframework.stereotype.Service;

import java.util.Collections;
import java.util.List;

/**
 * Timer integration service providing time-based triggers.
 * Implements the ServiceIntegration interface to enable auto-discovery.
 *
 * Timer service does not require authentication and provides actions (triggers)
 * based on time conditions like current date/time or recurring intervals.
 */
@Service
public class TimerIntegration implements ServiceIntegration {

    @Override
    public ServiceConnection.ServiceType getType() {
        return ServiceConnection.ServiceType.TIMER;
    }

    @Override
    public String getName() {
        return "Timer";
    }

    @Override
    public String getDescription() {
        return "Trigger actions based on time conditions - current date, current time, or recurring intervals";
    }

    @Override
    public List<ActionDefinition> getActions() {
        return List.of(
            new ActionDefinition(
                "timer.current_date",
                "Current Date (DD/MM)",
                "Triggers daily, providing the current date in DD/MM format",
                List.of(
                    new FieldDefinition(
                        "intervalMinutes",
                        "Check Interval (minutes)",
                        "number",
                        false,
                        "How often to check (default: 60 minutes)"
                    )
                )
            ),
            new ActionDefinition(
                "timer.current_time",
                "Current Time (HH:MM)",
                "Triggers at specified intervals, providing the current time in HH:MM format",
                List.of(
                    new FieldDefinition(
                        "intervalMinutes",
                        "Trigger Interval (minutes)",
                        "number",
                        true,
                        "How often to trigger (e.g., 5 for every 5 minutes)"
                    )
                )
            ),
            new ActionDefinition(
                "timer.days_until",
                "Days Until Date",
                "Triggers daily with 'In X days it will be Y' information",
                List.of(
                    new FieldDefinition(
                        "daysCount",
                        "Days Count",
                        "number",
                        true,
                        "Number of days to look ahead (e.g., 3)"
                    ),
                    new FieldDefinition(
                        "intervalMinutes",
                        "Check Interval (minutes)",
                        "number",
                        false,
                        "How often to check (default: 1440 minutes = 24 hours)"
                    )
                )
            ),
            new ActionDefinition(
                "timer.recurring",
                "Recurring Timer",
                "Triggers at a specified interval with timestamp information",
                List.of(
                    new FieldDefinition(
                        "intervalMinutes",
                        "Interval (minutes)",
                        "number",
                        true,
                        "Trigger every X minutes (e.g., 5 for every 5 minutes)"
                    )
                )
            )
        );
    }

    @Override
    public List<ReactionDefinition> getReactions() {
        // Timer only provides actions (triggers), not reactions
        return Collections.emptyList();
    }

    @Override
    public boolean requiresAuthentication() {
        return false; // Timer service does not require authentication
    }
}

