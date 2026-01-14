# Timer Service - Quick Reference

## Service Configuration

### Service Type
- **Type:** `TIMER`
- **Authentication Required:** No
- **Actions:** 4 types (triggers)
- **Reactions:** None

### Important Notes
- **No Service Connection Required:** Timer is time-based and doesn't need a ServiceConnection. Set `timerConnectionId` to `null` when creating areas.
- **Dedicated Endpoint:** Use `POST /api/areas/timer` (not the generic `/api/areas` endpoint) to create Timer areas.
- **actionConnectionId is null:** Timer areas have `actionConnectionId = null` in responses because no connection is required.
- **Wildcard Executor Matching:** The TimerActionExecutor uses `"timer.*"` to match all timer action types (current_date, current_time, days_until, recurring).

## Action Types

### 1. timer.current_date
**Description:** Triggers with current date in DD/MM format

**Config Fields:**
- `intervalMinutes` (optional, default: 60): Check interval in minutes

**Context Variables:**
- `date`: Current date (DD/MM)
- `time`: Current time (HH:MM)
- `timestamp`: Unix timestamp
- `dayOfWeek`: Day name (e.g., "Monday")

### 2. timer.current_time
**Description:** Triggers at specified intervals with current time

**Config Fields:**
- `intervalMinutes` (required): Trigger interval (e.g., 5 for every 5 minutes)

**Context Variables:**
- `date`: Current date (DD/MM)
- `time`: Current time (HH:MM)
- `timestamp`: Unix timestamp
- `dayOfWeek`: Day name

### 3. timer.days_until
**Description:** Triggers with "In X days it will be Y" information

**Config Fields:**
- `daysCount` (required): Number of days to look ahead
- `intervalMinutes` (optional, default: 1440): Check interval in minutes

**Context Variables:**
- `date`: Current date (DD/MM)
- `time`: Current time (HH:MM)
- `timestamp`: Unix timestamp
- `dayOfWeek`: Current day name
- `daysUntilMessage`: Full message (e.g., "In 3 days, it will be Friday (15/01)")
- `daysCount`: Number of days
- `futureDay`: Future day name
- `futureDate`: Future date (DD/MM)

### 4. timer.recurring
**Description:** General-purpose recurring timer

**Config Fields:**
- `intervalMinutes` (required): Trigger interval

**Context Variables:**
- `date`: Current date (DD/MM)
- `time`: Current time (HH:MM)
- `timestamp`: Unix timestamp
- `dayOfWeek`: Day name

## Usage Examples

### Backend - Creating Timer Area

```java
// Create timer configuration
TimerActionConfig timerConfig = new TimerActionConfig();
timerConfig.setTimerType("recurring");
timerConfig.setIntervalMinutes(5);

// Create area
Area area = new Area();
area.setActionType("timer.recurring");
area.setTimerConfig(timerConfig);
area.setActive(true);
```

### Frontend - API Calls

```javascript
// Get Timer service info
const response = await fetch('/api/service-registry/TIMER');
const timerService = await response.json();

// Create area with timer trigger
const timerAreaData = {
  timerConnectionId: null,  // Timer doesn't require connection (time-based)
  reactionConnectionId: discordConnectionId,
  timerType: "recurring",
  intervalMinutes: 5,
  daysCount: null,
  targetDay: null,
  discordChannelName: "general",
  discordMessageTemplate: "⏰ Timer Alert!\nDate: {{date}}\nTime: {{time}}",
  actionType: "timer.recurring",
  reactionType: "discord.send_webhook"
};

await fetch('/api/areas/timer', {  // Note: /timer endpoint, not /areas
  method: 'POST',
  headers: { 'Content-Type': 'application/json' },
  body: JSON.stringify(timerAreaData)
});
```

## Using Timer Variables in Reactions

### Discord Message Template Example

```
⏰ Timer Alert!
Current Date: {{date}}
Current Time: {{time}}
Day: {{dayOfWeek}}
```

### For days_until type:

```
📅 Future Date Info
{{daysUntilMessage}}

Details:
- Current: {{date}} ({{dayOfWeek}})
- Future: {{futureDate}} ({{futureDay}})
```

## Configuration

### Application Properties

```properties
# Polling interval affects timer precision
area.polling.enabled=true
area.polling.interval=60000    # Check every 60 seconds
area.polling.initial-delay=30000
```

### Timer Interval Guidelines

- **Minimum recommended interval:** 1 minute (60000ms polling interval)
- **For sub-minute triggers:** Reduce polling interval to 30000ms
- **Daily triggers:** Use 1440 minutes (24 hours)
- **Hourly triggers:** Use 60 minutes

## Database Schema

### TimerActionConfig (embedded in areas table)

| Column | Type | Description |
|--------|------|-------------|
| timer_type | VARCHAR | Type: current_date, current_time, days_until, recurring |
| timer_interval_minutes | INTEGER | Trigger/check interval |
| timer_target_day | VARCHAR | Target day (reserved for future use) |
| timer_days_count | INTEGER | Days to look ahead (days_until type) |

