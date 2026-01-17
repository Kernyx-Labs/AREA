import 'package:flutter/material.dart';
import 'package:flutter_bloc/flutter_bloc.dart';

import '../blocs/workflow/workflow.dart';
import '../constants/palette.dart';
import '../models/models.dart';
import '../models/workflow.dart';
import '../widgets/node_picker.dart';

class PipelineEditorScreen extends StatefulWidget {
  final Workflow? workflow;

  const PipelineEditorScreen({super.key, this.workflow});

  @override
  State<PipelineEditorScreen> createState() => _PipelineEditorScreenState();
}

class _PipelineEditorScreenState extends State<PipelineEditorScreen> {
  final TextEditingController _nameController = TextEditingController(text: 'New Automation');

  PipelineNode? _trigger;
  List<PipelineNode> _actions = [];
  bool _isLoading = false;
  bool _hasChanges = false;

  @override
  void initState() {
    super.initState();
    if (widget.workflow != null) {
      _loadWorkflow(widget.workflow!);
    }
    _nameController.addListener(_onNameChanged);
  }

  void _loadWorkflow(Workflow workflow) {
    _nameController.text = workflow.name;
    final workflowData = workflow.workflowData;
    if (workflowData == null) return;

    // Load trigger as action node
    if (workflowData.trigger != null) {
      final trigger = workflowData.trigger!;
      final serviceId = trigger.service.toLowerCase();
      final service = services[serviceId] ?? services['github']!;
      _trigger = PipelineNode(
        id: 1,
        type: NodeType.action,
        service: service,
        title: trigger.type,
        description: 'Trigger: ${trigger.service}',
        position: Offset.zero,
        actionType: trigger.type,
        config: trigger.config,
        connectionId: trigger.connectionId,
      );
    }

    // Load actions as reaction nodes
    _actions = [];
    for (final action in workflowData.actions) {
      final serviceId = action.service.toLowerCase();
      final service = services[serviceId] ?? services['discord']!;
      _actions.add(PipelineNode(
        id: _actions.length + 2,
        type: NodeType.reaction,
        service: service,
        title: action.type,
        description: 'Action: ${action.service}',
        position: Offset.zero,
        actionType: action.type,
        config: action.config,
        connectionId: action.connectionId,
      ));
    }

    setState(() {});
  }

  void _onNameChanged() {
    setState(() => _hasChanges = true);
  }

  @override
  void dispose() {
    _nameController.dispose();
    super.dispose();
  }

  Future<void> _showNodePicker(NodeType type) async {
    final selection = await showModalBottomSheet<NodeTemplateChoice>(
      context: context,
      isScrollControlled: true,
      backgroundColor: AppPalette.surface,
      shape: const RoundedRectangleBorder(
        borderRadius: BorderRadius.vertical(top: Radius.circular(20)),
      ),
      builder: (context) => NodePicker(initialType: type),
    );

    if (selection == null) return;

    setState(() {
      final template = selection.template;
      final nodeType = selection.type;
      final newId = DateTime.now().millisecondsSinceEpoch;
      final newNode = PipelineNode(
        id: newId,
        type: nodeType,
        service: services[template.serviceId]!,
        title: template.name,
        description: template.description,
        position: Offset.zero,
        actionType: template.name.toLowerCase().replaceAll(' ', '_'),
      );

      if (nodeType == NodeType.action) {
        _trigger = newNode;
      } else {
        _actions.add(newNode);
      }
      _hasChanges = true;
    });
  }

  void _removeTrigger() {
    setState(() {
      _trigger = null;
      _actions = [];
      _hasChanges = true;
    });
  }

  void _removeAction(int index) {
    setState(() {
      _actions.removeAt(index);
      _hasChanges = true;
    });
  }

  void _showNodeConfig(PipelineNode node, {bool isTrigger = false}) {
    showModalBottomSheet(
      context: context,
      isScrollControlled: true,
      backgroundColor: AppPalette.surface,
      shape: const RoundedRectangleBorder(
        borderRadius: BorderRadius.vertical(top: Radius.circular(20)),
      ),
      builder: (context) => _NodeConfigSheet(
        node: node,
        onSave: (config) {
          setState(() {
            if (isTrigger) {
              _trigger = node.copyWith(config: config);
            } else {
              final idx = _actions.indexWhere((n) => n.id == node.id);
              if (idx != -1) {
                _actions[idx] = node.copyWith(config: config);
              }
            }
            _hasChanges = true;
          });
          Navigator.pop(context);
        },
      ),
    );
  }

