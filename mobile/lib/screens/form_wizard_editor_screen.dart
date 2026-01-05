import 'package:flutter/material.dart';
import '../constants/palette.dart';
import '../models/area_form_data.dart';
import '../services/api_service.dart';
import '../widgets/variable_insertion_panel.dart';
import '../widgets/step_indicator.dart';
import '../widgets/connection_selector_card.dart';

class FormWizardEditorScreen extends StatefulWidget {
  const FormWizardEditorScreen({super.key});

  @override
  State<FormWizardEditorScreen> createState() => _FormWizardEditorScreenState();
}

class _FormWizardEditorScreenState extends State<FormWizardEditorScreen> {
  final PageController _pageController = PageController();
  int _currentStep = 0;
  final int _totalSteps = 5;

  // Form data
  final AreaFormData _formData = AreaFormData();

  // Service connections
  List<Map<String, dynamic>> _gmailConnections = [];
  List<Map<String, dynamic>> _discordConnections = [];
  bool _loading = true;
  String? _error;

  // Text controllers
  final _subjectController = TextEditingController();
  final _senderController = TextEditingController();
  final _webhookController = TextEditingController();
  final _channelController = TextEditingController();
  final _messageController = TextEditingController();

  @override
  void initState() {
    super.initState();
    _loadConnections();
  }

  @override
  void dispose() {
    _pageController.dispose();
    _subjectController.dispose();
    _senderController.dispose();
    _webhookController.dispose();
    _channelController.dispose();
    _messageController.dispose();
    super.dispose();
  }

  Future<void> _loadConnections() async {
    try {
      setState(() {
        _loading = true;
        _error = null;
      });

      final connections = await ApiService.getConnectedServices();

      setState(() {
        _gmailConnections = connections
            .where((c) => c['service'] == 'gmail')
            .cast<Map<String, dynamic>>()
            .toList();
        _discordConnections = connections
            .where((c) => c['service'] == 'discord')
            .cast<Map<String, dynamic>>()
            .toList();
        _loading = false;
      });
    } catch (e) {
      setState(() {
        _error = e.toString();
        _loading = false;
      });
    }
  }

  void _nextStep() {
    if (!_validateCurrentStep()) return;

    if (_currentStep < _totalSteps - 1) {
      setState(() => _currentStep++);
      _pageController.animateToPage(
        _currentStep,
        duration: const Duration(milliseconds: 300),
        curve: Curves.easeInOut,
      );
    }
  }

  void _previousStep() {
    if (_currentStep > 0) {
      setState(() => _currentStep--);
      _pageController.animateToPage(
        _currentStep,
        duration: const Duration(milliseconds: 300),
        curve: Curves.easeInOut,
      );
    }
  }

  void _jumpToStep(int step) {
    if (step >= 0 && step < _totalSteps) {
      setState(() => _currentStep = step);
      _pageController.animateToPage(
        _currentStep,
        duration: const Duration(milliseconds: 300),
        curve: Curves.easeInOut,
      );
    }
  }

  bool _validateCurrentStep() {
    switch (_currentStep) {
      case 0: // Gmail connection selection
        if (_formData.selectedGmailConnectionId == null) {
          _showError('Please select a Gmail account');
          return false;
        }
        return true;

      case 1: // Gmail configuration
        // All fields optional, just sync with form data
        _formData.gmailSubjectFilter = _subjectController.text;
        _formData.gmailFromFilter = _senderController.text;
        return true;

      case 2: // Discord connection selection
        if (_formData.selectedDiscordConnectionId == null) {
          _showError('Please select a Discord connection');
          return false;
        }
        return true;

      case 3: // Discord configuration
        _formData.discordWebhookUrl = _webhookController.text;
        _formData.discordChannelName = _channelController.text;
        _formData.discordMessageTemplate = _messageController.text;

        if (_formData.discordWebhookUrl == null ||
            _formData.discordWebhookUrl!.isEmpty) {
          _showError('Discord webhook URL is required');
          return false;
        }

        if (!_formData.isValidWebhookUrl(_formData.discordWebhookUrl!)) {
          _showError('Invalid Discord webhook URL format');
          return false;
        }
        return true;

      case 4: // Review
        return _formData.isValid();

      default:
        return true;
    }
  }

  void _showError(String message) {
    ScaffoldMessenger.of(context).showSnackBar(
      SnackBar(
        content: Text(message),
        backgroundColor: AppPalette.danger,
        duration: const Duration(seconds: 3),
      ),
    );
  }

