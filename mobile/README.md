# AREA Mobile App

Flutter-based Android mobile application for the AREA automation platform.

## Recent Updates (January 2026)

The mobile app has been completely refactored to match the new web frontend design (Phase 2). See [MOBILE_REFACTOR.md](/docs/MOBILE_REFACTOR.md) for detailed documentation.

### Key Changes

- **Black Theme**: Complete UI redesign matching web's dark theme
- **API Integration**: Comprehensive API service layer with all backend endpoints
- **Dashboard**: KPI cards with statistics and recent activity
- **Services**: Dynamic service discovery with OAuth flows
- **Logs**: New execution logs viewer with filtering and pagination
- **Navigation**: 5-tab bottom navigation (Dashboard, Editor, Services, Logs, Profile)

## Quick Start

### Prerequisites

- Flutter SDK ^3.10.1
- Dart SDK ^3.10.1
- Android SDK (for Android development)
- Backend server running on `localhost:8080`

### Installation

```bash
# Navigate to mobile directory
cd mobile

# Get dependencies
flutter pub get

# Run on connected device/emulator
flutter run
```

### Backend Configuration

The app connects to the backend API. Update the base URL in `/lib/services/api_service.dart` if needed:

```dart
// For Android emulator
static const String baseUrl = 'http://10.0.2.2:8080';

// For physical device on same network
static const String baseUrl = 'http://192.168.1.100:8080';
```

Make sure the backend is running:

```bash
# From project root
cd server
mvn spring-boot:run

# Or with Docker
docker-compose up
```

## Project Structure

```
mobile/
├── lib/
│   ├── services/
│   │   └── api_service.dart         # API integration layer
│   ├── screens/
│   │   ├── dashboard_screen.dart    # Dashboard with KPIs
│   │   ├── services_screen.dart     # Service connections
│   │   ├── logs_screen.dart         # Execution logs
│   │   ├── pipeline_editor_screen.dart  # Workflow editor
│   │   └── profile_screen.dart      # User profile
│   ├── constants/
│   │   ├── palette.dart             # Black theme colors
│   │   ├── shadows.dart             # Shadow definitions
│   │   └── pipeline_layout.dart     # Layout constants
│   ├── models/
│   │   └── ...                      # Data models
│   ├── widgets/
│   │   └── ...                      # Reusable widgets
│   ├── app.dart                     # App configuration
│   └── main.dart                    # Entry point
├── pubspec.yaml                     # Dependencies
└── README.md                        # This file
```

## Features

### Dashboard
- KPI cards: Total Areas, Connected Services, Executions (24h), Success Rate
- Recent activity feed
- Workflows and Areas management
- Test, Enable/Disable, Delete actions
- Pull-to-refresh

### Services
- Dynamic service discovery from backend
- OAuth flow integration (Gmail)
- Discord bot connection
- Service status and token expiry
- Connect, Disconnect, Refresh actions

### Logs
- Execution log viewing
- Pagination (20 items per page)
- Status filtering (Success, Failure, Skipped)
- Workflow ID filtering
- Time and duration display

### Pipeline Editor
- Visual workflow builder (existing, needs Phase 3 update)
- Drag-and-drop nodes
- Connection management

### Profile
- User profile (placeholder, needs backend implementation)

## Dependencies

```yaml
dependencies:
  flutter:
    sdk: flutter
  cupertino_icons: ^1.0.8
  http: ^1.2.1              # HTTP client
  provider: ^6.1.2          # State management
  intl: ^0.19.0             # Date/time formatting
  url_launcher: ^6.3.1      # OAuth URL launching
```

## Build Commands

```bash
# Development build
flutter run

# Release APK
flutter build apk --release

# Run tests
flutter test

# Code analysis
flutter analyze

# Format code
flutter format lib/
```

## API Endpoints Used

- `GET /api/dashboard/stats` - Dashboard KPIs
- `GET /api/workflows` - List workflows
- `GET /api/areas` - List areas
- `GET /api/services` - Service discovery
- `GET /api/service-connections` - Connected services
- `GET /api/logs` - Execution logs
- `POST /api/workflows/:id/execute` - Execute workflow
- `PATCH /api/workflows/:id/status` - Toggle workflow
- `DELETE /api/workflows/:id` - Delete workflow
- And more... (see api_service.dart for complete list)

## Design System

### Colors

- **Background**: Pure black (#000000)
- **Surface**: Dark charcoal (#1A1A1A)
- **Primary Accent**: Blue (#5B9BD5)
- **Success**: Green (#70C770)
- **Warning**: Orange (#FF8C42)
- **Danger**: Red (#FF6B6B)
- **Text Primary**: Bright white (#F0F0F0)
- **Text Secondary**: Ghost white (#D0D0D0)

See `/lib/constants/palette.dart` for complete color system.

## Known Issues

1. OAuth callback requires manual app return (no deep linking)
2. Pipeline Editor needs Phase 3 update
3. Profile screen is placeholder
4. No offline mode support
5. Service icons use initials instead of logos

## Future Enhancements

- Updated Pipeline Editor with visual workflow builder
- User authentication and JWT management
- Offline support with local database
- Push notifications for workflow executions
- Deep linking for OAuth callbacks
- Workflow templates and batch operations
- Export/import workflows
- Dark/light theme toggle

## Troubleshooting

### Cannot connect to backend

**Error**: `Failed to fetch dashboard statistics`

**Solution**:
- Check backend is running on `localhost:8080`
- For emulator: Use `http://10.0.2.2:8080`
- For physical device: Use your computer's IP address

### OAuth flow doesn't work

**Solution**: Add to `android/app/src/main/AndroidManifest.xml`:

```xml
<queries>
  <intent>
    <action android:name="android.intent.action.VIEW" />
    <data android:scheme="https" />
  </intent>
</queries>
```

### Dependency conflicts

**Solution**:
```bash
flutter pub upgrade
flutter pub get
```

## Documentation

- [MOBILE_REFACTOR.md](/docs/MOBILE_REFACTOR.md) - Detailed refactor documentation
- [CLAUDE.md](/CLAUDE.md) - Project overview and architecture

## Contributing

When making changes:

1. Follow Flutter/Dart style guide
2. Match web frontend design language
3. Use API service layer for all backend calls
4. Maintain black theme consistency
5. Add comments for complex logic
6. Test on both emulator and physical device

## License

See project root for license information.

## Support

For issues or questions, refer to the main project documentation or create an issue in the project repository.
