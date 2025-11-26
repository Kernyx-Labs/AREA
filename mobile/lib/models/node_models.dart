import 'dart:ui';

import 'service_definition.dart';

enum NodeType { action, reaction }

class PipelineNode {
  const PipelineNode({
    required this.id,
    required this.type,
    required this.service,
    required this.title,
    required this.description,
    required this.position,
  });

  final int id;
  final NodeType type;
  final ServiceDefinition service;
  final String title;
  final String description;
  final Offset position;

  PipelineNode copyWith({Offset? position}) {
    return PipelineNode(
      id: id,
      type: type,
      service: service,
      title: title,
      description: description,
      position: position ?? this.position,
    );
  }
}

class NodeConnection {
  const NodeConnection(this.from, this.to);
  final int from;
  final int to;
}

class NodeTemplate {
  const NodeTemplate({required this.serviceId, required this.name, required this.description});
  final String serviceId;
  final String name;
  final String description;
}

class NodeTemplateChoice {
  const NodeTemplateChoice({required this.type, required this.template});
  final NodeType type;
  final NodeTemplate template;
}

extension FirstOrNull<T> on Iterable<T> {
  T? get firstOrNull => isEmpty ? null : first;
}

const actionTemplates = [
  NodeTemplate(serviceId: 'gmail', name: 'New email received', description: 'Triggers when a new email arrives'),
  NodeTemplate(serviceId: 'gmail', name: 'Email with attachment', description: 'Triggers on emails with files'),
  NodeTemplate(serviceId: 'timer', name: 'Schedule', description: 'Triggers at specific times'),
  NodeTemplate(serviceId: 'timer', name: 'Interval', description: 'Triggers every X minutes'),
  NodeTemplate(serviceId: 'github', name: 'New Issue', description: 'Triggers on new issues'),
  NodeTemplate(serviceId: 'github', name: 'Pull Request', description: 'Triggers on new PRs'),
];

const reactionTemplates = [
  NodeTemplate(serviceId: 'discord', name: 'Send Message', description: 'Posts a message to a channel'),
  NodeTemplate(serviceId: 'gmail', name: 'Send Email', description: 'Sends an email'),
  NodeTemplate(serviceId: 'dropbox', name: 'Upload File', description: 'Uploads a file to Dropbox'),
  NodeTemplate(serviceId: 'outlook', name: 'Create Event', description: 'Creates a calendar event'),
  NodeTemplate(serviceId: 'github', name: 'Create Issue', description: 'Creates a new issue'),
];
