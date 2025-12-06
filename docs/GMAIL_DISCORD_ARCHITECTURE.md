# Gmail to Discord Webhook Integration - Backend Architecture

## Overview

This document describes the production-ready backend architecture for the Gmail to Discord webhook automation in the AREA platform. The implementation enables automatic monitoring of Gmail for new emails and triggers Discord webhook notifications when new messages arrive.

## Architecture Components

### 1. Data Model

#### Core Entities

**Area** (`/server/src/main/java/com/area/server/model/Area.java`)
- Represents a user-created automation
- Links action connection (Gmail) to reaction connection (Discord)
- Stores configuration for both Gmail filters and Discord webhooks
- Has `active` flag for enabling/disabling automations

**ServiceConnection** (`/server/src/main/java/com/area/server/model/ServiceConnection.java`)
- Stores OAuth tokens for external services
- Supports Gmail and Discord service types
- Enhanced with token expiration tracking (`tokenExpiresAt`, `lastRefreshAttempt`)
- Includes `needsRefresh()` method for automatic token refresh logic

**AreaTriggerState** (`/server/src/main/java/com/area/server/model/AreaTriggerState.java`) **[NEW]**
- Tracks the execution state of each AREA
- Stores `lastProcessedMessageId` to avoid duplicate notifications
- Implements circuit breaker pattern with `consecutiveFailures` counter
- Records timestamps for monitoring (`lastCheckedAt`, `lastTriggeredAt`)

**AreaExecutionLog** (`/server/src/main/java/com/area/server/model/AreaExecutionLog.java`) **[NEW]**
- Audit trail for all AREA executions
- Records SUCCESS, FAILURE, or SKIPPED status
- Captures performance metrics (`executionTimeMs`)
- Indexed by area_id and timestamp for efficient queries

#### Configuration Entities

**GmailActionConfig** (Embeddable)
- `label`: Filter by Gmail label
- `subjectContains`: Filter by subject text
- `fromAddress`: Filter by sender

**DiscordReactionConfig** (Embeddable)
- `webhookUrl`: Discord channel webhook URL (2048 chars)
- `messageTemplate`: Custom message template with placeholders
- `channelName`: Optional channel identifier

### 2. Service Layer

#### TokenRefreshService **[NEW]**
**Location:** `/server/src/main/java/com/area/server/service/TokenRefreshService.java`

**Purpose:** Automatically refresh expired OAuth tokens

**Key Features:**
- Checks if token needs refresh (5-minute buffer before expiration)
- Calls Google OAuth2 token endpoint with refresh token
- Updates ServiceConnection with new access token and expiration
- Graceful error handling with logging

**Configuration Required:**
```properties
google.oauth.client-id=${GOOGLE_CLIENT_ID}
google.oauth.client-secret=${GOOGLE_CLIENT_SECRET}
```

#### TriggerStateService **[NEW]**
**Location:** `/server/src/main/java/com/area/server/service/TriggerStateService.java`

**Purpose:** Manage trigger state and implement circuit breaker pattern

**Key Methods:**
- `shouldTrigger()`: Determines if new messages warrant a notification
- `updateStateAfterSuccess()`: Records successful execution
- `recordFailure()`: Increments failure counter
- `shouldSkipDueToFailures()`: Circuit breaker (skips after 5 failures)
- `resetFailureCount()`: Manual circuit breaker reset

#### EnhancedGmailService **[NEW]**
**Location:** `/server/src/main/java/com/area/server/service/EnhancedGmailService.java`

**Purpose:** Fetch email messages with full metadata (not just counts)

**Key Features:**
- Fetches list of unread messages matching filters
- Retrieves full message details (subject, sender, snippet, timestamp)
- Filters messages newer than `lastProcessedMessageId`
- Parses Gmail API response format
- Returns reactive `Mono<List<GmailMessage>>`

**API Integration:**
- List messages: `GET /gmail/v1/users/me/messages`
- Get message: `GET /gmail/v1/users/me/messages/{id}`

