# AREA Platform - System Architecture Diagrams

This document provides comprehensive system architecture diagrams for the AREA platform using Mermaid.

## Table of Contents

1. [High-Level Architecture](#high-level-architecture)
2. [Component Architecture](#component-architecture)
3. [Data Flow Diagram](#data-flow-diagram)
4. [Database Schema](#database-schema)
5. [Execution Flow Sequence](#execution-flow-sequence)
6. [Scheduler Process Flow](#scheduler-process-flow)
7. [Token Refresh Flow](#token-refresh-flow)

---

## High-Level Architecture

```mermaid
graph TB
    subgraph External["External Services"]
        Gmail["Gmail API<br/>OAuth 2.0<br/>Messages API"]
        GitHub["GitHub API<br/>Webhooks<br/>OAuth 2.0"]
        Discord["Discord API<br/>Webhooks<br/>Embeds"]
    end

    subgraph Clients["Client Applications"]
        Web["Web Client<br/>(Vue.js)<br/>Port 8081"]
        Mobile["Mobile Client<br/>(Flutter)<br/>Android"]
    end

    subgraph Backend["AREA Backend Server<br/>(Spring Boot - Port 8080)"]
        API["REST API Layer<br/>Controllers"]
        Service["Service Layer<br/>Business Logic"]
        Scheduler["Scheduler Layer<br/>Polling Engine"]
        Repo["Repository Layer<br/>JPA"]
    end

    DB[(PostgreSQL<br/>Database<br/>Port 8088)]

    Web -->|HTTP/REST| API
    Mobile -->|HTTP/REST| API

    API --> Service
    Service --> Repo
    Repo --> DB

    Scheduler -->|Poll Every 60s| Service
    Service -->|HTTPS| Gmail
    Service -->|HTTPS| GitHub
    Service -->|HTTPS| Discord

    style Backend fill:#e1f5ff
    style External fill:#fff4e6
    style Clients fill:#f3e5f5
    style DB fill:#e8f5e9
```

---

## Component Architecture

```mermaid
graph TB
    subgraph Presentation["Presentation Layer"]
        AuthCtrl["AuthController<br/>Login/Register"]
        AreaCtrl["AreaController<br/>CRUD Operations"]
        ServiceCtrl["ServiceController<br/>Connections"]
        UserCtrl["UserController<br/>Profile"]
    end

    subgraph Business["Business Logic Layer"]
        AuthSvc["AuthService<br/>JWT/OAuth2"]
        AreaSvc["AreaService<br/>AREA Management"]
        GmailSvc["EnhancedGmailService<br/>Email Integration"]
        DiscordSvc["EnhancedDiscordService<br/>Notifications"]
        TokenSvc["TokenRefreshService<br/>Token Management"]
        TriggerSvc["TriggerStateService<br/>State Tracking"]
    end

    subgraph Scheduling["Scheduler Layer"]
        Scheduler["AreaPollingScheduler<br/>@Scheduled(60s)"]
    end

    subgraph Data["Data Access Layer"]
        AreaRepo["AreaRepository"]
        UserRepo["UserRepository"]
        ConnRepo["ServiceConnectionRepository"]
        StateRepo["AreaTriggerStateRepository"]
        LogRepo["AreaExecutionLogRepository"]
    end

    subgraph Database["Database"]
        DB[(PostgreSQL)]
    end

    AuthCtrl --> AuthSvc
    AreaCtrl --> AreaSvc
    ServiceCtrl --> AreaSvc
    UserCtrl --> AuthSvc

    AreaSvc --> AreaRepo
    AreaSvc --> GmailSvc
    AreaSvc --> DiscordSvc

    Scheduler --> AreaSvc
    Scheduler --> TriggerSvc
    Scheduler --> TokenSvc

    TokenSvc --> ConnRepo
    TriggerSvc --> StateRepo
    AreaSvc --> LogRepo

    AreaRepo --> DB
    UserRepo --> DB
    ConnRepo --> DB
    StateRepo --> DB
    LogRepo --> DB

    style Presentation fill:#e3f2fd
    style Business fill:#f1f8e9
    style Scheduling fill:#fff3e0
    style Data fill:#fce4ec
    style Database fill:#e8f5e9
```

---

## Data Flow Diagram

```mermaid
sequenceDiagram
    participant User
    participant WebApp as Web/Mobile Client
    participant API as REST API
    participant Service as Service Layer
    participant DB as PostgreSQL
    participant Gmail as Gmail API
    participant Discord as Discord API

    User->>WebApp: Create AREA
    WebApp->>API: POST /api/areas
    API->>Service: createArea(areaData)
    Service->>DB: INSERT INTO areas
    Service->>DB: INSERT INTO area_trigger_states
    DB-->>Service: Success
    Service-->>API: Area Created
    API-->>WebApp: 201 Created
    WebApp-->>User: AREA Active

    Note over Service: Every 60 seconds...

    Service->>DB: SELECT active areas
    DB-->>Service: List of areas

    loop For each area
        Service->>DB: Get trigger state
        Service->>DB: Check token expiration

        alt Token Expired
            Service->>Gmail: Refresh OAuth token
            Gmail-->>Service: New tokens
            Service->>DB: UPDATE tokens
        end

        Service->>Gmail: GET unread messages
        Gmail-->>Service: Message list

        alt New Messages Found
            Service->>Gmail: GET message details
            Gmail-->>Service: Email content
            Service->>Discord: POST webhook (embed)
            Discord-->>Service: 200 OK
            Service->>DB: UPDATE trigger state
            Service->>DB: INSERT execution log
        end
    end
```

---

## Database Schema

```mermaid
erDiagram
    USERS ||--o{ AREAS : creates
    USERS ||--o{ SERVICE_CONNECTIONS : owns
    AREAS ||--|| AREA_TRIGGER_STATES : has
    AREAS ||--o{ AREA_EXECUTION_LOGS : logs
    AREAS }o--|| SERVICE_CONNECTIONS : "action connection"
    AREAS }o--|| SERVICE_CONNECTIONS : "reaction connection"

    USERS {
        bigint id PK
        varchar email UK
        varchar password
        varchar username
        timestamp created_at
        timestamp updated_at
    }

    AREAS {
        bigint id PK
        bigint user_id FK
        bigint action_conn_id FK
        bigint reaction_conn_id FK
        varchar name
        jsonb gmail_config
        jsonb discord_config
        boolean active
        timestamp created_at
        timestamp updated_at
    }

    SERVICE_CONNECTIONS {
        bigint id PK
        bigint user_id FK
        varchar type
        varchar access_token
        varchar refresh_token
        timestamp token_expires_at
        timestamp last_refresh_attempt
        integer expires_in_seconds
        jsonb metadata
        timestamp created_at
    }

    AREA_TRIGGER_STATES {
        bigint id PK
        bigint area_id FK "UNIQUE"
        varchar last_processed_msg_id
        timestamp last_checked_at
        timestamp last_triggered_at
        integer consecutive_failures
        text last_error_message
        timestamp created_at
    }

    AREA_EXECUTION_LOGS {
        bigint id PK
        bigint area_id FK
        timestamp executed_at
        varchar status
        integer unread_count
        boolean message_sent
        text error_message
        integer execution_time_ms
    }
```

---

## Execution Flow Sequence

```mermaid
sequenceDiagram
    autonumber
    participant Scheduler as AreaPollingScheduler
    participant AreaSvc as AreaService
    participant TokenSvc as TokenRefreshService
    participant TriggerSvc as TriggerStateService
    participant GmailSvc as EnhancedGmailService
    participant DiscordSvc as EnhancedDiscordService
    participant DB as Database
    participant Gmail as Gmail API
    participant Discord as Discord API

    Note over Scheduler: @Scheduled(fixedDelay=60000)

    Scheduler->>DB: Find active areas
    DB-->>Scheduler: List of areas

    loop For each AREA (max 5 concurrent)
        Scheduler->>TriggerSvc: Check circuit breaker

        alt Circuit Open
            TriggerSvc-->>Scheduler: Skip (too many failures)
        else Circuit Closed
            Scheduler->>TokenSvc: Check token expiration

            alt Token expires in < 5 min
                TokenSvc->>Gmail: POST /token (refresh)
                Gmail-->>TokenSvc: New access token
                TokenSvc->>DB: UPDATE service_connections
            end

            Scheduler->>GmailSvc: Fetch new messages
            GmailSvc->>TriggerSvc: Get last processed ID
            TriggerSvc->>DB: SELECT last_processed_msg_id
            DB-->>TriggerSvc: Last ID
            TriggerSvc-->>GmailSvc: Last ID

            GmailSvc->>Gmail: GET /messages?labelIds=UNREAD
            Gmail-->>GmailSvc: Message list

            GmailSvc->>GmailSvc: Filter new messages

            alt New messages found
                loop For each new message
                    GmailSvc->>Gmail: GET /messages/{id}
                    Gmail-->>GmailSvc: Message details
                end

                GmailSvc-->>Scheduler: New messages

                Scheduler->>DiscordSvc: Send notification
                DiscordSvc->>Discord: POST webhook (embed)
                Discord-->>DiscordSvc: 200 OK
                DiscordSvc-->>Scheduler: Success

                Scheduler->>TriggerSvc: Update state
                TriggerSvc->>DB: UPDATE area_trigger_states

                Scheduler->>DB: INSERT execution log (SUCCESS)
            else No new messages
                Scheduler->>TriggerSvc: Update checked_at
                TriggerSvc->>DB: UPDATE last_checked_at
                Scheduler->>DB: INSERT execution log (SKIPPED)
            end
        end
    end

    Scheduler->>Scheduler: Log summary statistics
```

---

## Scheduler Process Flow

```mermaid
flowchart TD
    Start([Scheduler Triggered<br/>Every 60 seconds]) --> FetchAreas[Fetch Active Areas<br/>FROM database]

    FetchAreas --> HasAreas{Areas<br/>Found?}
    HasAreas -->|No| End([End - Wait 60s])
    HasAreas -->|Yes| ProcessConcurrent[Process up to 5 AREAs<br/>Concurrently]

    ProcessConcurrent --> CheckCircuit{Circuit<br/>Breaker<br/>Closed?}

    CheckCircuit -->|Open| Skip[Skip AREA<br/>Log reason]
    CheckCircuit -->|Closed| CheckToken{Token<br/>Expires<br/>Soon?}

    CheckToken -->|Yes| RefreshToken[Refresh OAuth Token<br/>Update DB]
    CheckToken -->|No| FetchMessages
    RefreshToken --> FetchMessages[Fetch Gmail Messages<br/>Filter by label]

    FetchMessages --> GetState[Get Trigger State<br/>last_processed_msg_id]
    GetState --> FilterNew[Filter New Messages<br/>ID > last_processed_id]

    FilterNew --> HasNew{New<br/>Messages?}

    HasNew -->|No| UpdateChecked[Update last_checked_at<br/>Log: SKIPPED]
    HasNew -->|Yes| FetchDetails[Fetch Message Details<br/>Subject, From, Body]

    FetchDetails --> FormatEmbed[Format Discord Embed<br/>Rich notification]
    FormatEmbed --> SendDiscord[Send to Discord<br/>POST webhook]

    SendDiscord --> DiscordOK{Discord<br/>Success?}

    DiscordOK -->|Yes| UpdateState[Update Trigger State<br/>last_processed_msg_id<br/>Reset failures]
    DiscordOK -->|No| RetryDiscord[Retry 3 times<br/>Exponential backoff]

    RetryDiscord --> RetryOK{Retry<br/>Success?}
    RetryOK -->|Yes| UpdateState
    RetryOK -->|No| IncrementFailure[Increment Failures<br/>Check circuit breaker]

    UpdateState --> LogSuccess[Log Execution<br/>Status: SUCCESS]
    IncrementFailure --> LogFailure[Log Execution<br/>Status: FAILED]
    UpdateChecked --> NextArea

    LogSuccess --> NextArea{More<br/>Areas?}
    LogFailure --> NextArea
    Skip --> NextArea

    NextArea -->|Yes| CheckCircuit
    NextArea -->|No| LogSummary[Log Summary Statistics<br/>Total processed, success, failed]

    LogSummary --> End

    style Start fill:#c8e6c9
    style End fill:#ffcdd2
    style CheckCircuit fill:#fff9c4
    style HasNew fill:#fff9c4
    style DiscordOK fill:#fff9c4
    style SendDiscord fill:#bbdefb
    style LogSuccess fill:#c8e6c9
    style LogFailure fill:#ffcdd2
```

---

## Token Refresh Flow

```mermaid
flowchart TD
    Start([Token Check Initiated]) --> GetConnection[Get Service Connection<br/>from Database]

    GetConnection --> CheckExpiry{Token<br/>Expires in<br/>< 5 min?}

    CheckExpiry -->|No| Valid[Token Valid<br/>Continue]
    CheckExpiry -->|Yes| HasRefresh{Has<br/>Refresh<br/>Token?}

    HasRefresh -->|No| Error[Error: No Refresh Token<br/>Manual re-auth required]
    HasRefresh -->|Yes| PrepareRequest[Prepare Token Refresh<br/>client_id, client_secret<br/>refresh_token]

    PrepareRequest --> CallGoogle[POST to Google<br/>oauth2.googleapis.com/token]

    CallGoogle --> GoogleOK{Response<br/>200 OK?}

    GoogleOK -->|No| CheckError{Error<br/>Type?}
    GoogleOK -->|Yes| ParseResponse[Parse Response<br/>access_token<br/>expires_in]

    CheckError -->|invalid_grant| RevokedError[Token Revoked<br/>User must re-authenticate]
    CheckError -->|Other| RetryError[Retry with backoff<br/>or log error]

    ParseResponse --> CalculateExpiry[Calculate Expiry<br/>now + expires_in]
    CalculateExpiry --> UpdateDB[Update Database<br/>access_token<br/>token_expires_at<br/>last_refresh_attempt]

    UpdateDB --> Success[Token Refreshed<br/>Continue execution]

    Valid --> Continue([Continue with API call])
    Success --> Continue
    Error --> Stop([Stop execution<br/>Log error])
    RevokedError --> Stop
    RetryError --> Stop

    style Start fill:#c8e6c9
    style Success fill:#c8e6c9
    style Continue fill:#c8e6c9
    style Error fill:#ffcdd2
    style RevokedError fill:#ffcdd2
    style Stop fill:#ffcdd2
    style CheckExpiry fill:#fff9c4
    style GoogleOK fill:#fff9c4
    style UpdateDB fill:#bbdefb
```

---

## Key Components Explained

### Scheduler Configuration
```yaml
Polling Interval: 60 seconds
Concurrent Processing: Up to 5 AREAs
Timeout Protection: 2 minutes maximum
Initial Delay: 30 seconds after startup
Thread Pool Size: 5 threads
```

### Circuit Breaker Rules
- **Opens After**: 5 consecutive failures
- **Purpose**: Prevent wasting resources on broken AREAs
- **Reset**: Manual intervention required (fix configuration)

### Token Refresh Strategy
- **Check Threshold**: 5 minutes before expiration
- **Proactive**: Prevents mid-execution token expiry
- **Automatic**: No user intervention needed
- **Fallback**: Requires re-authentication if refresh fails

### Discord Retry Logic
- **Max Retries**: 3 attempts
- **Backoff**: Exponential (1s, 2s, 4s)
- **Timeout**: 10 seconds per request
- **Rate Limiting**: Respects Discord's limits

---

## API Endpoints Reference

```mermaid
graph LR
    subgraph Authentication
        A1[POST /api/auth/register]
        A2[POST /api/auth/login]
    end

    subgraph Areas
        B1[GET /api/areas]
        B2[POST /api/areas]
        B3[PUT /api/areas/:id]
        B4[DELETE /api/areas/:id]
        B5[POST /api/integrations/areas/:id/trigger]
    end

    subgraph Services
        C1[GET /api/services]
        C2[POST /api/connections]
        C3[GET /api/connections]
    end

    subgraph Validation
        D1[POST /api/integrations/actions/gmail/validate]
        D2[POST /api/integrations/reactions/discord/validate]
    end

    style Authentication fill:#e3f2fd
    style Areas fill:#f1f8e9
    style Services fill:#fff3e0
    style Validation fill:#fce4ec
```

---

## Deployment Architecture

```mermaid
graph TB
    subgraph Docker["Docker Compose Environment"]
        subgraph WebContainer["web container<br/>(nginx:alpine)"]
            Nginx["Nginx Web Server<br/>Port 80"]
        end

        subgraph ServerContainer["server container<br/>(openjdk:21)"]
            SpringBoot["Spring Boot App<br/>Port 8080"]
        end

        subgraph DBContainer["postgres container<br/>(postgres:16)"]
            PostgreSQL["PostgreSQL Database<br/>Port 5432 (internal)<br/>Port 8088 (external)"]
        end
    end

    Internet[Internet] -->|HTTP/HTTPS| Nginx
    Nginx -->|Proxy Pass<br/>/api/*| SpringBoot
    SpringBoot -->|JDBC| PostgreSQL

    SpringBoot -.->|Health Check| PostgreSQL

    style WebContainer fill:#e3f2fd
    style ServerContainer fill:#f1f8e9
    style DBContainer fill:#e8f5e9
```

---

*Generated: December 2025*
*For more details, see the complete documentation in `/docs`*
