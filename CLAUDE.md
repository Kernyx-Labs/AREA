# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Project Overview

AREA is an automation platform inspired by IFTTT/Zapier that connects actions from one service to reactions in another. The system consists of:
- **Backend**: Spring Boot 3.4.12 (Java 21) REST API with PostgreSQL
- **Web**: Vue 3 + Vite frontend
- **Mobile**: Flutter Android app

## Development Commands

### Docker (Recommended for full stack)
```bash
# Build and start all services (backend, web, postgres)
docker-compose build
docker-compose up

# Access points:
# - Web UI: http://localhost:80
# - API Server: http://localhost:8080
# - PostgreSQL: localhost:8088
```

### Backend (Spring Boot)
```bash
cd server

# Run with Maven wrapper (if available)
./mvnw spring-boot:run

# Or with system Maven
mvn spring-boot:run

# Run tests
mvn test

# Build JAR
mvn clean package

# Run with specific profile
mvn spring-boot:run -Dspring-boot.run.profiles=dev
```

**Note**: Backend requires PostgreSQL running. Use Docker Compose or local PostgreSQL on port 8088.

### Web Client (Vue 3)
```bash
cd web

# Install dependencies
npm install

# Development server with hot reload (uses Vite proxy to backend on :8080)
npm run dev

# Build for production
npm run build

# Preview production build
npm run preview
```

**Dev server runs on**: http://localhost:5173 (Vite default) with API proxying to http://localhost:8080

### Mobile (Flutter)
```bash
cd mobile

# Get dependencies
flutter pub get

# Run on connected device/emulator
flutter run

# Build APK
flutter build apk

# Run tests
flutter test
```

## Architecture

### Backend Architecture (Spring Boot)

**Package Structure**:
- `com.area.server.controller` - REST API endpoints
- `com.area.server.service` - Business logic layer
- `com.area.server.repository` - JPA data access
- `com.area.server.model` - JPA entities
- `com.area.server.scheduler` - Scheduled tasks (polling)
- `com.area.server.util` - Validators and utilities

**Key Architectural Patterns**:

1. **Polling-Based Trigger System** (NOT webhook-based):
   - `AreaPollingScheduler` runs every 60 seconds (`@Scheduled(fixedDelay=60000)`)
   - Polls all active AREA configurations
   - Checks Gmail for new messages matching action filters
   - Executes Discord reactions when conditions met
   - Uses Spring WebFlux (reactive) for concurrent processing

2. **State Management with Circuit Breaker**:
   - `AreaTriggerState` entity tracks last processed message ID
   - Circuit breaker: After 5 consecutive failures, area is skipped
   - Token refresh happens 5 minutes before expiration
   - State updates are transactional

3. **Service Integration Model**:
   - **Gmail**: OAuth 2.0 flow via `GmailOAuthController`
     - Scopes: `gmail.readonly`
     - Filters: label, subject contains, from address
     - Token stored in `ServiceConnection` entity
   - **Discord**: Webhook-based reactions (no OAuth)
     - Validates webhook URLs with regex
     - Sends rich embeds with email metadata
     - Retry logic: 3 retries with exponential backoff (2s base)

4. **Configuration as Embedded Entities**:
   - `GmailActionConfig` (`@Embeddable`) - stores filter criteria in Area entity
   - `DiscordReactionConfig` (`@Embeddable`) - stores webhook URL in Area entity
   - This avoids separate config tables and keeps AREA data cohesive

**Critical Database Entities**:
- `Area` - Core workflow (action + reaction + configs)
- `ServiceConnection` - OAuth credentials (one connection → many areas)
- `AreaTriggerState` - Execution state (1:1 with Area)
- `AreaExecutionLog` - Audit trail with pagination

**Environment Variables Required** (see `server/.env`):
```bash
POSTGRES_DB=area_db
POSTGRES_USER=area_user
POSTGRES_PASSWORD=<secure_password>
GOOGLE_CLIENT_ID=<google_oauth_client_id>
GOOGLE_CLIENT_SECRET=<google_oauth_secret>
GOOGLE_REDIRECT_URI=http://localhost:8080/oauth/callback
APP_JWT_SECRET=<jwt_secret>  # Placeholder for future auth
```

**Key Configuration** (`application.properties`):
- `spring.jpa.hibernate.ddl-auto=update` - Auto-creates schema (use `validate` in prod)
- `spring.jpa.show-sql=true` - SQL logging (disable in prod)
- `area.polling.interval=60000` - Scheduler interval (60s)
- HikariCP pool: max 20 connections, min 5 idle

