import 'package:flutter/material.dart';
import 'package:flutter_bloc/flutter_bloc.dart';

import '../blocs/workflow/workflow.dart';
import '../constants/palette.dart';
import '../constants/shadows.dart';
import '../models/workflow.dart';
import 'pipeline_editor_screen.dart';

class DashboardScreen extends StatefulWidget {
  const DashboardScreen({super.key});

  @override
  State<DashboardScreen> createState() => _DashboardScreenState();
}

class _DashboardScreenState extends State<DashboardScreen> {
  @override
  void initState() {
    super.initState();
    _loadWorkflows();
  }

  void _loadWorkflows() {
    context.read<WorkflowBloc>().add(const WorkflowsLoadRequested());
  }

  void _navigateToEditor({Workflow? workflow}) {
    Navigator.of(context).push(
      MaterialPageRoute(
        builder: (context) => Scaffold(
          body: SafeArea(
            child: Column(
              children: [
                Container(
                  padding: const EdgeInsets.symmetric(horizontal: 8, vertical: 4),
                  color: AppPalette.background,
                  child: Row(
                    children: [
                      IconButton(
                        icon: const Icon(Icons.arrow_back),
                        onPressed: () => Navigator.of(context).pop(),
                      ),
                      Text(
                        workflow != null ? 'Edit Workflow' : 'New Workflow',
                        style: const TextStyle(
                          fontSize: 18,
                          fontWeight: FontWeight.w600,
                        ),
                      ),
                    ],
                  ),
                ),
                Expanded(
                  child: PipelineEditorScreen(workflow: workflow),
                ),
              ],
            ),
          ),
        ),
      ),
    ).then((_) => _loadWorkflows());
  }

  void _toggleWorkflowStatus(Workflow workflow) {
    context.read<WorkflowBloc>().add(
          WorkflowStatusUpdateRequested(
            id: workflow.id!,
            active: !workflow.active,
          ),
        );
  }

  void _deleteWorkflow(Workflow workflow) {
    showDialog(
      context: context,
      builder: (context) => AlertDialog(
        title: const Text('Delete Workflow'),
        content: Text('Are you sure you want to delete "${workflow.name}"?'),
        actions: [
          TextButton(
            onPressed: () => Navigator.pop(context),
            child: const Text('Cancel'),
          ),
          TextButton(
            onPressed: () {
              Navigator.pop(context);
              context.read<WorkflowBloc>().add(
                    WorkflowDeleteRequested(id: workflow.id!),
                  );
            },
            style: TextButton.styleFrom(foregroundColor: AppPalette.error),
            child: const Text('Delete'),
          ),
        ],
      ),
    );
  }

  @override
  Widget build(BuildContext context) {
    return BlocConsumer<WorkflowBloc, WorkflowState>(
      listener: (context, state) {
        if (state.status == WorkflowStatus.deleted) {
          ScaffoldMessenger.of(context).showSnackBar(
            const SnackBar(
              content: Text('Workflow deleted'),
              backgroundColor: AppPalette.success,
            ),
          );
        } else if (state.status == WorkflowStatus.error) {
          ScaffoldMessenger.of(context).showSnackBar(
            SnackBar(
              content: Text(state.errorMessage ?? 'An error occurred'),
              backgroundColor: AppPalette.error,
            ),
          );
        }
      },
      builder: (context, state) {
        return Padding(
          padding: const EdgeInsets.symmetric(horizontal: 20, vertical: 24),
          child: Column(
            crossAxisAlignment: CrossAxisAlignment.start,
            children: [
              Text(
                'My Automations',
                style: Theme.of(context).textTheme.headlineMedium,
              ),
              const SizedBox(height: 4),
              Text(
                'Manage your AREA workflows',
                style: Theme.of(context)
                    .textTheme
                    .bodyMedium!
                    .copyWith(color: AppPalette.primary),
              ),
              const SizedBox(height: 24),
              FilledButton.icon(
                onPressed: () => _navigateToEditor(),
                style: FilledButton.styleFrom(
                  backgroundColor: AppPalette.accent,
                  foregroundColor: AppPalette.dark,
                  padding: const EdgeInsets.symmetric(
                    horizontal: 20,
                    vertical: 14,
                  ),
                  shape: RoundedRectangleBorder(
                    borderRadius: BorderRadius.circular(16),
                  ),
                ),
                icon: const Icon(Icons.add),
                label: const Text('New AREA'),
              ),
              const SizedBox(height: 24),
              Expanded(
                child: _buildWorkflowList(state),
              ),
            ],
          ),
        );
      },
    );
  }

