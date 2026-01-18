package com.area.server.model;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;

@Embeddable
public class TimerActionConfig {

    @Column(name = "timer_type")
    private String timerType; // "current_date", "current_time", "days_until"

    @Column(name = "timer_interval_minutes")
    private Integer intervalMinutes; // For recurring triggers (e.g., every 5 minutes)

    @Column(name = "timer_target_day")
    private String targetDay; // For "days_until" type (e.g., "Friday")

    @Column(name = "timer_days_count")
    private Integer daysCount; // For "days_until" type (e.g., 3)

    public String getTimerType() {
        return timerType;
    }

    public void setTimerType(String timerType) {
        this.timerType = timerType;
    }

    public Integer getIntervalMinutes() {
        return intervalMinutes;
    }

    public void setIntervalMinutes(Integer intervalMinutes) {
        this.intervalMinutes = intervalMinutes;
    }

    public String getTargetDay() {
        return targetDay;
    }

    public void setTargetDay(String targetDay) {
        this.targetDay = targetDay;
    }

    public Integer getDaysCount() {
        return daysCount;
    }

    public void setDaysCount(Integer daysCount) {
        this.daysCount = daysCount;
    }
}

