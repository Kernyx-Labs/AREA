# Gmail to Discord Integration - Setup Guide

This guide walks through setting up the Gmail to Discord webhook automation feature in the AREA platform.

## Prerequisites

1. **Google Cloud Project** with Gmail API enabled
2. **Discord Webhook** URL for target channel
3. **PostgreSQL Database** (version 12+)
4. **Java 21** and **Maven 3.8+**

## Step 1: Google Cloud Setup

### 1.1 Create Google Cloud Project

1. Go to [Google Cloud Console](https://console.cloud.google.com)
2. Create a new project or select existing one
3. Enable the Gmail API:
   - Navigate to "APIs & Services" > "Library"
   - Search for "Gmail API"
   - Click "Enable"

### 1.2 Create OAuth 2.0 Credentials

1. Go to "APIs & Services" > "Credentials"
2. Click "Create Credentials" > "OAuth client ID"
3. Configure OAuth consent screen if not done:
   - User Type: External (for testing) or Internal (for organization)
   - Add scopes: `https://www.googleapis.com/auth/gmail.readonly`
4. Create OAuth client ID:
   - Application type: Web application
   - Authorized redirect URIs: `http://localhost:8080/oauth/callback` (adjust for production)
5. Save the **Client ID** and **Client Secret**

### 1.3 Test OAuth Flow

Use Google OAuth Playground to get initial tokens:
1. Go to https://developers.google.com/oauthplayground
2. Select Gmail API v1 > `https://www.googleapis.com/auth/gmail.readonly`
3. Click "Authorize APIs"
4. Exchange authorization code for tokens
5. Save the **Access Token** and **Refresh Token**

## Step 2: Discord Setup

### 2.1 Create Discord Webhook

1. Open Discord and navigate to your server
2. Right-click the channel where notifications should appear
3. Select "Edit Channel" > "Integrations"
4. Click "Create Webhook" or "View Webhooks"
5. Customize webhook name and avatar (optional)
6. Copy the **Webhook URL** (e.g., `https://discord.com/api/webhooks/123456789/abcdefg...`)

### 2.2 Test Webhook

```bash
curl -X POST "https://discord.com/api/webhooks/YOUR_WEBHOOK_URL" \
  -H "Content-Type: application/json" \
  -d '{"content": "Test message from AREA platform"}'
```

You should see the message appear in your Discord channel.

## Step 3: Database Setup

### 3.1 Start PostgreSQL

Using Docker:
```bash
docker run -d \
  --name area-postgres \
  -e POSTGRES_DB=area \
  -e POSTGRES_USER=area_admin \
  -e POSTGRES_PASSWORD=SuperSecurePassword123! \
  -p 5432:5432 \
  postgres:15
```

Or use existing PostgreSQL instance.

### 3.2 Verify Connection

```bash
psql -h localhost -p 5432 -U area_admin -d area
```

The application will automatically create tables on first startup using Hibernate DDL auto-update.

## Step 4: Application Configuration

### 4.1 Set Environment Variables

Create a `.env` file in the `server` directory:

```bash
# Database Configuration
SPRING_DATASOURCE_URL=jdbc:postgresql://localhost:5432/area
SPRING_DATASOURCE_USERNAME=area_admin
SPRING_DATASOURCE_PASSWORD=SuperSecurePassword123!

# JWT Secret (generate random string)
APP_JWT_SECRET=your_random_jwt_secret_key_at_least_32_chars

# Google OAuth2
GOOGLE_CLIENT_ID=123456789-abcdefghijklmnop.apps.googleusercontent.com
GOOGLE_CLIENT_SECRET=GOCSPX-abcdefghijklmnopqrstuvwxyz
GOOGLE_REDIRECT_URI=http://localhost:8080/oauth/callback
```

### 4.2 Verify application.properties

Ensure `/server/src/main/resources/application.properties` contains:

```properties
# AREA Polling Configuration
area.polling.enabled=true
area.polling.interval=60000
area.polling.initial-delay=30000
```

## Step 5: Build and Run

### 5.1 Build Application

```bash
cd server
mvn clean install
```

### 5.2 Run Application

```bash
mvn spring-boot:run
```

Or run the JAR:
```bash
java -jar target/server-0.0.1-SNAPSHOT.jar
```

### 5.3 Verify Startup

Check logs for:
```
Started ServerApplication in X.XXX seconds
Starting AREA polling cycle
Found 0 active area(s) to process
```

The application is now running on http://localhost:8080

## Step 6: Create Service Connections

### 6.1 Create Gmail Service Connection

```bash
curl -X POST http://localhost:8080/api/connections \
  -H "Content-Type: application/json" \
  -d '{
    "type": "GMAIL",
    "accessToken": "ya29.a0AfH6SMB...",
    "refreshToken": "1//0gH1SMB...",
    "expiresInSeconds": 3600,
    "metadata": "{\"email\": \"user@gmail.com\"}"
  }'
```

Response:
```json
{
  "id": 1,
  "type": "GMAIL",
  "expiresInSeconds": 3600
}
```

Save the connection `id` (e.g., 1).

### 6.2 Create Discord Service Connection

```bash
curl -X POST http://localhost:8080/api/connections \
  -H "Content-Type: application/json" \
  -d '{
    "type": "DISCORD",
    "accessToken": "not_required_for_webhooks",
    "metadata": "{\"webhookUrl\": \"https://discord.com/api/webhooks/...\"}"
  }'
```

Response:
```json
{
  "id": 2,
  "type": "DISCORD"
}
```

Save the connection `id` (e.g., 2).

## Step 7: Create an AREA

### 7.1 Create Gmail to Discord AREA

```bash
curl -X POST http://localhost:8080/api/areas \
  -H "Content-Type: application/json" \
  -d '{
    "actionConnectionId": 1,
    "reactionConnectionId": 2,
    "gmailLabel": "INBOX",
    "gmailSubjectContains": "",
    "gmailFromAddress": "",
    "discordWebhookUrl": "https://discord.com/api/webhooks/...",
    "discordChannelName": "notifications",
    "discordMessageTemplate": "New email received!"
  }'
```

Response:
```json
{
  "id": 1,
  "actionConnection": {
    "id": 1,
    "type": "GMAIL"
  },
  "reactionConnection": {
    "id": 2,
    "type": "DISCORD"
  },
  "gmailConfig": {
    "label": "INBOX",
    "subjectContains": null,
    "fromAddress": null
  },
  "discordConfig": {
    "webhookUrl": "https://discord.com/api/webhooks/...",
    "channelName": "notifications",
    "messageTemplate": "New email received!"
  },
  "active": true
}
```

### 7.2 Verify AREA is Active

```bash
curl http://localhost:8080/api/areas
```

You should see your AREA in the list with `"active": true`.

## Step 8: Test the Integration

### 8.1 Manual Trigger Test

```bash
curl -X POST http://localhost:8080/api/integrations/areas/1/trigger
```

Response:
```json
{
  "areaId": 1,
  "unreadCount": 5,
  "message": "New email: Test Subject (from: sender@example.com)"
}
```

Check your Discord channel for the notification.

### 8.2 Automatic Polling Test

1. Send yourself a test email
2. Wait for the next polling cycle (default: 60 seconds)
3. Check Discord channel for notification
4. Check application logs:

```
Processing area 1
Area 1 triggered with 1 new message(s). Latest: 'Test Email Subject'
Successfully sent Discord notification
Successfully processed area 1 in 523ms
```

### 8.3 Verify Database State

```sql
-- Check trigger state
SELECT * FROM area_trigger_states WHERE area_id = 1;

-- Check execution logs
SELECT * FROM area_execution_logs WHERE area_id = 1 ORDER BY executed_at DESC LIMIT 10;
```

## Step 9: Advanced Configuration

### 9.1 Gmail Filters

Filter by specific label:
```json
{
  "gmailLabel": "Important"
}
```

Filter by subject keyword:
```json
{
  "gmailSubjectContains": "invoice"
}
```

Filter by sender:
```json
{
  "gmailFromAddress": "boss@company.com"
}
```

Combine filters:
```json
{
  "gmailLabel": "Work",
  "gmailSubjectContains": "urgent",
  "gmailFromAddress": "team@company.com"
}
```

### 9.2 Custom Discord Message Templates

Use placeholders in message template:
```json
{
  "discordMessageTemplate": "You have {{unreadCount}} new email(s) at {{timestamp}}"
}
```

Available placeholders:
- `{{unreadCount}}`: Number of new emails
- `{{timestamp}}`: Current timestamp

### 9.3 Adjust Polling Interval

Edit `application.properties`:
```properties
# Poll every 5 minutes (300000 ms)
area.polling.interval=300000
```

Restart application for changes to take effect.

## Monitoring and Troubleshooting

### View Execution Logs

```sql
SELECT
  el.executed_at,
  el.status,
  el.unread_count,
  el.message_sent,
  el.error_message,
  el.execution_time_ms
FROM area_execution_logs el
WHERE el.area_id = 1
ORDER BY el.executed_at DESC
LIMIT 20;
```

### Check Trigger State

```sql
SELECT
  ats.last_checked_at,
  ats.last_triggered_at,
  ats.last_processed_message_id,
  ats.consecutive_failures,
  ats.last_error_message
FROM area_trigger_states ats
WHERE ats.area_id = 1;
```

### Reset Circuit Breaker

If an AREA is stuck due to repeated failures:

```sql
UPDATE area_trigger_states
SET consecutive_failures = 0, last_error_message = NULL
WHERE area_id = 1;
```

Or programmatically via service:
```java
triggerStateService.resetFailureCount(area);
```

### Common Issues

**Issue: Token expired errors**
- Ensure refresh token is valid
- Check GOOGLE_CLIENT_ID and GOOGLE_CLIENT_SECRET are set
- Verify token_expires_at is being updated

**Issue: Discord webhook not working**
- Verify webhook URL is correct and not deleted
- Test webhook manually with curl
- Check Discord API status

**Issue: Duplicate notifications**
- Check last_processed_message_id is updating
- Verify scheduler is not running multiple times
- Review execution logs for duplicate triggers

**Issue: No notifications received**
- Check area is active: `SELECT * FROM areas WHERE id = 1`
- Verify polling is enabled: `area.polling.enabled=true`
- Check logs for errors during polling cycle
- Manually trigger to isolate issue

## Production Deployment

### Environment Variables for Production

```bash
# Use production database
SPRING_DATASOURCE_URL=jdbc:postgresql://prod-db.example.com:5432/area

# Use strong passwords
SPRING_DATASOURCE_PASSWORD=<strong-random-password>

# JWT secret (min 256 bits)
APP_JWT_SECRET=<cryptographically-secure-random-string>

# Production OAuth credentials
GOOGLE_CLIENT_ID=<prod-client-id>
GOOGLE_CLIENT_SECRET=<prod-client-secret>
GOOGLE_REDIRECT_URI=https://area.example.com/oauth/callback

# Adjust polling for scale
area.polling.interval=120000
```

### Docker Deployment

```bash
docker build -t area-server:1.0 ./server

docker run -d \
  --name area-server \
  -p 8080:8080 \
  --env-file .env.production \
  --restart unless-stopped \
  area-server:1.0
```

### Health Check

```bash
curl http://localhost:8080/actuator/health
```

### Scaling Considerations

For production workloads:
- Use managed PostgreSQL (AWS RDS, Cloud SQL)
- Enable database connection pooling
- Monitor Gmail API quota usage
- Set up application monitoring (Datadog, New Relic)
- Configure logging aggregation (ELK, Splunk)
- Implement alerting for failures

## Next Steps

1. Implement user authentication and authorization
2. Add web UI for creating and managing AREAs
3. Implement more actions (GitHub, Twitter, etc.)
4. Implement more reactions (Slack, SMS, Email)
5. Add Gmail Pub/Sub for real-time notifications
6. Implement user-specific OAuth flows
7. Add webhook signature verification
8. Implement rate limiting per user
9. Add comprehensive monitoring dashboard
10. Write automated tests

## Support

For issues and questions:
- Check application logs in `server/logs/`
- Review database state in PostgreSQL
- Consult the architecture documentation
- Open an issue in the project repository

## Security Checklist

Before going to production:

- [ ] Change all default passwords
- [ ] Use environment variables for secrets
- [ ] Enable HTTPS/TLS
- [ ] Implement proper authentication
- [ ] Add rate limiting
- [ ] Encrypt sensitive database fields
- [ ] Use secret management service (Vault, AWS Secrets Manager)
- [ ] Enable audit logging
- [ ] Implement CSRF protection
- [ ] Validate and sanitize all inputs
- [ ] Set up security monitoring
- [ ] Regular security updates
- [ ] Database backups configured
- [ ] Disaster recovery plan documented