### Frontend Architecture (Vue 3)

**Component Organization**:
- All components live in `/web/src/components/` (no separation of views vs reusable components)
- **Key Components**:
  - `AreasDashboard.vue` - Main dashboard with KPIs and area listings
  - `PipelineEditor.vue` - Workflow builder (triggers left, reactions right)
  - `ServicesView.vue` - Service connection management
  - `DiscordConnectionModal.vue` - Discord bot setup modal

**State Management**:
- **No centralized store** (no Pinia/Vuex)
- Component-local `ref()` for state
- `localStorage` for theme preference
- URL params for context (`/editor/:areaId`)
- API calls made directly from components

**API Integration** (`/web/src/services/api.js`):
- Uses native `fetch` API (no axios)
- All endpoints centralized in single module
- Base URL from `import.meta.env.VITE_API_URL`
- No request interceptors or auth headers (backend handles session)

**Routing** (`/web/src/router/index.js`):
```
/ → redirects to /dashboard
/dashboard → AreasDashboard
/editor/:areaId? → PipelineEditor (optional ID for edit)
/services → ServicesView
/profile → ProfileView (placeholder)
```

**Vite Proxy Configuration** (`vite.config.js`):
- Proxies `/api`, `/about.json`, `/oauth` to `http://localhost:8080`
- Enables seamless dev without CORS issues

**Authentication**:
- **No user authentication implemented**
- Service-specific OAuth (Gmail) handled per connection
- Discord uses manual bot token input
- No JWT or session tokens on frontend

### Mobile Architecture (Flutter)

Minimal Flutter app with basic structure:
- Dart SDK: ^3.10.1
- Flutter SDK required
- Material Design UI
- Target: Android (no iOS yet)

## Adding New Services

When adding a new service integration:

1. **Create Service Models**:
   - `{Service}ActionConfig` or `{Service}ReactionConfig` as `@Embeddable` in `model/`
   - Add fields to `Area` entity with `@Embedded` annotation

2. **Implement Service Layer** (`service/`):
   - Create `{Service}Service` class with `@Service` annotation
   - Use `WebClient` (reactive) for external API calls
   - Implement token refresh if OAuth-based
   - Add retry logic for transient failures

3. **Add Controller Endpoints** (`controller/`):
   - OAuth flow: `/api/services/{service}/auth-url` and `/callback`
   - Status check: `/api/services/{service}/status`
   - Follow existing `GmailOAuthController` pattern

4. **Update Scheduler**:
   - Modify `AreaPollingScheduler` to handle new action types
   - Add condition checking logic in polling loop
   - Update `WorkflowExecutionService` if needed

5. **Add Validation**:
   - Create validator in `util/` (e.g., `{Service}Validator.java`)
   - Validate configs before AREA creation

6. **Frontend Integration** (`web/`):
   - Add API methods in `src/services/api.js`
   - Create service card in `ServicesView.vue`
   - Add action/reaction options in `PipelineEditor.vue`

## Database Schema Management

- **Schema updates**: Hibernate auto-updates with `ddl-auto=update`
  - Safe for development
  - Use migrations (Flyway/Liquibase) for production
- **Manual migrations**: Add SQL scripts to `server/db-init/` (runs on first container start)
- **Indexes**: Add `@Table(indexes = {...})` annotations to entities
- **Column sizes**: Use `@Column(length=X)` for strings (defaults to 255)

## Testing Strategy

### Backend Testing
```bash
cd server
mvn test                    # Run all tests
mvn test -Dtest=ClassName   # Run specific test class
```

**Key test patterns**:
- Repository tests: Use `@DataJpaTest` with embedded H2
- Service tests: Mock repositories with `@MockBean`
- Controller tests: Use `@WebMvcTest` with MockMvc
- Integration tests: Use `@SpringBootTest` with TestContainers (if configured)

### Frontend Testing
```bash
cd web
npm run test      # Run tests (if configured)
```

Note: No test setup currently configured in `package.json`

## Common Development Workflows

### Local Development Setup (Without Docker)

1. **Start PostgreSQL**:
   ```bash
   # Option 1: Docker container
   docker run -d -p 8088:5432 \
     -e POSTGRES_DB=area_db \
     -e POSTGRES_USER=area_user \
     -e POSTGRES_PASSWORD=password \
     postgres:16

   # Option 2: Local PostgreSQL (adjust port to 8088)
   ```

2. **Configure environment**:
   ```bash
   cd server
   cp .env.example .env  # If exists, otherwise create .env
   # Edit .env with OAuth credentials
   ```

3. **Start backend**:
   ```bash
   cd server
   mvn spring-boot:run
   ```

