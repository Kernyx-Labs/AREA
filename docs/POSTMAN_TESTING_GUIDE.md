# AREA Backend API - Postman Testing Guide

This guide explains how to use the Postman collection to test the AREA backend API.

## 📦 Files Included

- **AREA_Backend_API.postman_collection.json** - Complete API collection with all endpoints
- **AREA_Backend_API.postman_environment.json** - Environment variables for local testing

## 🚀 Quick Start

### 1. Import Collection and Environment

1. Open Postman
2. Click **Import** button (top left)
3. Drag and drop both JSON files into the import dialog
4. Click **Import**

### 2. Select Environment

1. In the top-right corner, click the environment dropdown
2. Select **AREA Backend - Local**

### 3. Start the Backend Server

Make sure your backend server is running:

```bash
cd server
mvn spring-boot:run
```

Or using Docker:

```bash
docker-compose up
```

The server should be accessible at `http://localhost:8080`

## 📚 Collection Structure

The collection is organized into 7 main folders:

### 1. **About**
- Get basic info about the platform and available services

### 2. **Dashboard**
- Get comprehensive statistics and metrics
- Monitor active areas, executions, and success rates

### 3. **Areas (AREAs)**
- **Create Area** - Create Gmail → Discord automation
- **List Areas** - View all areas (with filtering)
- **Get Area by ID** - View specific area details
- **Update Area Status** - Activate/deactivate areas
- **Delete Area** - Remove areas
- **Get Execution Logs** - View area execution history
- **Get Trigger State** - Check area polling state

### 4. **Service Connections**
- **List Connections** - View all service connections
- **Create Connection** - Manually create connection (advanced)
- **Delete Connection** - Remove connection
- **Refresh Token** - Manually refresh Gmail OAuth token

### 5. **Gmail Service**
- **Get Auth URL** - Start Gmail OAuth flow
- **OAuth Callback** - Handle OAuth redirect (automatic)
- **Get Status** - Check Gmail configuration

### 6. **Discord Service**
- **Get Connection Info** - Setup instructions
- **Connect Discord** - Add Discord bot
- **Test Connection** - Verify bot works

### 7. **Service Registry**
- **List All Services** - View all available services
- **Get Service by Type** - View specific service details
- **Get Actions/Reactions** - View available triggers and reactions
- **Get Stats** - Service registry statistics

### 8. **Workflows**
- Generic workflow system (alternative to Areas)
- Create, update, execute, and monitor workflows

## 🧪 Testing Workflows

### Test 1: Basic Platform Status

1. **Get About Info** (`GET /about.json`)
   - Verify server is running
   - Check available services (Gmail, Discord)

2. **Get Dashboard Stats** (`GET /api/dashboard/stats`)
   - View current platform metrics
   - Check total areas and executions

### Test 2: Gmail OAuth Setup

1. **Get Gmail Status** (`GET /api/services/gmail/status`)
   - Verify OAuth is configured
   - Should show `configured: true`

2. **Get Gmail Auth URL** (`GET /api/services/gmail/auth-url`)
   - Copy the `authUrl` from response
   - Open URL in browser
   - Authorize Gmail access
   - You'll be redirected back automatically

3. **List Service Connections** (`GET /api/service-connections`)
   - Find your new Gmail connection
   - Copy the `id` value
   - Set `gmail_connection_id` environment variable

### Test 3: Discord Bot Setup

1. **Get Discord Connection Info** (`GET /api/services/discord/info`)
   - Follow the setup instructions
   - Create a bot at https://discord.com/developers/applications
   - Get bot token and channel ID

2. **Test Discord Connection** (`POST /api/services/discord/test`)
   - Update request body with your bot token and channel ID
   - Send request
   - Check Discord channel for test message

3. **Connect Discord** (`POST /api/services/discord/connect`)
   - Use same bot token and channel ID
   - Connection is created and validated
   - Copy the `connectionId` from response
   - Set `discord_connection_id` environment variable

### Test 4: Create and Test an AREA

1. **Create Area** (`POST /api/areas`)
   - Update request body:
     ```json
     {
       "actionConnectionId": {{gmail_connection_id}},
       "reactionConnectionId": {{discord_connection_id}},
       "gmailLabel": "INBOX",
       "gmailSubjectContains": "test",
       "gmailFromAddress": "",
       "discordWebhookUrl": "",
       "discordChannelName": "notifications",
       "discordMessageTemplate": "📧 New email from {from}: {subject}"
     }
     ```
   - Copy the `id` from response
   - Set `area_id` environment variable

2. **Get Area by ID** (`GET /api/areas/{{area_id}}`)
   - Verify area was created correctly
   - Check Gmail and Discord configs

3. **Trigger the Area**
   - Send yourself a test email with "test" in the subject
   - Wait up to 60 seconds (polling interval)
   - Check Discord channel for notification

4. **Get Execution Logs** (`GET /api/areas/{{area_id}}/logs`)
   - View execution history
   - Check for SUCCESS or FAILURE status
   - Review any error messages

5. **Get Trigger State** (`GET /api/areas/{{area_id}}/state`)
   - Check `lastProcessedMessageId`
   - View `consecutiveFailures` count
   - See `lastTriggeredAt` timestamp

### Test 5: Area Management

1. **List All Areas** (`GET /api/areas?activeOnly=false`)
   - View all areas

2. **List Active Areas** (`GET /api/areas?activeOnly=true`)
   - View only active areas

3. **Update Area Status** (`PUT /api/areas/{{area_id}}/status`)
   - Deactivate area: `{"active": false}`
   - Reactivate area: `{"active": true}`

