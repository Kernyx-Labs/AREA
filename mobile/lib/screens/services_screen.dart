import 'package:flutter/material.dart';
import 'package:url_launcher/url_launcher.dart';

import '../constants/palette.dart';
import '../constants/shadows.dart';
import '../services/api_service.dart';

class ServicesScreen extends StatefulWidget {
  const ServicesScreen({super.key});

  @override
  State<ServicesScreen> createState() => _ServicesScreenState();
}

class _ServicesScreenState extends State<ServicesScreen> {
  bool _loading = true;
  String? _error;
  List<Map<String, dynamic>> _displayServices = [];
  String? _connecting;
  String? _refreshing;
  String? _deleting;
  bool _showDiscordModal = false;

  final Map<String, Color> _serviceColors = {
    'gmail': const Color(0xFFEA4335),
    'discord': const Color(0xFF5865F2),
    'timer': const Color(0xFF10B981),
    'github': const Color(0xFF7C3AED),
    'dropbox': const Color(0xFF0061FF),
    'outlook': const Color(0xFF0078D4),
    'slack': const Color(0xFFE01E5A),
    'trello': const Color(0xFF0079BF),
    'spotify': const Color(0xFF1DB954),
    'twitter': const Color(0xFF1DA1F2),
    'notion': const Color(0xFF000000),
  };

  @override
  void initState() {
    super.initState();
    _loadServices();
  }

  Color _getServiceColor(String serviceName) {
    return _serviceColors[serviceName.toLowerCase()] ?? AppPalette.accentBlue;
  }

  String _formatExpiryDate(String? expiresAt) {
    if (expiresAt == null) return '';

    try {
      final date = DateTime.parse(expiresAt);
      final now = DateTime.now();
      final diff = date.difference(now);

      if (diff.isNegative) return 'expired';

      final diffMins = diff.inMinutes;
      final diffHours = diff.inHours;
      final diffDays = diff.inDays;

      if (diffDays > 0) return 'in $diffDays day${diffDays > 1 ? 's' : ''}';
      if (diffHours > 0) return 'in $diffHours hour${diffHours > 1 ? 's' : ''}';
      if (diffMins > 0) return 'in $diffMins minute${diffMins > 1 ? 's' : ''}';
      return 'soon';
    } catch (e) {
      return '';
    }
  }

  Future<void> _loadServices() async {
    try {
      setState(() {
        _loading = true;
        _error = null;
      });

      // Fetch available services from backend (dynamic discovery)
      final availableServices = await ApiService.getServices();

      // Fetch connected services
      final connectedServices = await ApiService.getConnectedServices();

      // Create a map of connected services by type
      final connectedMap = <String, Map<String, dynamic>>{};
      for (var conn in connectedServices) {
        final normalizedType = (conn['type'] as String).toLowerCase();
        connectedMap[normalizedType] = {
          'id': conn['id'],
          'expiresAt': conn['tokenExpiresAt'],
          'isExpired': conn['tokenExpiresAt'] != null
              ? DateTime.parse(conn['tokenExpiresAt']).isBefore(DateTime.now())
              : false,
        };
      }

      // Build display list using backend service metadata
      final services = <Map<String, dynamic>>[];
      for (var service in availableServices) {
        final serviceType = (service['type'] as String).toLowerCase();
        final connection = connectedMap[serviceType];

        services.add({
          'name': serviceType,
          'displayName': service['name'],
          'description': service['description'],
          'color': _getServiceColor(serviceType),
          'requiresAuth': service['requiresAuthentication'] ?? false,
          'actionCount': service['actionCount'] ?? 0,
          'reactionCount': service['reactionCount'] ?? 0,
          'isConnected': connection != null,
          'connectionId': connection?['id'],
          'expiresAt': connection?['expiresAt'],
          'isExpired': connection?['isExpired'] ?? false,
        });
      }

      setState(() {
        _displayServices = services;
        _loading = false;
      });
    } catch (error) {
      debugPrint('Error loading services: $error');
      setState(() {
        _error = 'Failed to load services. Please try again.';
        _loading = false;
      });
    }
  }

