import 'package:flutter/material.dart';
import 'package:intl/intl.dart';

import '../constants/palette.dart';
import '../constants/shadows.dart';
import '../services/api_service.dart';

class DashboardScreen extends StatefulWidget {
  const DashboardScreen({super.key});

  @override
  State<DashboardScreen> createState() => _DashboardScreenState();
}

class _DashboardScreenState extends State<DashboardScreen> {
  bool _loadingStats = true;
  bool _loadingAreas = true;
  Map<String, dynamic> _stats = {};
  List<Map<String, dynamic>> _areas = [];

  @override
  void initState() {
    super.initState();
    _loadDashboard();
  }

  Future<void> _loadDashboard() async {
    try {
      setState(() => _loadingStats = true);
      final stats = await ApiService.getDashboardStats();
      setState(() {
        _stats = stats;
        _loadingStats = false;
      });
    } catch (error) {
      debugPrint('Failed to load dashboard stats: $error');
      setState(() => _loadingStats = false);
    }

    try {
      setState(() => _loadingAreas = true);

      // Load both workflows and areas, then combine them
      final results = await Future.wait([
        ApiService.getWorkflows().catchError((e) => <dynamic>[]),
        ApiService.getAreas().catchError((e) => <dynamic>[]),
      ]);

      final workflows = results[0] as List<dynamic>;
      final areas = results[1] as List<dynamic>;

      final combinedAreas = <Map<String, dynamic>>[];

      // Add workflows
      for (var w in workflows) {
        combinedAreas.add({
          'id': 'workflow-${w['id']}',
          'realId': w['id'],
          'name': w['name'] ?? 'Workflow #${w['id']}',
          'active': w['active'] ?? false,
          'description': w['description'],
          'type': 'workflow',
        });
      }

      // Add areas (Gmail → Discord)
      for (var a in areas) {
        combinedAreas.add({
          'id': 'area-${a['id']}',
          'realId': a['id'],
          'name': 'Gmail → Discord #${a['id']}',
          'active': a['active'] ?? false,
          'description': 'Gmail to Discord automation',
          'type': 'area',
        });
      }

      setState(() {
        _areas = combinedAreas;
        _loadingAreas = false;
      });
    } catch (error) {
      debugPrint('Failed to load areas and workflows: $error');
      setState(() => _loadingAreas = false);
    }
  }

  String _formatTime(String? timestamp) {
    if (timestamp == null) return 'Never';

    try {
      final date = DateTime.parse(timestamp);
      final now = DateTime.now();
      final diff = now.difference(date);

      if (diff.inMinutes < 1) return 'Just now';
      if (diff.inMinutes < 60) return '${diff.inMinutes}m ago';
      if (diff.inHours < 24) return '${diff.inHours}h ago';
      if (diff.inDays == 1) return 'Yesterday';
      if (diff.inDays < 7) return '${diff.inDays}d ago';

      return DateFormat.yMMMd().format(date);
    } catch (e) {
      return 'N/A';
    }
  }

  Future<void> _toggleArea(Map<String, dynamic> area) async {
    try {
      if (area['type'] == 'area') {
        final updated = await ApiService.toggleAreaStatus(area['realId']);
        final index = _areas.indexWhere((a) => a['id'] == area['id']);
        if (index != -1) {
          setState(() {
            _areas[index]['active'] = updated['active'];
          });
        }
      } else {
        final updated = await ApiService.updateWorkflowStatus(area['realId'], !area['active']);
        final index = _areas.indexWhere((a) => a['id'] == area['id']);
        if (index != -1) {
          setState(() {
            _areas[index]['active'] = updated['active'];
          });
        }
      }

      _loadDashboard();
    } catch (error) {
      if (mounted) {
        ScaffoldMessenger.of(context).showSnackBar(
          SnackBar(content: Text('Failed to update status: $error')),
        );
      }
    }
  }