#### EnhancedDiscordService **[NEW]**
**Location:** `/server/src/main/java/com/area/server/service/EnhancedDiscordService.java`

**Purpose:** Send rich Discord notifications via webhooks

**Key Features:**
- `sendRichEmbed()`: Sends formatted Discord embed with email details
- `sendBatchNotification()`: Sends summary for multiple emails
- Automatic retry with exponential backoff (3 attempts, 2-second delay)
- Handles rate limiting and webhook errors
- Truncates long content to Discord limits

**Discord Embed Format:**
```json
{
  "title": "New Email: [Subject]",
  "description": "[Email snippet]",
  "color": 3447003,
  "fields": [
    {"name": "From", "value": "sender@example.com", "inline": true},
    {"name": "Received", "value": "2025-12-05 14:30:00", "inline": true}
  ],
  "timestamp": "2025-12-05T14:30:00Z",
  "footer": {"text": "AREA Gmail to Discord Integration"}
}
```

### 3. Automated Polling Scheduler

#### AreaPollingScheduler **[NEW]**
**Location:** `/server/src/main/java/com/area/server/scheduler/AreaPollingScheduler.java`

**Purpose:** Automatically poll active AREAs and trigger reactions

**Execution Flow:**
1. Fetch all active AREAs from database
2. For each AREA (max 5 concurrent):
   - Check circuit breaker status
   - Refresh OAuth token if needed
   - Fetch new Gmail messages
   - Determine if trigger condition is met
   - Send Discord notification
   - Update trigger state and log execution
3. Log summary statistics

**Configuration:**
```properties
# Enable/disable polling
area.polling.enabled=true

# Poll every 60 seconds
area.polling.interval=60000

# Wait 30 seconds after startup
area.polling.initial-delay=30000
```

**Concurrency Control:**
- Processes up to 5 AREAs in parallel
- 2-minute timeout for entire polling cycle
- Reactive programming with Flux/Mono

**Error Handling:**
- Individual AREA failures don't stop processing
- Errors logged with details
- Circuit breaker prevents repeated failures

### 4. Data Transfer Objects

#### GmailMessage
**Location:** `/server/src/main/java/com/area/server/dto/GmailMessage.java`

```java
public class GmailMessage {
    private String id;              // Gmail message ID
    private String threadId;        // Gmail thread ID
    private String subject;         // Email subject
    private String from;            // Sender email address
    private String snippet;         // Email preview (first 200 chars)
    private Instant receivedAt;     // Timestamp
}
```

#### GmailApiResponse
**Location:** `/server/src/main/java/com/area/server/dto/GmailApiResponse.java`

Contains nested classes for Gmail API response parsing:
- `MessageListResponse`: List of message references
- `MessageDetail`: Full message with headers and payload
- `Header`: Email header (Subject, From, Date, etc.)
- `TokenResponse`: OAuth2 token refresh response

### 5. Repository Layer

**New Repositories:**
- `AreaTriggerStateRepository`: Query trigger states by area ID
- `AreaExecutionLogRepository`: Query execution history with pagination

**Enhanced Repositories:**
- `AreaRepository`: Added `findByActiveTrue()` for scheduler

## Database Schema

### New Tables

**area_trigger_states**
```sql
CREATE TABLE area_trigger_states (
    id BIGSERIAL PRIMARY KEY,
    area_id BIGINT NOT NULL UNIQUE REFERENCES areas(id),
    last_unread_count INTEGER,
    last_processed_message_id VARCHAR(256),
    last_checked_at TIMESTAMP,
    last_triggered_at TIMESTAMP,
    consecutive_failures INTEGER DEFAULT 0,
    last_error_message VARCHAR(1024)
);
```

