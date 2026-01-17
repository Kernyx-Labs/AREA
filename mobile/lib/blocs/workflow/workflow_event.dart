import 'package:equatable/equatable.dart';
import '../../models/workflow.dart';

abstract class WorkflowEvent extends Equatable {
  const WorkflowEvent();

  @override
  List<Object?> get props => [];
}

class WorkflowsLoadRequested extends WorkflowEvent {
  final bool activeOnly;

  const WorkflowsLoadRequested({this.activeOnly = false});

  @override
  List<Object?> get props => [activeOnly];
}

class WorkflowLoadRequested extends WorkflowEvent {
  final int id;

  const WorkflowLoadRequested({required this.id});

  @override
  List<Object?> get props => [id];
}

class WorkflowCreateRequested extends WorkflowEvent {
  final CreateWorkflowRequest request;

  const WorkflowCreateRequested({required this.request});

  @override
  List<Object?> get props => [request];
}

class WorkflowUpdateRequested extends WorkflowEvent {
  final int id;
  final Map<String, dynamic> updates;

  const WorkflowUpdateRequested({
    required this.id,
    required this.updates,
  });

  @override
  List<Object?> get props => [id, updates];
}

class WorkflowStatusUpdateRequested extends WorkflowEvent {
  final int id;
  final bool active;

  const WorkflowStatusUpdateRequested({
    required this.id,
    required this.active,
  });

  @override
  List<Object?> get props => [id, active];
}

class WorkflowDeleteRequested extends WorkflowEvent {
  final int id;

  const WorkflowDeleteRequested({required this.id});

  @override
  List<Object?> get props => [id];
}

class WorkflowExecuteRequested extends WorkflowEvent {
  final int id;

  const WorkflowExecuteRequested({required this.id});

  @override
  List<Object?> get props => [id];
}

class AvailableNodesLoadRequested extends WorkflowEvent {
  const AvailableNodesLoadRequested();
}
