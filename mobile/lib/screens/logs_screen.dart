import 'package:flutter/material.dart';
import 'package:intl/intl.dart';

import '../constants/palette.dart';
import '../constants/shadows.dart';
import '../services/api_service.dart';

class LogsScreen extends StatefulWidget {
  const LogsScreen({super.key});

  @override
  State<LogsScreen> createState() => _LogsScreenState();
}

class _LogsScreenState extends State<LogsScreen> {
  bool _loading = true;
  List<dynamic> _logs = [];
  int _total = 0;
  int _page = 0;
  final int _pageSize = 20;

  String? _statusFilter;
  String? _workflowIdFilter;

  @override
  void initState() {
    super.initState();
    _loadLogs();
  }

  Future<void> _loadLogs() async {
    try {
      setState(() => _loading = true);

      final filters = <String, dynamic>{
        'page': _page,
        'pageSize': _pageSize,
      };

      if (_statusFilter != null && _statusFilter!.isNotEmpty) {
        filters['status'] = _statusFilter;
      }

      if (_workflowIdFilter != null && _workflowIdFilter!.isNotEmpty) {
        filters['workflowId'] = _workflowIdFilter;
      }

      final result = await ApiService.getLogs(filters: filters);

      setState(() {
        _logs = result['logs'] ?? [];
        _total = result['total'] ?? 0;
        _loading = false;
      });
    } catch (error) {
      debugPrint('Failed to load logs: $error');
      setState(() => _loading = false);
      if (mounted) {
        ScaffoldMessenger.of(context).showSnackBar(
          SnackBar(content: Text('Failed to load logs: $error')),
        );
      }
    }
  }

  String _formatTime(String? timestamp) {
    if (timestamp == null) return 'N/A';

    try {
      final date = DateTime.parse(timestamp);
      final now = DateTime.now();
      final diff = now.difference(date);

      if (diff.inMinutes < 1) return 'Just now';
      if (diff.inMinutes < 60) return '${diff.inMinutes}m ago';
      if (diff.inHours < 24) return '${diff.inHours}h ago';
      if (diff.inDays == 1) return 'Yesterday';
      if (diff.inDays < 7) return '${diff.inDays}d ago';

      return DateFormat('MMM d, y HH:mm').format(date);
    } catch (e) {
      return 'N/A';
    }
  }

  Color _getStatusColor(String status) {
    switch (status.toUpperCase()) {
      case 'SUCCESS':
        return AppPalette.success;
      case 'FAILURE':
      case 'ERROR':
        return AppPalette.danger;
      case 'SKIPPED':
        return AppPalette.warning;
      default:
        return AppPalette.textMuted;
    }
  }

  IconData _getStatusIcon(String status) {
    switch (status.toUpperCase()) {
      case 'SUCCESS':
        return Icons.check_circle;
      case 'FAILURE':
      case 'ERROR':
        return Icons.error;
      case 'SKIPPED':
        return Icons.skip_next;
      default:
        return Icons.info;
    }
  }

  void _showFilterDialog() {
    showDialog(
      context: context,
      builder: (context) {
        String? tempStatusFilter = _statusFilter;
        String? tempWorkflowIdFilter = _workflowIdFilter;

        return AlertDialog(
          backgroundColor: AppPalette.surface,
          title: const Text(
            'Filter Logs',
            style: TextStyle(color: AppPalette.textPrimary),
          ),
          content: Column(
            mainAxisSize: MainAxisSize.min,
            children: [
              DropdownButtonFormField<String>(
                value: tempStatusFilter,
                dropdownColor: AppPalette.surface,
                style: const TextStyle(color: AppPalette.textPrimary),
                decoration: const InputDecoration(
                  labelText: 'Status',
                  labelStyle: TextStyle(color: AppPalette.textSecondary),
                ),
                items: [
                  const DropdownMenuItem(value: null, child: Text('All')),
                  const DropdownMenuItem(value: 'SUCCESS', child: Text('Success')),
                  const DropdownMenuItem(value: 'FAILURE', child: Text('Failure')),
                  const DropdownMenuItem(value: 'SKIPPED', child: Text('Skipped')),
                ],
                onChanged: (value) {
                  tempStatusFilter = value;
                },
              ),
              const SizedBox(height: 16),
              TextField(
                style: const TextStyle(color: AppPalette.textPrimary),
                decoration: const InputDecoration(
                  labelText: 'Workflow ID',
                  labelStyle: TextStyle(color: AppPalette.textSecondary),
                  enabledBorder: UnderlineInputBorder(
                    borderSide: BorderSide(color: AppPalette.borderDefault),
                  ),
                  focusedBorder: UnderlineInputBorder(
                    borderSide: BorderSide(color: AppPalette.accentBlue),
                  ),
                ),
                controller: TextEditingController(text: tempWorkflowIdFilter),
                onChanged: (value) {
                  tempWorkflowIdFilter = value.isEmpty ? null : value;
                },
              ),
            ],
          ),
          actions: [
            TextButton(
              onPressed: () {
                setState(() {
                  _statusFilter = null;
                  _workflowIdFilter = null;
                  _page = 0;
                });
                Navigator.pop(context);
                _loadLogs();
              },
              child: const Text('Clear'),
            ),
            TextButton(
              onPressed: () => Navigator.pop(context),
              child: const Text('Cancel'),
            ),
            TextButton(
              onPressed: () {
                setState(() {
                  _statusFilter = tempStatusFilter;
                  _workflowIdFilter = tempWorkflowIdFilter;
                  _page = 0;
                });
                Navigator.pop(context);
                _loadLogs();
              },
              child: const Text('Apply'),
            ),
          ],
        );
      },
    );
  }

