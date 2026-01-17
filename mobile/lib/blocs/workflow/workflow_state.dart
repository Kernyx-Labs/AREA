import 'package:equatable/equatable.dart';
import '../../models/workflow.dart';

enum WorkflowStatus {
  initial,
  loading,
  loaded,
  error,
  creating,
  created,
  updating,
  updated,
  deleting,
  deleted,
  executing,
  executed,
}

class WorkflowState extends Equatable {
  final WorkflowStatus status;
  final List<Workflow> workflows;
  final Workflow? currentWorkflow;
  final Map<String, dynamic>? availableNodes;
  final String? errorMessage;

  const WorkflowState({
    this.status = WorkflowStatus.initial,
    this.workflows = const [],
    this.currentWorkflow,
    this.availableNodes,
    this.errorMessage,
  });

  WorkflowState copyWith({
    WorkflowStatus? status,
    List<Workflow>? workflows,
    Workflow? currentWorkflow,
    Map<String, dynamic>? availableNodes,
    String? errorMessage,
    bool clearCurrentWorkflow = false,
  }) {
    return WorkflowState(
      status: status ?? this.status,
      workflows: workflows ?? this.workflows,
      currentWorkflow: clearCurrentWorkflow ? null : (currentWorkflow ?? this.currentWorkflow),
      availableNodes: availableNodes ?? this.availableNodes,
      errorMessage: errorMessage ?? this.errorMessage,
    );
  }

  @override
  List<Object?> get props => [
        status,
        workflows,
        currentWorkflow,
        availableNodes,
        errorMessage,
      ];
}