  Future<void> _saveWorkflow() async {
    if (_nameController.text.trim().isEmpty) {
      ScaffoldMessenger.of(context).showSnackBar(
        const SnackBar(content: Text('Please enter a workflow name')),
      );
      return;
    }

    if (_trigger == null) {
      ScaffoldMessenger.of(context).showSnackBar(
        const SnackBar(content: Text('Please add a trigger')),
      );
      return;
    }

    if (_actions.isEmpty) {
      ScaffoldMessenger.of(context).showSnackBar(
        const SnackBar(content: Text('Please add at least one action')),
      );
      return;
    }

    setState(() => _isLoading = true);

    final actions = _actions.map((node) => WorkflowAction(
          service: node.service.id.toUpperCase(),
          type: node.actionType ?? node.title.toLowerCase().replaceAll(' ', '_'),
          config: node.config,
          connectionId: node.connectionId,
        )).toList();

    final request = CreateWorkflowRequest(
      name: _nameController.text.trim(),
      trigger: WorkflowTrigger(
        service: _trigger!.service.id.toUpperCase(),
        type: _trigger!.actionType ?? _trigger!.title.toLowerCase().replaceAll(' ', '_'),
        config: _trigger!.config,
        connectionId: _trigger!.connectionId,
      ),
      actions: actions,
    );

    if (widget.workflow != null) {
      context.read<WorkflowBloc>().add(WorkflowUpdateRequested(
            id: widget.workflow!.id!,
            updates: {
              'name': request.name,
              'workflowData': {
                'trigger': request.trigger?.toJson(),
                'actions': request.actions.map((a) => a.toJson()).toList(),
              },
            },
          ));
    } else {
      context.read<WorkflowBloc>().add(WorkflowCreateRequested(request: request));
    }
  }

