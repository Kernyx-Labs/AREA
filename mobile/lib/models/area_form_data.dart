/// Form data model for creating AREA automations with dynamic trigger/action support.
///
/// Supports any combination of services (Gmail, Discord, Timer, GitHub, etc.)
/// instead of being hardcoded to Gmail → Discord only.
class AreaFormData {
  // Trigger (action) configuration
  String? selectedTriggerService; // e.g., 'gmail', 'timer', 'github'
  String? selectedTriggerType; // e.g., 'new_email', 'scheduled'
  int? triggerConnectionId; // For OAuth services only
  Map<String, dynamic> triggerConfig = {}; // Dynamic config fields

  // Action (reaction) configuration
  String? selectedActionService; // e.g., 'discord', 'dropbox', 'outlook'
  String? selectedActionType; // e.g., 'send_message', 'upload_file'
  int? actionConnectionId; // For OAuth services only
  Map<String, dynamic> actionConfig = {}; // Dynamic config fields

  AreaFormData({
    this.selectedTriggerService,
    this.selectedTriggerType,
    this.triggerConnectionId,
    Map<String, dynamic>? triggerConfig,
    this.selectedActionService,
    this.selectedActionType,
    this.actionConnectionId,
    Map<String, dynamic>? actionConfig,
  }) {
    if (triggerConfig != null) {
      this.triggerConfig = Map<String, dynamic>.from(triggerConfig);
    }
    if (actionConfig != null) {
      this.actionConfig = Map<String, dynamic>.from(actionConfig);
    }
  }

  /// Validates that all required fields are filled
  bool isValid() {
    // Must have trigger service and type selected
    if (selectedTriggerService == null || selectedTriggerType == null) {
      return false;
    }

    // Must have action service and type selected
    if (selectedActionService == null || selectedActionType == null) {
      return false;
    }

    // For services requiring authentication, must have connection ID
    // (This will be checked based on ServiceMetadata.requiresAuthentication)

    return true;
  }

  /// Converts form data to JSON for API request
  ///
  /// The backend expects specific field names based on the selected services.
  /// This method creates a dynamic payload based on the selections.
  Map<String, dynamic> toJson() {
    final json = <String, dynamic>{
      // Connection IDs (if applicable)
      if (triggerConnectionId != null)
        'actionConnectionId': triggerConnectionId,
      if (actionConnectionId != null)
        'reactionConnectionId': actionConnectionId,

      // Service types
      'triggerService': selectedTriggerService,
      'triggerType': selectedTriggerType,
      'actionService': selectedActionService,
      'actionType': selectedActionType,
    };

    // Add trigger config fields with service-specific prefixes
    if (selectedTriggerService == 'gmail') {
      json['gmailLabel'] = triggerConfig['label'] ?? 'INBOX';
      if (triggerConfig['subjectFilter']?.isNotEmpty == true) {
        json['gmailSubjectContains'] = triggerConfig['subjectFilter'];
      }
      if (triggerConfig['fromFilter']?.isNotEmpty == true) {
        json['gmailFromAddress'] = triggerConfig['fromFilter'];
      }
    } else if (selectedTriggerService == 'timer') {
      if (selectedTriggerType == 'scheduled') {
        json['timerScheduledTime'] = triggerConfig['scheduledTime'];
        json['timerTimezone'] = triggerConfig['timezone'] ?? 'UTC';
      } else if (selectedTriggerType == 'interval') {
        json['timerIntervalMinutes'] = triggerConfig['intervalMinutes'];
      }
    } else if (selectedTriggerService == 'github') {
      json['githubRepository'] = triggerConfig['repository'];
      if (triggerConfig['labels']?.isNotEmpty == true) {
        json['githubLabels'] = triggerConfig['labels'];
      }
    }

    // Add action config fields with service-specific prefixes
    if (selectedActionService == 'discord') {
      json['discordWebhookUrl'] = actionConfig['webhookUrl'];
      json['discordChannelName'] = actionConfig['channelName'] ?? 'general';

      if (selectedActionType == 'send_message') {
        json['discordMessageTemplate'] =
            actionConfig['messageTemplate'] ?? 'Automation triggered';
      } else if (selectedActionType == 'send_embed') {
        json['discordEmbedTitle'] = actionConfig['embedTitle'];
        json['discordEmbedDescription'] = actionConfig['embedDescription'];
        json['discordEmbedColor'] = actionConfig['embedColor'] ?? '#0099FF';
      }
    } else if (selectedActionService == 'dropbox') {
      if (selectedActionType == 'upload_file') {
        json['dropboxFilePath'] = actionConfig['filePath'];
        json['dropboxFileName'] = actionConfig['fileName'];
      } else if (selectedActionType == 'create_folder') {
        json['dropboxFolderPath'] = actionConfig['folderPath'];
      }
    } else if (selectedActionService == 'outlook') {
      if (selectedActionType == 'send_email') {
        json['outlookTo'] = actionConfig['to'];
        json['outlookSubject'] = actionConfig['subject'];
        json['outlookBody'] = actionConfig['body'];
      } else if (selectedActionType == 'create_event') {
        json['outlookEventTitle'] = actionConfig['eventTitle'];
        json['outlookEventStart'] = actionConfig['eventStart'];
        json['outlookEventEnd'] = actionConfig['eventEnd'];
      }
    } else if (selectedActionService == 'gmail') {
      if (selectedActionType == 'send_email') {
        json['gmailTo'] = actionConfig['to'];
        json['gmailSubject'] = actionConfig['subject'];
        json['gmailBody'] = actionConfig['body'];
      }
    }

    return json;
  }