  void _showSuccess(String message) {
    ScaffoldMessenger.of(context).showSnackBar(
      SnackBar(
        content: Text(message),
        backgroundColor: AppPalette.success,
        duration: const Duration(seconds: 2),
      ),
    );
  }

  Future<void> _saveArea() async {
    if (!_formData.isValid()) {
      _showError('Please complete all required fields');
      return;
    }

    try {
      setState(() => _loading = true);

      await ApiService.createArea(_formData.toJson());

      if (mounted) {
        _showSuccess('Automation created successfully!');
        // Navigate back to dashboard
        Navigator.of(context).pop();
      }
    } catch (e) {
      if (mounted) {
        setState(() => _loading = false);
        _showError('Failed to create automation: $e');
      }
    }
  }

  @override
  Widget build(BuildContext context) {
    return Scaffold(
      backgroundColor: AppPalette.background,
      appBar: AppBar(
        title: const Text('Create Automation'),
        backgroundColor: AppPalette.surface,
        foregroundColor: AppPalette.textPrimary,
        elevation: 0,
      ),
      body: Column(
        children: [
          // Step indicator
          StepIndicator(
            currentStep: _currentStep + 1,
            totalSteps: _totalSteps,
            stepTitles: const [
              'Select Gmail',
              'Configure Trigger',
              'Select Discord',
              'Configure Message',
              'Review',
            ],
          ),

          // Page content
          Expanded(
            child: _loading && _gmailConnections.isEmpty
                ? const Center(child: CircularProgressIndicator())
                : _error != null
                    ? Center(
                        child: Column(
                          mainAxisAlignment: MainAxisAlignment.center,
                          children: [
                            Text(
                              'Error loading connections',
                              style: TextStyle(color: AppPalette.danger),
                            ),
                            const SizedBox(height: 16),
                            ElevatedButton(
                              onPressed: _loadConnections,
                              child: const Text('Retry'),
                            ),
                          ],
                        ),
                      )
                    : PageView(
                        controller: _pageController,
                        physics: const NeverScrollableScrollPhysics(),
                        onPageChanged: (index) =>
                            setState(() => _currentStep = index),
                        children: [
                          _buildStep0GmailSelection(),
                          _buildStep1GmailConfig(),
                          _buildStep2DiscordSelection(),
                          _buildStep3DiscordConfig(),
                          _buildStep4Review(),
                        ],
                      ),
          ),

          // Bottom action bar
          Container(
            padding: const EdgeInsets.all(16),
            decoration: BoxDecoration(
              color: AppPalette.surface,
              border: Border(
                top: BorderSide(color: AppPalette.borderDefault),
              ),
            ),
            child: Row(
              children: [
                if (_currentStep > 0)
                  Expanded(
                    child: OutlinedButton(
                      onPressed: _previousStep,
                      style: OutlinedButton.styleFrom(
                        foregroundColor: AppPalette.textPrimary,
                        side: BorderSide(color: AppPalette.borderDefault),
                        padding: const EdgeInsets.symmetric(vertical: 16),
                      ),
                      child: const Text('Back'),
                    ),
                  ),
                if (_currentStep > 0) const SizedBox(width: 16),
                Expanded(
                  child: ElevatedButton(
                    onPressed: _loading
                        ? null
                        : (_currentStep == _totalSteps - 1
                            ? _saveArea
                            : _nextStep),
                    style: ElevatedButton.styleFrom(
                      backgroundColor: AppPalette.accentBlue,
                      foregroundColor: Colors.white,
                      padding: const EdgeInsets.symmetric(vertical: 16),
                    ),
                    child: _loading
                        ? const SizedBox(
                            height: 20,
                            width: 20,
                            child: CircularProgressIndicator(
                              strokeWidth: 2,
                              valueColor:
                                  AlwaysStoppedAnimation<Color>(Colors.white),
                            ),
                          )
                        : Text(_currentStep == _totalSteps - 1
                            ? 'Save & Activate'
                            : 'Next'),
                  ),
                ),
              ],
            ),
          ),
        ],
      ),
    );
  }