**area_execution_logs**
```sql
CREATE TABLE area_execution_logs (
    id BIGSERIAL PRIMARY KEY,
    area_id BIGINT NOT NULL REFERENCES areas(id),
    executed_at TIMESTAMP NOT NULL,
    status VARCHAR(32) NOT NULL,  -- SUCCESS, FAILURE, SKIPPED
    unread_count INTEGER,
    message_sent VARCHAR(2048),
    error_message VARCHAR(1024),
    execution_time_ms BIGINT
);

CREATE INDEX idx_area_timestamp ON area_execution_logs(area_id, executed_at);
```

### Modified Tables

**service_connections** (added columns)
```sql
ALTER TABLE service_connections
ADD COLUMN token_expires_at TIMESTAMP,
ADD COLUMN last_refresh_attempt TIMESTAMP;
```

## Configuration

### Environment Variables

Required for production:

```bash
# PostgreSQL Database
SPRING_DATASOURCE_URL=jdbc:postgresql://localhost:5432/area
SPRING_DATASOURCE_USERNAME=area_admin
SPRING_DATASOURCE_PASSWORD=your_secure_password

# JWT Secret
APP_JWT_SECRET=your_jwt_secret_key

# Google OAuth2 (for Gmail)
GOOGLE_CLIENT_ID=your_google_client_id
GOOGLE_CLIENT_SECRET=your_google_client_secret
GOOGLE_REDIRECT_URI=https://your-domain.com/oauth/callback
```

### Application Properties

See `/server/src/main/resources/application.properties` for full configuration.

Key settings:
- Polling interval: 60 seconds (configurable)
- Connection pool: 20 max connections
- Task scheduler: 5 threads
- Token refresh: 5-minute buffer before expiration

## Security Considerations

### Current Implementation

1. **Token Storage:**
   - Access tokens stored in database (plaintext)
   - Refresh tokens stored in database (plaintext)

   **Recommendation:** Implement encryption at rest using JPA Converters with AES-256

2. **Webhook URLs:**
   - Discord webhook URLs stored in plaintext

   **Risk:** If database is compromised, attackers can send messages to Discord channels

   **Recommendation:** Encrypt webhook URLs or use secret references

3. **API Rate Limiting:**
   - Discord: Retry logic with exponential backoff
   - Gmail: No explicit rate limiting implemented

   **Recommendation:** Add rate limiter using Resilience4j or Bucket4j

4. **Input Validation:**
   - Basic validation with Jakarta Validation annotations

   **Recommendation:** Add stricter validation for webhook URLs (verify Discord domain)

### Recommended Enhancements

1. **Encrypt Sensitive Fields:**
```java
@Convert(converter = EncryptedStringConverter.class)
private String accessToken;
```

2. **Add Rate Limiting:**
```java
@RateLimiter(name = "gmail", fallbackMethod = "gmailFallback")
public Mono<List<GmailMessage>> fetchNewMessages(...) { ... }
```

3. **Implement Secret Management:**
- Use AWS Secrets Manager or HashiCorp Vault
- Rotate credentials regularly
- Never commit secrets to version control

## Scalability Considerations

### Current Design

**Strengths:**
- Reactive programming (WebFlux) for non-blocking I/O
- Concurrent AREA processing (5 parallel)
- Connection pooling (HikariCP)
- Indexed database queries

**Limitations:**
- Single scheduler instance (not horizontally scalable)
- In-memory state during polling
- Database bottleneck for high AREA count

### Scaling Recommendations

#### For 100-1000 AREAs:
Current implementation sufficient with:
- Increase polling interval to 120-300 seconds
- Increase concurrent processing to 10-20
- Add database read replicas

#### For 1000-10000 AREAs:
1. **Distributed Scheduling:**
   - Use ShedLock or Quartz with database locking
   - Deploy multiple server instances
   - Each instance processes a subset of AREAs

2. **Message Queue:**
   - Replace polling with event-driven architecture
   - Use Gmail Pub/Sub push notifications
   - Process events asynchronously with RabbitMQ/Kafka

3. **Caching:**
   - Cache trigger states in Redis
   - Reduce database queries
   - Improve response times

