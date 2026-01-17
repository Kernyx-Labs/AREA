# AREA Mobile App - Setup & Configuration Guide

## Table of Contents
1. [Prerequisites](#prerequisites)
2. [Environment Setup](#environment-setup)
3. [Backend Configuration](#backend-configuration)
4. [Mobile App Configuration](#mobile-app-configuration)
5. [API Endpoints Reference](#api-endpoints-reference)
6. [Running the App](#running-the-app)
7. [Troubleshooting](#troubleshooting)

---

## Prerequisites

### Required Software

| Software | Version | Download |
|----------|---------|----------|
| Flutter SDK | 3.10+ | https://flutter.dev/docs/get-started/install |
| Android Studio | Latest | https://developer.android.com/studio |
| Android SDK | 33+ | Via Android Studio SDK Manager |
| Java JDK | 17+ | For backend development |

### Android SDK Components (via SDK Manager)

- Android SDK Platform 33+
- Android SDK Build-Tools
- Android SDK Command-line Tools (latest)
- Android Emulator
- Android SDK Platform-Tools

---

## Environment Setup

### 1. Flutter Installation (Linux/Fedora)

```bash
# Download Flutter
cd ~
git clone https://github.com/flutter/flutter.git -b stable

# Add to PATH (add to ~/.bashrc or ~/.zshrc)
export PATH="$PATH:$HOME/flutter/bin"

# Verify installation
flutter doctor
```

### 2. Android SDK Setup

```bash
# Set environment variables (add to ~/.bashrc or ~/.zshrc)
export ANDROID_HOME="$HOME/Android/Sdk"
export PATH="$PATH:$ANDROID_HOME/emulator"
export PATH="$PATH:$ANDROID_HOME/platform-tools"
export PATH="$PATH:$ANDROID_HOME/cmdline-tools/latest/bin"
```

### 3. Install Android cmdline-tools (if missing)

```bash
mkdir -p ~/Android/Sdk/cmdline-tools
cd ~/Android/Sdk/cmdline-tools
wget https://dl.google.com/android/repository/commandlinetools-linux-11076708_latest.zip
unzip commandlinetools-linux-11076708_latest.zip
mv cmdline-tools latest
rm commandlinetools-linux-11076708_latest.zip
```

### 4. Accept Android Licenses

```bash
flutter doctor --android-licenses
# Press 'y' for each license
```

---

## Backend Configuration

### Backend Server URL

The mobile app connects to the backend API. The URL is configured in:

**File:** `lib/services/api_config.dart`

```dart
class ApiConfig {
  // Android emulator uses 10.0.2.2 to reach host's localhost
  static const String baseUrl = 'http://10.0.2.2:8080';

  // iOS simulator uses localhost directly
  static const String iosBaseUrl = 'http://localhost:8080';

  // For physical devices, use your computer's local IP
  // static const String baseUrl = 'http://192.168.1.XXX:8080';
}
```

### Finding Your Local IP (for physical devices)

```bash
# Linux
ip addr show | grep "inet " | grep -v 127.0.0.1

# Or
hostname -I
```

### URL Mapping

| Device Type | URL | Notes |
|-------------|-----|-------|
| Android Emulator | `http://10.0.2.2:8080` | Maps to host localhost |
| iOS Simulator | `http://localhost:8080` | Direct localhost access |
| Physical Device | `http://YOUR_IP:8080` | Use computer's LAN IP |

---

## Mobile App Configuration

### Project Structure

```
mobile/
├── lib/
│   ├── main.dart                 # App entry point
│   ├── app.dart                  # Main app widget & navigation
│   ├── blocs/                    # State management (Bloc pattern)
│   │   ├── auth/                 # Authentication state
│   │   │   ├── auth_bloc.dart
│   │   │   ├── auth_event.dart
│   │   │   └── auth_state.dart
│   │   └── workflow/             # Workflow state
│   │       ├── workflow_bloc.dart
│   │       ├── workflow_event.dart
│   │       └── workflow_state.dart
│   ├── constants/                # App constants
│   │   ├── palette.dart          # Color scheme
│   │   ├── pipeline_layout.dart  # Editor layout constants
│   │   └── shadows.dart          # Shadow styles
│   ├── models/                   # Data models
│   │   ├── auth_models.dart      # Login/Register models
│   │   ├── node_models.dart      # Pipeline node models
│   │   ├── service_definition.dart
│   │   ├── user.dart             # User model
│   │   └── workflow.dart         # Workflow model
│   ├── screens/                  # UI screens
│   │   ├── auth_wrapper.dart     # Auth navigation
│   │   ├── dashboard_screen.dart # Workflow list
│   │   ├── login_screen.dart     # Login UI
│   │   ├── pipeline_editor_screen.dart
│   │   ├── profile_screen.dart   # User profile
│   │   ├── register_screen.dart  # Registration UI
│   │   ├── services_screen.dart  # Service connections
│   │   └── splash_screen.dart    # Loading screen
│   ├── services/                 # API services
│   │   ├── api_client.dart       # HTTP client (Dio)
│   │   ├── api_config.dart       # API configuration
│   │   ├── auth_service.dart     # Auth API calls
│   │   ├── service_registry.dart # Service connections
│   │   ├── token_storage.dart    # Secure token storage
│   │   └── workflow_service.dart # Workflow API calls
│   └── widgets/                  # Reusable widgets
│       ├── canvas/
│       │   ├── connection_painter.dart
│       │   └── grid_painter.dart
│       ├── node_picker.dart
│       └── pipeline_node_widget.dart
├── pubspec.yaml                  # Dependencies
└── SETUP.md                      # This file
```

### Dependencies

```yaml
dependencies:
  flutter:
    sdk: flutter
  cupertino_icons: ^1.0.8
  dio: ^5.4.0                    # HTTP client
  flutter_bloc: ^8.1.3           # State management
  equatable: ^2.0.5              # Value equality
  flutter_secure_storage: ^9.0.0 # Secure token storage
  json_annotation: ^4.8.1        # JSON serialization
  url_launcher: ^6.2.1           # OAuth URL handling

dev_dependencies:
  flutter_test:
    sdk: flutter
  flutter_lints: ^6.0.0
  build_runner: ^2.4.7
  json_serializable: ^6.7.1
```

### Color Palette

**File:** `lib/constants/palette.dart`

```dart
class AppPalette {
  static const primary = Color(0xFF3B4883);      // Primary blue
  static const accent = Color(0xFFFF7124);       // Orange accent
  static const background = Color(0xFFE8DFD5);   // Beige background
  static const surface = Color(0xFFDBBBA7);      // Card surface
  static const nodeAction = Color(0xFF3B4883);   // Trigger node color
  static const nodeReaction = Color(0xFFA35139); // Action node color
  static const dark = Color(0xFF202124);         # Dark text/nav
  static const canvas = Color(0xFFDBBBA7);       # Editor canvas
  static const surfaceText = Color(0xFF6B6B6B);  # Secondary text
}
```

---

## API Endpoints Reference

### Authentication

| Method | Endpoint | Description | Auth Required |
|--------|----------|-------------|---------------|
| POST | `/auth/register` | Register new user | No |
| POST | `/auth/login` | Login | No |
| POST | `/auth/refresh` | Refresh access token | No |
| POST | `/auth/logout` | Logout | No |
| GET | `/auth/me` | Get current user | Yes |

#### Register Request
```json
{
  "email": "user@example.com",
  "username": "username123",
  "password": "password123",
  "fullName": "John Doe"  // optional
}
```

#### Login Request
```json
{
  "email": "user@example.com",
  "password": "password123"
}
```

#### Auth Response
```json
{
  "success": true,
  "message": "Login successful",
  "data": {
    "accessToken": "eyJhbGciOiJIUzI1NiIs...",
    "refreshToken": "eyJhbGciOiJIUzI1NiIs...",
    "tokenType": "Bearer",
    "expiresIn": 3600000,
    "user": {
      "id": 1,
      "email": "user@example.com",
      "username": "username123",
      "fullName": "John Doe"
    }
  }
}
```

### Workflows

| Method | Endpoint | Description |
|--------|----------|-------------|
| GET | `/api/workflows` | List all workflows |
| GET | `/api/workflows/{id}` | Get workflow by ID |
| POST | `/api/workflows` | Create workflow |
| PUT | `/api/workflows/{id}` | Update workflow |
| PATCH | `/api/workflows/{id}/status` | Toggle active status |
| DELETE | `/api/workflows/{id}` | Delete workflow |
| POST | `/api/workflows/{id}/execute` | Manual execution |
| GET | `/api/workflows/available-nodes` | Get available triggers/actions |
| GET | `/api/workflows/{id}/stats` | Get execution stats |

#### Create Workflow Request
```json
{
  "name": "My Workflow",
  "trigger": {
    "service": "GMAIL",
    "type": "new_email",
    "config": {
      "from": "sender@example.com",
      "subject": "Important"
    },
    "connectionId": 1
  },
  "actions": [
    {
      "service": "DISCORD",
      "type": "send_message",
      "config": {
        "message": "New email: {{subject}}"
      },
      "connectionId": 2
    }
  ]
}
```

#### Workflow Response
```json
{
  "success": true,
  "data": {
    "id": 1,
    "name": "My Workflow",
    "description": "Trigger: gmail → Actions: discord",
    "active": true,
    "createdAt": "2024-01-15T10:30:00Z",
    "updatedAt": "2024-01-15T10:30:00Z",
    "workflowData": {
      "trigger": { ... },
      "actions": [ ... ]
    }
  }
}
```

### Services

| Method | Endpoint | Description |
|--------|----------|-------------|
| GET | `/api/services` | List all services |
| GET | `/api/services/{type}` | Get service by type |
| GET | `/api/service-connections` | List user's connections |
| DELETE | `/api/service-connections/{id}` | Delete connection |

### OAuth Endpoints

| Method | Endpoint | Description |
|--------|----------|-------------|
| GET | `/api/services/gmail/auth-url` | Get Gmail OAuth URL |
| GET | `/api/services/github/auth-url` | Get GitHub OAuth URL |
| POST | `/api/services/discord/connect` | Connect Discord bot |

---

## Running the App

### 1. Start the Backend

```bash
cd ~/Delivery/AREA
docker-compose up -d

# Or run directly with Gradle
cd server
./gradlew bootRun
```

Verify backend is running:
```bash
curl http://localhost:8080/about.json
```

### 2. Install Flutter Dependencies

```bash
cd ~/Delivery/AREA/mobile
flutter pub get
```

### 3. Run on Android Emulator

```bash
# List available emulators
flutter emulators

# Launch emulator
flutter emulators --launch Medium_Phone_API_36.1

# Wait for emulator to boot, then run
flutter run
```

### 4. Run on Physical Device

1. Enable **Developer Options** on your phone
   - Go to Settings → About Phone
   - Tap "Build Number" 7 times

2. Enable **USB Debugging**
   - Go to Settings → Developer Options
   - Enable "USB Debugging"

3. Connect via USB and authorize

4. Update API URL for your network:
   ```dart
   // In lib/services/api_config.dart
   static const String baseUrl = 'http://192.168.1.XXX:8080';
   ```

5. Run the app:
   ```bash
   flutter devices  # Verify device is detected
   flutter run
   ```

### 5. Build APK

```bash
# Debug APK
flutter build apk --debug

# Release APK
flutter build apk --release

# APK location
ls build/app/outputs/flutter-apk/
```

---

## Troubleshooting

### Common Issues

#### 1. "cmdline-tools component is missing"

```bash
# Install cmdline-tools
mkdir -p ~/Android/Sdk/cmdline-tools
cd ~/Android/Sdk/cmdline-tools
wget https://dl.google.com/android/repository/commandlinetools-linux-11076708_latest.zip
unzip commandlinetools-linux-11076708_latest.zip
mv cmdline-tools latest
```

#### 2. "Android license status unknown"

```bash
flutter doctor --android-licenses
# Press 'y' for each license
```

#### 3. "Connection refused" to backend

- Verify backend is running: `curl http://localhost:8080/about.json`
- For emulator, use `10.0.2.2` not `localhost`
- For physical device, use your computer's LAN IP
- Check firewall: `sudo firewall-cmd --add-port=8080/tcp`

#### 4. "Gradle build failed"

```bash
cd ~/Delivery/AREA/mobile/android
./gradlew clean
cd ..
flutter clean
flutter pub get
flutter run
```

#### 5. Emulator not starting

```bash
# Check if virtualization is enabled
egrep -c '(vmx|svm)' /proc/cpuinfo  # Should be > 0

# Kill existing emulator processes
adb kill-server
killall qemu-system-x86_64

# Restart emulator
flutter emulators --launch Medium_Phone_API_36.1
```

#### 6. "INSTALL_FAILED_INSUFFICIENT_STORAGE"

```bash
# Clear emulator data
adb shell pm clear com.example.area
# Or wipe emulator data via AVD Manager in Android Studio
```

### Debug Commands

```bash
# Check Flutter setup
flutter doctor -v

# List connected devices
flutter devices

# View app logs
flutter logs

# Run with verbose output
flutter run -v

# Check Android SDK location
echo $ANDROID_HOME

# ADB commands
adb devices          # List devices
adb logcat           # View device logs
adb shell            # Shell into device
```

---

## Token Storage

Tokens are stored securely using `flutter_secure_storage`:

- **Android**: EncryptedSharedPreferences
- **iOS**: Keychain

Storage keys:
- `access_token` - JWT access token (1 hour expiry)
- `refresh_token` - Refresh token (7 days expiry)
- `user_id` - Current user ID

---

## Authentication Flow

```
┌─────────────┐     ┌─────────────┐     ┌─────────────┐
│   App Start │────▶│ Check Token │────▶│  Has Token? │
└─────────────┘     └─────────────┘     └──────┬──────┘
                                               │
                    ┌──────────────────────────┼──────────────────────────┐
                    │ No                       │                     Yes  │
                    ▼                          │                          ▼
           ┌─────────────┐                     │                 ┌─────────────┐
           │ Login Screen│                     │                 │  GET /auth/me│
           └──────┬──────┘                     │                 └──────┬──────┘
                  │                            │                        │
                  ▼                            │                        ▼
           ┌─────────────┐                     │                 ┌─────────────┐
           │POST /auth/  │                     │                 │   Valid?    │
           │   login     │                     │                 └──────┬──────┘
           └──────┬──────┘                     │                        │
                  │                            │         ┌──────────────┼──────────────┐
                  ▼                            │         │ No           │         Yes  │
           ┌─────────────┐                     │         ▼              │              ▼
           │ Save Tokens │                     │  ┌─────────────┐       │       ┌─────────────┐
           └──────┬──────┘                     │  │Clear Tokens │       │       │  Home Screen│
                  │                            │  └──────┬──────┘       │       └─────────────┘
                  └────────────────────────────┴─────────┴──────────────┘
```

---

## Service Types

Available service integrations:

| Service | Type | Actions (Triggers) | Reactions |
|---------|------|-------------------|-----------|
| Gmail | `GMAIL` | New email, Email with attachment | Send email |
| Discord | `DISCORD` | - | Send message |
| GitHub | `GITHUB` | New issue, New PR | Create issue, Create PR |
| Timer | `TIMER` | Schedule, Interval | - |

---

## Support

For issues and feature requests:
- GitHub Issues: https://github.com/your-repo/area/issues
- Documentation: This file and inline code comments
