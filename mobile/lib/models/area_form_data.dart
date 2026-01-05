/// Form data model for creating AREA automations (Gmail → Discord)
class AreaFormData {
  // Gmail trigger configuration
  int? selectedGmailConnectionId;
  String gmailLabel;
  String? gmailSubjectFilter;
  String? gmailFromFilter;

  // Discord reaction configuration
  int? selectedDiscordConnectionId;
  String? discordWebhookUrl;
  String? discordChannelName;
  String? discordMessageTemplate;

  AreaFormData({
    this.selectedGmailConnectionId,
    this.gmailLabel = 'INBOX',
    this.gmailSubjectFilter,
    this.gmailFromFilter,
    this.selectedDiscordConnectionId,
    this.discordWebhookUrl,
    this.discordChannelName,
    this.discordMessageTemplate,
  });

  /// Validates that all required fields are filled
  bool isValid() {
    return selectedGmailConnectionId != null &&
        selectedDiscordConnectionId != null &&
        discordWebhookUrl != null &&
        discordWebhookUrl!.isNotEmpty &&
        isValidWebhookUrl(discordWebhookUrl!);
  }

  /// Validates Discord webhook URL format
  bool isValidWebhookUrl(String url) {
    return url.startsWith('https://discord.com/api/webhooks/') ||
        url.startsWith('https://discordapp.com/api/webhooks/');
  }

  /// Converts form data to JSON for API request
  Map<String, dynamic> toJson() {
    return {
      'actionConnectionId': selectedGmailConnectionId,
      'reactionConnectionId': selectedDiscordConnectionId,
      'gmailLabel': gmailLabel,
      'gmailSubjectContains': gmailSubjectFilter?.isEmpty == true
          ? null
          : gmailSubjectFilter,
      'gmailFromAddress': gmailFromFilter?.isEmpty == true
          ? null
          : gmailFromFilter,
      'discordWebhookUrl': discordWebhookUrl,
      'discordChannelName': discordChannelName?.isEmpty == true
          ? 'general'
          : discordChannelName,
      'discordMessageTemplate': discordMessageTemplate?.isEmpty == true
          ? 'New email from {{sender}}: {{subject}}'
          : discordMessageTemplate,
    };
  }

  /// Creates a copy with updated fields
  AreaFormData copyWith({
    int? selectedGmailConnectionId,
    String? gmailLabel,
    String? gmailSubjectFilter,
    String? gmailFromFilter,
    int? selectedDiscordConnectionId,
    String? discordWebhookUrl,
    String? discordChannelName,
    String? discordMessageTemplate,
  }) {
    return AreaFormData(
      selectedGmailConnectionId: selectedGmailConnectionId ?? this.selectedGmailConnectionId,
      gmailLabel: gmailLabel ?? this.gmailLabel,
      gmailSubjectFilter: gmailSubjectFilter ?? this.gmailSubjectFilter,
      gmailFromFilter: gmailFromFilter ?? this.gmailFromFilter,
      selectedDiscordConnectionId: selectedDiscordConnectionId ?? this.selectedDiscordConnectionId,
      discordWebhookUrl: discordWebhookUrl ?? this.discordWebhookUrl,
      discordChannelName: discordChannelName ?? this.discordChannelName,
      discordMessageTemplate: discordMessageTemplate ?? this.discordMessageTemplate,
    );
  }

  /// Resets all form data to initial state
  void reset() {
    selectedGmailConnectionId = null;
    gmailLabel = 'INBOX';
    gmailSubjectFilter = null;
    gmailFromFilter = null;
    selectedDiscordConnectionId = null;
    discordWebhookUrl = null;
    discordChannelName = null;
    discordMessageTemplate = null;
  }
}