  @override
  Widget build(BuildContext context) {
    return Scaffold(
      body: Column(
        children: [
          Container(
            padding: const EdgeInsets.all(20),
            color: AppPalette.surface,
            child: Row(
              children: [
                Expanded(
                  child: Text(
                    'Execution Logs',
                    style: Theme.of(context).textTheme.headlineSmall?.copyWith(
                          color: AppPalette.textPrimary,
                          fontWeight: FontWeight.bold,
                        ),
                  ),
                ),
                IconButton(
                  icon: const Icon(Icons.filter_list),
                  onPressed: _showFilterDialog,
                  color: AppPalette.accentBlue,
                ),
                IconButton(
                  icon: const Icon(Icons.refresh),
                  onPressed: _loadLogs,
                  color: AppPalette.accentBlue,
                ),
              ],
            ),
          ),
          Expanded(
            child: _loading
                ? const Center(child: CircularProgressIndicator())
                : _logs.isEmpty
                    ? Center(
                        child: Column(
                          mainAxisAlignment: MainAxisAlignment.center,
                          children: [
                            Icon(Icons.inbox_outlined, size: 64, color: AppPalette.textMuted),
                            const SizedBox(height: 16),
                            Text(
                              'No logs found',
                              style: Theme.of(context).textTheme.titleMedium?.copyWith(
                                    color: AppPalette.textSecondary,
                                  ),
                            ),
                          ],
                        ),
                      )
                    : RefreshIndicator(
                        onRefresh: _loadLogs,
                        child: ListView.separated(
                          padding: const EdgeInsets.all(20),
                          itemCount: _logs.length,
                          separatorBuilder: (_, __) => const SizedBox(height: 12),
                          itemBuilder: (context, index) {
                            final log = _logs[index];
                            return _buildLogCard(log);
                          },
                        ),
                      ),
          ),
          if (_logs.isNotEmpty)
            Container(
              padding: const EdgeInsets.symmetric(horizontal: 20, vertical: 12),
              decoration: BoxDecoration(
                color: AppPalette.surface,
                border: Border(
                  top: BorderSide(color: AppPalette.borderDefault),
                ),
              ),
              child: Row(
                mainAxisAlignment: MainAxisAlignment.spaceBetween,
                children: [
                  Text(
                    'Showing ${_page * _pageSize + 1}-${(_page + 1) * _pageSize > _total ? _total : (_page + 1) * _pageSize} of $_total',
                    style: const TextStyle(color: AppPalette.textSecondary, fontSize: 12),
                  ),
                  Row(
                    children: [
                      IconButton(
                        icon: const Icon(Icons.chevron_left),
                        onPressed: _page > 0
                            ? () {
                                setState(() => _page--);
                                _loadLogs();
                              }
                            : null,
                        color: AppPalette.accentBlue,
                      ),
                      IconButton(
                        icon: const Icon(Icons.chevron_right),
                        onPressed: (_page + 1) * _pageSize < _total
                            ? () {
                                setState(() => _page++);
                                _loadLogs();
                              }
                            : null,
                        color: AppPalette.accentBlue,
                      ),
                    ],
                  ),
                ],
              ),
            ),
        ],
      ),
    );
  }

  Widget _buildLogCard(Map<String, dynamic> log) {
    final status = log['status'] ?? 'UNKNOWN';
    final statusColor = _getStatusColor(status);
    final statusIcon = _getStatusIcon(status);

    return Container(
      padding: const EdgeInsets.all(16),
      decoration: BoxDecoration(
        color: AppPalette.surface,
        borderRadius: BorderRadius.circular(12),
        border: Border.all(color: AppPalette.borderDefault),
        boxShadow: AppShadows.card,
      ),
      child: Column(
        crossAxisAlignment: CrossAxisAlignment.start,
        children: [
          Row(
            children: [
              Icon(statusIcon, color: statusColor, size: 20),
              const SizedBox(width: 8),
              Expanded(
                child: Text(
                  log['workflowName'] ?? 'Workflow #${log['workflowId']}',
                  style: Theme.of(context).textTheme.titleSmall?.copyWith(
                        color: AppPalette.textPrimary,
                        fontWeight: FontWeight.w600,
                      ),
                ),
              ),
              Container(
                padding: const EdgeInsets.symmetric(horizontal: 8, vertical: 4),
                decoration: BoxDecoration(
                  color: statusColor.withOpacity(0.1),
                  borderRadius: BorderRadius.circular(8),
                ),
                child: Text(
                  status,
                  style: TextStyle(
                    color: statusColor,
                    fontSize: 11,
                    fontWeight: FontWeight.w600,
                  ),
                ),
              ),
            ],
          ),
          if (log['message'] != null) ...[
            const SizedBox(height: 8),
            Text(
              log['message'],
              style: Theme.of(context).textTheme.bodySmall?.copyWith(
                    color: AppPalette.textSecondary,
                  ),
              maxLines: 2,
              overflow: TextOverflow.ellipsis,
            ),
          ],
          const SizedBox(height: 12),
          Row(
            children: [
              Icon(Icons.access_time, size: 14, color: AppPalette.textMuted),
              const SizedBox(width: 4),
              Text(
                _formatTime(log['executedAt']),
                style: const TextStyle(color: AppPalette.textMuted, fontSize: 12),
              ),
              if (log['executionTimeMs'] != null) ...[
                const SizedBox(width: 16),
                Icon(Icons.timer, size: 14, color: AppPalette.textMuted),
                const SizedBox(width: 4),
                Text(
                  '${log['executionTimeMs']}ms',
                  style: const TextStyle(color: AppPalette.textMuted, fontSize: 12),
                ),
              ],
            ],
          ),
        ],
      ),
    );
  }
}