  Future<void> _connectService(Map<String, dynamic> service) async {
    setState(() => _connecting = service['name']);

    try {
      if (service['name'] == 'gmail') {
        // Gmail requires OAuth flow
        final authData = await ApiService.getGmailAuthUrl();
        final authUrl = authData['authUrl'] as String;

        // Launch OAuth URL
        final uri = Uri.parse(authUrl);
        if (await canLaunchUrl(uri)) {
          await launchUrl(uri, mode: LaunchMode.externalApplication);

          // Show message to user
          if (mounted) {
            ScaffoldMessenger.of(context).showSnackBar(
              const SnackBar(
                content: Text('Complete authorization in browser, then return to app'),
                duration: Duration(seconds: 5),
              ),
            );
          }

          // Refresh services after a delay
          await Future.delayed(const Duration(seconds: 3));
          await _loadServices();
        } else {
          throw Exception('Could not launch OAuth URL');
        }
      } else if (service['name'] == 'discord') {
        // Discord - show modal for bot token and channel ID
        setState(() => _showDiscordModal = true);
      } else {
        // For other services
        debugPrint('Connecting to ${service['name']}...');
        // TODO: Implement other service connections
      }
    } catch (error) {
      debugPrint('Error connecting to ${service['name']}: $error');
      if (mounted) {
        ScaffoldMessenger.of(context).showSnackBar(
          SnackBar(content: Text('Failed to connect to ${service['name']}: $error')),
        );
      }
    } finally {
      setState(() => _connecting = null);
    }
  }

  Future<void> _deleteService(int connectionId, String serviceName) async {
    final confirmed = await showDialog<bool>(
      context: context,
      builder: (context) => AlertDialog(
        backgroundColor: AppPalette.surface,
        title: Text(
          'Delete $serviceName connection?',
          style: const TextStyle(color: AppPalette.textPrimary),
        ),
        content: const Text(
          'This will disconnect the service.',
          style: TextStyle(color: AppPalette.textSecondary),
        ),
        actions: [
          TextButton(
            onPressed: () => Navigator.pop(context, false),
            child: const Text('Cancel'),
          ),
          TextButton(
            onPressed: () => Navigator.pop(context, true),
            style: TextButton.styleFrom(foregroundColor: AppPalette.danger),
            child: const Text('Delete'),
          ),
        ],
      ),
    );

    if (confirmed != true) return;

    try {
      setState(() => _deleting = serviceName);
      await ApiService.disconnectService(connectionId);
      await _loadServices();
    } catch (error) {
      if (mounted) {
        ScaffoldMessenger.of(context).showSnackBar(
          SnackBar(content: Text('Failed to delete $serviceName: $error')),
        );
      }
    } finally {
      setState(() => _deleting = null);
    }
  }

  Future<void> _refreshToken(int connectionId, String serviceName) async {
    try {
      setState(() => _refreshing = serviceName);
      await ApiService.refreshServiceToken(connectionId);
      if (mounted) {
        ScaffoldMessenger.of(context).showSnackBar(
          const SnackBar(content: Text('Token refreshed successfully!')),
        );
      }
      await _loadServices();
    } catch (error) {
      if (mounted) {
        ScaffoldMessenger.of(context).showSnackBar(
          SnackBar(content: Text('Failed to refresh token: $error')),
        );
      }
    } finally {
      setState(() => _refreshing = null);
    }
  }

  Future<void> _showDiscordConnectionModal() async {
    final botTokenController = TextEditingController();
    final channelIdController = TextEditingController();

    await showDialog(
      context: context,
      builder: (context) => AlertDialog(
        backgroundColor: AppPalette.surface,
        title: const Text(
          'Connect Discord',
          style: TextStyle(color: AppPalette.textPrimary),
        ),
        content: Column(
          mainAxisSize: MainAxisSize.min,
          children: [
            TextField(
              controller: botTokenController,
              style: const TextStyle(color: AppPalette.textPrimary),
              decoration: const InputDecoration(
                labelText: 'Bot Token',
                labelStyle: TextStyle(color: AppPalette.textSecondary),
                enabledBorder: UnderlineInputBorder(
                  borderSide: BorderSide(color: AppPalette.borderDefault),
                ),
                focusedBorder: UnderlineInputBorder(
                  borderSide: BorderSide(color: AppPalette.accentBlue),
                ),
              ),
            ),
            const SizedBox(height: 16),
            TextField(
              controller: channelIdController,
              style: const TextStyle(color: AppPalette.textPrimary),
              decoration: const InputDecoration(
                labelText: 'Channel ID',
                labelStyle: TextStyle(color: AppPalette.textSecondary),
                enabledBorder: UnderlineInputBorder(
                  borderSide: BorderSide(color: AppPalette.borderDefault),
                ),
                focusedBorder: UnderlineInputBorder(
                  borderSide: BorderSide(color: AppPalette.accentBlue),
                ),
              ),
            ),
          ],
        ),
        actions: [
          TextButton(
            onPressed: () => Navigator.pop(context),
            child: const Text('Cancel'),
          ),
          TextButton(
            onPressed: () async {
              final botToken = botTokenController.text.trim();
              final channelId = channelIdController.text.trim();

              if (botToken.isEmpty || channelId.isEmpty) {
                ScaffoldMessenger.of(context).showSnackBar(
                  const SnackBar(content: Text('Please fill in all fields')),
                );
                return;
              }

              try {
                await ApiService.connectDiscord(botToken, channelId);
                if (context.mounted) {
                  Navigator.pop(context);
                  ScaffoldMessenger.of(context).showSnackBar(
                    const SnackBar(content: Text('Discord connected successfully!')),
                  );
                }
                _loadServices();
              } catch (error) {
                if (context.mounted) {
                  ScaffoldMessenger.of(context).showSnackBar(
                    SnackBar(content: Text('Failed to connect Discord: $error')),
                  );
                }
              }
            },
            child: const Text('Connect'),
          ),
        ],
      ),
    );
  }