4. **Start frontend**:
   ```bash
   cd web
   npm install
   npm run dev
   ```

### Debugging Polling Issues

1. **Check scheduler status**:
   - Look for `AreaPollingScheduler` logs in backend console
   - Verify `area.polling.enabled=true` in `application.properties`

2. **Inspect trigger state**:
   - Use `/api/areas/{id}/state` endpoint
   - Check `consecutiveFailures` count (should be < 5)
   - Verify `lastProcessedMessageId` is updating

3. **Debug Gmail API calls**:
   - Enable SQL logging: `spring.jpa.show-sql=true`
   - Check token expiration in `ServiceConnection` entity
   - Manual token refresh: `POST /api/service-connections/{id}/refresh`

4. **View execution logs**:
   - Use `/api/areas/{id}/logs?page=0&size=20` endpoint
   - Check `status` field: SUCCESS, FAILURE, SKIPPED

### OAuth Configuration

**Gmail OAuth Setup**:
1. Create OAuth 2.0 credentials in Google Cloud Console
2. Add redirect URI: `http://localhost:8080/oauth/callback`
3. Enable Gmail API
4. Set scopes: `https://www.googleapis.com/auth/gmail.readonly`
5. Add credentials to `server/.env`

**Frontend OAuth Flow**:
1. User clicks "Connect Gmail" in `ServicesView.vue`
2. Frontend calls `/api/services/gmail/auth-url`
3. Opens popup with Google consent screen
4. User authorizes → redirected to `/oauth/callback`
5. Backend exchanges code for tokens
6. Tokens stored in `ServiceConnection` table

## Key Technical Decisions

1. **Polling vs Webhooks**: System uses polling (60s interval) instead of webhooks
   - Simpler to implement and debug
   - No need for public endpoints or ngrok in development
   - Trade-off: ~60s latency vs real-time

2. **Reactive Spring WebFlux**: Used for external API calls (Gmail, Discord)
   - Enables concurrent processing of multiple areas
   - Non-blocking I/O for better resource utilization
   - Configured with 5 concurrent items in Flux stream

3. **Embedded Configs**: Action/Reaction configs stored as `@Embeddable` in Area
   - Avoids polymorphism complexity
   - Simpler queries and JSON serialization
   - Trade-off: More nullable fields in Area table

4. **No Frontend State Management**: Uses component-local state only
   - Simpler for small app
   - May need Pinia if cross-component state grows

5. **Circuit Breaker Pattern**: Areas auto-skip after 5 consecutive failures
   - Prevents resource waste on broken configurations
   - Auto-recovers when state is manually refreshed or area edited

## Port Reference

| Service       | Port | URL                        |
|--------------|------|----------------------------|
| Backend API  | 8080 | http://localhost:8080      |
| Web UI       | 80   | http://localhost:80        |
| Web Dev      | 5173 | http://localhost:5173      |
| PostgreSQL   | 8088 | jdbc:postgresql://localhost:8088/area_db |

## Logs and Debugging

**Backend Logs**:
- Console output shows SQL queries (when `show-sql=true`)
- Scheduler logs: Look for `AreaPollingScheduler` class logs
- OAuth flow: `GmailOAuthController` logs token exchanges

**Frontend Debugging**:
- Vue DevTools enabled via `vite-plugin-vue-devtools`
- Browser console shows API fetch errors
- Network tab shows API request/response

**Database Debugging**:
```bash
# Connect to PostgreSQL in Docker
docker exec -it area-postgres psql -U area_user -d area_db

# Useful queries
SELECT * FROM area WHERE active = true;
SELECT * FROM area_trigger_state;
SELECT * FROM area_execution_log ORDER BY executed_at DESC LIMIT 10;
SELECT * FROM service_connection;
```

## Performance Considerations

- **Polling concurrency**: Configured for 5 concurrent areas in Flux stream
- **Connection pool**: HikariCP max 20 connections (adjust for load)
- **Scheduler thread pool**: 5 threads for `@Scheduled` tasks
- **Token refresh buffer**: 5 minutes before expiration to avoid race conditions
- **Discord retry backoff**: Exponential with 2s base, 3 max retries

## Security Notes

- OAuth tokens stored in database (consider encryption at rest)
- JWT secret required but not yet implemented in auth flow
- Discord webhook URLs validated with regex before storage
- No rate limiting on API endpoints (add nginx/Spring rate limiter for production)
- CORS not configured (add `@CrossOrigin` or global CORS config if needed)

## Documentation

All the documentation you need to created nee to be in the folder `/docs`
