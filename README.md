# AREA - Action-Reaction Automation Platform

An automation platform inspired by IFTTT and Zapier that allows users to create workflows by connecting actions from one service to reactions in another.

## 📋 Table of Contents

- [AREA - Action-Reaction Automation Platform](#area---action-reaction-automation-platform)
  - [📋 Table of Contents](#-table-of-contents)
  - [🎯 Overview](#-overview)
  - [✨ Features](#-features)
    - [User Management](#user-management)
    - [AREA Creation](#area-creation)
    - [Supported Services](#supported-services)
    - [Hook System](#hook-system)
  - [🏗️ Architecture](#️-architecture)
  - [🛠️ Tech Stack](#️-tech-stack)
    - [Backend](#backend)
    - [Web Frontend](#web-frontend)
    - [Mobile](#mobile)
    - [DevOps](#devops)
  - [📦 Prerequisites](#-prerequisites)
  - [🚀 Installation \& Setup](#-installation--setup)
    - [Quick Start with Docker](#quick-start-with-docker)
    - [Environment Variables](#environment-variables)
    - [Local Development Setup](#local-development-setup)
      - [Backend (Spring Boot)](#backend-spring-boot)
      - [Web Client (Vue.js)](#web-client-vuejs)
      - [Mobile Client (Flutter)](#mobile-client-flutter)
  - [🔌 Available Services](#-available-services)
    - [Timer Service](#timer-service)
    - [Gmail Service](#gmail-service)
    - [GitHub Service](#github-service)
    - [Discord Service](#discord-service)
  - [Example AREA Workflows](#example-area-workflows)
  - [🤝 Contributing](#-contributing)
  - [👥 Team](#-team)
  - [License](#license)
  - [🐛 Known Issues](#-known-issues)
  - [🔮 Future Enhancements](#-future-enhancements)


## 🎯 Overview

AREA is a comprehensive automation platform that enables users to create custom workflows by connecting various online services. The platform consists of three main components:

- **Application Server**: REST API handling all business logic
- **Web Client**: Browser-based interface for managing automations
- **Mobile Client**: Android application for on-the-go access

## ✨ Features

### User Management
- Registration and authentication with email/password
- OAuth2 integration (Google, GitHub)
- User profile management
- Service connection management

### AREA Creation
- Connect actions from one service to reactions in another
- Configure parameters for actions and reactions
- Enable/disable areas without deletion
- View execution history and logs

### Supported Services
- **Timer**: Scheduled triggers based on time/date
- **Gmail**: Email-based actions and reactions
- **GitHub**: Repository event monitoring and actions
- **Discord**: Webhook-based notifications

### Hook System
- Automatic execution of reactions when action conditions are met
- Real-time monitoring of connected services
- Error handling and retry logic
- Execution logging and history

## 🏗️ Architecture

```
┌─────────────┐         ┌─────────────┐
│ Web Client  │────────▶│             │
│  (Vue.js)   │         │             │
│  Port 8081  │         │   Spring    │
└─────────────┘         │    Boot     │◀────┐
                        │   Server    │     │
┌─────────────┐         │  Port 8080  │     │
│   Mobile    │────────▶│             │     │
│  (Flutter)  │         │             │     │
│   Android   │         └──────┬──────┘     │
└─────────────┘                │            │
                               │            │
                        ┌──────▼──────┐     │
                        │ PostgreSQL  │     │
                        │  Database   │     │
                        └─────────────┘     │
                                            │
                        External Services ──┘
                        (Gmail, GitHub, etc.)
```

The application follows a client-server architecture where:
- Clients handle UI/UX only
- Server manages all business logic
- Communication via REST API
- OAuth2 for service authentication

## 🛠️ Tech Stack

### Backend
- **Framework**: Spring Boot 3.x
- **Language**: Java 17+
- **Database**: PostgreSQL
- **Security**: Spring Security + JWT
- **OAuth2**: Spring Security OAuth2 Client
- **Documentation**: Springdoc OpenAPI (Swagger)

### Web Frontend
- **Framework**: Vue 3
- **Build Tool**: Vite
- **HTTP Client**: Axios
- **Routing**: Vue Router

### Desktop (optional)
- **Wrapper**: Tauri (re-uses the Web UI)
- **Targets**: Windows / macOS / Linux (built on each OS)
- **Styling**: CSS3 with custom design system

### Mobile
- **Framework**: Flutter
- **Language**: Dart
- **Platform**: Android
- **HTTP**: http package
- **Storage**: shared_preferences

### DevOps
- **Containerization**: Docker & Docker Compose
- **CI/CD**: Jenkins
- **Code Quality**: SonarQube

## 📦 Prerequisites

- **Docker** 20.10+
- **Docker Compose** 2.0+
- **Git**

For local development without Docker:
- **Java** 17+
- **Node.js** 18+
- **Flutter** 3.10+
- **PostgreSQL** 14+

## 🚀 Installation & Setup

### Quick Start with Docker

1. **Clone the repository**
```bash
git clone <repository-url>
cd area
```

2. **Build all services**
```bash
docker-compose build
```

3. **Start the application**
```bash
docker-compose up
```

4. **Access the services**
- Web Client: http://localhost:8081
- API Server: http://localhost:8080
- API Documentation: http://localhost:8080/swagger-ui.html
- Download Mobile APK: http://localhost:8081/client.apk

### Environment Variables

Create a `.env` file in the root directory:

```env
# Database Configuration
POSTGRES_DB=area_db
POSTGRES_USER=area_user
POSTGRES_PASSWORD=secure_password

# JWT Configuration
JWT_SECRET=your-secret-key-here
JWT_EXPIRATION=86400000

# OAuth2 - Google
GOOGLE_CLIENT_ID=your-google-client-id
GOOGLE_CLIENT_SECRET=your-google-client-secret

# OAuth2 - GitHub
GITHUB_CLIENT_ID=your-github-client-id
GITHUB_CLIENT_SECRET=your-github-client-secret

# Server Configuration
SERVER_URL=http://localhost:8080
WEB_URL=http://localhost:8081
```

### Local Development Setup

#### Backend (Spring Boot)
```bash
cd server
./mvnw spring-boot:run
```

#### Web Client (Vue.js)
```bash
cd web
npm install
npm run dev
```

#### Desktop App (Tauri)

The desktop app reuses the Web UI (Vue/Vite) and is packaged with Tauri.

Important: for native installers/bundles you generally build on each OS (Linux build on Linux, Windows build on Windows, macOS build on macOS).

Prerequisites (all OS):
- Node.js + npm
- Rust toolchain (cargo + rustc, typically installed via rustup)

Linux note: depending on your distro you may need additional system packages for WebView (GTK/WebKit) before `tauri build` works.

```bash
# from repo root
./scripts/build-desktop.sh
```

Outputs are copied into `bin/<os>/latest/` (and `bin/<os>/<version>/` when the version is available).

#### Mobile Client (Flutter)
```bash
cd mobile
flutter pub get
flutter run
```

## 🔌 Available Services

### Timer Service
**Actions:**
- Time matches HH:MM format
- Current date matches DD/MM format
- Specific day of week
- Every X minutes

**Reactions:**
- Execute at scheduled time

### Gmail Service
**Actions:**
- Email received from specific sender
- Email received with subject containing keyword

**Reactions:**
- Send email to recipient

### GitHub Service
**Actions:**
- New issue created on repository
- New push to repository branch

**Reactions:**
- Create issue on repository
- Create comment on issue

### Discord Service
**Reactions:**
- Send message to Discord webhook


## Example AREA Workflows

1. **Daily Standup Reminder**
   - Action: Timer - Every day at 09:00
   - Reaction: Discord - Send message to webhook

2. **GitHub to Discord Notifications**
   - Action: GitHub - New issue created
   - Reaction: Discord - Send notification

3. **Email to GitHub Issue**
   - Action: Gmail - Email received with specific subject
   - Reaction: GitHub - Create issue from email content

## 🤝 Contributing

Please read [CONTRIBUTING.md](CONTRIBUTING.md) for details on:
- Adding new services
- Creating new actions and reactions
- Code style guidelines
- Pull request process

## 👥 Team

Made by Aincrad-Flux Organisation by the SOURCE team members:
- [Thibault POUCH](https://github.com/ThibaultPouch59200)   - Mobile / CICD Developer
- [Dany LHOIR](https://github.com/DanyLhoir)       - Backend Developer
- [Antoine PAPILLON](https://github.com/Antoine2507) - Web Developer

## License

This project is licensed under the GNU GENERAL PUBLIC LICENSE (GPL) License - see the [LICENSE](LICENSE) file for details.
And this project is from the Epitech PGE curriculum, all subject returns to Epitech. See the [NOTICE](NOTICE.md) file for details.

## 🐛 Known Issues

- OAuth callback requires proper redirect URI configuration
- Mobile app requires Android API level 21+
- Some services may have rate limiting

## 🔮 Future Enhancements

- iOS mobile client
- More service integrations (Slack, Trello, Notion)
- Advanced scheduling options
- Area templates and sharing
- Multi-language support
- Desktop application

---

For questions or issues, please open an issue on the project repository.