  @override
  Widget build(BuildContext context) {
    return BlocListener<WorkflowBloc, WorkflowState>(
      listener: (context, state) {
        if (state.status == WorkflowStatus.created ||
            state.status == WorkflowStatus.updated) {
          setState(() {
            _isLoading = false;
            _hasChanges = false;
          });
          ScaffoldMessenger.of(context).showSnackBar(
            SnackBar(
              content: Text(
                state.status == WorkflowStatus.created
                    ? 'Workflow created successfully'
                    : 'Workflow updated successfully',
              ),
              backgroundColor: AppPalette.success,
            ),
          );
        } else if (state.status == WorkflowStatus.error) {
          setState(() => _isLoading = false);
          ScaffoldMessenger.of(context).showSnackBar(
            SnackBar(
              content: Text(state.errorMessage ?? 'Failed to save workflow'),
              backgroundColor: AppPalette.error,
            ),
          );
        }
      },
      child: Column(
        crossAxisAlignment: CrossAxisAlignment.stretch,
        children: [
          // Header
          Container(
            padding: const EdgeInsets.all(16),
            decoration: BoxDecoration(
              color: AppPalette.surface,
              border: Border(bottom: BorderSide(color: AppPalette.border)),
            ),
            child: Row(
              children: [
                Expanded(
                  child: TextField(
                    controller: _nameController,
                    style: const TextStyle(
                      fontSize: 18,
                      fontWeight: FontWeight.w600,
                    ),
                    decoration: InputDecoration(
                      hintText: 'Enter workflow name...',
                      hintStyle: TextStyle(color: AppPalette.surfaceText),
                      border: InputBorder.none,
                      isDense: true,
                      contentPadding: EdgeInsets.zero,
                    ),
                  ),
                ),
                const SizedBox(width: 8),
                if (_isLoading)
                  const SizedBox(
                    width: 24,
                    height: 24,
                    child: CircularProgressIndicator(strokeWidth: 2),
                  )
                else
                  FilledButton.icon(
                    onPressed: _hasChanges ? _saveWorkflow : null,
                    style: FilledButton.styleFrom(
                      backgroundColor: AppPalette.accent,
                      foregroundColor: Colors.white,
                      disabledBackgroundColor: AppPalette.border,
                      shape: RoundedRectangleBorder(
                        borderRadius: BorderRadius.circular(12),
                      ),
                    ),
                    icon: const Icon(Icons.save, size: 18),
                    label: const Text('Save'),
                  ),
              ],
            ),
          ),

          // Main content
          Expanded(
            child: Container(
              color: AppPalette.canvas,
              child: SingleChildScrollView(
                padding: const EdgeInsets.all(20),
                child: Column(
                  crossAxisAlignment: CrossAxisAlignment.stretch,
                  children: [
                    // Trigger Zone
                    _buildZoneHeader('Trigger', 'When this happens...'),
                    const SizedBox(height: 12),
                    _buildDropZone(
                      child: _trigger != null
                          ? _WorkflowBlockWidget(
                              node: _trigger!,
                              onTap: () => _showNodeConfig(_trigger!, isTrigger: true),
                              onRemove: _removeTrigger,
                            )
                          : _buildEmptyState(
                              icon: Icons.flash_on,
                              text: 'Add a trigger to start',
                              hint: 'Tap the button below',
                              onAdd: () => _showNodePicker(NodeType.action),
                            ),
                    ),

                    // Arrow connector
                    if (_trigger != null) ...[
                      const SizedBox(height: 20),
                      Center(
                        child: Icon(
                          Icons.arrow_downward,
                          color: AppPalette.accent,
                          size: 32,
                        ),
                      ),
                      const SizedBox(height: 20),
                    ],

                    // Actions Zone
                    if (_trigger != null) ...[
                      _buildZoneHeader('Actions', 'Do this...'),
                      const SizedBox(height: 12),
                      if (_actions.isEmpty)
                        _buildDropZone(
                          child: _buildEmptyState(
                            icon: Icons.layers,
                            text: 'Add an action',
                            hint: 'Actions run when trigger fires',
                            onAdd: () => _showNodePicker(NodeType.reaction),
                          ),
                        )
                      else
                        ..._actions.asMap().entries.map((entry) {
                          final index = entry.key;
                          final action = entry.value;
                          return Padding(
                            padding: const EdgeInsets.only(bottom: 16),
                            child: _buildDropZone(
                              child: _WorkflowBlockWidget(
                                node: action,
                                onTap: () => _showNodeConfig(action),
                                onRemove: () => _removeAction(index),
                              ),
                            ),
                          );
                        }).toList(),
                      const SizedBox(height: 8),
                      if (_actions.isNotEmpty && _actions.length < 3)
                        OutlinedButton.icon(
                          onPressed: () => _showNodePicker(NodeType.reaction),
                          style: OutlinedButton.styleFrom(
                            foregroundColor: AppPalette.accent,
                            side: BorderSide(color: AppPalette.accent),
                            padding: const EdgeInsets.symmetric(vertical: 14),
                            shape: RoundedRectangleBorder(
                              borderRadius: BorderRadius.circular(12),
                            ),
                          ),
                          icon: const Icon(Icons.add),
                          label: const Text('Add Another Action'),
                        ),
                    ],
                  ],
                ),
              ),
            ),
          ),
        ],
      ),
    );
  }

  Widget _buildZoneHeader(String title, String subtitle) {
    return Column(
      crossAxisAlignment: CrossAxisAlignment.start,
      children: [
        Text(
          title,
          style: const TextStyle(
            fontSize: 20,
            fontWeight: FontWeight.w600,
            color: AppPalette.dark,
          ),
        ),
        const SizedBox(height: 4),
        Text(
          subtitle,
          style: TextStyle(
            fontSize: 14,
            color: AppPalette.surfaceText,
          ),
        ),
      ],
    );
  }

  Widget _buildDropZone({required Widget child}) {
    return Container(
      decoration: BoxDecoration(
        color: AppPalette.surface,
        borderRadius: BorderRadius.circular(16),
        border: Border.all(color: AppPalette.border, width: 2),
      ),
      child: child,
    );
  }

