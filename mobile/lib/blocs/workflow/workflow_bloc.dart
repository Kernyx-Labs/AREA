import 'package:flutter_bloc/flutter_bloc.dart';
import '../../services/api_client.dart';
import '../../services/workflow_service.dart';
import 'workflow_event.dart';
import 'workflow_state.dart';

class WorkflowBloc extends Bloc<WorkflowEvent, WorkflowState> {
  final WorkflowService _workflowService;

  WorkflowBloc({required WorkflowService workflowService})
      : _workflowService = workflowService,
        super(const WorkflowState()) {
    on<WorkflowsLoadRequested>(_onWorkflowsLoadRequested);
    on<WorkflowLoadRequested>(_onWorkflowLoadRequested);
    on<WorkflowCreateRequested>(_onWorkflowCreateRequested);
    on<WorkflowUpdateRequested>(_onWorkflowUpdateRequested);
    on<WorkflowStatusUpdateRequested>(_onWorkflowStatusUpdateRequested);
    on<WorkflowDeleteRequested>(_onWorkflowDeleteRequested);
    on<WorkflowExecuteRequested>(_onWorkflowExecuteRequested);
    on<AvailableNodesLoadRequested>(_onAvailableNodesLoadRequested);
  }

  Future<void> _onWorkflowsLoadRequested(
    WorkflowsLoadRequested event,
    Emitter<WorkflowState> emit,
  ) async {
    emit(state.copyWith(status: WorkflowStatus.loading));
    try {
      final workflows = await _workflowService.getWorkflows(
        activeOnly: event.activeOnly,
      );
      emit(state.copyWith(
        status: WorkflowStatus.loaded,
        workflows: workflows,
      ));
    } on ApiException catch (e) {
      emit(state.copyWith(
        status: WorkflowStatus.error,
        errorMessage: e.message,
      ));
    } catch (e) {
      emit(state.copyWith(
        status: WorkflowStatus.error,
        errorMessage: 'An unexpected error occurred',
      ));
    }
  }

  Future<void> _onWorkflowLoadRequested(
    WorkflowLoadRequested event,
    Emitter<WorkflowState> emit,
  ) async {
    emit(state.copyWith(status: WorkflowStatus.loading));
    try {
      final workflow = await _workflowService.getWorkflow(event.id);
      emit(state.copyWith(
        status: WorkflowStatus.loaded,
        currentWorkflow: workflow,
      ));
    } on ApiException catch (e) {
      emit(state.copyWith(
        status: WorkflowStatus.error,
        errorMessage: e.message,
      ));
    } catch (e) {
      emit(state.copyWith(
        status: WorkflowStatus.error,
        errorMessage: 'An unexpected error occurred',
      ));
    }
  }

  Future<void> _onWorkflowCreateRequested(
    WorkflowCreateRequested event,
    Emitter<WorkflowState> emit,
  ) async {
    emit(state.copyWith(status: WorkflowStatus.creating));
    try {
      final workflow = await _workflowService.createWorkflow(event.request);
      final updatedWorkflows = [...state.workflows, workflow];
      emit(state.copyWith(
        status: WorkflowStatus.created,
        workflows: updatedWorkflows,
        currentWorkflow: workflow,
      ));
    } on ApiException catch (e) {
      emit(state.copyWith(
        status: WorkflowStatus.error,
        errorMessage: e.message,
      ));
    } catch (e) {
      emit(state.copyWith(
        status: WorkflowStatus.error,
        errorMessage: 'An unexpected error occurred',
      ));
    }
  }

  Future<void> _onWorkflowUpdateRequested(
    WorkflowUpdateRequested event,
    Emitter<WorkflowState> emit,
  ) async {
    emit(state.copyWith(status: WorkflowStatus.updating));
    try {
      final workflow = await _workflowService.updateWorkflow(
        event.id,
        event.updates,
      );
      final updatedWorkflows = state.workflows.map((w) {
        return w.id == event.id ? workflow : w;
      }).toList();
      emit(state.copyWith(
        status: WorkflowStatus.updated,
        workflows: updatedWorkflows,
        currentWorkflow: workflow,
      ));
    } on ApiException catch (e) {
      emit(state.copyWith(
        status: WorkflowStatus.error,
        errorMessage: e.message,
      ));
    } catch (e) {
      emit(state.copyWith(
        status: WorkflowStatus.error,
        errorMessage: 'An unexpected error occurred',
      ));
    }
  }

  Future<void> _onWorkflowStatusUpdateRequested(
    WorkflowStatusUpdateRequested event,
    Emitter<WorkflowState> emit,
  ) async {
    emit(state.copyWith(status: WorkflowStatus.updating));
    try {
      final workflow = await _workflowService.updateWorkflowStatus(
        event.id,
        event.active,
      );
      final updatedWorkflows = state.workflows.map((w) {
        return w.id == event.id ? workflow : w;
      }).toList();
      emit(state.copyWith(
        status: WorkflowStatus.updated,
        workflows: updatedWorkflows,
      ));
    } on ApiException catch (e) {
      emit(state.copyWith(
        status: WorkflowStatus.error,
        errorMessage: e.message,
      ));
    } catch (e) {
      emit(state.copyWith(
        status: WorkflowStatus.error,
        errorMessage: 'An unexpected error occurred',
      ));
    }
  }

  Future<void> _onWorkflowDeleteRequested(
    WorkflowDeleteRequested event,
    Emitter<WorkflowState> emit,
  ) async {
    emit(state.copyWith(status: WorkflowStatus.deleting));
    try {
      await _workflowService.deleteWorkflow(event.id);
      final updatedWorkflows =
          state.workflows.where((w) => w.id != event.id).toList();
      emit(state.copyWith(
        status: WorkflowStatus.deleted,
        workflows: updatedWorkflows,
        clearCurrentWorkflow: state.currentWorkflow?.id == event.id,
      ));
    } on ApiException catch (e) {
      emit(state.copyWith(
        status: WorkflowStatus.error,
        errorMessage: e.message,
      ));
    } catch (e) {
      emit(state.copyWith(
        status: WorkflowStatus.error,
        errorMessage: 'An unexpected error occurred',
      ));
    }
  }

  Future<void> _onWorkflowExecuteRequested(
    WorkflowExecuteRequested event,
    Emitter<WorkflowState> emit,
  ) async {
    emit(state.copyWith(status: WorkflowStatus.executing));
    try {
      await _workflowService.executeWorkflow(event.id);
      emit(state.copyWith(status: WorkflowStatus.executed));
    } on ApiException catch (e) {
      emit(state.copyWith(
        status: WorkflowStatus.error,
        errorMessage: e.message,
      ));
    } catch (e) {
      emit(state.copyWith(
        status: WorkflowStatus.error,
        errorMessage: 'An unexpected error occurred',
      ));
    }
  }

  Future<void> _onAvailableNodesLoadRequested(
    AvailableNodesLoadRequested event,
    Emitter<WorkflowState> emit,
  ) async {
    try {
      final availableNodes = await _workflowService.getAvailableNodes();
      emit(state.copyWith(availableNodes: availableNodes));
    } on ApiException catch (e) {
      emit(state.copyWith(
        status: WorkflowStatus.error,
        errorMessage: e.message,
      ));
    } catch (e) {
      emit(state.copyWith(
        status: WorkflowStatus.error,
        errorMessage: 'An unexpected error occurred',
      ));
    }
  }
}
