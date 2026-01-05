import 'package:flutter/material.dart';

/// Metadata for available services, triggers, and actions.
///
/// This provides a centralized definition of all supported services
/// and their capabilities until the backend provides this dynamically.
class ServiceMetadata {
  // Service display information
  static const serviceInfo = {
    'gmail': {
      'name': 'Gmail',
      'description': 'Email service by Google',
      'icon': Icons.mail,
      'color': Color(0xFFEA4335), // Gmail red
      'requiresAuth': true,
    },
    'discord': {
      'name': 'Discord',
      'description': 'Team communication platform',
      'icon': Icons.chat,
      'color': Color(0xFF5865F2), // Discord blurple
      'requiresAuth': true,
    },
    'timer': {
      'name': 'Timer',
      'description': 'Schedule-based triggers',
      'icon': Icons.schedule,
      'color': Color(0xFF9C27B0), // Purple
      'requiresAuth': false,
    },
    'github': {
      'name': 'GitHub',
      'description': 'Code repository platform',
      'icon': Icons.code,
      'color': Color(0xFF181717), // GitHub black
      'requiresAuth': true,
    },
    'dropbox': {
      'name': 'Dropbox',
      'description': 'Cloud storage service',
      'icon': Icons.cloud_upload,
      'color': Color(0xFF0061FF), // Dropbox blue
      'requiresAuth': true,
    },
    'outlook': {
      'name': 'Outlook',
      'description': 'Email and calendar service',
      'icon': Icons.email,
      'color': Color(0xFF0078D4), // Microsoft blue
      'requiresAuth': true,
    },
  };

  // Trigger types for each service
  static const triggerTypes = {
    'gmail': [
      {
        'type': 'new_email',
        'name': 'New email received',
        'description': 'Triggers when a new email arrives in your inbox',
        'icon': Icons.mail_outline,
        'variables': ['sender', 'subject', 'body', 'receivedAt', 'unreadCount'],
      },
      {
        'type': 'email_with_attachment',
        'name': 'Email with attachment',
        'description': 'Triggers when an email with attachments is received',
        'icon': Icons.attach_file,
        'variables': [
          'sender',
          'subject',
          'body',
          'attachmentCount',
          'attachmentNames',
          'receivedAt'
        ],
      },
    ],
    'timer': [
      {
        'type': 'scheduled',
        'name': 'At a specific time',
        'description': 'Triggers at a scheduled time every day',
        'icon': Icons.alarm,
        'variables': ['currentTime', 'currentDate', 'dayOfWeek'],
      },
      {
        'type': 'interval',
        'name': 'Every X minutes',
        'description': 'Triggers repeatedly at fixed intervals',
        'icon': Icons.timer,
        'variables': ['executionCount', 'lastExecution', 'currentTime'],
      },
    ],
    'github': [
      {
        'type': 'new_issue',
        'name': 'New issue created',
        'description': 'Triggers when a new issue is opened',
        'icon': Icons.bug_report,
        'variables': [
          'issueTitle',
          'issueBody',
          'issueAuthor',
          'issueNumber',
          'repositoryName',
          'labels'
        ],
      },
      {
        'type': 'pull_request',
        'name': 'New pull request',
        'description': 'Triggers when a pull request is opened',
        'icon': Icons.merge_type,
        'variables': [
          'prTitle',
          'prAuthor',
          'prNumber',
          'repositoryName',
          'sourceBranch',
          'targetBranch'
        ],
      },
      {
        'type': 'push',
        'name': 'Code pushed',
        'description': 'Triggers when code is pushed to a repository',
        'icon': Icons.cloud_upload,
        'variables': ['commitMessage', 'commitAuthor', 'branch', 'repositoryName'],
      },
    ],
  };

