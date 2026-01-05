import 'package:flutter/material.dart';
import '../constants/palette.dart';
import '../models/area_form_data.dart';
import '../models/service_metadata.dart';
import 'variable_insertion_panel.dart';

/// Dynamic form for configuring action settings based on service and action type.
///
/// This widget renders different form fields depending on the selected
/// action service and type (e.g., Discord message, Dropbox upload, etc.).
class DynamicActionConfigForm extends StatefulWidget {
  final String serviceType;
  final String actionType;
  final AreaFormData formData;
  final List<String> availableVariables;
  final Function(String key, dynamic value) onConfigChanged;

  const DynamicActionConfigForm({
    super.key,
    required this.serviceType,
    required this.actionType,
    required this.formData,
    required this.availableVariables,
    required this.onConfigChanged,
  });

  @override
  State<DynamicActionConfigForm> createState() =>
      _DynamicActionConfigFormState();
}

class _DynamicActionConfigFormState extends State<DynamicActionConfigForm> {
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
    final config = widget.formData.actionConfig;

    if (widget.serviceType == 'discord') {
      _controllers['webhookUrl'] =
          TextEditingController(text: config['webhookUrl'] ?? '');
      _controllers['channelName'] =
          TextEditingController(text: config['channelName'] ?? '');

      if (widget.actionType == 'send_message') {
        _controllers['messageTemplate'] =
            TextEditingController(text: config['messageTemplate'] ?? '');
      } else if (widget.actionType == 'send_embed') {
        _controllers['embedTitle'] =
            TextEditingController(text: config['embedTitle'] ?? '');
        _controllers['embedDescription'] =
            TextEditingController(text: config['embedDescription'] ?? '');
      }
    } else if (widget.serviceType == 'dropbox') {
      if (widget.actionType == 'upload_file') {
        _controllers['filePath'] =
            TextEditingController(text: config['filePath'] ?? '');
        _controllers['fileName'] =
            TextEditingController(text: config['fileName'] ?? '');
      } else if (widget.actionType == 'create_folder') {
        _controllers['folderPath'] =
            TextEditingController(text: config['folderPath'] ?? '');
      }
    } else if (widget.serviceType == 'outlook' || widget.serviceType == 'gmail') {
      if (widget.actionType == 'send_email') {
        _controllers['to'] = TextEditingController(text: config['to'] ?? '');
        _controllers['subject'] =
            TextEditingController(text: config['subject'] ?? '');
        _controllers['body'] = TextEditingController(text: config['body'] ?? '');
      } else if (widget.actionType == 'create_event') {
        _controllers['eventTitle'] =
            TextEditingController(text: config['eventTitle'] ?? '');
        _controllers['eventStart'] =
            TextEditingController(text: config['eventStart'] ?? '');
        _controllers['eventEnd'] =
            TextEditingController(text: config['eventEnd'] ?? '');
      }
    }
  }

  @override
  Widget build(BuildContext context) {
    final serviceName = ServiceMetadata.getServiceName(widget.serviceType);
    final serviceColor = ServiceMetadata.getServiceColor(widget.serviceType);
    final actionTypeName = ServiceMetadata.getActionTypeName(
      widget.serviceType,
      widget.actionType,
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
                    'Configure Action',
                    style: TextStyle(
                      fontSize: 20,
                      fontWeight: FontWeight.w600,
                      color: AppPalette.textPrimary,
                    ),
                  ),
                  const SizedBox(height: 4),
                  Text(
                    '$serviceName: $actionTypeName',
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

        // Dynamic form fields based on service and action type
        ..._buildFormFields(),
      ],
    );
  }

  List<Widget> _buildFormFields() {
    if (widget.serviceType == 'discord') {
      return _buildDiscordFields();
    } else if (widget.serviceType == 'dropbox') {
      return _buildDropboxFields();
    } else if (widget.serviceType == 'outlook' || widget.serviceType == 'gmail') {
      if (widget.actionType == 'send_email') {
        return _buildEmailFields();
      } else if (widget.actionType == 'create_event') {
        return _buildCalendarEventFields();
      }
    }

    return [
      Text(
        'No configuration needed for this action',
        style: TextStyle(
          fontSize: 14,
          color: AppPalette.textSecondary,
        ),
      ),
    ];
  }

  List<Widget> _buildDiscordFields() {
    final widgets = <Widget>[
      // Webhook URL
      _buildLabel('Discord Webhook URL *'),
      const SizedBox(height: 8),
      TextField(
        controller: _controllers['webhookUrl'],
        style: TextStyle(color: AppPalette.textPrimary, fontSize: 13),
        maxLines: 2,
        decoration: _buildInputDecoration('https://discord.com/api/webhooks/...'),
        onChanged: (value) => widget.onConfigChanged('webhookUrl', value),
      ),
      const SizedBox(height: 24),

      // Channel name
      _buildLabel('Channel Name (optional)'),
      const SizedBox(height: 8),
      TextField(
        controller: _controllers['channelName'],
        style: TextStyle(color: AppPalette.textPrimary),
        decoration: _buildInputDecoration('e.g., "general" or "notifications"'),
        onChanged: (value) => widget.onConfigChanged('channelName', value),
      ),
      const SizedBox(height: 24),
    ];

    if (widget.actionType == 'send_message') {
      widgets.addAll([
        _buildLabel('Message Template'),
        const SizedBox(height: 8),
        TextField(
          controller: _controllers['messageTemplate'],
          style: TextStyle(color: AppPalette.textPrimary),
          maxLines: 6,
          decoration: _buildInputDecoration(
            'Use variables like {{sender}}, {{subject}}',
          ),
          onChanged: (value) => widget.onConfigChanged('messageTemplate', value),
        ),
        const SizedBox(height: 16),

        // Variable insertion panel
        if (widget.availableVariables.isNotEmpty)
          VariableInsertionPanel(
            controller: _controllers['messageTemplate']!,
            variables: widget.availableVariables,
          ),
      ]);
    } else if (widget.actionType == 'send_embed') {
      widgets.addAll([
        _buildLabel('Embed Title'),
        const SizedBox(height: 8),
        TextField(
          controller: _controllers['embedTitle'],
          style: TextStyle(color: AppPalette.textPrimary),
          decoration: _buildInputDecoration('Title of the embed'),
          onChanged: (value) => widget.onConfigChanged('embedTitle', value),
        ),
        const SizedBox(height: 24),
        _buildLabel('Embed Description'),
        const SizedBox(height: 8),
        TextField(
          controller: _controllers['embedDescription'],
          style: TextStyle(color: AppPalette.textPrimary),
          maxLines: 4,
          decoration: _buildInputDecoration('Description with variables'),
          onChanged: (value) => widget.onConfigChanged('embedDescription', value),
        ),
      ]);
    }

    return widgets;
  }

  List<Widget> _buildDropboxFields() {
    if (widget.actionType == 'upload_file') {
      return [
        _buildLabel('File Path'),
        const SizedBox(height: 8),
        TextField(
          controller: _controllers['filePath'],
          style: TextStyle(color: AppPalette.textPrimary),
          decoration: _buildInputDecoration('/Documents/MyFolder'),
          onChanged: (value) => widget.onConfigChanged('filePath', value),
        ),
        const SizedBox(height: 24),
        _buildLabel('File Name'),
        const SizedBox(height: 8),
        TextField(
          controller: _controllers['fileName'],
          style: TextStyle(color: AppPalette.textPrimary),
          decoration: _buildInputDecoration('document.txt'),
          onChanged: (value) => widget.onConfigChanged('fileName', value),
        ),
      ];
    } else if (widget.actionType == 'create_folder') {
      return [
        _buildLabel('Folder Path'),
        const SizedBox(height: 8),
        TextField(
          controller: _controllers['folderPath'],
          style: TextStyle(color: AppPalette.textPrimary),
          decoration: _buildInputDecoration('/Documents/NewFolder'),
          onChanged: (value) => widget.onConfigChanged('folderPath', value),
        ),
      ];
    }

    return [];
  }

  List<Widget> _buildEmailFields() {
    return [
      _buildLabel('To'),
      const SizedBox(height: 8),
      TextField(
        controller: _controllers['to'],
        style: TextStyle(color: AppPalette.textPrimary),
        keyboardType: TextInputType.emailAddress,
        decoration: _buildInputDecoration('recipient@example.com'),
        onChanged: (value) => widget.onConfigChanged('to', value),
      ),
      const SizedBox(height: 24),
      _buildLabel('Subject'),
      const SizedBox(height: 8),
      TextField(
        controller: _controllers['subject'],
        style: TextStyle(color: AppPalette.textPrimary),
        decoration: _buildInputDecoration('Email subject'),
        onChanged: (value) => widget.onConfigChanged('subject', value),
      ),
      const SizedBox(height: 24),
      _buildLabel('Body'),
      const SizedBox(height: 8),
      TextField(
        controller: _controllers['body'],
        style: TextStyle(color: AppPalette.textPrimary),
        maxLines: 6,
        decoration: _buildInputDecoration('Email body with {{variables}}'),
        onChanged: (value) => widget.onConfigChanged('body', value),
      ),
      const SizedBox(height: 16),

      // Variable insertion panel
      if (widget.availableVariables.isNotEmpty)
        VariableInsertionPanel(
          controller: _controllers['body']!,
          variables: widget.availableVariables,
        ),
    ];
  }

  List<Widget> _buildCalendarEventFields() {
    return [
      _buildLabel('Event Title'),
      const SizedBox(height: 8),
      TextField(
        controller: _controllers['eventTitle'],
        style: TextStyle(color: AppPalette.textPrimary),
        decoration: _buildInputDecoration('Meeting with team'),
        onChanged: (value) => widget.onConfigChanged('eventTitle', value),
      ),
      const SizedBox(height: 24),
      _buildLabel('Start Time'),
      const SizedBox(height: 8),
      TextField(
        controller: _controllers['eventStart'],
        style: TextStyle(color: AppPalette.textPrimary),
        decoration: _buildInputDecoration('YYYY-MM-DD HH:MM'),
        onChanged: (value) => widget.onConfigChanged('eventStart', value),
      ),
      const SizedBox(height: 24),
      _buildLabel('End Time'),
      const SizedBox(height: 8),
      TextField(
        controller: _controllers['eventEnd'],
        style: TextStyle(color: AppPalette.textPrimary),
        decoration: _buildInputDecoration('YYYY-MM-DD HH:MM'),
        onChanged: (value) => widget.onConfigChanged('eventEnd', value),
      ),
    ];
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
