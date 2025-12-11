# AREA Server Documentation

## 🖥️ Table of Contents

1. [Overview](#overview)
2. [Technology Stack](#technology-stack)
3. [Prerequisites](#prerequisites)
4. [Installation & Setup](#installation--setup)
5. [Project Structure](#project-structure)
6. [Architecture](#architecture)
7. [Database Design](#database-design)
8. [API Documentation](#api-documentation)
9. [Build & Run](#build--run)
10. [Development Guide](#development-guide)
11. [Testing](#testing)
12. [Deployment](#deployment)
13. [Troubleshooting](#troubleshooting)

---

## Overview

The AREA server is a REST API built with Spring Boot that serves as the backend for the automation platform. It handles authentication, service integration, AREA management, and automation execution.

### Key Features

- **RESTful API**: Clean, resource-based API design
- **JWT Authentication**: Secure token-based authentication
- **OAuth2 Integration**: Support for Google and GitHub
- **Service Orchestration**: Manage multiple external services
- **Automation Engine**: Poll and execute AREA workflows
- **Database Persistence**: PostgreSQL for reliable data storage
- **Scheduled Tasks**: Automatic polling and execution
- **Connection Pooling**: Optimized database connections

### Core Responsibilities

1. **Authentication & Authorization**: User management and security
2. **Service Integration**: Connect to Gmail, GitHub, Discord, etc.
3. **AREA Management**: CRUD operations for automation workflows
4. **Execution Engine**: Monitor triggers and execute reactions
5. **API Gateway**: Serve web and mobile clients

---

## Technology Stack

### Core Framework
- **Spring Boot**: 3.4.12
- **Java**: 21 (LTS)
- **Maven**: 3.8+

### Spring Dependencies
- **spring-boot-starter-web**: REST API development
- **spring-boot-starter-data-jpa**: ORM and database access
- **spring-boot-starter-validation**: Input validation
- **spring-boot-starter-webflux**: Reactive HTTP client
- **spring-boot-starter-test**: Testing framework

### Database
- **PostgreSQL**: 16 (Production)
- **H2**: In-memory database (Development/Testing)
- **Hibernate**: ORM framework

### Additional Libraries
- **Jackson**: JSON serialization
- **HikariCP**: Connection pooling (included in Spring Boot)

### External APIs
- **Google APIs**: Gmail, OAuth2
- **GitHub API**: Repository events, OAuth2
- **Discord Webhooks**: Notifications

---

## Prerequisites

### Required Software

1. **Java Development Kit (JDK) 21**:
   ```bash
   # Check version
   java -version  # Should be 21
   javac -version

   # Install on Ubuntu/Debian
   sudo apt install openjdk-21-jdk

   # Install on macOS
   brew install openjdk@21
   ```

2. **Maven 3.8+**:
   ```bash
   # Check version
   mvn -version

   # Install on Ubuntu/Debian
   sudo apt install maven

   # Install on macOS
   brew install maven
   ```

3. **PostgreSQL 16**:
   ```bash
   # Install on Ubuntu/Debian
   sudo apt install postgresql-16

   # Install on macOS
   brew install postgresql@16

   # Start PostgreSQL
   sudo systemctl start postgresql
   ```

### Recommended Tools

- **IntelliJ IDEA** or **Eclipse** (IDE)
- **Postman** (API testing)
- **DBeaver** or **pgAdmin** (Database management)
- **Docker** (Containerization)

---

## Installation & Setup

### 1. Navigate to Server Directory

```bash
cd AREA/server
```

### 2. Configure Environment Variables

Create `.env` file in server directory:

```bash
# Database Configuration
POSTGRES_DB=area_db
POSTGRES_USER=area_user
POSTGRES_PASSWORD=your_secure_password

# Database Connection (for local development)
SPRING_DATASOURCE_URL=jdbc:postgresql://localhost:8088/area_db
SPRING_DATASOURCE_USERNAME=area_user
SPRING_DATASOURCE_PASSWORD=your_secure_password

# JWT Configuration
APP_JWT_SECRET=your_jwt_secret_key_at_least_256_bits_long_for_security

# Google OAuth2
GOOGLE_CLIENT_ID=your_google_client_id
GOOGLE_CLIENT_SECRET=your_google_client_secret
GOOGLE_REDIRECT_URI=http://localhost:8080/oauth/callback

# Discord Bot
DISCORD_CLIENT_SECRET=your_discord_bot_token
DISCORD_CHANNEL_ID=your_discord_channel_id

# Server Configuration
SERVER_PORT=8080
SPRING_PROFILES_ACTIVE=dev
```

### 3. Setup PostgreSQL Database

```bash
# Connect to PostgreSQL
sudo -u postgres psql

# Create database and user
CREATE DATABASE area_db;
CREATE USER area_user WITH ENCRYPTED PASSWORD 'your_secure_password';
GRANT ALL PRIVILEGES ON DATABASE area_db TO area_user;

# Exit
\q
```

### 4. Install Dependencies

```bash
# Download dependencies
mvn dependency:resolve

# Or install project
mvn clean install
```

### 5. Run Database Migrations

```bash
# Hibernate will automatically create tables on first run
# Or you can use db-init scripts

# Run initialization scripts
psql -U area_user -d area_db -f db-init/init.sql
```

---

## Project Structure

```
server/
├── db-data/                      # PostgreSQL data (Docker volume)
├── db-init/                      # Database initialization scripts
│   └── init.sql                 # Schema and seed data
│
├── src/
│   ├── main/
│   │   ├── java/com/             # Java source code
│   │   │   ├── config/          # Configuration classes
│   │   │   │   ├── CorsConfig.java       # CORS settings
│   │   │   │   ├── SecurityConfig.java   # Security settings
│   │   │   │   └── JwtConfig.java        # JWT configuration
│   │   │   │
│   │   │   ├── controller/      # REST Controllers
│   │   │   │   ├── AuthController.java        # Authentication
│   │   │   │   ├── AreaController.java        # AREA management
│   │   │   │   ├── ServiceController.java     # Service management
│   │   │   │   └── UserController.java        # User management
│   │   │   │
│   │   │   ├── model/           # Entity models (JPA)
│   │   │   │   ├── User.java              # User entity
│   │   │   │   ├── Area.java              # AREA entity
│   │   │   │   ├── Service.java           # Service entity
│   │   │   │   └── ExecutionLog.java      # Log entity
│   │   │   │
│   │   │   ├── repository/      # Data access layer (JPA)
│   │   │   │   ├── UserRepository.java
│   │   │   │   ├── AreaRepository.java
│   │   │   │   └── ServiceRepository.java
│   │   │   │
│   │   │   ├── service/         # Business logic layer
│   │   │   │   ├── AuthService.java
│   │   │   │   ├── AreaService.java
│   │   │   │   ├── ServiceIntegrationService.java
│   │   │   │   └── ExecutionEngine.java
│   │   │   │
│   │   │   ├── dto/             # Data Transfer Objects
│   │   │   │   ├── LoginRequest.java
│   │   │   │   ├── AreaRequest.java
│   │   │   │   └── ServiceResponse.java
│   │   │   │
│   │   │   ├── security/        # Security components
│   │   │   │   ├── JwtUtil.java          # JWT utilities
│   │   │   │   ├── JwtFilter.java        # JWT authentication filter
│   │   │   │   └── UserDetailsServiceImpl.java
│   │   │   │
│   │   │   ├── exception/       # Exception handling
│   │   │   │   ├── GlobalExceptionHandler.java
│   │   │   │   ├── ResourceNotFoundException.java
│   │   │   │   └── UnauthorizedException.java
│   │   │   │
│   │   │   └── scheduler/       # Scheduled tasks
│   │   │       └── AreaScheduler.java    # Polling scheduler
│   │   │
│   │   └── resources/           # Configuration files
│   │       ├── application.properties    # Main config
│   │       ├── application-dev.properties    # Dev config
│   │       └── application-prod.properties   # Prod config
│   │
│   └── test/
│       └── java/com/             # Test classes
│           ├── controller/       # Controller tests
│           ├── service/          # Service tests
│           └── integration/      # Integration tests
│
├── Dockerfile                    # Docker build configuration
├── Jenkinsfile                   # CI/CD pipeline
├── pom.xml                       # Maven configuration
└── README.md                     # Server-specific README
```

---

## Architecture

### Layered Architecture

```
┌──────────────────────────────────────────┐
│         Presentation Layer               │
│  (Controllers, REST Endpoints, DTOs)     │
└──────────────┬───────────────────────────┘
               │
┌──────────────▼───────────────────────────┐
│         Business Logic Layer             │
│  (Services, Business Rules, Validators)  │
└──────────────┬───────────────────────────┘
               │
┌──────────────▼───────────────────────────┐
│         Data Access Layer                │
│  (Repositories, JPA, Entity Models)      │
└──────────────┬───────────────────────────┘
               │
┌──────────────▼───────────────────────────┐
│            Database                      │
│         (PostgreSQL)                     │
└──────────────────────────────────────────┘
```

### Request Flow

```
Client Request
     │
     ▼
┌─────────────┐
│ Controller  │ ← Validates input, maps to DTOs
└──────┬──────┘
       │
       ▼
┌─────────────┐
│  Service    │ ← Business logic, orchestration
└──────┬──────┘
       │
       ▼
┌─────────────┐
│ Repository  │ ← Data access, queries
└──────┬──────┘
       │
       ▼
┌─────────────┐
│  Database   │ ← Persistence
└─────────────┘
```

### Security Flow

```
HTTP Request
     │
     ▼
┌──────────────┐
│  JWT Filter  │ ← Extract & validate token
└──────┬───────┘
       │
       ▼
┌──────────────┐
│   Security   │ ← Load user details
│   Context    │
└──────┬───────┘
       │
       ▼
┌──────────────┐
│  Controller  │ ← Execute business logic
└──────────────┘
```

### Automation Engine

```
┌──────────────────┐
│  Area Scheduler  │ ← Runs every 60 seconds
└────────┬─────────┘
         │
         ▼
┌──────────────────┐
│  Fetch Active    │
│     AREAs        │
└────────┬─────────┘
         │
         ▼
┌──────────────────┐
│  Check Action    │
│   Triggers       │ ← Poll external services
└────────┬─────────┘
         │
         ▼
┌──────────────────┐
│  Execute         │
│  Reactions       │ ← Trigger actions
└────────┬─────────┘
         │
         ▼
┌──────────────────┐
│  Log Execution   │
└──────────────────┘
```

---

## Database Design

### Entity Relationship Diagram

```
┌─────────────┐         ┌─────────────┐
│    User     │         │   Service   │
├─────────────┤         ├─────────────┤
│ id (PK)     │         │ id (PK)     │
│ email       │         │ name        │
│ password    │         │ type        │
│ username    │         │ enabled     │
│ created_at  │         │ config      │
└──────┬──────┘         └──────┬──────┘
       │                       │
       │ 1                  N  │
       │                       │
       └───────┬───────────────┘
               │
               │ N
               │
        ┌──────▼──────┐
        │    Area     │
        ├─────────────┤
        │ id (PK)     │
        │ user_id (FK)│
        │ name        │
        │ action_svc  │
        │ action_type │
        │ action_cfg  │
        │ reaction_svc│
        │ reaction_typ│
        │ reaction_cfg│
        │ enabled     │
        │ created_at  │
        └──────┬──────┘
               │
               │ 1
               │
               │ N
        ┌──────▼──────────┐
        │ ExecutionLog    │
        ├─────────────────┤
        │ id (PK)         │
        │ area_id (FK)    │
        │ executed_at     │
        │ status          │
        │ message         │
        └─────────────────┘
```

### Key Tables

#### Users Table
```sql
CREATE TABLE users (
    id BIGSERIAL PRIMARY KEY,
    email VARCHAR(255) UNIQUE NOT NULL,
    password VARCHAR(255) NOT NULL,
    username VARCHAR(100) NOT NULL,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);
```

#### Areas Table
```sql
CREATE TABLE areas (
    id BIGSERIAL PRIMARY KEY,
    user_id BIGINT REFERENCES users(id) ON DELETE CASCADE,
    name VARCHAR(255) NOT NULL,
    action_service VARCHAR(100) NOT NULL,
    action_type VARCHAR(100) NOT NULL,
    action_config JSONB,
    reaction_service VARCHAR(100) NOT NULL,
    reaction_type VARCHAR(100) NOT NULL,
    reaction_config JSONB,
    enabled BOOLEAN DEFAULT true,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);
```

#### Services Table
```sql
CREATE TABLE services (
    id BIGSERIAL PRIMARY KEY,
    user_id BIGINT REFERENCES users(id) ON DELETE CASCADE,
    name VARCHAR(100) NOT NULL,
    type VARCHAR(50) NOT NULL,
    enabled BOOLEAN DEFAULT true,
    credentials JSONB,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);
```

---

## API Documentation

### Base URL
```
http://localhost:8080/api
```

### Authentication Endpoints

#### POST /api/auth/register
Register a new user.

**Request**:
```json
{
  "email": "user@example.com",
  "password": "SecurePassword123!",
  "username": "johndoe"
}
```

**Response** (200):
```json
{
  "id": 1,
  "email": "user@example.com",
  "username": "johndoe",
  "token": "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9..."
}
```

#### POST /api/auth/login
Authenticate user.

**Request**:
```json
{
  "email": "user@example.com",
  "password": "SecurePassword123!"
}
```

**Response** (200):
```json
{
  "token": "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...",
  "user": {
    "id": 1,
    "email": "user@example.com",
    "username": "johndoe"
  }
}
```

### Area Endpoints

#### GET /api/areas
Get all areas for authenticated user.

**Headers**:
```
Authorization: Bearer <token>
```

**Response** (200):
```json
[
  {
    "id": 1,
    "name": "GitHub to Discord",
    "actionService": "github",
    "actionType": "new_issue",
    "actionConfig": {
      "repository": "user/repo"
    },
    "reactionService": "discord",
    "reactionType": "send_message",
    "reactionConfig": {
      "webhookUrl": "https://..."
    },
    "enabled": true,
    "createdAt": "2024-12-11T10:00:00Z"
  }
]
```

#### POST /api/areas
Create new area.

**Headers**:
```
Authorization: Bearer <token>
Content-Type: application/json
```

**Request**:
```json
{
  "name": "Gmail to Discord",
  "actionService": "gmail",
  "actionType": "new_email",
  "actionConfig": {
    "from": "important@example.com"
  },
  "reactionService": "discord",
  "reactionType": "send_message",
  "reactionConfig": {
    "webhookUrl": "https://discord.com/api/webhooks/..."
  }
}
```

**Response** (201):
```json
{
  "id": 2,
  "name": "Gmail to Discord",
  "enabled": true,
  "createdAt": "2024-12-11T10:30:00Z"
}
```

#### PUT /api/areas/{id}
Update area.

**Headers**:
```
Authorization: Bearer <token>
Content-Type: application/json
```

**Request**:
```json
{
  "name": "Updated Name",
  "enabled": false
}
```

**Response** (200):
```json
{
  "id": 2,
  "name": "Updated Name",
  "enabled": false
}
```

#### DELETE /api/areas/{id}
Delete area.

**Headers**:
```
Authorization: Bearer <token>
```

**Response** (204): No content

### Service Endpoints

#### GET /api/services
Get available services.

**Response** (200):
```json
[
  {
    "id": "gmail",
    "name": "Gmail",
    "actions": ["new_email", "label_changed"],
    "reactions": ["send_email", "mark_read"]
  },
  {
    "id": "github",
    "name": "GitHub",
    "actions": ["new_issue", "new_pr", "push"],
    "reactions": ["create_issue", "comment"]
  }
]
```

#### POST /api/services/{serviceId}/connect
Connect service with OAuth2.

**Headers**:
```
Authorization: Bearer <token>
Content-Type: application/json
```

**Request**:
```json
{
  "code": "oauth_authorization_code"
}
```

**Response** (200):
```json
{
  "success": true,
  "service": "gmail",
  "connected": true
}
```

### User Endpoints

#### GET /api/user/profile
Get user profile.

**Headers**:
```
Authorization: Bearer <token>
```

**Response** (200):
```json
{
  "id": 1,
  "email": "user@example.com",
  "username": "johndoe",
  "createdAt": "2024-12-01T10:00:00Z",
  "connectedServices": ["gmail", "github"]
}
```

#### PUT /api/user/profile
Update user profile.

**Headers**:
```
Authorization: Bearer <token>
Content-Type: application/json
```

**Request**:
```json
{
  "username": "newusername"
}
```

**Response** (200):
```json
{
  "id": 1,
  "email": "user@example.com",
  "username": "newusername"
}
```

### About Endpoint

#### GET /api/about.json
Get server information (for mobile client).

**Response** (200):
```json
{
  "client": {
    "host": "10.0.2.2"
  },
  "server": {
    "current_time": 1733911200,
    "services": [
      {
        "name": "gmail",
        "actions": [
          {"name": "new_email", "description": "Trigger on new email"}
        ],
        "reactions": [
          {"name": "send_email", "description": "Send an email"}
        ]
      }
    ]
  }
}
```

---

## Build & Run

### Development Mode

#### Using Maven

```bash
# Run with embedded Tomcat
mvn spring-boot:run

# Run with specific profile
mvn spring-boot:run -Dspring-boot.run.profiles=dev

# Run with debug
mvn spring-boot:run -Dspring-boot.run.jvmArguments="-agentlib:jdwp=transport=dt_socket,server=y,suspend=n,address=*:5005"
```

#### Using IDE

**IntelliJ IDEA**:
1. Open `server/pom.xml` as project
2. Wait for Maven import
3. Run `ServerApplication.java`
4. Configure environment variables in Run Configuration

**Eclipse**:
1. Import as Maven project
2. Right-click project → Run As → Spring Boot App

### Build for Production

```bash
# Clean and build
mvn clean package

# Skip tests
mvn clean package -DskipTests

# Output: target/server-0.0.1-SNAPSHOT.jar
```

### Run JAR File

```bash
# Run built JAR
java -jar target/server-0.0.1-SNAPSHOT.jar

# With specific profile
java -jar -Dspring.profiles.active=prod target/server-0.0.1-SNAPSHOT.jar

# With environment variables
APP_JWT_SECRET=secret java -jar target/server-0.0.1-SNAPSHOT.jar
```

### Docker Build

```bash
# Build image
docker build -t area-server:latest .

# Run container
docker run -p 8080:8080 --env-file .env area-server:latest

# Or use docker-compose
cd ..
docker-compose up server
```

---

## Development Guide

### Creating a New Controller

```java
// src/main/java/com/area/controller/ExampleController.java
package com.area.controller;

import org.springframework.web.bind.annotation.*;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import com.area.model.User;

@RestController
@RequestMapping("/api/examples")
public class ExampleController {

    private final ExampleService exampleService;

    public ExampleController(ExampleService exampleService) {
        this.exampleService = exampleService;
    }

    @GetMapping
    public ResponseEntity<List<Example>> getAll(
        @AuthenticationPrincipal User user
    ) {
        return ResponseEntity.ok(exampleService.findByUser(user));
    }

    @PostMapping
    public ResponseEntity<Example> create(
        @Valid @RequestBody ExampleRequest request,
        @AuthenticationPrincipal User user
    ) {
        Example example = exampleService.create(request, user);
        return ResponseEntity.status(201).body(example);
    }
}
```

### Creating a Service

```java
// src/main/java/com/area/service/ExampleService.java
package com.area.service;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional
public class ExampleService {

    private final ExampleRepository repository;

    public ExampleService(ExampleRepository repository) {
        this.repository = repository;
    }

    public List<Example> findByUser(User user) {
        return repository.findByUserId(user.getId());
    }

    public Example create(ExampleRequest request, User user) {
        Example example = new Example();
        example.setUser(user);
        example.setName(request.getName());
        return repository.save(example);
    }
}
```

### Creating an Entity

```java
// src/main/java/com/area/model/Example.java
package com.area.model;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "examples")
public class Example {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String name;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt = LocalDateTime.now();

    // Getters and Setters
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public User getUser() { return user; }
    public void setUser(User user) { this.user = user; }
}
```

### Creating a Repository

```java
// src/main/java/com/area/repository/ExampleRepository.java
package com.area.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;

@Repository
public interface ExampleRepository extends JpaRepository<Example, Long> {
    List<Example> findByUserId(Long userId);
    List<Example> findByNameContaining(String name);
}
```

### Exception Handling

```java
// src/main/java/com/area/exception/GlobalExceptionHandler.java
package com.area.exception;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(ResourceNotFoundException.class)
    public ResponseEntity<ErrorResponse> handleNotFound(
        ResourceNotFoundException ex
    ) {
        ErrorResponse error = new ErrorResponse(
            HttpStatus.NOT_FOUND.value(),
            ex.getMessage()
        );
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(error);
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorResponse> handleGeneral(Exception ex) {
        ErrorResponse error = new ErrorResponse(
            HttpStatus.INTERNAL_SERVER_ERROR.value(),
            "Internal server error"
        );
        return ResponseEntity.status(500).body(error);
    }
}
```

---

## Testing

### Unit Tests

```java
// src/test/java/com/area/service/ExampleServiceTest.java
package com.area.service;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import static org.mockito.Mockito.*;
import static org.junit.jupiter.api.Assertions.*;

@ExtendWith(MockitoExtension.class)
class ExampleServiceTest {

    @Mock
    private ExampleRepository repository;

    @InjectMocks
    private ExampleService service;

    @Test
    void testFindByUser() {
        User user = new User();
        user.setId(1L);

        List<Example> examples = Arrays.asList(new Example());
        when(repository.findByUserId(1L)).thenReturn(examples);

        List<Example> result = service.findByUser(user);

        assertEquals(1, result.size());
        verify(repository).findByUserId(1L);
    }
}
```

### Integration Tests

```java
// src/test/java/com/area/controller/ExampleControllerTest.java
package com.area.controller;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.web.servlet.MockMvc;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(ExampleController.class)
class ExampleControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private ExampleService service;

    @Test
    void testGetAll() throws Exception {
        mockMvc.perform(get("/api/examples"))
            .andExpect(status().isOk())
            .andExpect(content().contentType("application/json"));
    }
}
```

### Running Tests

```bash
# Run all tests
mvn test

# Run specific test class
mvn test -Dtest=ExampleServiceTest

# Run with coverage
mvn test jacoco:report

# Skip tests
mvn install -DskipTests
```

---

## Deployment

### Production Configuration

```properties
# application-prod.properties
spring.profiles.active=prod

# Use production database
spring.datasource.url=${SPRING_DATASOURCE_URL}
spring.datasource.username=${SPRING_DATASOURCE_USERNAME}
spring.datasource.password=${SPRING_DATASOURCE_PASSWORD}

# Disable DDL auto
spring.jpa.hibernate.ddl-auto=validate
spring.jpa.show-sql=false

# Production logging
logging.level.root=WARN
logging.level.com.area=INFO

# Connection pool
spring.datasource.hikari.maximum-pool-size=50
spring.datasource.hikari.minimum-idle=10
```

### Docker Deployment

```bash
# Build image
docker build -t area-server:1.0 .

# Run with environment file
docker run -d \
  -p 8080:8080 \
  --name area-server \
  --env-file .env \
  area-server:1.0
```

### Systemd Service

```ini
# /etc/systemd/system/area-server.service
[Unit]
Description=AREA Server
After=postgresql.service

[Service]
User=area
WorkingDirectory=/opt/area
ExecStart=/usr/bin/java -jar /opt/area/server.jar
EnvironmentFile=/opt/area/.env
Restart=always

[Install]
WantedBy=multi-user.target
```

```bash
# Enable and start
sudo systemctl enable area-server
sudo systemctl start area-server
sudo systemctl status area-server
```

---

## Troubleshooting

### Common Issues

#### 1. Port Already in Use

**Error**: `Port 8080 is already in use`

**Solution**:
```bash
# Find process
lsof -i :8080

# Kill process
kill -9 <PID>

# Or change port
SERVER_PORT=8081 mvn spring-boot:run
```

#### 2. Database Connection Failed

**Error**: `Unable to connect to database`

**Solution**:
- Check PostgreSQL is running: `sudo systemctl status postgresql`
- Verify connection string in `.env`
- Test connection: `psql -U area_user -d area_db`

#### 3. JWT Token Invalid

**Error**: `401 Unauthorized`

**Solution**:
- Verify `APP_JWT_SECRET` is set
- Check token expiration
- Ensure token is sent in Authorization header

#### 4. Maven Build Fails

**Solution**:
```bash
# Clean and rebuild
mvn clean install -U

# Clear local repository
rm -rf ~/.m2/repository
mvn clean install
```

### Debug Mode

```bash
# Run with debug logging
mvn spring-boot:run -Dspring-boot.run.arguments=--logging.level.root=DEBUG

# Remote debugging
mvn spring-boot:run -Dspring-boot.run.jvmArguments="-agentlib:jdwp=transport=dt_socket,server=y,suspend=n,address=*:5005"
```

---

## Best Practices

1. **Use DTOs for API responses** - Don't expose entities directly
2. **Validate input** - Use @Valid and Bean Validation
3. **Handle exceptions globally** - Use @RestControllerAdvice
4. **Use transactions** - Mark service methods with @Transactional
5. **Secure sensitive data** - Never commit credentials
6. **Write tests** - Maintain good code coverage
7. **Use profiles** - Separate dev, test, prod configurations
8. **Log appropriately** - Use different log levels
9. **Document APIs** - Consider Spring Doc/Swagger
10. **Monitor performance** - Use Spring Actuator

---

*Last Updated: December 2025*