  // Action types for each service
  static const actionTypes = {
    'discord': [
      {
        'type': 'send_message',
        'name': 'Send message',
        'description': 'Send a message to a Discord channel via webhook',
        'icon': Icons.message,
      },
      {
        'type': 'send_embed',
        'name': 'Send rich embed',
        'description': 'Send a formatted embed message with fields',
        'icon': Icons.featured_play_list,
      },
    ],
    'dropbox': [
      {
        'type': 'upload_file',
        'name': 'Upload file',
        'description': 'Upload a file to your Dropbox',
        'icon': Icons.upload_file,
      },
      {
        'type': 'create_folder',
        'name': 'Create folder',
        'description': 'Create a new folder in Dropbox',
        'icon': Icons.create_new_folder,
      },
    ],
    'outlook': [
      {
        'type': 'send_email',
        'name': 'Send email',
        'description': 'Send an email from your Outlook account',
        'icon': Icons.email,
      },
      {
        'type': 'create_event',
        'name': 'Create calendar event',
        'description': 'Add an event to your Outlook calendar',
        'icon': Icons.calendar_today,
      },
    ],
    'gmail': [
      {
        'type': 'send_email',
        'name': 'Send email',
        'description': 'Send an email from your Gmail account',
        'icon': Icons.send,
      },
    ],
  };

  /// Get service information by type
  static Map<String, dynamic>? getServiceInfo(String serviceType) {
    return serviceInfo[serviceType];
  }

  /// Get all available services with triggers
  static List<Map<String, dynamic>> getServicesWithTriggers() {
    return triggerTypes.keys
        .map((type) => {
              'type': type,
              ...?serviceInfo[type],
            })
        .toList();
  }

  /// Get all available services with actions
  static List<Map<String, dynamic>> getServicesWithActions() {
    return actionTypes.keys
        .map((type) => {
              'type': type,
              ...?serviceInfo[type],
            })
        .toList();
  }

  /// Get trigger types for a specific service
  static List<Map<String, dynamic>>? getTriggerTypes(String serviceType) {
    final triggers = triggerTypes[serviceType];
    if (triggers == null) return null;

    return List<Map<String, dynamic>>.from(
      triggers.map((t) => Map<String, dynamic>.from(t)),
    );
  }

  /// Get action types for a specific service
  static List<Map<String, dynamic>>? getActionTypes(String serviceType) {
    final actions = actionTypes[serviceType];
    if (actions == null) return null;

    return List<Map<String, dynamic>>.from(
      actions.map((a) => Map<String, dynamic>.from(a)),
    );
  }

  /// Get available variables for a trigger type
  static List<String> getTriggerVariables(String serviceType, String triggerType) {
    final triggers = triggerTypes[serviceType];
    if (triggers == null) return [];

    final trigger = triggers.firstWhere(
      (t) => t['type'] == triggerType,
      orElse: () => {},
    );

    final variables = trigger['variables'];
    return variables != null ? List<String>.from(variables) : [];
  }

  /// Check if a service requires authentication
  static bool requiresAuthentication(String serviceType) {
    final info = serviceInfo[serviceType];
    return info?['requiresAuth'] ?? false;
  }

  /// Get display name for a service
  static String getServiceName(String serviceType) {
    return serviceInfo[serviceType]?['name'] ?? serviceType;
  }

  /// Get icon for a service
  static IconData getServiceIcon(String serviceType) {
    return serviceInfo[serviceType]?['icon'] ?? Icons.cloud;
  }

  /// Get color for a service
  static Color getServiceColor(String serviceType) {
    return serviceInfo[serviceType]?['color'] ?? const Color(0xFF6B7280);
  }

  /// Get trigger type display name
  static String getTriggerTypeName(String serviceType, String triggerType) {
    final triggers = triggerTypes[serviceType];
    if (triggers == null) return triggerType;

    final trigger = triggers.firstWhere(
      (t) => t['type'] == triggerType,
      orElse: () => {},
    );

    return trigger['name'] ?? triggerType;
  }

  /// Get action type display name
  static String getActionTypeName(String serviceType, String actionType) {
    final actions = actionTypes[serviceType];
    if (actions == null) return actionType;

    final action = actions.firstWhere(
      (a) => a['type'] == actionType,
      orElse: () => {},
    );

    return action['name'] ?? actionType;
  }
}
