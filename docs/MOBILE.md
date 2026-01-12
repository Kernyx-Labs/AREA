# AREA Mobile Application Documentation

## 📱 Table of Contents

1. [Overview](#overview)
2. [Technology Stack](#technology-stack)
3. [Prerequisites](#prerequisites)
4. [Installation & Setup](#installation--setup)
5. [Project Structure](#project-structure)
6. [Architecture](#architecture)
7. [Build & Run](#build--run)
8. [Development Guide](#development-guide)
9. [Testing](#testing)
10. [Deployment](#deployment)
11. [Troubleshooting](#troubleshooting)

---

## Overview

The AREA mobile application is an Android client built with Flutter that provides users with on-the-go access to their automation workflows. The app mirrors the functionality of the web client while offering a native mobile experience.

### Key Features

- **User Authentication**: Email/password and OAuth2 (Google, GitHub)
- **Dashboard View**: Visual overview of all AREAs and their status
- **Pipeline Editor**: Drag-and-drop interface for creating workflows
- **Service Management**: Connect and manage external services
- **Profile Management**: User settings and service connections
- **Real-time Updates**: Monitor AREA executions
- **Offline Support**: Cache critical data for offline viewing

### Target Platforms

- **Primary**: Android 5.0 (API Level 21) and above
- **Future**: iOS support (Flutter supports cross-platform)

---

## Technology Stack

### Core Framework
- **Flutter**: 3.10.1+
- **Dart SDK**: 3.10.1+

### Key Dependencies
- **cupertino_icons**: ^1.0.8 - iOS-style icons

### Dev Dependencies
- **flutter_test**: Testing framework
- **flutter_lints**: ^6.0.0 - Linting rules

### Architecture Pattern
- **Provider/Bloc** (recommended for state management)
- **Clean Architecture** principles
- **Repository Pattern** for data layer

---

## Prerequisites

### Required Software

1. **Flutter SDK**:
   ```bash
   # Download from https://flutter.dev/docs/get-started/install
   flutter --version  # Should be 3.10+
   ```

2. **Android Studio**:
   - Download from https://developer.android.com/studio
   - Install Android SDK
   - Install Android SDK Platform-Tools
   - Install Android SDK Build-Tools

3. **Android SDK**:
   - Minimum SDK: API 21 (Android 5.0)
   - Target SDK: API 34 (Android 14)
   - Compile SDK: API 34

4. **Java Development Kit (JDK)**:
   - JDK 17 or higher
   ```bash
   java -version
   ```

5. **Android Device/Emulator**:
   - Physical Android device with USB debugging enabled
   - OR Android Emulator via Android Studio

### Optional Tools

- **VS Code** with Flutter extension
- **Dart DevTools** for debugging
- **Android Debug Bridge (ADB)**

---

## Installation & Setup

### 1. Install Flutter

**Linux**:
```bash
# Download Flutter
cd ~
wget https://storage.googleapis.com/flutter_infra_release/releases/stable/linux/flutter_linux_3.10.1-stable.tar.xz

# Extract
tar xf flutter_linux_3.10.1-stable.tar.xz

# Add to PATH
echo 'export PATH="$PATH:$HOME/flutter/bin"' >> ~/.bashrc
source ~/.bashrc

# Verify installation
flutter doctor
```

**macOS**:
```bash
# Using Homebrew
brew install flutter

# Or download manually
# https://docs.flutter.dev/get-started/install/macos
```

**Windows**:
- Download Flutter SDK from https://flutter.dev
- Extract to C:\src\flutter
- Add to PATH environment variable

### 2. Setup Android Development

```bash
# Accept Android licenses
flutter doctor --android-licenses

# Verify setup
flutter doctor -v
```

### 3. Clone and Setup Project

```bash
# Navigate to project
cd AREA/mobile

# Get dependencies
flutter pub get

# Verify no issues
flutter doctor
```

### 4. Configure API Endpoint

Update the API base URL in your configuration:

```dart
// lib/constants/api_config.dart (create if needed)
class ApiConfig {
  // For Android Emulator
  static const String baseUrl = 'http://10.0.2.2:8080';

  // For Physical Device (replace with your machine's IP)
  // static const String baseUrl = 'http://192.168.1.100:8080';

  // For Production
  // static const String baseUrl = 'https://api.area.com';
}
```

---

## Project Structure

```
mobile/
├── android/                      # Android-specific configuration
│   ├── app/
│   │   ├── src/
│   │   │   ├── main/
│   │   │   │   ├── AndroidManifest.xml   # App permissions & config
│   │   │   │   ├── kotlin/               # Native Android code
│   │   │   │   └── res/                  # Android resources
│   │   │   ├── debug/                    # Debug build config
│   │   │   └── profile/                  # Profile build config
│   │   └── build.gradle.kts              # App-level Gradle config
│   ├── gradle/                           # Gradle wrapper
│   ├── build.gradle.kts                  # Project-level Gradle
│   ├── settings.gradle.kts
│   └── gradle.properties
│
├── lib/                          # Dart source code
│   ├── main.dart                # App entry point
│   ├── app.dart                 # Main app widget
│   │
│   ├── constants/               # App constants
│   │   ├── palette.dart        # Color palette
│   │   ├── pipeline_layout.dart # Layout constants
│   │   └── shadows.dart        # Shadow styles
│   │
│   ├── models/                  # Data models
│   │   ├── models.dart         # General models
│   │   ├── node_models.dart    # Pipeline node models
│   │   ├── service_definition.dart # Service definitions
│   │   └── workflow.dart       # Workflow models
│   │
│   ├── screens/                 # App screens (pages)
│   │   ├── dashboard_screen.dart      # Main dashboard
│   │   ├── pipeline_editor_screen.dart # Visual editor
│   │   ├── profile_screen.dart        # User profile
│   │   └── services_screen.dart       # Service management
│   │
│   └── widgets/                 # Reusable UI components
│       ├── node_picker.dart           # Node selection widget
│       ├── pipeline_node_widget.dart  # Node display widget
│       └── canvas/
│           ├── connection_painter.dart # Draw connections
│           └── grid_painter.dart      # Background grid
│
├── test/                         # Unit and widget tests
│   ├── widget_test.dart
│   ├── app/
│   ├── helpers/
│   └── screens/
│
├── analysis_options.yaml         # Linting rules
├── pubspec.yaml                  # Dependencies & config
├── pubspec.lock                  # Locked dependency versions
├── README.md                     # Mobile-specific README
└── Jenkinsfile                   # CI/CD pipeline
```

### Key Files Explained

- **`main.dart`**: Entry point, initializes the app
- **`app.dart`**: Root widget with MaterialApp configuration
- **`pubspec.yaml`**: Dependency management, assets, app metadata
- **`analysis_options.yaml`**: Dart analyzer and linter configuration
- **`AndroidManifest.xml`**: Android permissions, app name, launcher config

---

## Architecture

### Application Architecture

```
┌─────────────────────────────────────────┐
│           Presentation Layer            │
│  (Screens, Widgets, UI Components)     │
└──────────────┬──────────────────────────┘
               │
┌──────────────▼──────────────────────────┐
│          Business Logic Layer           │
│    (State Management, ViewModels)       │
└──────────────┬──────────────────────────┘
               │
┌──────────────▼──────────────────────────┐
│            Data Layer                   │
│  (Repositories, API Services, Models)   │
└──────────────┬──────────────────────────┘
               │
┌──────────────▼──────────────────────────┐
│        External Services                │
│    (REST API, Local Storage, OAuth)     │
└─────────────────────────────────────────┘
```

### Screen Flow

```
Splash Screen
     │
     ▼
Login/Register ──────────────────┐
     │                           │
     ▼                           │
Dashboard ──────┬────────────────┤
     │          │                │
     │          ▼                │
     │   Pipeline Editor         │
     │          │                │
     ▼          │                │
Services ───────┴────────────────┤
     │                           │
     ▼                           │
Profile ─────────────────────────┘
```

### Data Flow

1. **User Action** → UI Widget
2. **Event** → State Management (Provider/Bloc)
3. **Business Logic** → Repository
4. **API Call** → Backend Server
5. **Response** → Model Parsing
6. **State Update** → UI Rebuild

---

## Build & Run

### Development Mode

#### Using Command Line

```bash
cd mobile

# List available devices
flutter devices

# Run on connected device/emulator
flutter run

# Run with specific device
flutter run -d <device-id>

# Run with hot reload enabled (default)
flutter run --hot

# Run in debug mode
flutter run --debug

# Run in profile mode (performance testing)
flutter run --profile
```

#### Using Android Studio

1. Open `mobile/` folder in Android Studio
2. Wait for Gradle sync to complete
3. Select target device from dropdown
4. Click Run button (▶️) or press `Shift + F10`

#### Using VS Code

1. Open `mobile/` folder in VS Code
2. Install Flutter extension
3. Press `F5` or use "Run → Start Debugging"
4. Select device from status bar

### Build for Production

#### Debug APK (for testing)

```bash
flutter build apk --debug

# Output: build/app/outputs/flutter-apk/app-debug.apk
```

#### Release APK

```bash
# Build release APK
flutter build apk --release

# Output: build/app/outputs/flutter-apk/app-release.apk

# Build split APKs (smaller size)
flutter build apk --split-per-abi --release
# Creates separate APKs for different CPU architectures
```

#### App Bundle (for Google Play)

```bash
# Build Android App Bundle
flutter build appbundle --release

# Output: build/app/outputs/bundle/release/app-release.aab
```

### Build Configuration

#### Signing Configuration (for Release)

Create `android/key.properties`:
```properties
storePassword=<your-keystore-password>
keyPassword=<your-key-password>
keyAlias=<your-key-alias>
storeFile=<path-to-keystore-file>
```

Update `android/app/build.gradle.kts`:
```kotlin
android {
    signingConfigs {
        create("release") {
            val keystoreProperties = Properties()
            val keystorePropertiesFile = rootProject.file("key.properties")
            if (keystorePropertiesFile.exists()) {
                keystoreProperties.load(FileInputStream(keystorePropertiesFile))
            }

            keyAlias = keystoreProperties["keyAlias"] as String
            keyPassword = keystoreProperties["keyPassword"] as String
            storeFile = file(keystoreProperties["storeFile"] as String)
            storePassword = keystoreProperties["storePassword"] as String
        }
    }

    buildTypes {
        release {
            signingConfig = signingConfigs.getByName("release")
        }
    }
}
```

---

## Development Guide

### Adding Dependencies

1. **Update `pubspec.yaml`**:
```yaml
dependencies:
  http: ^1.1.0           # HTTP client
  provider: ^6.1.0       # State management
  shared_preferences: ^2.2.0  # Local storage
```

2. **Install**:
```bash
flutter pub get
```

3. **Import in code**:
```dart
import 'package:http/http.dart' as http;
import 'package:provider/provider.dart';
```

### Creating a New Screen

```dart
// lib/screens/new_screen.dart
import 'package:flutter/material.dart';

class NewScreen extends StatefulWidget {
  const NewScreen({Key? key}) : super(key: key);

  @override
  State<NewScreen> createState() => _NewScreenState();
}

class _NewScreenState extends State<NewScreen> {
  @override
  Widget build(BuildContext context) {
    return Scaffold(
      appBar: AppBar(
        title: const Text('New Screen'),
      ),
      body: Center(
        child: Text('Content'),
      ),
    );
  }
}
```

### Creating Reusable Widgets

```dart
// lib/widgets/custom_button.dart
import 'package:flutter/material.dart';

class CustomButton extends StatelessWidget {
  final String text;
  final VoidCallback onPressed;

  const CustomButton({
    Key? key,
    required this.text,
    required this.onPressed,
  }) : super(key: key);

  @override
  Widget build(BuildContext context) {
    return ElevatedButton(
      onPressed: onPressed,
      child: Text(text),
    );
  }
}
```

### API Integration

```dart
// lib/services/api_service.dart
import 'dart:convert';
import 'package:http/http.dart' as http;

class ApiService {
  static const String baseUrl = 'http://10.0.2.2:8080';

  Future<Map<String, dynamic>> login(String email, String password) async {
    final response = await http.post(
      Uri.parse('$baseUrl/api/auth/login'),
      headers: {'Content-Type': 'application/json'},
      body: jsonEncode({
        'email': email,
        'password': password,
      }),
    );

    if (response.statusCode == 200) {
      return jsonDecode(response.body);
    } else {
      throw Exception('Failed to login');
    }
  }

  Future<List<dynamic>> getAreas(String token) async {
    final response = await http.get(
      Uri.parse('$baseUrl/api/areas'),
      headers: {
        'Authorization': 'Bearer $token',
        'Content-Type': 'application/json',
      },
    );

    if (response.statusCode == 200) {
      return jsonDecode(response.body);
    } else {
      throw Exception('Failed to fetch areas');
    }
  }
}
```

### State Management Example

```dart
// lib/providers/auth_provider.dart
import 'package:flutter/foundation.dart';

class AuthProvider extends ChangeNotifier {
  String? _token;
  bool _isAuthenticated = false;

  bool get isAuthenticated => _isAuthenticated;
  String? get token => _token;

  void login(String token) {
    _token = token;
    _isAuthenticated = true;
    notifyListeners();
  }

  void logout() {
    _token = null;
    _isAuthenticated = false;
    notifyListeners();
  }
}
```

### Code Style Guidelines

1. **Follow Dart conventions**:
   - Use `lowerCamelCase` for variables and functions
   - Use `UpperCamelCase` for classes
   - Use `lowercase_with_underscores` for libraries and packages

2. **Run formatter**:
```bash
flutter format lib/
```

3. **Run analyzer**:
```bash
flutter analyze
```

4. **Fix common issues**:
```bash
dart fix --apply
```

---

## Testing

### Unit Tests

```dart
// test/models/workflow_test.dart
import 'package:flutter_test/flutter_test.dart';
import 'package:area/models/workflow.dart';

void main() {
  test('Workflow creation', () {
    final workflow = Workflow(
      id: '1',
      name: 'Test Workflow',
      enabled: true,
    );

    expect(workflow.id, '1');
    expect(workflow.name, 'Test Workflow');
    expect(workflow.enabled, true);
  });
}
```

### Widget Tests

```dart
// test/widgets/custom_button_test.dart
import 'package:flutter/material.dart';
import 'package:flutter_test/flutter_test.dart';
import 'package:area/widgets/custom_button.dart';

void main() {
  testWidgets('CustomButton displays text', (WidgetTester tester) async {
    await tester.pumpWidget(
      MaterialApp(
        home: Scaffold(
          body: CustomButton(
            text: 'Click Me',
            onPressed: () {},
          ),
        ),
      ),
    );

    expect(find.text('Click Me'), findsOneWidget);
  });
}
```

### Running Tests

```bash
# Run all tests
flutter test

# Run specific test file
flutter test test/models/workflow_test.dart

# Run with coverage
flutter test --coverage

# View coverage report
genhtml coverage/lcov.info -o coverage/html
```

### Integration Tests

```bash
# Run integration tests
flutter test integration_test/

# Run on specific device
flutter test integration_test/ -d <device-id>
```

---

## Deployment

### Prepare for Release

1. **Update version in `pubspec.yaml`**:
```yaml
version: 1.0.0+1  # version+build_number
```

2. **Update app name and icon**:
   - Icon: Place icon in `assets/` and update `pubspec.yaml`
   - Name: Update in `AndroidManifest.xml`

3. **Configure permissions in `AndroidManifest.xml`**:
```xml
<uses-permission android:name="android.permission.INTERNET"/>
<uses-permission android:name="android.permission.ACCESS_NETWORK_STATE"/>
```

4. **Test thoroughly**:
```bash
flutter test
flutter build apk --release
```

### Generate Keystore

```bash
keytool -genkey -v -keystore ~/area-release-key.jks \
  -keyalg RSA -keysize 2048 -validity 10000 \
  -alias area-key
```

### Build Release

```bash
# Build App Bundle (recommended for Play Store)
flutter build appbundle --release

# Build APK
flutter build apk --release
```

### Google Play Store Submission

1. Create developer account
2. Upload AAB file
3. Fill in store listing details
4. Set up content rating
5. Submit for review

---

## Troubleshooting

### Common Issues

#### 1. Gradle Build Fails

**Error**: `Could not resolve all files for configuration`

**Solution**:
```bash
cd android
./gradlew clean
cd ..
flutter clean
flutter pub get
```

#### 2. Cannot Connect to API

**Problem**: Network request fails from emulator

**Solution**:
- Use `http://10.0.2.2:8080` for Android emulator (not `localhost`)
- Check `AndroidManifest.xml` has INTERNET permission
- For physical device, use machine's IP address

#### 3. Hot Reload Not Working

**Solution**:
```bash
# Stop and restart
flutter run --hot
```

#### 4. Device Not Detected

**Solution**:
```bash
# Check ADB
adb devices

# Restart ADB
adb kill-server
adb start-server

# Check Flutter devices
flutter devices
```

#### 5. Build Cache Issues

**Solution**:
```bash
flutter clean
flutter pub get
cd android && ./gradlew clean && cd ..
flutter run
```

### Performance Optimization

1. **Use const constructors**:
```dart
const Text('Hello')  // Better than Text('Hello')
```

2. **Avoid rebuilding widgets unnecessarily**:
```dart
// Use keys for stateful widgets
class MyWidget extends StatefulWidget {
  const MyWidget({Key? key}) : super(key: key);
}
```

3. **Profile the app**:
```bash
flutter run --profile
# Use DevTools for performance analysis
```

### Debug Tools

```bash
# Open DevTools
flutter pub global activate devtools
flutter pub global run devtools

# Run with debugging
flutter run --debug

# View logs
flutter logs
```

---

## Best Practices

1. **Follow Flutter/Dart style guide**
2. **Use meaningful variable names**
3. **Write tests for critical functionality**
4. **Handle errors gracefully**
5. **Implement proper loading states**
6. **Use async/await for asynchronous operations**
7. **Optimize images and assets**
8. **Implement proper navigation**
9. **Use environment variables for configuration**
10. **Keep widgets small and focused**

---

## Additional Resources

- **Flutter Documentation**: https://flutter.dev/docs
- **Dart Documentation**: https://dart.dev/guides
- **Flutter Cookbook**: https://flutter.dev/docs/cookbook
- **Package Repository**: https://pub.dev
- **Flutter Community**: https://flutter.dev/community

---

*Last Updated: December 2025*
