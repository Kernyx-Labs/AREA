# Quick Start Guide - Gmail to Discord Integration

## Installing Java and Maven on Fedora

```bash
# Install Java 21 and Maven
sudo dnf install java-21-openjdk java-21-openjdk-devel maven

# Verify installation
java -version    # Should show version 21
mvn -version     # Should show Maven 3.x
```

## Quick Test

### 1. Compile the Project

```bash
cd /home/pandor/Delivery/AREA/server
mvn clean compile
```

Expected output:
```
[INFO] BUILD SUCCESS
[INFO] Total time: XX.XXX s
```

### 2. Run Tests (Optional)

```bash
mvn test
```

### 3. Start the Application

```bash
# Option A: Development mode (hot reload)
mvn spring-boot:run

# Option B: Build and run JAR
mvn clean package -DskipTests
java -jar target/server-0.0.1-SNAPSHOT.jar
```

### 4. Verify Application is Running

```bash
# Check if application started
curl http://localhost:8080/actuator/health

# Check if scheduler is running (check logs)
tail -f logs/spring.log | grep "polling cycle"
```

Expected log output:
```
2025-12-05 14:30:00 INFO  AreaPollingScheduler - === Starting AREA polling cycle ===
2025-12-05 14:30:00 INFO  AreaPollingScheduler - Found 0 active area(s) to process
2025-12-05 14:30:00 INFO  AreaPollingScheduler - === Polling cycle completed in 123ms ===
```

## Next Steps

1. Follow the full setup guide: `/home/pandor/Delivery/AREA/docs/GMAIL_DISCORD_SETUP_GUIDE.md`
2. Review the architecture: `/home/pandor/Delivery/AREA/docs/GMAIL_DISCORD_ARCHITECTURE.md`
3. Check implementation summary: `/home/pandor/Delivery/AREA/docs/IMPLEMENTATION_SUMMARY.md`

## Common Build Issues

### Issue: Java version mismatch
```bash
# Set Java 21 as default
sudo alternatives --config java
# Select Java 21 from the list
```

### Issue: Maven out of memory
```bash
export MAVEN_OPTS="-Xmx1024m"
mvn clean compile
```

### Issue: PostgreSQL not running
```bash
# Start PostgreSQL container
docker-compose up -d postgres
```

## File Structure Overview

```
server/
├── src/main/java/com/area/server/
│   ├── model/              # JPA entities
│   │   ├── AreaTriggerState.java (NEW)
│   │   └── AreaExecutionLog.java (NEW)
│   ├── repository/         # Spring Data JPA repos
│   │   ├── AreaTriggerStateRepository.java (NEW)
│   │   └── AreaExecutionLogRepository.java (NEW)
│   ├── service/            # Business logic
│   │   ├── TokenRefreshService.java (NEW)
│   │   ├── TriggerStateService.java (NEW)
│   │   ├── EnhancedGmailService.java (NEW)
│   │   └── EnhancedDiscordService.java (NEW)
│   ├── scheduler/          # Background tasks
│   │   └── AreaPollingScheduler.java (NEW)
│   ├── dto/                # Data transfer objects
│   │   ├── GmailMessage.java (NEW)
│   │   └── GmailApiResponse.java (NEW)
│   └── controller/         # REST API endpoints (existing)
└── src/main/resources/
    └── application.properties (MODIFIED)
```

## Key Features Implemented

1. **Automated Polling** - Checks Gmail every 60 seconds
2. **State Tracking** - Prevents duplicate notifications
3. **Token Refresh** - Automatically refreshes OAuth tokens
4. **Rich Notifications** - Formatted Discord embeds with email details
5. **Circuit Breaker** - Stops retrying after repeated failures
6. **Audit Logging** - Comprehensive execution history

## Configuration Required

Before running, set these environment variables:

```bash
export GOOGLE_CLIENT_ID="your-client-id"
export GOOGLE_CLIENT_SECRET="your-client-secret"
export APP_JWT_SECRET="your-jwt-secret"
```

Or create a `.env` file (don't commit this!):
```
GOOGLE_CLIENT_ID=your-client-id
GOOGLE_CLIENT_SECRET=your-client-secret
APP_JWT_SECRET=your-jwt-secret
```

## Troubleshooting

### Application won't start
1. Check PostgreSQL is running: `docker ps | grep postgres`
2. Check environment variables are set: `env | grep GOOGLE`
3. Check logs: `tail -f logs/spring.log`

### Scheduler not running
1. Check `area.polling.enabled=true` in application.properties
2. Look for error in logs during startup
3. Verify Spring Task Scheduler bean initialized

### Build fails
1. Ensure Java 21 is installed: `java -version`
2. Ensure Maven is installed: `mvn -version`
3. Clean build cache: `mvn clean`
4. Try: `mvn clean compile -X` for verbose output

## Quick Database Check

```sql
-- Connect to database
psql -h localhost -p 5432 -U area_admin -d area

-- Check tables were created
\dt

-- Expected tables:
-- areas
-- service_connections
-- area_trigger_states (NEW)
-- area_execution_logs (NEW)

-- Check scheduler is creating logs
SELECT COUNT(*) FROM area_execution_logs;
```

## Development Workflow

```bash
# 1. Make code changes
vim server/src/main/java/com/area/server/...

# 2. Compile
mvn compile

# 3. Run tests
mvn test

# 4. Start application
mvn spring-boot:run

# 5. Test changes
curl -X POST http://localhost:8080/api/areas ...

# 6. Check logs
tail -f logs/spring.log
```

## Performance Testing

```bash
# Create 10 test AREAs
for i in {1..10}; do
  curl -X POST http://localhost:8080/api/areas \
    -H "Content-Type: application/json" \
    -d '{...}'
done

# Monitor polling performance
tail -f logs/spring.log | grep "Polling cycle completed"

# Check database performance
psql -U area_admin -d area -c "
  SELECT
    AVG(execution_time_ms) as avg_ms,
    MAX(execution_time_ms) as max_ms,
    COUNT(*) as total_executions
  FROM area_execution_logs;
"
```

## Need Help?

1. Check the comprehensive documentation:
   - Architecture: `/docs/GMAIL_DISCORD_ARCHITECTURE.md`
   - Setup Guide: `/docs/GMAIL_DISCORD_SETUP_GUIDE.md`
   - Implementation: `/docs/IMPLEMENTATION_SUMMARY.md`

2. Common issues are documented in the setup guide

3. Check application logs in `logs/spring.log`

4. Review database state with SQL queries

5. Use debug logging:
   ```properties
   logging.level.com.area.server=DEBUG
   ```