4. **Delete Area** (`DELETE /api/areas/{{area_id}}`)
   - Permanently remove area
   - Use with caution!

### Test 6: Service Registry

1. **List All Services** (`GET /api/services`)
   - View Gmail and Discord services
   - See action/reaction counts

2. **Get Gmail Actions** (`GET /api/services/GMAIL/actions`)
   - View available Gmail triggers
   - Check action configurations

3. **Get Discord Reactions** (`GET /api/services/DISCORD/reactions`)
   - View available Discord reactions
   - Check reaction configurations

4. **Get Registry Stats** (`GET /api/services/stats`)
   - Overall service statistics

## 🔧 Environment Variables

Update these in your Postman environment:

| Variable | Description | Example |
|----------|-------------|---------|
| `base_url` | Backend server URL | `http://localhost:8080` |
| `area_id` | ID of test area | `1` |
| `connection_id` | Generic connection ID | `1` |
| `workflow_id` | ID of test workflow | `1` |
| `gmail_connection_id` | Gmail connection ID | Set after OAuth |
| `discord_connection_id` | Discord connection ID | Set after bot setup |
| `discord_bot_token` | Your Discord bot token | `MTIzNDU2...` |
| `discord_channel_id` | Target Discord channel | `1234567890123456789` |

## 📝 Common Request Bodies

### Create Area
```json
{
  "actionConnectionId": 1,
  "reactionConnectionId": 2,
  "gmailLabel": "INBOX",
  "gmailSubjectContains": "important",
  "gmailFromAddress": "boss@company.com",
  "discordWebhookUrl": "",
  "discordChannelName": "notifications",
  "discordMessageTemplate": "📧 Email from {from}\n**Subject:** {subject}\n**Snippet:** {snippet}"
}
```

### Connect Discord Bot
```json
{
  "botToken": "MTIzNDU2Nzg5MDEyMzQ1Njc4.XXXXXX.YYYYYYYYYYYYYYYYYYYYYYYYYY",
  "channelId": "1234567890123456789"
}
```

### Update Area Status
```json
{
  "active": true
}
```

### Create Workflow
```json
{
  "name": "Important Emails to Discord",
  "trigger": {
    "service": "gmail",
    "action": "new_email",
    "config": {
      "from": "important@example.com",
      "subject": "urgent"
    }
  },
  "action": {
    "service": "discord",
    "reaction": "send_message",
    "config": {
      "message": "🚨 Urgent: {subject}"
    }
  }
}
```

## 🐛 Troubleshooting

### Connection Refused
- **Problem:** Cannot connect to `http://localhost:8080`
- **Solution:** Make sure backend server is running with `mvn spring-boot:run` or `docker-compose up`

### Gmail OAuth Not Configured
- **Problem:** `"configured": false` in Gmail status
- **Solution:** Set `GOOGLE_CLIENT_ID` and `GOOGLE_CLIENT_SECRET` in `server/.env`

### Discord Bot Can't Send Messages
- **Problem:** 403 or 404 errors when testing Discord
- **Solution:**
  - Verify bot is in the server
  - Check bot has "Send Messages" permission
  - Verify channel ID is correct (right-click channel → Copy ID in Discord)

### Area Not Triggering
- **Problem:** Email received but no Discord message
- **Solution:**
  - Check area is active: `GET /api/areas/{{area_id}}`
  - View trigger state: `GET /api/areas/{{area_id}}/state`
  - Check execution logs: `GET /api/areas/{{area_id}}/logs`
  - Verify Gmail filters match your test email
  - Wait up to 60 seconds (polling interval)

### Token Expired
- **Problem:** `401 Unauthorized` on Gmail actions
- **Solution:** Refresh token with `POST /api/service-connections/{{connection_id}}/refresh`

## 📊 Response Examples

### Successful Area Creation
```json
{
  "success": true,
  "message": "AREA created successfully",
  "data": {
    "id": 1,
    "actionConnectionId": 1,
    "reactionConnectionId": 2,
    "active": true,
    "gmailConfig": {
      "label": "INBOX",
      "subjectContains": "important",
      "fromAddress": "boss@company.com"
    },
    "discordConfig": {
      "webhookUrl": "",
      "channelName": "notifications",
      "messageTemplate": "📧 New email from {from}: {subject}"
    }
  }
}
```

### Dashboard Stats
```json
{
  "success": true,
  "data": {
    "stats": {
      "totalAreas": 5,
      "activeAreas": 3,
      "inactiveAreas": 2,
      "connectedServices": 4,
      "executionsLast24h": 47,
      "successfulExecutions": 45,
      "failedExecutions": 2,
      "successRate": 95.7,
      "executionTrend": 12.5,
      "topAreas": [...],
      "recentActivity": [...]
    }
  }
}
```

## 🎯 Best Practices

1. **Use Environment Variables** - Don't hardcode IDs in requests
2. **Check Status First** - Verify services are configured before testing
3. **Test Incrementally** - Test each service separately before creating areas
4. **Monitor Logs** - Always check execution logs when debugging
5. **Clean Up** - Delete test areas and connections when done

## 🔗 Related Documentation

- [CLAUDE.md](../CLAUDE.md) - Complete project documentation
- [Backend README](../server/README.md) - Backend setup guide
- [API Documentation](../docs/) - Additional API docs

## 📞 Support

If you encounter issues:
1. Check backend logs: `docker-compose logs server`
2. Verify database: `docker exec -it area-postgres psql -U area_user -d area_db`
3. Review execution logs via API: `GET /api/areas/{id}/logs`
4. Check trigger state: `GET /api/areas/{id}/state`

---

**Happy Testing! 🚀**