  // Step 0: Select Gmail Connection
  Widget _buildStep0GmailSelection() {
    return ListView(
      padding: const EdgeInsets.all(16),
      children: [
        Text(
          'Select Gmail Account',
          style: TextStyle(
            fontSize: 20,
            fontWeight: FontWeight.w600,
            color: AppPalette.textPrimary,
          ),
        ),
        const SizedBox(height: 8),
        Text(
          'Choose which Gmail account to monitor for new emails',
          style: TextStyle(
            fontSize: 14,
            color: AppPalette.textSecondary,
          ),
        ),
        const SizedBox(height: 24),
        if (_gmailConnections.isEmpty)
          Card(
            child: Padding(
              padding: const EdgeInsets.all(16),
              child: Column(
                children: [
                  Icon(
                    Icons.mail_outline,
                    size: 48,
                    color: AppPalette.textSecondary,
                  ),
                  const SizedBox(height: 16),
                  Text(
                    'No Gmail accounts connected',
                    style: TextStyle(
                      fontSize: 16,
                      fontWeight: FontWeight.w600,
                      color: AppPalette.textPrimary,
                    ),
                  ),
                  const SizedBox(height: 8),
                  Text(
                    'Connect a Gmail account in the Services tab first',
                    textAlign: TextAlign.center,
                    style: TextStyle(
                      fontSize: 14,
                      color: AppPalette.textSecondary,
                    ),
                  ),
                ],
              ),
            ),
          )
        else
          ..._gmailConnections.map((connection) {
            final isSelected =
                _formData.selectedGmailConnectionId == connection['id'];
            return ConnectionSelectorCard(
              service: 'Gmail',
              email: connection['connectedAccount'] ?? 'Unknown',
              isActive: connection['status'] == 'active',
              isSelected: isSelected,
              onTap: () {
                setState(() {
                  _formData.selectedGmailConnectionId = connection['id'];
                });
              },
            );
          }).toList(),
      ],
    );
  }

  // Step 1: Configure Gmail Trigger
  Widget _buildStep1GmailConfig() {
    return ListView(
      padding: const EdgeInsets.all(16),
      children: [
        Text(
          'Configure Gmail Trigger',
          style: TextStyle(
            fontSize: 20,
            fontWeight: FontWeight.w600,
            color: AppPalette.textPrimary,
          ),
        ),
        const SizedBox(height: 8),
        Text(
          'Set up filters to specify which emails trigger this automation',
          style: TextStyle(
            fontSize: 14,
            color: AppPalette.textSecondary,
          ),
        ),
        const SizedBox(height: 24),

        // Gmail label selector
        Text(
          'Check for new emails in',
          style: TextStyle(
            fontSize: 14,
            fontWeight: FontWeight.w600,
            color: AppPalette.textSecondary,
          ),
        ),
        const SizedBox(height: 8),
        DropdownButtonFormField<String>(
          value: _formData.gmailLabel,
          dropdownColor: AppPalette.surfaceRaised,
          style: TextStyle(color: AppPalette.textPrimary),
          decoration: InputDecoration(
            filled: true,
            fillColor: AppPalette.surface,
            border: OutlineInputBorder(
              borderRadius: BorderRadius.circular(8),
              borderSide: BorderSide(color: AppPalette.borderDefault),
            ),
            contentPadding:
                const EdgeInsets.symmetric(horizontal: 16, vertical: 12),
          ),
          items: ['INBOX', 'SENT', 'DRAFTS', 'TRASH', 'SPAM']
              .map((label) => DropdownMenuItem(
                    value: label,
                    child: Text(label),
                  ))
              .toList(),
          onChanged: (value) {
            if (value != null) {
              setState(() => _formData.gmailLabel = value);
            }
          },
        ),

        const SizedBox(height: 24),

        // Subject filter
        Text(
          'Filter by subject (optional)',
          style: TextStyle(
            fontSize: 14,
            fontWeight: FontWeight.w600,
            color: AppPalette.textSecondary,
          ),
        ),
        const SizedBox(height: 8),
        TextField(
          controller: _subjectController,
          style: TextStyle(color: AppPalette.textPrimary),
          decoration: InputDecoration(
            hintText: 'e.g., "Invoice" or "Important"',
            hintStyle: TextStyle(color: AppPalette.textSecondary),
            filled: true,
            fillColor: AppPalette.surface,
            border: OutlineInputBorder(
              borderRadius: BorderRadius.circular(8),
              borderSide: BorderSide(color: AppPalette.borderDefault),
            ),
            contentPadding:
                const EdgeInsets.symmetric(horizontal: 16, vertical: 12),
          ),
        ),

        const SizedBox(height: 24),

        // Sender filter
        Text(
          'Filter by sender (optional)',
          style: TextStyle(
            fontSize: 14,
            fontWeight: FontWeight.w600,
            color: AppPalette.textSecondary,
          ),
        ),
        const SizedBox(height: 8),
        TextField(
          controller: _senderController,
          style: TextStyle(color: AppPalette.textPrimary),
          keyboardType: TextInputType.emailAddress,
          decoration: InputDecoration(
            hintText: 'e.g., "boss@company.com"',
            hintStyle: TextStyle(color: AppPalette.textSecondary),
            filled: true,
            fillColor: AppPalette.surface,
            border: OutlineInputBorder(
              borderRadius: BorderRadius.circular(8),
              borderSide: BorderSide(color: AppPalette.borderDefault),
            ),
            contentPadding:
                const EdgeInsets.symmetric(horizontal: 16, vertical: 12),
          ),
        ),

        const SizedBox(height: 24),

        // Available variables info
        Container(
          padding: const EdgeInsets.all(16),
          decoration: BoxDecoration(
            color: AppPalette.accentBlue.withOpacity(0.1),
            borderRadius: BorderRadius.circular(8),
            border: Border.all(color: AppPalette.accentBlue.withOpacity(0.3)),
          ),
          child: Column(
            crossAxisAlignment: CrossAxisAlignment.start,
            children: [
              Row(
                children: [
                  Icon(
                    Icons.info_outline,
                    size: 20,
                    color: AppPalette.accentBlue,
                  ),
                  const SizedBox(width: 8),
                  Text(
                    'Available Variables',
                    style: TextStyle(
                      fontSize: 14,
                      fontWeight: FontWeight.w600,
                      color: AppPalette.accentBlue,
                    ),
                  ),
                ],
              ),
              const SizedBox(height: 12),
              Text(
                'You can use these variables in your Discord message:',
                style: TextStyle(
                  fontSize: 13,
                  color: AppPalette.textSecondary,
                ),
              ),
              const SizedBox(height: 8),
              Wrap(
                spacing: 8,
                runSpacing: 8,
                children: [
                  _buildVariableChip('{{sender}}'),
                  _buildVariableChip('{{subject}}'),
                  _buildVariableChip('{{body}}'),
                  _buildVariableChip('{{unreadCount}}'),
                  _buildVariableChip('{{receivedAt}}'),
                ],
              ),
            ],
          ),
        ),
      ],
    );
  }

