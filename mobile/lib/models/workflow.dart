import 'package:equatable/equatable.dart';

class Workflow extends Equatable {
  final int? id;
  final String name;
  final String? description;
  final bool active;
  final DateTime? createdAt;
  final DateTime? updatedAt;
  final WorkflowData? workflowData;

  const Workflow({
    this.id,
    required this.name,
    this.description,
    this.active = true,
    this.createdAt,
    this.updatedAt,
    this.workflowData,
  });

  factory Workflow.fromJson(Map<String, dynamic> json) {
    return Workflow(
      id: json['id'] as int?,
      name: json['name'] as String,
      description: json['description'] as String?,
      active: json['active'] as bool? ?? true,
      createdAt: json['createdAt'] != null
          ? DateTime.parse(json['createdAt'] as String)
          : null,
      updatedAt: json['updatedAt'] != null
          ? DateTime.parse(json['updatedAt'] as String)
          : null,
      workflowData: json['workflowData'] != null
          ? WorkflowData.fromJson(json['workflowData'] as Map<String, dynamic>)
          : null,
    );
  }

  Map<String, dynamic> toJson() {
    return {
      if (id != null) 'id': id,
      'name': name,
      if (description != null) 'description': description,
      'active': active,
      if (workflowData != null) 'workflowData': workflowData!.toJson(),
    };
  }

  Workflow copyWith({
    int? id,
    String? name,
    String? description,
    bool? active,
    DateTime? createdAt,
    DateTime? updatedAt,
    WorkflowData? workflowData,
  }) {
    return Workflow(
      id: id ?? this.id,
      name: name ?? this.name,
      description: description ?? this.description,
      active: active ?? this.active,
      createdAt: createdAt ?? this.createdAt,
      updatedAt: updatedAt ?? this.updatedAt,
      workflowData: workflowData ?? this.workflowData,
    );
  }

  @override
  List<Object?> get props =>
      [id, name, description, active, createdAt, updatedAt, workflowData];
}

class WorkflowData extends Equatable {
  final WorkflowTrigger? trigger;
  final List<WorkflowAction> actions;

  const WorkflowData({
    this.trigger,
    this.actions = const [],
  });

  factory WorkflowData.fromJson(Map<String, dynamic> json) {
    return WorkflowData(
      trigger: json['trigger'] != null
          ? WorkflowTrigger.fromJson(json['trigger'] as Map<String, dynamic>)
          : null,
      actions: (json['actions'] as List<dynamic>?)
              ?.map((e) => WorkflowAction.fromJson(e as Map<String, dynamic>))
              .toList() ??
          [],
    );
  }

  Map<String, dynamic> toJson() {
    return {
      if (trigger != null) 'trigger': trigger!.toJson(),
      'actions': actions.map((a) => a.toJson()).toList(),
    };
  }

  @override
  List<Object?> get props => [trigger, actions];
}

class WorkflowTrigger extends Equatable {
  final String service;
  final String type;
  final Map<String, dynamic> config;
  final int? connectionId;

  const WorkflowTrigger({
    required this.service,
    required this.type,
    this.config = const {},
    this.connectionId,
  });

  factory WorkflowTrigger.fromJson(Map<String, dynamic> json) {
    return WorkflowTrigger(
      service: json['service'] as String,
      type: json['type'] as String,
      config: (json['config'] as Map<String, dynamic>?) ?? {},
      connectionId: json['connectionId'] as int?,
    );
  }

  Map<String, dynamic> toJson() {
    return {
      'service': service,
      'type': type,
      'config': config,
      if (connectionId != null) 'connectionId': connectionId,
    };
  }

  @override
  List<Object?> get props => [service, type, config, connectionId];
}

class WorkflowAction extends Equatable {
  final String service;
  final String type;
  final Map<String, dynamic> config;
  final int? connectionId;

  const WorkflowAction({
    required this.service,
    required this.type,
    this.config = const {},
    this.connectionId,
  });

  factory WorkflowAction.fromJson(Map<String, dynamic> json) {
    return WorkflowAction(
      service: json['service'] as String,
      type: json['type'] as String,
      config: (json['config'] as Map<String, dynamic>?) ?? {},
      connectionId: json['connectionId'] as int?,
    );
  }

  Map<String, dynamic> toJson() {
    return {
      'service': service,
      'type': type,
      'config': config,
      if (connectionId != null) 'connectionId': connectionId,
    };
  }

  @override
  List<Object?> get props => [service, type, config, connectionId];
}

class CreateWorkflowRequest {
  final String name;
  final WorkflowTrigger? trigger;
  final List<WorkflowAction> actions;

  const CreateWorkflowRequest({
    required this.name,
    this.trigger,
    this.actions = const [],
  });

  Map<String, dynamic> toJson() {
    return {
      'name': name,
      if (trigger != null) 'trigger': trigger!.toJson(),
      'actions': actions.map((a) => a.toJson()).toList(),
    };
  }
}

class WorkflowStats extends Equatable {
  final int totalExecutions;
  final int successfulExecutions;
  final int failedExecutions;
  final DateTime? lastExecutedAt;

  const WorkflowStats({
    required this.totalExecutions,
    required this.successfulExecutions,
    required this.failedExecutions,
    this.lastExecutedAt,
  });

  factory WorkflowStats.fromJson(Map<String, dynamic> json) {
    return WorkflowStats(
      totalExecutions: json['totalExecutions'] as int? ?? 0,
      successfulExecutions: json['successfulExecutions'] as int? ?? 0,
      failedExecutions: json['failedExecutions'] as int? ?? 0,
      lastExecutedAt: json['lastExecutedAt'] != null
          ? DateTime.parse(json['lastExecutedAt'] as String)
          : null,
    );
  }

  @override
  List<Object?> get props =>
      [totalExecutions, successfulExecutions, failedExecutions, lastExecutedAt];
}