  Widget _buildEmptyState({
    required IconData icon,
    required String text,
    required String hint,
    required VoidCallback onAdd,
  }) {
    return Padding(
      padding: const EdgeInsets.all(32),
      child: Column(
        children: [
          Icon(icon, size: 48, color: AppPalette.surfaceText.withOpacity(0.5)),
          const SizedBox(height: 16),
          Text(
            text,
            style: const TextStyle(
              fontSize: 16,
              fontWeight: FontWeight.w600,
              color: AppPalette.dark,
            ),
          ),
          const SizedBox(height: 4),
          Text(
            hint,
            style: TextStyle(
              fontSize: 13,
              color: AppPalette.surfaceText,
            ),
          ),
          const SizedBox(height: 16),
          FilledButton.icon(
            onPressed: onAdd,
            style: FilledButton.styleFrom(
              backgroundColor: AppPalette.accent,
              foregroundColor: Colors.white,
            ),
            icon: const Icon(Icons.add, size: 18),
            label: const Text('Add'),
          ),
        ],
      ),
    );
  }
}

class _WorkflowBlockWidget extends StatelessWidget {
  final PipelineNode node;
  final VoidCallback onTap;
  final VoidCallback onRemove;

  const _WorkflowBlockWidget({
    required this.node,
    required this.onTap,
    required this.onRemove,
  });

  @override
  Widget build(BuildContext context) {
    return InkWell(
      onTap: onTap,
      borderRadius: BorderRadius.circular(16),
      child: Padding(
        padding: const EdgeInsets.all(16),
        child: Row(
          children: [
            Container(
              width: 48,
              height: 48,
              decoration: BoxDecoration(
                color: node.service.color,
                borderRadius: BorderRadius.circular(12),
              ),
              child: Icon(
                node.service.icon,
                color: Colors.white,
                size: 24,
              ),
            ),
            const SizedBox(width: 16),
            Expanded(
              child: Column(
                crossAxisAlignment: CrossAxisAlignment.start,
                children: [
                  Text(
                    node.service.name.toUpperCase(),
                    style: TextStyle(
                      fontSize: 11,
                      fontWeight: FontWeight.w600,
                      color: AppPalette.surfaceText,
                      letterSpacing: 0.5,
                    ),
                  ),
                  const SizedBox(height: 4),
                  Text(
                    node.title,
                    style: const TextStyle(
                      fontSize: 16,
                      fontWeight: FontWeight.w600,
                      color: AppPalette.dark,
                    ),
                  ),
                  if (node.description.isNotEmpty) ...[
                    const SizedBox(height: 2),
                    Text(
                      node.description,
                      style: TextStyle(
                        fontSize: 13,
                        color: AppPalette.surfaceText,
                      ),
                      maxLines: 1,
                      overflow: TextOverflow.ellipsis,
                    ),
                  ],
                ],
              ),
            ),
            IconButton(
              onPressed: onRemove,
              icon: const Icon(Icons.close),
              color: AppPalette.surfaceText,
              iconSize: 20,
            ),
          ],
        ),
      ),
    );
  }
}

class _NodeConfigSheet extends StatefulWidget {
  final PipelineNode node;
  final void Function(Map<String, dynamic> config) onSave;

  const _NodeConfigSheet({
    required this.node,
    required this.onSave,
  });

  @override
  State<_NodeConfigSheet> createState() => _NodeConfigSheetState();
}

class _NodeConfigSheetState extends State<_NodeConfigSheet> {
  late Map<String, TextEditingController> _controllers;

  @override
  void initState() {
    super.initState();
    _controllers = {};
    for (final entry in widget.node.config.entries) {
      _controllers[entry.key] = TextEditingController(text: entry.value.toString());
    }
    _addDefaultFields();
  }

  void _addDefaultFields() {
    final serviceId = widget.node.service.id.toLowerCase();
    final isAction = widget.node.type == NodeType.action;

    if (serviceId == 'gmail') {
      if (isAction) {
        _controllers.putIfAbsent('from', () => TextEditingController());
        _controllers.putIfAbsent('subject', () => TextEditingController());
      } else {
        _controllers.putIfAbsent('to', () => TextEditingController());
        _controllers.putIfAbsent('subject', () => TextEditingController());
        _controllers.putIfAbsent('body', () => TextEditingController());
      }
    } else if (serviceId == 'discord') {
      _controllers.putIfAbsent('message', () => TextEditingController());
      _controllers.putIfAbsent('channel', () => TextEditingController());
    } else if (serviceId == 'github') {
      if (isAction) {
        _controllers.putIfAbsent('repository', () => TextEditingController());
      } else {
        _controllers.putIfAbsent('repository', () => TextEditingController());
        _controllers.putIfAbsent('title', () => TextEditingController());
        _controllers.putIfAbsent('body', () => TextEditingController());
      }
    } else if (serviceId == 'timer') {
      _controllers.putIfAbsent('interval', () => TextEditingController());
      _controllers.putIfAbsent('unit', () => TextEditingController(text: 'minutes'));
    }
  }