  /// Creates a copy with updated fields
  AreaFormData copyWith({
    String? selectedTriggerService,
    String? selectedTriggerType,
    int? triggerConnectionId,
    Map<String, dynamic>? triggerConfig,
    String? selectedActionService,
    String? selectedActionType,
    int? actionConnectionId,
    Map<String, dynamic>? actionConfig,
  }) {
    return AreaFormData(
      selectedTriggerService:
          selectedTriggerService ?? this.selectedTriggerService,
      selectedTriggerType: selectedTriggerType ?? this.selectedTriggerType,
      triggerConnectionId: triggerConnectionId ?? this.triggerConnectionId,
      triggerConfig: triggerConfig ?? Map.from(this.triggerConfig),
      selectedActionService:
          selectedActionService ?? this.selectedActionService,
      selectedActionType: selectedActionType ?? this.selectedActionType,
      actionConnectionId: actionConnectionId ?? this.actionConnectionId,
      actionConfig: actionConfig ?? Map.from(this.actionConfig),
    );
  }

  /// Resets all form data to initial state
  void reset() {
    selectedTriggerService = null;
    selectedTriggerType = null;
    triggerConnectionId = null;
    triggerConfig = {};
    selectedActionService = null;
    selectedActionType = null;
    actionConnectionId = null;
    actionConfig = {};
  }

  /// Helper method to update trigger config field
  void updateTriggerConfig(String key, dynamic value) {
    triggerConfig[key] = value;
  }

  /// Helper method to update action config field
  void updateActionConfig(String key, dynamic value) {
    actionConfig[key] = value;
  }

  /// Helper method to get trigger config value
  T? getTriggerConfig<T>(String key) {
    return triggerConfig[key] as T?;
  }

  /// Helper method to get action config value
  T? getActionConfig<T>(String key) {
    return actionConfig[key] as T?;
  }

  // Backward compatibility methods for legacy Gmail → Discord workflow

  /// Legacy: Get Gmail connection ID (if trigger is Gmail)
  int? get selectedGmailConnectionId =>
      selectedTriggerService == 'gmail' ? triggerConnectionId : null;

  /// Legacy: Set Gmail connection ID
  set selectedGmailConnectionId(int? value) {
    if (selectedTriggerService == 'gmail') {
      triggerConnectionId = value;
    }
  }

  /// Legacy: Get Gmail label
  String get gmailLabel => triggerConfig['label'] ?? 'INBOX';

  /// Legacy: Set Gmail label
  set gmailLabel(String value) {
    triggerConfig['label'] = value;
  }

  /// Legacy: Get Gmail subject filter
  String? get gmailSubjectFilter => triggerConfig['subjectFilter'];

  /// Legacy: Set Gmail subject filter
  set gmailSubjectFilter(String? value) {
    if (value != null) {
      triggerConfig['subjectFilter'] = value;
    } else {
      triggerConfig.remove('subjectFilter');
    }
  }

  /// Legacy: Get Gmail from filter
  String? get gmailFromFilter => triggerConfig['fromFilter'];

  /// Legacy: Set Gmail from filter
  set gmailFromFilter(String? value) {
    if (value != null) {
      triggerConfig['fromFilter'] = value;
    } else {
      triggerConfig.remove('fromFilter');
    }
  }

  /// Legacy: Get Discord connection ID (if action is Discord)
  int? get selectedDiscordConnectionId =>
      selectedActionService == 'discord' ? actionConnectionId : null;

  /// Legacy: Set Discord connection ID
  set selectedDiscordConnectionId(int? value) {
    if (selectedActionService == 'discord') {
      actionConnectionId = value;
    }
  }

  /// Legacy: Get Discord webhook URL
  String? get discordWebhookUrl => actionConfig['webhookUrl'];

  /// Legacy: Set Discord webhook URL
  set discordWebhookUrl(String? value) {
    if (value != null) {
      actionConfig['webhookUrl'] = value;
    } else {
      actionConfig.remove('webhookUrl');
    }
  }

  /// Legacy: Get Discord channel name
  String? get discordChannelName => actionConfig['channelName'];

  /// Legacy: Set Discord channel name
  set discordChannelName(String? value) {
    if (value != null) {
      actionConfig['channelName'] = value;
    } else {
      actionConfig.remove('channelName');
    }
  }

  /// Legacy: Get Discord message template
  String? get discordMessageTemplate => actionConfig['messageTemplate'];

  /// Legacy: Set Discord message template
  set discordMessageTemplate(String? value) {
    if (value != null) {
      actionConfig['messageTemplate'] = value;
    } else {
      actionConfig.remove('messageTemplate');
    }
  }

  /// Legacy: Validates Discord webhook URL format
  bool isValidWebhookUrl(String url) {
    return url.startsWith('https://discord.com/api/webhooks/') ||
        url.startsWith('https://discordapp.com/api/webhooks/');
  }
}