#### For 10000+ AREAs:
1. **Microservices Architecture:**
   - Separate Gmail polling service
   - Separate Discord notification service
   - Central orchestration service

2. **Gmail Pub/Sub:**
   - Replace polling with real-time push notifications
   - Reduce API quota usage
   - Instant trigger response

3. **Sharding:**
   - Partition AREAs across multiple databases
   - Geographic distribution
   - Tenant-based isolation

## Gmail Pub/Sub Integration (Future Enhancement)

### Architecture

```
Gmail → Pub/Sub Topic → Cloud Function/Webhook → AREA Server → Discord
```

### Benefits
- Real-time notifications (no polling delay)
- Reduced Gmail API quota usage
- Scales to millions of users
- Lower server resource usage

### Implementation Steps

1. **Setup Gmail Push Notifications:**
```bash
gcloud pubsub topics create gmail-notifications
gcloud pubsub subscriptions create area-gmail-sub --topic=gmail-notifications
```

2. **Watch Gmail Mailbox:**
```java
public void setupGmailWatch(String userEmail, String accessToken) {
    WatchRequest watchRequest = new WatchRequest()
        .setLabelIds(List.of("INBOX"))
        .setTopicName("projects/your-project/topics/gmail-notifications");

    gmail.users().watch(userEmail, watchRequest).execute();
}
```

3. **Handle Pub/Sub Messages:**
```java
@PostMapping("/webhooks/gmail")
public ResponseEntity<Void> handleGmailNotification(@RequestBody PubSubMessage message) {
    String historyId = message.getHistoryId();
    // Fetch new messages since historyId
    // Trigger matching AREAs
    return ResponseEntity.ok().build();
}
```

## Monitoring and Observability

### Metrics to Track

1. **Polling Cycle Metrics:**
   - Total cycle duration
   - Success/failure/skipped counts
   - Average execution time per AREA

2. **AREA Metrics:**
   - Active AREA count
   - Triggered AREAs per cycle
   - Circuit breaker activations

3. **Service Metrics:**
   - Gmail API latency
   - Discord webhook latency
   - Token refresh success rate
   - Database query performance

### Logging

Current implementation includes structured logging:
- Scheduler cycle summaries
- Individual AREA processing
- Token refresh attempts
- Error details with stack traces

**Recommendation:** Add correlation IDs for request tracing:
```java
MDC.put("areaId", area.getId().toString());
logger.info("Processing area");
MDC.clear();
```

### Alerting

Recommended alerts:
1. Polling cycle takes > 2 minutes
2. Failure rate > 10%
3. Circuit breaker open for > 5 minutes
4. Database connection pool exhausted
5. Gmail API quota exceeded

## Testing Strategy

### Unit Tests (Recommended)

```java
@Test
void testShouldTriggerOnNewMessages() {
    // Test TriggerStateService logic
}

@Test
void testTokenRefreshWhenExpired() {
    // Test TokenRefreshService with mock WebClient
}

@Test
void testCircuitBreakerOpensAfter5Failures() {
    // Test TriggerStateService failure handling
}
```

### Integration Tests (Recommended)

```java
@SpringBootTest
@Testcontainers
class AreaPollingSchedulerIntegrationTest {
    @Container
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:15");

    @Test
    void testCompletePollingCycle() {
        // Test end-to-end flow with test containers
    }
}
```

### Manual Testing

1. **Create AREA via API:**
```bash
POST /api/areas
{
  "actionConnectionId": 1,
  "reactionConnectionId": 2,
  "discordWebhookUrl": "https://discord.com/api/webhooks/...",
  "gmailLabel": "INBOX"
}
```

2. **Trigger Manually:**
```bash
POST /api/integrations/areas/{id}/trigger
```

3. **View Execution Logs:**
Query `area_execution_logs` table to verify notifications sent

## Deployment

### Docker Compose (Development)

Already configured in `/docker-compose.yml`

### Production Deployment

