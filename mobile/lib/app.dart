import 'package:flutter/material.dart';

import 'constants/palette.dart';
import 'constants/shadows.dart';
import 'models/models.dart';
import 'screens/dashboard_screen.dart';
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
        seedColor: AppPalette.accent,
        brightness: Brightness.light,
      ),
      textTheme: Theme.of(context).textTheme.apply(
            displayColor: AppPalette.dark,
            bodyColor: AppPalette.dark,
          ),
    );

    return MaterialApp(
      title: 'AREA Mobile',
      theme: theme,
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
    ProfileScreen(),
  ];
  int _index = 1;

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
        backgroundColor: AppPalette.dark,
        indicatorColor: AppPalette.accent.withOpacity(.2),
        labelBehavior: NavigationDestinationLabelBehavior.alwaysHide,
        destinations: const [
          NavigationDestination(icon: Icon(Icons.dashboard_outlined), label: 'Dashboard'),
          NavigationDestination(icon: Icon(Icons.device_hub_outlined), label: 'Editor'),
          NavigationDestination(icon: Icon(Icons.extension_outlined), label: 'Services'),
          NavigationDestination(icon: Icon(Icons.person_outline), label: 'Profile'),
        ],
        onDestinationSelected: (value) => setState(() => _index = value),
      ),
    );
  }
}