  Widget _buildVariableChip(String variable) {
    return Chip(
      label: Text(
        variable,
        style: TextStyle(
          fontSize: 12,
          fontFamily: 'monospace',
          color: AppPalette.accentBlue,
        ),
      ),
      backgroundColor: AppPalette.accentBlue.withOpacity(0.2),
      side: BorderSide(color: AppPalette.accentBlue.withOpacity(0.5)),
    );
  }

  // Step 2: Select Discord Connection
  Widget _buildStep2DiscordSelection() {
    return ListView(
      padding: const EdgeInsets.all(16),
      children: [
        Text(
          'Select Discord Connection',
          style: TextStyle(
            fontSize: 20,
            fontWeight: FontWeight.w600,
            color: AppPalette.textPrimary,
          ),
        ),
        const SizedBox(height: 8),
        Text(
          'Choose which Discord connection to use for sending messages',
          style: TextStyle(
            fontSize: 14,
            color: AppPalette.textSecondary,
          ),
        ),
        const SizedBox(height: 24),
        if (_discordConnections.isEmpty)
          Card(
            child: Padding(
              padding: const EdgeInsets.all(16),
              child: Column(
                children: [
                  Icon(
                    Icons.chat_bubble_outline,
                    size: 48,
                    color: AppPalette.textSecondary,
                  ),
                  const SizedBox(height: 16),
                  Text(
                    'No Discord connections',
                    style: TextStyle(
                      fontSize: 16,
                      fontWeight: FontWeight.w600,
                      color: AppPalette.textPrimary,
                    ),
                  ),
                  const SizedBox(height: 8),
                  Text(
                    'Connect Discord in the Services tab first',
                    textAlign: TextAlign.center,
                    style: TextStyle(
                      fontSize: 14,
                      color: AppPalette.textSecondary,
                    ),
                  ),
                ],
              ),
            ),
          )
        else
          ..._discordConnections.map((connection) {
            final isSelected =
                _formData.selectedDiscordConnectionId == connection['id'];
            return ConnectionSelectorCard(
              service: 'Discord',
              email: connection['connectedAccount'] ?? 'Discord Bot',
              isActive: connection['status'] == 'active',
              isSelected: isSelected,
              onTap: () {
                setState(() {
                  _formData.selectedDiscordConnectionId = connection['id'];
                  // Pre-fill webhook URL if available
                  if (connection['webhookUrl'] != null) {
                    _webhookController.text = connection['webhookUrl'];
                  }
                });
              },
            );
          }).toList(),
      ],
    );
  }