  @override
  void dispose() {
    for (final controller in _controllers.values) {
      controller.dispose();
    }
    super.dispose();
  }

  @override
  Widget build(BuildContext context) {
    return DraggableScrollableSheet(
      initialChildSize: 0.6,
      minChildSize: 0.4,
      maxChildSize: 0.9,
      expand: false,
      builder: (context, scrollController) {
        return Padding(
          padding: EdgeInsets.only(
            bottom: MediaQuery.of(context).viewInsets.bottom,
          ),
          child: Column(
            children: [
              Container(
                padding: const EdgeInsets.all(16),
                decoration: BoxDecoration(
                  color: widget.node.service.color,
                  borderRadius: const BorderRadius.vertical(top: Radius.circular(20)),
                ),
                child: Row(
                  children: [
                    Icon(
                      widget.node.service.icon,
                      color: Colors.white,
                      size: 24,
                    ),
                    const SizedBox(width: 12),
                    Expanded(
                      child: Column(
                        crossAxisAlignment: CrossAxisAlignment.start,
                        children: [
                          Text(
                            widget.node.title,
                            style: const TextStyle(
                              fontSize: 18,
                              fontWeight: FontWeight.w600,
                              color: Colors.white,
                            ),
                          ),
                          Text(
                            widget.node.service.name,
                            style: const TextStyle(
                              fontSize: 13,
                              color: Colors.white70,
                            ),
                          ),
                        ],
                      ),
                    ),
                    FilledButton(
                      onPressed: () {
                        final config = <String, dynamic>{};
                        for (final entry in _controllers.entries) {
                          if (entry.value.text.isNotEmpty) {
                            config[entry.key] = entry.value.text;
                          }
                        }
                        widget.onSave(config);
                      },
                      style: FilledButton.styleFrom(
                        backgroundColor: Colors.white,
                        foregroundColor: widget.node.service.color,
                      ),
                      child: const Text('Save'),
                    ),
                  ],
                ),
              ),
              Expanded(
                child: ListView(
                  controller: scrollController,
                  padding: const EdgeInsets.all(16),
                  children: _controllers.entries.map((entry) {
                    return Padding(
                      padding: const EdgeInsets.only(bottom: 16),
                      child: TextField(
                        controller: entry.value,
                        decoration: InputDecoration(
                          labelText: _formatLabel(entry.key),
                          hintText: _getHint(entry.key),
                          filled: true,
                          fillColor: AppPalette.surfaceElevated,
                          border: OutlineInputBorder(
                            borderRadius: BorderRadius.circular(12),
                            borderSide: BorderSide(color: AppPalette.border),
                          ),
                          enabledBorder: OutlineInputBorder(
                            borderRadius: BorderRadius.circular(12),
                            borderSide: BorderSide(color: AppPalette.border),
                          ),
                          focusedBorder: OutlineInputBorder(
                            borderRadius: BorderRadius.circular(12),
                            borderSide: BorderSide(color: widget.node.service.color, width: 2),
                          ),
                        ),
                        maxLines: entry.key == 'body' || entry.key == 'message' ? 3 : 1,
                      ),
                    );
                  }).toList(),
                ),
              ),
            ],
          ),
        );
      },
    );
  }

  String _formatLabel(String key) {
    return key.replaceAll('_', ' ').split(' ').map((word) {
      if (word.isEmpty) return word;
      return word[0].toUpperCase() + word.substring(1);
    }).join(' ');
  }

  String _getHint(String key) {
    switch (key) {
      case 'from':
        return 'Filter by sender email';
      case 'subject':
        return 'Email subject or filter';
      case 'to':
        return 'Recipient email address';
      case 'body':
        return 'Message content (supports {{variables}})';
      case 'message':
        return 'Message to send (supports {{variables}})';
      case 'channel':
        return 'Discord channel name';
      case 'repository':
        return 'owner/repo format';
      case 'title':
        return 'Issue or PR title';
      case 'interval':
        return 'Time interval (e.g., 5)';
      case 'unit':
        return 'seconds, minutes, hours, days';
      default:
        return '';
    }
  }
}
