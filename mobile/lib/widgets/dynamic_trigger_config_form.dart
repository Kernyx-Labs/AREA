import 'package:flutter/material.dart';
import '../constants/palette.dart';
import '../models/area_form_data.dart';
import '../models/service_metadata.dart';

/// Dynamic form for configuring trigger settings based on service and trigger type.
///
/// This widget renders different form fields depending on the selected
/// trigger service and type (e.g., Gmail filters, Timer schedule, etc.).
class DynamicTriggerConfigForm extends StatefulWidget {
  final String serviceType;
  final String triggerType;
  final AreaFormData formData;
  final Function(String key, dynamic value) onConfigChanged;

  const DynamicTriggerConfigForm({
    super.key,
    required this.serviceType,
    required this.triggerType,
    required this.formData,
    required this.onConfigChanged,
  });

  @override
  State<DynamicTriggerConfigForm> createState() =>
      _DynamicTriggerConfigFormState();
}

class _DynamicTriggerConfigFormState extends State<DynamicTriggerConfigForm> {
  // Text controllers for form fields
  final Map<String, TextEditingController> _controllers = {};

  @override
  void initState() {
    super.initState();
    _initializeControllers();
  }

  @override
  void dispose() {
    // Dispose all controllers
    for (var controller in _controllers.values) {
      controller.dispose();
    }
    super.dispose();
  }

  void _initializeControllers() {
    // Initialize controllers based on existing form data
    final config = widget.formData.triggerConfig;

    if (widget.serviceType == 'gmail') {
      _controllers['subjectFilter'] =
          TextEditingController(text: config['subjectFilter'] ?? '');
      _controllers['fromFilter'] =
          TextEditingController(text: config['fromFilter'] ?? '');
    } else if (widget.serviceType == 'timer') {
      if (widget.triggerType == 'scheduled') {
        _controllers['scheduledTime'] =
            TextEditingController(text: config['scheduledTime'] ?? '');
      } else if (widget.triggerType == 'interval') {
        _controllers['intervalMinutes'] =
            TextEditingController(text: config['intervalMinutes']?.toString() ?? '');
      }
    } else if (widget.serviceType == 'github') {
      _controllers['repository'] =
          TextEditingController(text: config['repository'] ?? '');
      _controllers['labels'] =
          TextEditingController(text: config['labels'] ?? '');
    }
  }

  @override
  Widget build(BuildContext context) {
    final serviceName = ServiceMetadata.getServiceName(widget.serviceType);
    final serviceColor = ServiceMetadata.getServiceColor(widget.serviceType);
    final triggerTypeName = ServiceMetadata.getTriggerTypeName(
      widget.serviceType,
      widget.triggerType,
    );

    return ListView(
      padding: const EdgeInsets.all(16),
      children: [
        // Header
        Row(
          children: [
            Container(
              width: 48,
              height: 48,
              decoration: BoxDecoration(
                color: serviceColor.withOpacity(0.1),
                borderRadius: BorderRadius.circular(12),
              ),
              child: Icon(
                ServiceMetadata.getServiceIcon(widget.serviceType),
                color: serviceColor,
                size: 28,
              ),
            ),
            const SizedBox(width: 16),
            Expanded(
              child: Column(
                crossAxisAlignment: CrossAxisAlignment.start,
                children: [
                  Text(
                    'Configure Trigger',
                    style: TextStyle(
                      fontSize: 20,
                      fontWeight: FontWeight.w600,
                      color: AppPalette.textPrimary,
                    ),
                  ),
                  const SizedBox(height: 4),
                  Text(
                    '$serviceName: $triggerTypeName',
                    style: TextStyle(
                      fontSize: 14,
                      color: AppPalette.textSecondary,
                    ),
                  ),
                ],
              ),
            ),
          ],
        ),
        const SizedBox(height: 24),

        // Dynamic form fields based on service and trigger type
        ..._buildFormFields(),

        const SizedBox(height: 24),

        // Available variables info
        _buildVariablesInfo(),
      ],
    );
  }

  List<Widget> _buildFormFields() {
    if (widget.serviceType == 'gmail') {
      return _buildGmailFields();
    } else if (widget.serviceType == 'timer') {
      return _buildTimerFields();
    } else if (widget.serviceType == 'github') {
      return _buildGitHubFields();
    }

    return [
      Text(
        'No configuration needed for this trigger',
        style: TextStyle(
          fontSize: 14,
          color: AppPalette.textSecondary,
        ),
      ),
    ];
  }

  List<Widget> _buildGmailFields() {
    return [
      // Gmail label selector
      _buildLabel('Check for new emails in'),
      const SizedBox(height: 8),
      DropdownButtonFormField<String>(
        value: widget.formData.triggerConfig['label'] ?? 'INBOX',
        dropdownColor: AppPalette.surfaceRaised,
        style: TextStyle(color: AppPalette.textPrimary),
        decoration: _buildInputDecoration('Select label'),
        items: ['INBOX', 'SENT', 'DRAFTS', 'TRASH', 'SPAM']
            .map((label) => DropdownMenuItem(
                  value: label,
                  child: Text(label),
                ))
            .toList(),
        onChanged: (value) {
          if (value != null) {
            widget.onConfigChanged('label', value);
          }
        },
      ),
      const SizedBox(height: 24),

      // Subject filter
      _buildLabel('Filter by subject (optional)'),
      const SizedBox(height: 8),
      TextField(
        controller: _controllers['subjectFilter'],
        style: TextStyle(color: AppPalette.textPrimary),
        decoration: _buildInputDecoration('e.g., "Invoice" or "Important"'),
        onChanged: (value) => widget.onConfigChanged('subjectFilter', value),
      ),
      const SizedBox(height: 24),

      // Sender filter
      _buildLabel('Filter by sender (optional)'),
      const SizedBox(height: 8),
      TextField(
        controller: _controllers['fromFilter'],
        style: TextStyle(color: AppPalette.textPrimary),
        keyboardType: TextInputType.emailAddress,
        decoration: _buildInputDecoration('e.g., "boss@company.com"'),
        onChanged: (value) => widget.onConfigChanged('fromFilter', value),
      ),
    ];
  }