  // Step 3: Configure Discord Message
  Widget _buildStep3DiscordConfig() {
    return ListView(
      padding: const EdgeInsets.all(16),
      children: [
        Text(
          'Configure Discord Message',
          style: TextStyle(
            fontSize: 20,
            fontWeight: FontWeight.w600,
            color: AppPalette.textPrimary,
          ),
        ),
        const SizedBox(height: 8),
        Text(
          'Set up the message that will be sent to Discord',
          style: TextStyle(
            fontSize: 14,
            color: AppPalette.textSecondary,
          ),
        ),
        const SizedBox(height: 24),

        // Webhook URL
        Text(
          'Discord Webhook URL *',
          style: TextStyle(
            fontSize: 14,
            fontWeight: FontWeight.w600,
            color: AppPalette.textSecondary,
          ),
        ),
        const SizedBox(height: 8),
        TextField(
          controller: _webhookController,
          style: TextStyle(color: AppPalette.textPrimary, fontSize: 13),
          maxLines: 2,
          decoration: InputDecoration(
            hintText: 'https://discord.com/api/webhooks/...',
            hintStyle: TextStyle(color: AppPalette.textSecondary),
            filled: true,
            fillColor: AppPalette.surface,
            border: OutlineInputBorder(
              borderRadius: BorderRadius.circular(8),
              borderSide: BorderSide(color: AppPalette.borderDefault),
            ),
            contentPadding:
                const EdgeInsets.symmetric(horizontal: 16, vertical: 12),
          ),
        ),

        const SizedBox(height: 24),

        // Channel name
        Text(
          'Channel Name (optional)',
          style: TextStyle(
            fontSize: 14,
            fontWeight: FontWeight.w600,
            color: AppPalette.textSecondary,
          ),
        ),
        const SizedBox(height: 8),
        TextField(
          controller: _channelController,
          style: TextStyle(color: AppPalette.textPrimary),
          decoration: InputDecoration(
            hintText: 'e.g., "general" or "notifications"',
            hintStyle: TextStyle(color: AppPalette.textSecondary),
            filled: true,
            fillColor: AppPalette.surface,
            border: OutlineInputBorder(
              borderRadius: BorderRadius.circular(8),
              borderSide: BorderSide(color: AppPalette.borderDefault),
            ),
            contentPadding:
                const EdgeInsets.symmetric(horizontal: 16, vertical: 12),
          ),
        ),

        const SizedBox(height: 24),

        // Message template
        Text(
          'Message Template',
          style: TextStyle(
            fontSize: 14,
            fontWeight: FontWeight.w600,
            color: AppPalette.textSecondary,
          ),
        ),
        const SizedBox(height: 8),
        TextField(
          controller: _messageController,
          style: TextStyle(color: AppPalette.textPrimary),
          maxLines: 6,
          decoration: InputDecoration(
            hintText:
                'New email from {{sender}}!\nSubject: {{subject}}\n\n{{body}}',
            hintStyle: TextStyle(color: AppPalette.textSecondary),
            filled: true,
            fillColor: AppPalette.surface,
            border: OutlineInputBorder(
              borderRadius: BorderRadius.circular(8),
              borderSide: BorderSide(color: AppPalette.borderDefault),
            ),
            contentPadding: const EdgeInsets.all(16),
          ),
        ),

        const SizedBox(height: 16),

        // Variable insertion panel
        VariableInsertionPanel(
          controller: _messageController,
          variables: const [
            'sender',
            'subject',
            'body',
            'unreadCount',
            'receivedAt'
          ],
        ),
      ],
    );
  }