  Future<void> _deleteArea(Map<String, dynamic> area) async {
    final confirmed = await showDialog<bool>(
      context: context,
      builder: (context) => AlertDialog(
        backgroundColor: AppPalette.surface,
        title: Text('Delete "${area['name']}"?', style: const TextStyle(color: AppPalette.textPrimary)),
        content: const Text(
          'This action cannot be undone.',
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
      if (area['type'] == 'area') {
        await ApiService.deleteArea(area['realId']);
      } else {
        await ApiService.deleteWorkflow(area['realId']);
      }

      setState(() {
        _areas.removeWhere((a) => a['id'] == area['id']);
      });

      _loadDashboard();
    } catch (error) {
      if (mounted) {
        ScaffoldMessenger.of(context).showSnackBar(
          SnackBar(content: Text('Failed to delete: $error')),
        );
      }
    }
  }

  Future<void> _testWorkflow(Map<String, dynamic> area) async {
    final confirmed = await showDialog<bool>(
      context: context,
      builder: (context) => AlertDialog(
        backgroundColor: AppPalette.surface,
        title: Text('Execute "${area['name']}" now?', style: const TextStyle(color: AppPalette.textPrimary)),
        content: const Text(
          'This will test the workflow immediately.',
          style: TextStyle(color: AppPalette.textSecondary),
        ),
        actions: [
          TextButton(
            onPressed: () => Navigator.pop(context, false),
            child: const Text('Cancel'),
          ),
          TextButton(
            onPressed: () => Navigator.pop(context, true),
            child: const Text('Execute'),
          ),
        ],
      ),
    );

    if (confirmed != true) return;

    try {
      final result = await ApiService.executeWorkflow(area['realId']);
      if (mounted) {
        ScaffoldMessenger.of(context).showSnackBar(
          SnackBar(content: Text(result['message'] ?? 'Workflow executed successfully!')),
        );
      }
      _loadDashboard();
    } catch (error) {
      if (mounted) {
        ScaffoldMessenger.of(context).showSnackBar(
          SnackBar(content: Text('Failed to execute workflow: $error')),
        );
      }
    }
  }

  @override
  Widget build(BuildContext context) {
    return RefreshIndicator(
      onRefresh: _loadDashboard,
      child: SingleChildScrollView(
        physics: const AlwaysScrollableScrollPhysics(),
        padding: const EdgeInsets.all(20),
        child: Column(
          crossAxisAlignment: CrossAxisAlignment.start,
          children: [
            // KPI Section
            Text(
              'Overview',
              style: Theme.of(context).textTheme.headlineSmall?.copyWith(
                    color: AppPalette.textPrimary,
                    fontWeight: FontWeight.bold,
                  ),
            ),
            const SizedBox(height: 16),
            _loadingStats
                ? const Center(child: CircularProgressIndicator())
                : _buildKPIGrid(),
            const SizedBox(height: 32),

            // Recent Activity
            if (_stats['recentActivity'] != null && (_stats['recentActivity'] as List).isNotEmpty) ...[
              Text(
                'Recent Activity',
                style: Theme.of(context).textTheme.headlineSmall?.copyWith(
                      color: AppPalette.textPrimary,
                      fontWeight: FontWeight.bold,
                    ),
              ),
              const SizedBox(height: 16),
              _buildRecentActivity(),
              const SizedBox(height: 32),
            ],

            // Areas List
            Row(
              mainAxisAlignment: MainAxisAlignment.spaceBetween,
              children: [
                Column(
                  crossAxisAlignment: CrossAxisAlignment.start,
                  children: [
                    Text(
                      'My Automations',
                      style: Theme.of(context).textTheme.headlineSmall?.copyWith(
                            color: AppPalette.textPrimary,
                            fontWeight: FontWeight.bold,
                          ),
                    ),
                    const SizedBox(height: 4),
                    Text(
                      'Manage your AREA workflows',
                      style: Theme.of(context).textTheme.bodyMedium?.copyWith(
                            color: AppPalette.textSecondary,
                          ),
                    ),
                  ],
                ),
              ],
            ),
            const SizedBox(height: 16),
            _loadingAreas
                ? const Center(child: CircularProgressIndicator())
                : _areas.isEmpty
                    ? _buildEmptyState()
                    : _buildAreasList(),
          ],
        ),
      ),
    );
  }

  Widget _buildKPIGrid() {
    return GridView.count(
      crossAxisCount: 2,
      shrinkWrap: true,
      physics: const NeverScrollableScrollPhysics(),
      mainAxisSpacing: 16,
      crossAxisSpacing: 16,
      childAspectRatio: 1.2,
      children: [
        _buildKPICard(
          icon: Icons.dashboard_outlined,
          label: 'Total Areas',
          value: '${_stats['totalAreas'] ?? 0}',
          footer: '${_stats['activeAreas'] ?? 0} active • ${_stats['inactiveAreas'] ?? 0} inactive',
        ),
        _buildKPICard(
          icon: Icons.link,
          label: 'Connected Services',
          value: '${_stats['connectedServices'] ?? 0}',
          footer: 'Services integrated',
        ),
        _buildKPICard(
          icon: Icons.flash_on,
          label: 'Executions (24h)',
          value: '${_stats['executionsLast24h'] ?? 0}',
          footer: _buildTrendText(),
        ),
        _buildKPICard(
          icon: Icons.check_circle_outline,
          label: 'Success Rate',
          value: '${_stats['successRate'] ?? 0}%',
          footer: '${_stats['successfulExecutions'] ?? 0} successful • ${_stats['failedExecutions'] ?? 0} failed',
        ),
      ],
    );
  }

  String _buildTrendText() {
    final trend = _stats['executionTrend'] ?? 0;
    final symbol = trend > 0 ? '↑' : trend < 0 ? '↓' : '→';
    return '$symbol ${trend.abs()}% vs last week';
  }

  Widget _buildKPICard({
    required IconData icon,
    required String label,
    required String value,
    required String footer,
  }) {
    return Container(
      padding: const EdgeInsets.all(16),
      decoration: BoxDecoration(
        color: AppPalette.surface,
        borderRadius: BorderRadius.circular(16),
        border: Border.all(color: AppPalette.borderDefault),
        boxShadow: AppShadows.card,
      ),
      child: Column(
        crossAxisAlignment: CrossAxisAlignment.start,
        children: [
          Row(
            children: [
              Icon(icon, color: AppPalette.accentBlue, size: 20),
              const SizedBox(width: 8),
              Expanded(
                child: Text(
                  label,
                  style: const TextStyle(
                    color: AppPalette.textSecondary,
                    fontSize: 12,
                    fontWeight: FontWeight.w500,
                  ),
                ),
              ),
            ],
          ),
          const Spacer(),
          Text(
            value,
            style: const TextStyle(
              color: AppPalette.textPrimary,
              fontSize: 28,
              fontWeight: FontWeight.bold,
            ),
          ),
          const SizedBox(height: 4),
          Text(
            footer,
            style: const TextStyle(
              color: AppPalette.textMuted,
              fontSize: 11,
            ),
            maxLines: 1,
            overflow: TextOverflow.ellipsis,
          ),
        ],
      ),
    );
  }

  Widget _buildRecentActivity() {
    final activities = _stats['recentActivity'] as List;
    return Container(
      decoration: BoxDecoration(
        color: AppPalette.surface,
        borderRadius: BorderRadius.circular(16),
        border: Border.all(color: AppPalette.borderDefault),
      ),
      child: ListView.separated(
        shrinkWrap: true,
        physics: const NeverScrollableScrollPhysics(),
        itemCount: activities.length,
        separatorBuilder: (_, __) => const Divider(color: AppPalette.borderSubtle, height: 1),
        itemBuilder: (context, index) {
          final activity = activities[index];
          final success = activity['success'] ?? false;
          return ListTile(
            leading: Icon(
              success ? Icons.check_circle : Icons.error,
              color: success ? AppPalette.success : AppPalette.danger,
              size: 20,
            ),
            title: Text(
              activity['message'] ?? 'Activity',
              style: const TextStyle(color: AppPalette.textPrimary, fontSize: 14),
            ),
            subtitle: Text(
              _formatTime(activity['executedAt']),
              style: const TextStyle(color: AppPalette.textMuted, fontSize: 12),
            ),
            trailing: Container(
              padding: const EdgeInsets.symmetric(horizontal: 8, vertical: 4),
              decoration: BoxDecoration(
                color: success ? AppPalette.success.withOpacity(0.1) : AppPalette.danger.withOpacity(0.1),
                borderRadius: BorderRadius.circular(8),
              ),
              child: Text(
                activity['status'] ?? '',
                style: TextStyle(
                  color: success ? AppPalette.success : AppPalette.danger,
                  fontSize: 11,
                  fontWeight: FontWeight.w600,
                ),
              ),
            ),
          );
        },
      ),
    );
  }

  Widget _buildEmptyState() {
    return Center(
      child: Padding(
        padding: const EdgeInsets.all(32),
        child: Column(
          children: [
            Icon(Icons.inbox_outlined, size: 64, color: AppPalette.textMuted),
            const SizedBox(height: 16),
            Text(
              'No areas yet',
              style: Theme.of(context).textTheme.titleMedium?.copyWith(
                    color: AppPalette.textSecondary,
                  ),
            ),
            const SizedBox(height: 8),
            Text(
              'Create your first automation!',
              style: Theme.of(context).textTheme.bodyMedium?.copyWith(
                    color: AppPalette.textMuted,
                  ),
            ),
          ],
        ),
      ),
    );
  }

  Widget _buildAreasList() {
    return ListView.separated(
      shrinkWrap: true,
      physics: const NeverScrollableScrollPhysics(),
      itemCount: _areas.length,
      separatorBuilder: (_, __) => const SizedBox(height: 16),
      itemBuilder: (context, index) {
        final area = _areas[index];
        return Container(
          padding: const EdgeInsets.all(16),
          decoration: BoxDecoration(
            color: AppPalette.surface,
            borderRadius: BorderRadius.circular(16),
            border: Border.all(color: AppPalette.borderDefault),
            boxShadow: AppShadows.card,
          ),
          child: Column(
            crossAxisAlignment: CrossAxisAlignment.start,
            children: [
              Row(
                children: [
                  Expanded(
                    child: Text(
                      area['name'],
                      style: Theme.of(context).textTheme.titleMedium?.copyWith(
                            color: AppPalette.textPrimary,
                            fontWeight: FontWeight.w600,
                          ),
                    ),
                  ),
                  Container(
                    padding: const EdgeInsets.symmetric(horizontal: 12, vertical: 4),
                    decoration: BoxDecoration(
                      color: area['active'] ? AppPalette.success.withOpacity(0.1) : AppPalette.textMuted.withOpacity(0.1),
                      borderRadius: BorderRadius.circular(12),
                    ),
                    child: Text(
                      area['active'] ? 'Active' : 'Inactive',
                      style: TextStyle(
                        color: area['active'] ? AppPalette.success : AppPalette.textMuted,
                        fontSize: 12,
                        fontWeight: FontWeight.w600,
                      ),
                    ),
                  ),
                ],
              ),
              const SizedBox(height: 8),
              Text(
                area['description'] ?? 'Workflow automation',
                style: Theme.of(context).textTheme.bodyMedium?.copyWith(
                      color: AppPalette.textSecondary,
                    ),
              ),
              const SizedBox(height: 12),
              Wrap(
                spacing: 8,
                runSpacing: 8,
                children: [
                  _buildActionButton(
                    icon: Icons.play_arrow,
                    label: 'Test',
                    onPressed: () => _testWorkflow(area),
                  ),
                  _buildActionButton(
                    icon: area['active'] ? Icons.power_settings_new : Icons.play_circle_outline,
                    label: area['active'] ? 'Disable' : 'Enable',
                    onPressed: () => _toggleArea(area),
                    color: area['active'] ? AppPalette.warning : AppPalette.success,
                  ),
                  _buildActionButton(
                    icon: Icons.delete_outline,
                    label: 'Delete',
                    onPressed: () => _deleteArea(area),
                    color: AppPalette.danger,
                  ),
                ],
              ),
            ],
          ),
        );
      },
    );
  }

  Widget _buildActionButton({
    required IconData icon,
    required String label,
    required VoidCallback onPressed,
    Color? color,
  }) {
    return InkWell(
      onTap: onPressed,
      borderRadius: BorderRadius.circular(8),
      child: Container(
        padding: const EdgeInsets.symmetric(horizontal: 12, vertical: 6),
        decoration: BoxDecoration(
          color: AppPalette.surfaceRaised,
          borderRadius: BorderRadius.circular(8),
          border: Border.all(color: color?.withOpacity(0.3) ?? AppPalette.borderDefault),
        ),
        child: Row(
          mainAxisSize: MainAxisSize.min,
          children: [
            Icon(icon, color: color ?? AppPalette.accentBlue, size: 16),
            const SizedBox(width: 6),
            Text(
              label,
              style: TextStyle(
                color: color ?? AppPalette.textPrimary,
                fontSize: 12,
                fontWeight: FontWeight.w500,
              ),
            ),
          ],
        ),
      ),
    );
  }
}