  List<Widget> _buildTimerFields() {
    if (widget.triggerType == 'scheduled') {
      return [
        _buildLabel('Scheduled time'),
        const SizedBox(height: 8),
        TextField(
          controller: _controllers['scheduledTime'],
          style: TextStyle(color: AppPalette.textPrimary),
          decoration: _buildInputDecoration('HH:MM (e.g., 09:00)'),
          onChanged: (value) => widget.onConfigChanged('scheduledTime', value),
        ),
        const SizedBox(height: 24),
        _buildLabel('Timezone'),
        const SizedBox(height: 8),
        DropdownButtonFormField<String>(
          value: widget.formData.triggerConfig['timezone'] ?? 'UTC',
          dropdownColor: AppPalette.surfaceRaised,
          style: TextStyle(color: AppPalette.textPrimary),
          decoration: _buildInputDecoration('Select timezone'),
          items: ['UTC', 'America/New_York', 'Europe/London', 'Asia/Tokyo']
              .map((tz) => DropdownMenuItem(
                    value: tz,
                    child: Text(tz),
                  ))
              .toList(),
          onChanged: (value) {
            if (value != null) {
              widget.onConfigChanged('timezone', value);
            }
          },
        ),
      ];
    } else if (widget.triggerType == 'interval') {
      return [
        _buildLabel('Interval (minutes)'),
        const SizedBox(height: 8),
        TextField(
          controller: _controllers['intervalMinutes'],
          style: TextStyle(color: AppPalette.textPrimary),
          keyboardType: TextInputType.number,
          decoration: _buildInputDecoration('e.g., 5, 15, 30, 60'),
          onChanged: (value) {
            final minutes = int.tryParse(value);
            if (minutes != null) {
              widget.onConfigChanged('intervalMinutes', minutes);
            }
          },
        ),
      ];
    }

    return [];
  }

  List<Widget> _buildGitHubFields() {
    return [
      _buildLabel('Repository'),
      const SizedBox(height: 8),
      TextField(
        controller: _controllers['repository'],
        style: TextStyle(color: AppPalette.textPrimary),
        decoration: _buildInputDecoration('e.g., "owner/repo-name"'),
        onChanged: (value) => widget.onConfigChanged('repository', value),
      ),
      const SizedBox(height: 24),
      _buildLabel('Filter by labels (optional)'),
      const SizedBox(height: 8),
      TextField(
        controller: _controllers['labels'],
        style: TextStyle(color: AppPalette.textPrimary),
        decoration: _buildInputDecoration('e.g., "bug, enhancement"'),
        onChanged: (value) => widget.onConfigChanged('labels', value),
      ),
    ];
  }

  Widget _buildVariablesInfo() {
    final variables = ServiceMetadata.getTriggerVariables(
      widget.serviceType,
      widget.triggerType,
    );

    if (variables.isEmpty) {
      return const SizedBox.shrink();
    }

    return Container(
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
            'You can use these variables in your action configuration:',
            style: TextStyle(
              fontSize: 13,
              color: AppPalette.textSecondary,
            ),
          ),
          const SizedBox(height: 8),
          Wrap(
            spacing: 8,
            runSpacing: 8,
            children: variables
                .map((variable) => Chip(
                      label: Text(
                        '{{$variable}}',
                        style: TextStyle(
                          fontSize: 12,
                          fontFamily: 'monospace',
                          color: AppPalette.accentBlue,
                        ),
                      ),
                      backgroundColor: AppPalette.accentBlue.withOpacity(0.2),
                      side: BorderSide(
                          color: AppPalette.accentBlue.withOpacity(0.5)),
                    ))
                .toList(),
          ),
        ],
      ),
    );
  }

  Widget _buildLabel(String text) {
    return Text(
      text,
      style: TextStyle(
        fontSize: 14,
        fontWeight: FontWeight.w600,
        color: AppPalette.textSecondary,
      ),
    );
  }

  InputDecoration _buildInputDecoration(String hint) {
    return InputDecoration(
      hintText: hint,
      hintStyle: TextStyle(color: AppPalette.textSecondary),
      filled: true,
      fillColor: AppPalette.surface,
      border: OutlineInputBorder(
        borderRadius: BorderRadius.circular(8),
        borderSide: BorderSide(color: AppPalette.borderDefault),
      ),
      enabledBorder: OutlineInputBorder(
        borderRadius: BorderRadius.circular(8),
        borderSide: BorderSide(color: AppPalette.borderDefault),
      ),
      focusedBorder: OutlineInputBorder(
        borderRadius: BorderRadius.circular(8),
        borderSide: BorderSide(color: AppPalette.accentBlue, width: 2),
      ),
      contentPadding: const EdgeInsets.symmetric(horizontal: 16, vertical: 12),
    );
  }
}