  // Step 4: Review
  Widget _buildStep4Review() {
    final gmailConnection = _gmailConnections.firstWhere(
      (c) => c['id'] == _formData.selectedGmailConnectionId,
      orElse: () => <String, dynamic>{},
    );
    final discordConnection = _discordConnections.firstWhere(
      (c) => c['id'] == _formData.selectedDiscordConnectionId,
      orElse: () => <String, dynamic>{},
    );

    return ListView(
      padding: const EdgeInsets.all(16),
      children: [
        Text(
          'Review & Save',
          style: TextStyle(
            fontSize: 20,
            fontWeight: FontWeight.w600,
            color: AppPalette.textPrimary,
          ),
        ),
        const SizedBox(height: 8),
        Text(
          'Review your automation configuration before saving',
          style: TextStyle(
            fontSize: 14,
            color: AppPalette.textSecondary,
          ),
        ),
        const SizedBox(height: 24),

        // Gmail trigger summary
        _buildSummaryCard(
          title: 'TRIGGER: Gmail',
          icon: Icons.mail_outline,
          iconColor: AppPalette.accentGreen,
          onEdit: () => _jumpToStep(0),
          children: [
            _buildSummaryRow('Account', gmailConnection['connectedAccount'] ?? 'Unknown'),
            _buildSummaryRow('Label', _formData.gmailLabel),
            if (_formData.gmailSubjectFilter?.isNotEmpty == true)
              _buildSummaryRow('Subject contains', _formData.gmailSubjectFilter!),
            if (_formData.gmailFromFilter?.isNotEmpty == true)
              _buildSummaryRow('From address', _formData.gmailFromFilter!),
          ],
        ),

        const SizedBox(height: 16),

        // Discord action summary
        _buildSummaryCard(
          title: 'ACTION: Discord',
          icon: Icons.chat_bubble_outline,
          iconColor: AppPalette.accentPurple,
          onEdit: () => _jumpToStep(2),
          children: [
            _buildSummaryRow('Connection', discordConnection['connectedAccount'] ?? 'Unknown'),
            _buildSummaryRow('Channel', _formData.discordChannelName ?? 'general'),
            _buildSummaryRow(
              'Webhook',
              _formData.discordWebhookUrl?.substring(0, 40) ?? '' '...',
            ),
            if (_formData.discordMessageTemplate?.isNotEmpty == true)
              Padding(
                padding: const EdgeInsets.only(top: 8),
                child: Column(
                  crossAxisAlignment: CrossAxisAlignment.start,
                  children: [
                    Text(
                      'Message Template:',
                      style: TextStyle(
                        fontSize: 13,
                        fontWeight: FontWeight.w600,
                        color: AppPalette.textSecondary,
                      ),
                    ),
                    const SizedBox(height: 4),
                    Container(
                      padding: const EdgeInsets.all(12),
                      decoration: BoxDecoration(
                        color: AppPalette.background,
                        borderRadius: BorderRadius.circular(6),
                        border: Border.all(color: AppPalette.borderDefault),
                      ),
                      child: Text(
                        _formData.discordMessageTemplate!,
                        style: TextStyle(
                          fontSize: 13,
                          fontFamily: 'monospace',
                          color: AppPalette.textSecondary,
                        ),
                      ),
                    ),
                  ],
                ),
              ),
          ],
        ),
      ],
    );
  }

  Widget _buildSummaryCard({
    required String title,
    required IconData icon,
    required Color iconColor,
    required VoidCallback onEdit,
    required List<Widget> children,
  }) {
    return Card(
      child: Padding(
        padding: const EdgeInsets.all(16),
        child: Column(
          crossAxisAlignment: CrossAxisAlignment.start,
          children: [
            Row(
              children: [
                Icon(icon, color: iconColor, size: 24),
                const SizedBox(width: 12),
                Expanded(
                  child: Text(
                    title,
                    style: TextStyle(
                      fontSize: 16,
                      fontWeight: FontWeight.w600,
                      color: AppPalette.textPrimary,
                    ),
                  ),
                ),
                TextButton(
                  onPressed: onEdit,
                  child: Text(
                    'Edit',
                    style: TextStyle(color: AppPalette.accentBlue),
                  ),
                ),
              ],
            ),
            const SizedBox(height: 16),
            ...children,
          ],
        ),
      ),
    );
  }

  Widget _buildSummaryRow(String label, String value) {
    return Padding(
      padding: const EdgeInsets.only(bottom: 8),
      child: Row(
        crossAxisAlignment: CrossAxisAlignment.start,
        children: [
          SizedBox(
            width: 120,
            child: Text(
              label,
              style: TextStyle(
                fontSize: 13,
                fontWeight: FontWeight.w600,
                color: AppPalette.textSecondary,
              ),
            ),
          ),
          Expanded(
            child: Text(
              value,
              style: TextStyle(
                fontSize: 13,
                color: AppPalette.textPrimary,
              ),
            ),
          ),
        ],
      ),
    );
  }
}