  Widget _buildWorkflowList(WorkflowState state) {
    if (state.status == WorkflowStatus.loading && state.workflows.isEmpty) {
      return const Center(
        child: CircularProgressIndicator(),
      );
    }

    if (state.workflows.isEmpty) {
      return Center(
        child: Column(
          mainAxisAlignment: MainAxisAlignment.center,
          children: [
            Icon(
              Icons.device_hub_outlined,
              size: 64,
              color: AppPalette.surfaceText.withOpacity(0.6),
            ),
            const SizedBox(height: 16),
            Text(
              'No workflows yet',
              style: TextStyle(
                fontSize: 18,
                color: AppPalette.surfaceText,
              ),
            ),
            const SizedBox(height: 8),
            Text(
              'Create your first automation!',
              style: TextStyle(
                fontSize: 14,
                color: AppPalette.surfaceText.withOpacity(0.8),
              ),
            ),
          ],
        ),
      );
    }

    return RefreshIndicator(
      onRefresh: () async {
        _loadWorkflows();
        await Future.delayed(const Duration(seconds: 1));
      },
      child: ListView.separated(
        itemBuilder: (context, index) {
          final workflow = state.workflows[index];
          return _WorkflowCard(
            workflow: workflow,
            onTap: () => _navigateToEditor(workflow: workflow),
            onToggle: () => _toggleWorkflowStatus(workflow),
            onDelete: () => _deleteWorkflow(workflow),
          );
        },
        separatorBuilder: (_, __) => const SizedBox(height: 16),
        itemCount: state.workflows.length,
      ),
    );
  }
}

class _WorkflowCard extends StatelessWidget {
  final Workflow workflow;
  final VoidCallback onTap;
  final VoidCallback onToggle;
  final VoidCallback onDelete;

  const _WorkflowCard({
    required this.workflow,
    required this.onTap,
    required this.onToggle,
    required this.onDelete,
  });

  @override
  Widget build(BuildContext context) {
    final actionsCount = workflow.workflowData?.actions.length ?? 0;
    final hasTrigger = workflow.workflowData?.trigger != null;
    final nodeCount = actionsCount + (hasTrigger ? 1 : 0);

    return GestureDetector(
      onTap: onTap,
      child: Container(
        padding: const EdgeInsets.all(20),
        decoration: BoxDecoration(
          color: Colors.white,
          borderRadius: BorderRadius.circular(20),
          boxShadow: AppShadows.card,
        ),
        child: Row(
          children: [
            Expanded(
              child: Column(
                crossAxisAlignment: CrossAxisAlignment.start,
                children: [
                  Text(
                    workflow.name,
                    style: Theme.of(context).textTheme.titleMedium,
                  ),
                  const SizedBox(height: 4),
                  if (workflow.description != null)
                    Text(
                      workflow.description!,
                      style: Theme.of(context).textTheme.bodySmall!.copyWith(
                            color: AppPalette.surfaceText,
                          ),
                      maxLines: 1,
                      overflow: TextOverflow.ellipsis,
                    ),
                  const SizedBox(height: 8),
                  Text(
                    '$nodeCount nodes • ${workflow.active ? 'Active' : 'Inactive'}',
                    style: Theme.of(context).textTheme.bodyMedium!.copyWith(
                          color: AppPalette.surfaceText,
                        ),
                  ),
                ],
              ),
            ),
            Column(
              children: [
                GestureDetector(
                  onTap: onToggle,
                  child: Container(
                    width: 14,
                    height: 14,
                    decoration: BoxDecoration(
                      color: workflow.active ? AppPalette.success : AppPalette.surfaceText,
                      shape: BoxShape.circle,
                    ),
                  ),
                ),
                const SizedBox(height: 12),
                GestureDetector(
                  onTap: onDelete,
                  child: Icon(
                    Icons.delete_outline,
                    size: 20,
                    color: AppPalette.surfaceText.withOpacity(0.6),
                  ),
                ),
              ],
            ),
          ],
        ),
      ),
    );
  }
}