  @override
  Widget build(BuildContext context) {
    return Scaffold(
      body: RefreshIndicator(
        onRefresh: _loadServices,
        child: _loading
            ? const Center(child: CircularProgressIndicator())
            : _error != null
                ? Center(
                    child: Column(
                      mainAxisAlignment: MainAxisAlignment.center,
                      children: [
                        Text(_error!, style: const TextStyle(color: AppPalette.danger)),
                        const SizedBox(height: 16),
                        ElevatedButton(
                          onPressed: _loadServices,
                          child: const Text('Retry'),
                        ),
                      ],
                    ),
                  )
                : ListView(
                    padding: const EdgeInsets.all(20),
                    children: [
                      Text(
                        'Connected Services',
                        style: Theme.of(context).textTheme.headlineSmall?.copyWith(
                              color: AppPalette.textPrimary,
                              fontWeight: FontWeight.bold,
                            ),
                      ),
                      const SizedBox(height: 20),
                      ..._displayServices.map((service) {
                        return Padding(
                          padding: const EdgeInsets.only(bottom: 16),
                          child: _buildServiceCard(service),
                        );
                      }).toList(),
                    ],
                  ),
      ),
    );
  }

  Widget _buildServiceCard(Map<String, dynamic> service) {
    final isConnected = service['isConnected'] as bool;
    final isExpired = service['isExpired'] as bool;
    final expiresAt = service['expiresAt'] as String?;
    final description = service['description'] as String? ?? '';
    final actionCount = service['actionCount'] as int? ?? 0;
    final reactionCount = service['reactionCount'] as int? ?? 0;

    return Container(
      padding: const EdgeInsets.all(20),
      decoration: BoxDecoration(
        color: AppPalette.surface,
        borderRadius: BorderRadius.circular(16),
        border: Border.all(color: AppPalette.borderDefault),
        boxShadow: AppShadows.card,
      ),
      child: Column(
        crossAxisAlignment: CrossAxisAlignment.start,
        children: [
          // Header: Icon + Title + Status
          Row(
            children: [
              CircleAvatar(
                backgroundColor: service['color'],
                radius: 32,
                child: Text(
                  service['displayName'][0].toUpperCase(),
                  style: const TextStyle(
                    color: Colors.white,
                    fontSize: 28,
                    fontWeight: FontWeight.bold,
                  ),
                ),
              ),
              const SizedBox(width: 16),
              Expanded(
                child: Column(
                  crossAxisAlignment: CrossAxisAlignment.start,
                  children: [
                    Text(
                      service['displayName'],
                      style: Theme.of(context).textTheme.titleLarge?.copyWith(
                            color: AppPalette.textPrimary,
                            fontWeight: FontWeight.w600,
                          ),
                    ),
                    const SizedBox(height: 4),
                    Container(
                      padding: const EdgeInsets.symmetric(horizontal: 10, vertical: 5),
                      decoration: BoxDecoration(
                        color: isExpired
                            ? AppPalette.danger.withOpacity(0.15)
                            : isConnected
                                ? AppPalette.success.withOpacity(0.15)
                                : AppPalette.textMuted.withOpacity(0.15),
                        borderRadius: BorderRadius.circular(8),
                      ),
                      child: Text(
                        isExpired ? 'Expired' : (isConnected ? 'Connected' : 'Not Connected'),
                        style: TextStyle(
                          color: isExpired
                              ? AppPalette.danger
                              : isConnected
                                  ? AppPalette.success
                                  : AppPalette.textMuted,
                          fontSize: 12,
                          fontWeight: FontWeight.w600,
                        ),
                      ),
                    ),
                  ],
                ),
              ),
            ],
          ),

          // Description
          if (description.isNotEmpty) ...[
            const SizedBox(height: 16),
            Text(
              description,
              style: const TextStyle(
                color: AppPalette.textSecondary,
                fontSize: 14,
                height: 1.4,
              ),
            ),
          ],

          // Action/Reaction counts
          if (actionCount > 0 || reactionCount > 0) ...[
            const SizedBox(height: 12),
            Row(
              children: [
                if (actionCount > 0) ...[
                  Icon(Icons.bolt, size: 16, color: AppPalette.accentBlue),
                  const SizedBox(width: 4),
                  Text(
                    '$actionCount trigger${actionCount != 1 ? 's' : ''}',
                    style: const TextStyle(
                      color: AppPalette.textMuted,
                      fontSize: 13,
                    ),
                  ),
                  const SizedBox(width: 16),
                ],
                if (reactionCount > 0) ...[
                  Icon(Icons.flash_on, size: 16, color: AppPalette.accentGreen),
                  const SizedBox(width: 4),
                  Text(
                    '$reactionCount action${reactionCount != 1 ? 's' : ''}',
                    style: const TextStyle(
                      color: AppPalette.textMuted,
                      fontSize: 13,
                    ),
                  ),
                ],
              ],
            ),
          ],

          // Expiry info
          if (isConnected && expiresAt != null) ...[
            const SizedBox(height: 12),
            Row(
              children: [
                Icon(
                  Icons.access_time,
                  size: 14,
                  color: isExpired ? AppPalette.danger : AppPalette.textMuted,
                ),
                const SizedBox(width: 4),
                Text(
                  isExpired ? 'Token expired' : 'Expires ${_formatExpiryDate(expiresAt)}',
                  style: TextStyle(
                    color: isExpired ? AppPalette.danger : AppPalette.textMuted,
                    fontSize: 12,
                  ),
                ),
              ],
            ),
          ],

          const SizedBox(height: 20),

          // Action buttons
          if (!isConnected)
            SizedBox(
              width: double.infinity,
              child: ElevatedButton(
                onPressed: _connecting == service['name'] ? null : () => _connectService(service),
                style: ElevatedButton.styleFrom(
                  backgroundColor: AppPalette.accentBlue,
                  foregroundColor: Colors.white,
                  padding: const EdgeInsets.symmetric(vertical: 14),
                  shape: RoundedRectangleBorder(
                    borderRadius: BorderRadius.circular(10),
                  ),
                ),
                child: Text(
                  _connecting == service['name'] ? 'Connecting...' : 'Connect',
                  style: const TextStyle(fontSize: 15, fontWeight: FontWeight.w600),
                ),
              ),
            )
          else
            Row(
              children: [
                if (isExpired && service['name'] == 'gmail') ...[
                  Expanded(
                    child: OutlinedButton(
                      onPressed: _refreshing == service['name']
                          ? null
                          : () => _refreshToken(service['connectionId'], service['name']),
                      style: OutlinedButton.styleFrom(
                        foregroundColor: AppPalette.accentBlue,
                        side: const BorderSide(color: AppPalette.accentBlue, width: 1.5),
                        padding: const EdgeInsets.symmetric(vertical: 14),
                        shape: RoundedRectangleBorder(
                          borderRadius: BorderRadius.circular(10),
                        ),
                      ),
                      child: Text(
                        _refreshing == service['name'] ? 'Refreshing...' : 'Refresh',
                        style: const TextStyle(fontSize: 14, fontWeight: FontWeight.w600),
                      ),
                    ),
                  ),
                  const SizedBox(width: 12),
                ],
                Expanded(
                  child: OutlinedButton(
                    onPressed: _deleting == service['name']
                        ? null
                        : () => _deleteService(service['connectionId'], service['name']),
                    style: OutlinedButton.styleFrom(
                      foregroundColor: AppPalette.danger,
                      side: const BorderSide(color: AppPalette.danger, width: 1.5),
                      padding: const EdgeInsets.symmetric(vertical: 14),
                      shape: RoundedRectangleBorder(
                        borderRadius: BorderRadius.circular(10),
                      ),
                    ),
                    child: Text(
                      _deleting == service['name'] ? 'Deleting...' : 'Disconnect',
                      style: const TextStyle(fontSize: 14, fontWeight: FontWeight.w600),
                    ),
                  ),
                ),
              ],
            ),
        ],
      ),
    );
  }
}
