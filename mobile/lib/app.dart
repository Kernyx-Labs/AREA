import 'package:flutter/material.dart';

import 'constants/palette.dart';
import 'screens/dashboard_screen.dart';
import 'screens/logs_screen.dart';
import 'screens/pipeline_editor_screen.dart';
import 'screens/profile_screen.dart';
import 'screens/services_screen.dart';

class AreaApp extends StatelessWidget {
  const AreaApp({super.key});

  @override
  Widget build(BuildContext context) {
    final theme = ThemeData(
      useMaterial3: true,
      scaffoldBackgroundColor: AppPalette.background,
      colorScheme: ColorScheme.fromSeed(
        seedColor: AppPalette.accentBlue,
        brightness: Brightness.dark,
        background: AppPalette.background,
        surface: AppPalette.surface,
        primary: AppPalette.accentBlue,
        secondary: AppPalette.accentPurple,
      ),
      navigationBarTheme: NavigationBarThemeData(
        backgroundColor: AppPalette.deepBlack,
        indicatorColor: AppPalette.accentBlue.withOpacity(0.2),
        iconTheme: MaterialStateProperty.resolveWith((states) {
          if (states.contains(MaterialState.selected)) {
            return const IconThemeData(color: AppPalette.accentBlue);
          }
          return const IconThemeData(color: AppPalette.textMuted);
        }),
        labelTextStyle: MaterialStateProperty.all(
          const TextStyle(
            color: AppPalette.textMuted,
            fontSize: 12,
          ),
        ),
      ),
      textTheme: const TextTheme(
        displayLarge: TextStyle(color: AppPalette.textPrimary),
        displayMedium: TextStyle(color: AppPalette.textPrimary),
        displaySmall: TextStyle(color: AppPalette.textPrimary),
        headlineLarge: TextStyle(color: AppPalette.textPrimary),
        headlineMedium: TextStyle(color: AppPalette.textPrimary),
        headlineSmall: TextStyle(color: AppPalette.textPrimary),
        titleLarge: TextStyle(color: AppPalette.textPrimary),
        titleMedium: TextStyle(color: AppPalette.textPrimary),
        titleSmall: TextStyle(color: AppPalette.textPrimary),
        bodyLarge: TextStyle(color: AppPalette.textSecondary),
        bodyMedium: TextStyle(color: AppPalette.textSecondary),
        bodySmall: TextStyle(color: AppPalette.textSecondary),
        labelLarge: TextStyle(color: AppPalette.textSecondary),
        labelMedium: TextStyle(color: AppPalette.textSecondary),
        labelSmall: TextStyle(color: AppPalette.textSecondary),
      ),
      appBarTheme: const AppBarTheme(
        backgroundColor: AppPalette.surface,
        foregroundColor: AppPalette.textPrimary,
        elevation: 0,
      ),
      cardTheme: CardThemeData(
        color: AppPalette.surface,
        elevation: 0,
        shape: RoundedRectangleBorder(
          borderRadius: BorderRadius.circular(12),
          side: const BorderSide(color: AppPalette.borderDefault),
        ),
      ),
      dialogTheme: DialogThemeData(
        backgroundColor: AppPalette.surface,
        titleTextStyle: const TextStyle(
          color: AppPalette.textPrimary,
          fontSize: 20,
          fontWeight: FontWeight.w600,
        ),
        contentTextStyle: const TextStyle(
          color: AppPalette.textSecondary,
          fontSize: 14,
        ),
      ),
      snackBarTheme: SnackBarThemeData(
        backgroundColor: AppPalette.surfaceRaised,
        contentTextStyle: const TextStyle(color: AppPalette.textPrimary),
        shape: RoundedRectangleBorder(borderRadius: BorderRadius.circular(8)),
      ),
    );

    return MaterialApp(
      title: 'AREA Mobile',
      theme: theme,
      darkTheme: theme,
      themeMode: ThemeMode.dark,
      debugShowCheckedModeBanner: false,
      home: const HomeShell(),
    );
  }
}

class HomeShell extends StatefulWidget {
  const HomeShell({super.key});

  @override
  State<HomeShell> createState() => _HomeShellState();
}

class _HomeShellState extends State<HomeShell> {
  final _pages = const [
    DashboardScreen(),
    PipelineEditorScreen(),
    ServicesScreen(),
    LogsScreen(),
    ProfileScreen(),
  ];
  int _index = 0;

  @override
  Widget build(BuildContext context) {
    return Scaffold(
      body: SafeArea(
        child: IndexedStack(
          index: _index,
          children: _pages,
        ),
      ),
      bottomNavigationBar: NavigationBar(
        selectedIndex: _index,
        backgroundColor: AppPalette.deepBlack,
        indicatorColor: AppPalette.accentBlue.withOpacity(0.2),
        labelBehavior: NavigationDestinationLabelBehavior.onlyShowSelected,
        destinations: const [
          NavigationDestination(
            icon: Icon(Icons.dashboard_outlined),
            selectedIcon: Icon(Icons.dashboard),
            label: 'Dashboard',
          ),
          NavigationDestination(
            icon: Icon(Icons.device_hub_outlined),
            selectedIcon: Icon(Icons.device_hub),
            label: 'Editor',
          ),
          NavigationDestination(
            icon: Icon(Icons.extension_outlined),
            selectedIcon: Icon(Icons.extension),
            label: 'Services',
          ),
          NavigationDestination(
            icon: Icon(Icons.article_outlined),
            selectedIcon: Icon(Icons.article),
            label: 'Logs',
          ),
          NavigationDestination(
            icon: Icon(Icons.person_outline),
            selectedIcon: Icon(Icons.person),
            label: 'Profile',
          ),
        ],
        onDestinationSelected: (value) => setState(() => _index = value),
      ),
    );
  }
}