1. **Build Application:**
```bash
cd server
mvn clean package -DskipTests
```

2. **Create Docker Image:**
```bash
docker build -t area-server:latest .
```

3. **Deploy with Environment Variables:**
```bash
docker run -d \
  -p 8080:8080 \
  -e SPRING_DATASOURCE_URL=jdbc:postgresql://db:5432/area \
  -e GOOGLE_CLIENT_ID=... \
  -e GOOGLE_CLIENT_SECRET=... \
  area-server:latest
```

### Health Checks

Add actuator endpoints:
```xml
<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-actuator</artifactId>
</dependency>
```

Check health:
```bash
curl http://localhost:8080/actuator/health
```

## Troubleshooting

### Common Issues

1. **Scheduler Not Running:**
   - Check `area.polling.enabled=true` in properties
   - Verify no exceptions during startup
   - Check logs for scheduler initialization

2. **Token Refresh Failures:**
   - Verify GOOGLE_CLIENT_ID and GOOGLE_CLIENT_SECRET set
   - Check refresh token is valid and not revoked
   - Review OAuth consent screen settings

3. **Discord Webhook Errors:**
   - Verify webhook URL is valid and not deleted
   - Check Discord API status
   - Review error logs for 4xx vs 5xx errors

4. **Duplicate Notifications:**
   - Check `last_processed_message_id` is updating
   - Verify trigger state service working correctly
   - Review execution logs for duplicate triggers

5. **Circuit Breaker Stuck Open:**
   - Check error messages in `area_trigger_states`
   - Manually reset with `TriggerStateService.resetFailureCount()`
   - Fix underlying issue before resetting

## Performance Benchmarks

Estimated performance with current implementation:

- **100 active AREAs:** < 5 seconds per cycle
- **500 active AREAs:** 15-20 seconds per cycle
- **1000 active AREAs:** 30-40 seconds per cycle

Factors affecting performance:
- Gmail API response time (50-200ms per request)
- Discord webhook response time (100-300ms per request)
- Database query performance
- Concurrent processing limit (5)

## API Endpoints

### Existing Endpoints

**Create AREA:**
```http
POST /api/areas
Content-Type: application/json

{
  "actionConnectionId": 1,
  "reactionConnectionId": 2,
  "gmailLabel": "INBOX",
  "gmailSubjectContains": "important",
  "gmailFromAddress": "boss@company.com",
  "discordWebhookUrl": "https://discord.com/api/webhooks/...",
  "discordMessageTemplate": "New email: {{unreadCount}} unread"
}
```

**List AREAs:**
```http
GET /api/areas
```

**Delete AREA:**
```http
DELETE /api/areas/{id}
```

**Manual Trigger:**
```http
POST /api/integrations/areas/{id}/trigger
```

**Validate Gmail Configuration:**
```http
POST /api/integrations/actions/gmail/validate
{
  "connectionId": 1,
  "label": "INBOX",
  "subjectContains": "test"
}
```

**Validate Discord Webhook:**
```http
POST /api/integrations/reactions/discord/validate
{
  "webhookUrl": "https://discord.com/api/webhooks/...",
  "message": "Test message"
}
```

### Recommended New Endpoints

```http
# Get AREA execution history
GET /api/areas/{id}/executions?page=0&size=20

# Get AREA trigger state
GET /api/areas/{id}/state

# Reset circuit breaker
POST /api/areas/{id}/reset-failures

# Get polling statistics
GET /api/admin/polling/stats
```

## Conclusion

This architecture provides a robust, production-ready foundation for Gmail to Discord webhook automation. The implementation includes:

- Automatic polling with configurable intervals
- State tracking to avoid duplicate notifications
- Token refresh for OAuth2 credentials
- Circuit breaker for fault tolerance
- Rich Discord notifications with email details
- Comprehensive execution logging
- Scalability path for growing user base

The system is ready for deployment with proper environment configuration and can scale to handle thousands of active automations with the recommended enhancements.
