# AREA Project - Complete Documentation

## 📖 Table of Contents

- [AREA Project - Complete Documentation](#area-project---complete-documentation)
  - [📖 Table of Contents](#-table-of-contents)
  - [Introduction](#introduction)
    - [Key Features](#key-features)
  - [Project Overview](#project-overview)
    - [System Components](#system-components)
    - [Directory Structure](#directory-structure)
  - [Architecture](#architecture)
    - [High-Level Architecture](#high-level-architecture)
    - [Data Flow](#data-flow)
  - [Installation](#installation)
    - [Prerequisites](#prerequisites)
      - [For Docker Deployment (Recommended)](#for-docker-deployment-recommended)
      - [For Local Development](#for-local-development)
    - [Quick Start with Docker](#quick-start-with-docker)
    - [Environment Configuration](#environment-configuration)
  - [Build Instructions](#build-instructions)
    - [Building with Docker](#building-with-docker)
    - [Building Locally](#building-locally)
      - [Backend (Spring Boot)](#backend-spring-boot)
      - [Web Client (Vue.js)](#web-client-vuejs)
      - [Mobile Client (Flutter)](#mobile-client-flutter)
  - [Usage](#usage)
    - [Starting the Application](#starting-the-application)
      - [With Docker (Recommended)](#with-docker-recommended)
      - [Local Development](#local-development)
    - [Using the Platform](#using-the-platform)
      - [1. User Registration/Login](#1-user-registrationlogin)
      - [2. Connecting Services](#2-connecting-services)
      - [3. Creating an AREA](#3-creating-an-area)
      - [4. Managing AREAs](#4-managing-areas)
    - [API Usage](#api-usage)
  - [Troubleshooting](#troubleshooting)
    - [Common Issues and Solutions](#common-issues-and-solutions)
      - [1. Docker Issues](#1-docker-issues)
      - [2. Database Issues](#2-database-issues)
      - [3. Backend Issues](#3-backend-issues)
      - [4. Web Client Issues](#4-web-client-issues)
      - [5. Mobile Issues](#5-mobile-issues)
      - [6. OAuth2 Issues](#6-oauth2-issues)
    - [Debug Mode](#debug-mode)
      - [Backend Debug Mode](#backend-debug-mode)
      - [Web Debug Mode](#web-debug-mode)
    - [Logging](#logging)
      - [View All Logs](#view-all-logs)
      - [Backend Logs](#backend-logs)
      - [Check System Resources](#check-system-resources)
  - [Deployment](#deployment)
    - [Production Deployment with Docker](#production-deployment-with-docker)
    - [CI/CD Pipeline](#cicd-pipeline)
    - [Monitoring](#monitoring)
  - [Contributing](#contributing)
    - [Development Workflow](#development-workflow)
    - [Code Standards](#code-standards)
    - [Testing](#testing)
  - [Additional Resources](#additional-resources)
  - [Support](#support)


---

## Introduction

AREA (Action-REAction) is a comprehensive automation platform inspired by IFTTT and Zapier. It enables users to create workflows by connecting actions from one service to reactions in another, providing a seamless automation experience across web and mobile platforms.

### Key Features

- **Multi-platform Support**: Web (Vue.js), Mobile (Flutter), and Backend (Spring Boot)
- **Service Integration**: Connect multiple services like Gmail, GitHub, Discord, and Timer
- **Real-time Automation**: Automatic execution of reactions when action conditions are met
- **OAuth2 Support**: Secure authentication with Google and GitHub
- **Docker-ready**: Containerized deployment for easy scaling

---

## Project Overview

### System Components

The AREA platform consists of three main components:

1. **Backend Server** (`/server`)
   - Spring Boot REST API
   - PostgreSQL database
   - JWT authentication
   - Service integration layer
   - Port: 8080

2. **Web Client** (`/web`)
   - Vue.js 3 single-page application
   - Vue Router for navigation
   - Vite build tool
   - Port: 80 (production), 8081 (development)

3. **Mobile Client** (`/mobile`)
   - Flutter framework for Android
   - Native performance
   - Shared codebase potential for iOS

### Directory Structure

```
AREA/
├── server/                 # Spring Boot backend
│   ├── src/
│   │   ├── main/java/com/  # Java source code
│   │   └── resources/      # Configuration files
│   ├── db-init/            # Database initialization scripts
│   ├── Dockerfile
│   └── pom.xml             # Maven configuration
│
├── web/                    # Vue.js web client
│   ├── src/
│   │   ├── components/     # Vue components
│   │   ├── services/       # API services
│   │   └── router/         # Route configuration
│   ├── Dockerfile
│   └── package.json        # Node dependencies
│
├── mobile/                 # Flutter mobile app
│   ├── lib/
│   │   ├── screens/        # App screens
│   │   ├── widgets/        # Reusable widgets
│   │   └── models/         # Data models
│   ├── android/            # Android-specific config
│   └── pubspec.yaml        # Flutter dependencies
│
├── docs/                   # Documentation
├── docker-compose.yml      # Docker orchestration
└── Jenkinsfile             # CI/CD pipeline
```

---

## Architecture

### High-Level Architecture

```
┌─────────────────┐         ┌─────────────────┐
│   Web Client    │         │  Mobile Client  │
│    (Vue.js)     │         │    (Flutter)    │
│   Port: 8081    │         │    Android      │
└────────┬────────┘         └────────┬────────┘
         │                           │
         │     HTTP/REST API         │
         │                           │
         └───────────┬───────────────┘
                     │
                     ▼
         ┌───────────────────────┐
         │   Backend Server      │
         │   (Spring Boot)       │
         │   Port: 8080          │
         └───────────┬───────────┘
                     │
                     │ JDBC
                     ▼
         ┌───────────────────────┐
         │   PostgreSQL          │
         │   Database            │
         │   Port: 8088 (ext)    │
         └───────────────────────┘
                     ▲
                     │
         ┌───────────┴───────────┐
         │  External Services    │
         │  - Gmail API          │
         │  - GitHub API         │
         │  - Discord Webhooks   │
         └───────────────────────┘
```

### Data Flow

1. **User Authentication**:
   - User registers/logs in via web or mobile
   - JWT token issued by backend
   - Token stored and sent with subsequent requests

2. **Service Connection**:
   - User connects external services (OAuth2)
   - Credentials securely stored in database
   - Service status monitored by backend

3. **AREA Creation**:
   - User selects action service and trigger
   - User selects reaction service and effect
   - Configuration saved to database
   - Hook system activates monitoring

4. **Automation Execution**:
   - Backend monitors action triggers
   - When condition met, reaction executed
   - Logs recorded for user review

---

## Installation

### Prerequisites

#### For Docker Deployment (Recommended)
- Docker Engine 20.10+
- Docker Compose 2.0+
- 2GB RAM minimum
- 5GB disk space

#### For Local Development
- **Backend**:
  - Java 21 (JDK)
  - Maven 3.8+
  - PostgreSQL 16

- **Web**:
  - Node.js 20.19+ or 22.12+
  - npm 10+

- **Mobile**:
  - Flutter SDK 3.10+
  - Android Studio
  - Android SDK API Level 21+

### Quick Start with Docker

1. **Clone the Repository**:
   ```bash
   git clone https://github.com/Aincrad-Flux/AREA.git
   cd AREA
   ```

2. **Configure Environment**:
   ```bash
   cd server
   cp .env.example .env
   # Edit .env with your configuration
   nano .env
   ```

3. **Start All Services**:
   ```bash
   cd ..
   docker-compose up -d
   ```

4. **Verify Installation**:
   - Backend: http://localhost:8080
   - Web: http://localhost:80
   - Database: localhost:8088

5. **Stop Services**:
   ```bash
   docker-compose down
   ```

### Environment Configuration

Create `server/.env` file with the following variables:

```env
# Database Configuration
POSTGRES_DB=area_db
POSTGRES_USER=area_user
POSTGRES_PASSWORD=your_secure_password

# JWT Configuration
JWT_SECRET=your_jwt_secret_key_min_256_bits
JWT_EXPIRATION=86400000

# OAuth2 - Google
GOOGLE_CLIENT_ID=your_google_client_id
GOOGLE_CLIENT_SECRET=your_google_client_secret
GOOGLE_REDIRECT_URI=http://localhost:8080/oauth2/callback/google

# OAuth2 - GitHub
GITHUB_CLIENT_ID=your_github_client_id
GITHUB_CLIENT_SECRET=your_github_client_secret
GITHUB_REDIRECT_URI=http://localhost:8080/oauth2/callback/github

# Gmail API
GMAIL_API_KEY=your_gmail_api_key
GMAIL_CLIENT_ID=your_gmail_client_id
GMAIL_CLIENT_SECRET=your_gmail_client_secret

# Discord Webhooks
DISCORD_WEBHOOK_URL=your_discord_webhook_url

# Application Settings
SERVER_PORT=8080
SPRING_PROFILES_ACTIVE=dev
```

---

## Build Instructions

### Building with Docker

**Build All Services**:
```bash
docker-compose build
```

**Build Specific Service**:
```bash
docker-compose build server
docker-compose build web
```

### Building Locally

#### Backend (Spring Boot)

```bash
cd server

# Install dependencies and build
mvn clean install

# Build without tests
mvn clean install -DskipTests

# Create JAR file
mvn package

# Output: target/server-0.0.1-SNAPSHOT.jar
```

#### Web Client (Vue.js)

```bash
cd web

# Install dependencies
npm install

# Development build
npm run dev

# Production build
npm run build

# Output: dist/
```

#### Mobile Client (Flutter)

```bash
cd mobile

# Get dependencies
flutter pub get

# Build APK (debug)
flutter build apk --debug

# Build APK (release)
flutter build apk --release

# Build App Bundle (for Play Store)
flutter build appbundle --release

# Output: build/app/outputs/
```

---

## Usage

### Starting the Application

#### With Docker (Recommended)

```bash
# Start all services
docker-compose up

# Start in detached mode
docker-compose up -d

# View logs
docker-compose logs -f

# View specific service logs
docker-compose logs -f server
```

#### Local Development

**Backend**:
```bash
cd server
mvn spring-boot:run
# Or
java -jar target/server-0.0.1-SNAPSHOT.jar
```

**Web**:
```bash
cd web
npm run dev
# Access at http://localhost:8081
```

**Mobile**:
```bash
cd mobile
flutter run
# Or use Android Studio
```

### Using the Platform

#### 1. User Registration/Login

**Via Web**:
- Navigate to http://localhost:80
- Click "Sign Up" or "Login"
- Register with email/password or use OAuth2

**Via API**:
```bash
# Register
curl -X POST http://localhost:8080/api/auth/register \
  -H "Content-Type: application/json" \
  -d '{
    "email": "user@example.com",
    "password": "securePassword123",
    "username": "johndoe"
  }'

# Login
curl -X POST http://localhost:8080/api/auth/login \
  -H "Content-Type: application/json" \
  -d '{
    "email": "user@example.com",
    "password": "securePassword123"
  }'
```

#### 2. Connecting Services

1. Navigate to "Services" section
2. Click "Connect" on desired service
3. Complete OAuth2 flow (if required)
4. Service appears as "Connected"

#### 3. Creating an AREA

1. Navigate to "Dashboard" or "Pipeline Editor"
2. Click "Create New AREA"
3. Select Action service and trigger
4. Configure action parameters
5. Select Reaction service and effect
6. Configure reaction parameters
7. Save and enable AREA

#### 4. Managing AREAs

- **View**: Dashboard shows all AREAs
- **Edit**: Click AREA to modify configuration
- **Enable/Disable**: Toggle switch
- **Delete**: Remove AREA permanently
- **Logs**: View execution history

### API Usage

In the root, you can find a postman collection `AREA_API.postman_collection.json` to test all available endpoints.

See [API Documentation](API_DOCUMENTATION.md) for detailed endpoint information. Or the swagger UI at: `http://localhost:8080/swagger-ui.html`

---

## Troubleshooting

### Common Issues and Solutions

#### 1. Docker Issues

**Problem**: Port already in use
```
Error: bind: address already in use
```

**Solution**:
```bash
# Check what's using the port
sudo lsof -i :8080
sudo lsof -i :80
sudo lsof -i :8088

# Kill the process or change port in docker-compose.yml
```

**Problem**: Container fails to start

**Solution**:
```bash
# Check logs
docker-compose logs server

# Restart specific service
docker-compose restart server

# Rebuild and restart
docker-compose up -d --build server
```

#### 2. Database Issues

**Problem**: Connection refused to PostgreSQL

**Solution**:
```bash
# Check if PostgreSQL is running
docker-compose ps

# Check health status
docker-compose exec postgres pg_isready -U area_user

# Reset database
docker-compose down -v
docker-compose up -d
```

**Problem**: Database not initializing

**Solution**:
- Check `server/db-init/` scripts
- Verify environment variables in `.env`
- Remove volume and recreate:
  ```bash
  docker-compose down -v
  sudo rm -rf server/db-data
  docker-compose up -d
  ```

#### 3. Backend Issues

**Problem**: Spring Boot won't start

**Solution**:
```bash
# Check Java version
java -version  # Should be 21

# Check if .env file exists
ls -la server/.env

# Run with debug logging
cd server
mvn spring-boot:run -Dspring-boot.run.arguments=--debug
```

**Problem**: 401 Unauthorized errors

**Solution**:
- Check JWT token is included in request
- Verify token hasn't expired
- Check JWT_SECRET in environment variables

#### 4. Web Client Issues

**Problem**: Cannot connect to backend

**Solution**:
- Verify `VITE_API_URL` in environment
- Check CORS configuration in backend
- Ensure backend is running:
  ```bash
  curl http://localhost:8080/actuator/health
  ```

**Problem**: Build fails

**Solution**:
```bash
# Clear cache and reinstall
cd web
rm -rf node_modules package-lock.json
npm install
npm run build
```

#### 5. Mobile Issues

**Problem**: Flutter build fails

**Solution**:
```bash
# Clean and rebuild
cd mobile
flutter clean
flutter pub get
flutter build apk
```

**Problem**: Cannot connect to API

**Solution**:
- For Android emulator, use `http://10.0.2.2:8080` instead of `localhost:8080`
- For physical device, use your machine's IP address
- Check network permissions in `AndroidManifest.xml`

#### 6. OAuth2 Issues

**Problem**: OAuth2 redirect fails

**Solution**:
- Verify redirect URI in OAuth2 provider settings
- Check `GOOGLE_REDIRECT_URI` and `GITHUB_REDIRECT_URI` in `.env`
- Ensure URIs match exactly (including http/https)

**Problem**: Token exchange fails

**Solution**:
- Verify client ID and secret
- Check OAuth2 provider console for errors
- Ensure proper scopes are requested

### Debug Mode

#### Backend Debug Mode
```bash
cd server
mvn spring-boot:run -Dspring-boot.run.jvmArguments="-agentlib:jdwp=transport=dt_socket,server=y,suspend=n,address=*:5005"
```

#### Web Debug Mode
```bash
cd web
npm run dev -- --debug
```

### Logging

#### View All Logs
```bash
docker-compose logs -f
```

#### Backend Logs
```bash
docker-compose logs -f server
# Or locally:
tail -f server/logs/spring-boot-logger.log
```

#### Check System Resources
```bash
docker stats
```

---

## Deployment

### Production Deployment with Docker

1. **Configure Production Environment**:
   ```bash
   # Update server/.env with production values
   SPRING_PROFILES_ACTIVE=prod
   # Use strong passwords and secrets
   ```

2. **Build for Production**:
   ```bash
   docker-compose build --no-cache
   ```

3. **Deploy**:
   ```bash
   docker-compose -f docker-compose.yml up -d
   ```

4. **Configure Reverse Proxy** (Nginx example):
   ```nginx
   server {
       listen 80;
       server_name area.yourdomain.com;

       location / {
           proxy_pass http://localhost:80;
       }

       location /api {
           proxy_pass http://localhost:8080;
       }
   }
   ```

### CI/CD Pipeline

The project includes Jenkinsfiles for automated builds:

- **Main Pipeline**: `/Jenkinsfile`
- **Server Pipeline**: `/server/Jenkinsfile`
- **Web Pipeline**: `/web/Jenkinsfile`
- **Mobile Pipeline**: `/mobile/Jenkinsfile`

### Monitoring

1. **Health Check Endpoints**:
   - Backend: http://localhost:8080/actuator/health
   - Web: http://localhost:80

2. **Database Monitoring**:
   ```bash
   docker-compose exec postgres psql -U area_user -d area_db
   ```

---

## Contributing

Please refer to [CONTRIBUTING.md](../CONTRIBUTING.md) for contribution guidelines.

### Development Workflow

1. Fork the repository
2. Create feature branch: `git checkout -b feature/my-feature`
3. Make changes and test
4. Commit: `git commit -am 'Add new feature'`
5. Push: `git push origin feature/my-feature`
6. Create Pull Request

### Code Standards

- **Java**: Follow Google Java Style Guide
- **JavaScript/Vue**: ESLint with recommended rules
- **Dart/Flutter**: Follow Dart style guide with `flutter_lints`

### Testing

```bash
# Backend tests
cd server && mvn test

# Web tests
cd web && npm test

# Mobile tests
cd mobile && flutter test
```

---

## Additional Resources

- **[Mobile Documentation](MOBILE.md)** - Flutter app specific docs
- **[Web Documentation](WEB.md)** - Vue.js client specific docs
- **[Server Documentation](SERVER.md)** - Spring Boot backend specific docs
- **[Stack Comparison](STACK_COMPARISON.md)** - Technology choices explained
- **[Gmail API Setup](services/GMAIL_API_SETUP.md)** - Gmail integration guide

---

## Support

For issues and questions:
- Create an issue on GitHub
- Check existing documentation
- Review troubleshooting section above

---

*Last Updated: December 2025*
