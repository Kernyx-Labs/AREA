import 'package:flutter/material.dart';
import 'package:flutter_bloc/flutter_bloc.dart';
import 'package:url_launcher/url_launcher.dart';

import '../constants/palette.dart';
import '../constants/shadows.dart';
import '../models/models.dart';
import '../services/api_client.dart';
import '../services/service_registry.dart';

class ServicesScreen extends StatefulWidget {
  const ServicesScreen({super.key});

  @override
  State<ServicesScreen> createState() => _ServicesScreenState();
}

class _ServicesScreenState extends State<ServicesScreen> {
  List<ServiceInfo> _services = [];
  List<ServiceConnection> _connections = [];
  bool _isLoading = true;
  String? _error;

  late ServiceRegistry _serviceRegistry;

  @override
  void initState() {
    super.initState();
    _serviceRegistry = ServiceRegistry(apiClient: context.read<ApiClient>());
    _loadData();
  }

  Future<void> _loadData() async {
    setState(() {
      _isLoading = true;
      _error = null;
    });

    try {
      final results = await Future.wait([
        _serviceRegistry.getServices(),
        _serviceRegistry.getConnections(),
      ]);

      setState(() {
        _services = results[0] as List<ServiceInfo>;
        _connections = results[1] as List<ServiceConnection>;
        _isLoading = false;
      });
    } on ApiException catch (e) {
      setState(() {
        _error = e.message;
        _isLoading = false;
      });
    } catch (e) {
      setState(() {
        _error = 'Failed to load services';
        _isLoading = false;
      });
    }
  }

  bool _isConnected(String serviceType) {
    return _connections.any((c) => c.type.toUpperCase() == serviceType.toUpperCase());
  }

  ServiceConnection? _getConnection(String serviceType) {
    try {
      return _connections.firstWhere(
        (c) => c.type.toUpperCase() == serviceType.toUpperCase(),
      );
    } catch (_) {
      return null;
    }
  }

  Future<void> _connectService(ServiceInfo service) async {
    try {
      final authUrl = await _serviceRegistry.getOAuthUrl(service.type);
      final uri = Uri.parse(authUrl);
      if (await canLaunchUrl(uri)) {
        await launchUrl(uri, mode: LaunchMode.externalApplication);
        // Reload after returning from OAuth
        Future.delayed(const Duration(seconds: 2), _loadData);
      } else {
        if (mounted) {
          ScaffoldMessenger.of(context).showSnackBar(
            SnackBar(content: Text('Could not open $authUrl')),
          );
        }
      }
    } on ApiException catch (e) {
      if (mounted) {
        ScaffoldMessenger.of(context).showSnackBar(
          SnackBar(content: Text(e.message), backgroundColor: AppPalette.error),
        );
      }
    }
  }

  Future<void> _disconnectService(ServiceConnection connection) async {
    final confirmed = await showDialog<bool>(
      context: context,
      builder: (context) => AlertDialog(
        title: const Text('Disconnect Service'),
        content: Text(
          'Are you sure you want to disconnect ${connection.type}?',
        ),
        actions: [
          TextButton(
            onPressed: () => Navigator.pop(context, false),
            child: const Text('Cancel'),
          ),
          TextButton(
            onPressed: () => Navigator.pop(context, true),
            style: TextButton.styleFrom(foregroundColor: AppPalette.error),
            child: const Text('Disconnect'),
          ),
        ],
      ),
    );

    if (confirmed == true) {
      try {
        await _serviceRegistry.deleteConnection(connection.id);
        _loadData();
        if (mounted) {
          ScaffoldMessenger.of(context).showSnackBar(
            const SnackBar(
              content: Text('Service disconnected'),
              backgroundColor: AppPalette.success,
            ),
          );
        }
      } on ApiException catch (e) {
        if (mounted) {
          ScaffoldMessenger.of(context).showSnackBar(
            SnackBar(content: Text(e.message), backgroundColor: AppPalette.error),
          );
        }
      }
    }
  }

  @override
  Widget build(BuildContext context) {
    if (_isLoading) {
      return const Center(child: CircularProgressIndicator());
    }

    if (_error != null) {
      return Center(
        child: Column(
          mainAxisAlignment: MainAxisAlignment.center,
          children: [
            Icon(Icons.error_outline, size: 48, color: AppPalette.error.withOpacity(0.7)),
            const SizedBox(height: 16),
            Text(_error!, style: TextStyle(color: AppPalette.error.withOpacity(0.7))),
            const SizedBox(height: 16),
            ElevatedButton(
              onPressed: _loadData,
              child: const Text('Retry'),
            ),
          ],
        ),
      );
    }

    // Combine API services with local services for complete list
    final allServices = _buildServiceList();

    return RefreshIndicator(
      onRefresh: _loadData,
      child: ListView(
        padding: const EdgeInsets.all(20),
        children: [
          Text(
            'Connected Services',
            style: Theme.of(context).textTheme.headlineMedium,
          ),
          const SizedBox(height: 8),
          Text(
            '${_connections.length} of ${allServices.length} connected',
            style: Theme.of(context).textTheme.bodyMedium!.copyWith(
                  color: AppPalette.surfaceText,
                ),
          ),
          const SizedBox(height: 16),
          GridView.count(
            crossAxisCount: 2,
            mainAxisSpacing: 16,
            crossAxisSpacing: 16,
            shrinkWrap: true,
            physics: const NeverScrollableScrollPhysics(),
            children: allServices.map((service) {
              final isConnected = _isConnected(service.type);
              final connection = _getConnection(service.type);
              final localService = services[service.type.toLowerCase()];

              return GestureDetector(
                onTap: () {
                  if (isConnected && connection != null) {
                    _disconnectService(connection);
                  } else if (service.requiresAuthentication) {
                    _connectService(service);
                  }
                },
                child: Container(
                  decoration: BoxDecoration(
                    color: Colors.white,
                    borderRadius: BorderRadius.circular(20),
                    boxShadow: AppShadows.card,
                    border: isConnected
                        ? Border.all(color: AppPalette.success, width: 2)
                        : null,
                  ),
                  padding: const EdgeInsets.all(16),
                  child: Column(
                    mainAxisAlignment: MainAxisAlignment.center,
                    children: [
                      CircleAvatar(
                        backgroundColor:
                            localService?.color ?? AppPalette.primary,
                        radius: 28,
                        child: Icon(
                          localService?.icon ?? Icons.extension,
                          color: Colors.white,
                        ),
                      ),
                      const SizedBox(height: 12),
                      Text(
                        service.name,
                        style: Theme.of(context).textTheme.titleMedium,
                        textAlign: TextAlign.center,
                      ),
                      const SizedBox(height: 4),
                      Text(
                        isConnected ? 'Connected' : 'Tap to connect',
                        style: TextStyle(
                          color: isConnected ? AppPalette.success : AppPalette.surfaceText,
                          fontSize: 12,
                        ),
                      ),
                    ],
                  ),
                ),
              );
            }).toList(),
          ),
        ],
      ),
    );
  }

  List<ServiceInfo> _buildServiceList() {
    // Start with API services
    final serviceMap = <String, ServiceInfo>{};

    for (final service in _services) {
      serviceMap[service.type.toUpperCase()] = service;
    }

    // Add local services that might not be in API
    for (final entry in services.entries) {
      final type = entry.key.toUpperCase();
      if (!serviceMap.containsKey(type)) {
        serviceMap[type] = ServiceInfo(
          type: type,
          name: entry.value.name,
          requiresAuthentication: true,
          actionCount: 0,
          reactionCount: 0,
        );
      }
    }

    return serviceMap.values.toList();
  }
}