### Area table additions

| Column | Type | Description |
|--------|------|-------------|
| action_type | VARCHAR | Action identifier (e.g., "timer.recurring") |
| reaction_type | VARCHAR | Reaction identifier (e.g., "discord.send_message") |

## API Endpoints

### Get Timer Service Info
```
GET /api/service-registry/TIMER
```

Response:
```json
{
  "status": "success",
  "data": {
    "type": "TIMER",
    "name": "Timer",
    "description": "Trigger actions based on time conditions...",
    "requiresAuthentication": false,
    "actions": [
      {
        "id": "timer.current_date",
        "name": "Current Date (DD/MM)",
        "description": "Triggers daily...",
        "configFields": [...]
      },
      ...
    ],
    "reactions": []
  }
}
```

### Create Timer Area
```
POST /api/areas/timer
```

Request Body:
```json
{
  "timerConnectionId": null,
  "reactionConnectionId": 2,
  "timerType": "recurring",
  "intervalMinutes": 5,
  "daysCount": null,
  "targetDay": null,
  "discordChannelName": "general",
  "discordMessageTemplate": "⏰ Timer Alert!\nDate: {{date}}\nTime: {{time}}",
  "actionType": "timer.recurring",
  "reactionType": "discord.send_webhook"
}
```

Response:
```json
{
  "status": "success",
  "message": "Timer AREA created successfully",
  "data": {
    "id": 3,
    "actionConnectionId": null,
    "reactionConnectionId": 2,
    "active": true,
    "timerConfig": {
      "timerType": "recurring",
      "intervalMinutes": 5,
      "daysCount": null,
      "targetDay": null
    },
    "discordConfig": {
      "channelName": "general",
      "messageTemplate": "⏰ Timer Alert!..."
    }
  }
}
```

## Common Use Cases

### 1. Hourly Status Update
```java
timerConfig.setTimerType("recurring");
timerConfig.setIntervalMinutes(60);
```

### 2. Every 5 Minutes Reminder
```java
timerConfig.setTimerType("recurring");
timerConfig.setIntervalMinutes(5);
```

### 3. Daily "3 Days Until" Notification
```java
timerConfig.setTimerType("days_until");
timerConfig.setDaysCount(3);
timerConfig.setIntervalMinutes(1440); // 24 hours
```

### 4. Daily Date/Time Report
```java
timerConfig.setTimerType("current_date");
timerConfig.setIntervalMinutes(1440); // 24 hours
```

## Troubleshooting

### Timer not triggering
1. Check area is active: `area.active = true`
2. Verify polling is enabled: `area.polling.enabled=true`
3. Check interval hasn't elapsed yet
4. Review logs for errors

### Timer triggers too frequently
1. Check `intervalMinutes` configuration
2. Verify polling interval matches requirements

### Missing context variables
1. Ensure correct timer type is set
2. Check `timerConfig` is properly saved
3. Verify executor is generating context

## Testing

### Manual Test - Create Timer Area
```bash
curl -X POST http://localhost:8080/api/areas/timer \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer YOUR_TOKEN" \
  -d '{
    "timerConnectionId": null,
    "reactionConnectionId": 2,
    "timerType": "recurring",
    "intervalMinutes": 1,
    "daysCount": null,
    "targetDay": null,
    "discordChannelName": "general",
    "discordMessageTemplate": "Timer triggered!",
    "actionType": "timer.recurring",
    "reactionType": "discord.send_webhook"
  }'
```

### Check Execution Logs
```sql
SELECT * FROM area_execution_logs 
WHERE area_id = YOUR_AREA_ID 
ORDER BY executed_at DESC 
LIMIT 10;
```

## Architecture

```
AreaPollingScheduler (every 60s)
    ↓
determineActionType(area)
    ↓
ActionExecutorRegistry.getExecutorForAction("timer.*")
    ↓
TimerActionExecutor.isTriggered(area)
    ↓ (if interval elapsed)
TimerActionExecutor.getTriggerContext(area)
    ↓
ReactionExecutor.execute(area, context)
    ↓
TriggerStateService.updateStateAfterTimerSuccess(area)
```

## Implementation Details

- **Executor Pattern:** Uses wildcard matching ("timer.*")
- **State Management:** Tracks last check time in `AreaTriggerState`
- **Context Generation:** Formats dates/times using Java `DateTimeFormatter`
- **Reactive:** Uses Project Reactor for async processing
- **Circuit Breaker:** Skips areas with too many consecutive failures

## Future Enhancements

Potential additions:
- Specific time of day triggers (e.g., "daily at 9:00 AM")
- Specific date triggers (e.g., "on 15th of every month")
- Cron-like expressions
- Timezone support
- Holiday/weekend exclusions
- Multiple time windows per day
