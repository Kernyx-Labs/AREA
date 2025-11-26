import 'package:flutter/material.dart';

import '../constants/palette.dart';
import '../constants/pipeline_layout.dart';
import '../constants/shadows.dart';
import '../models/models.dart';
import '../widgets/canvas/connection_painter.dart';
import '../widgets/canvas/grid_painter.dart';
import '../widgets/node_picker.dart';
import '../widgets/pipeline_node_widget.dart';

class PipelineEditorScreen extends StatefulWidget {
  const PipelineEditorScreen({super.key});

  @override
  State<PipelineEditorScreen> createState() => _PipelineEditorScreenState();
}

class _PipelineEditorScreenState extends State<PipelineEditorScreen> {
  final TextEditingController _nameController = TextEditingController(text: 'New Automation');

  final List<PipelineNode> _nodes = [
    PipelineNode(
      id: 1,
      type: NodeType.action,
      service: services['github']!,
      title: 'New Issue',
      description: 'Triggers on new issues',
      position: PipelineLayout.initialNodePosition,
    ),
    PipelineNode(
      id: 2,
      type: NodeType.reaction,
      service: services['discord']!,
      title: 'Send Message',
      description: 'Posts to #alerts',
      position: PipelineLayout.initialNodePosition +
          Offset(0, PipelineLayout.nodeHeight + PipelineLayout.nodeSpacing),
    ),
  ];

  final List<NodeConnection> _connections = [const NodeConnection(1, 2)];
  int? _selectedNodeId;
  int? _connectingFrom;
  String _areaName = 'New Automation';
  int? _activeCanvasPointerId;
  Offset _canvasOffset = Offset.zero;

  @override
  void initState() {
    super.initState();
    _nameController.text = _areaName;
    _nameController.addListener(() => _areaName = _nameController.text);
  }

  @override
  void dispose() {
    _nameController.dispose();
    super.dispose();
  }

  void _onNodePan(int id, Offset delta) {
    setState(() {
      final idx = _nodes.indexWhere((node) => node.id == id);
      if (idx == -1) return;
      final node = _nodes[idx];
      _nodes[idx] = node.copyWith(position: node.position + delta);
    });
  }

  Offset _nextNodePosition() {
    final double y = PipelineLayout.initialNodePosition.dy +
        _nodes.length * (PipelineLayout.nodeHeight + PipelineLayout.nodeSpacing);
    return Offset(PipelineLayout.initialNodePosition.dx, y);
  }

  void _onDelete(int id) {
    setState(() {
      _nodes.removeWhere((node) => node.id == id);
      _connections.removeWhere((conn) => conn.from == id || conn.to == id);
      if (_selectedNodeId == id) {
        _selectedNodeId = null;
      }
    });
  }

  Future<void> _showNodePicker() async {
    final selection = await showModalBottomSheet<NodeTemplateChoice>(
      context: context,
      isScrollControlled: true,
      backgroundColor: Colors.white,
      shape: const RoundedRectangleBorder(borderRadius: BorderRadius.vertical(top: Radius.circular(30))),
      builder: (context) => NodePicker(initialType: NodeType.action),
    );

    if (selection == null) return;

    setState(() {
      final template = selection.template;
      final type = selection.type;
      final newId = DateTime.now().millisecondsSinceEpoch;
      _nodes.add(
        PipelineNode(
          id: newId,
          type: type,
          service: services[template.serviceId]!,
          title: template.name,
          description: template.description,
          position: _nextNodePosition(),
        ),
      );
    });
  }

  void _startConnection(int id) {
    setState(() => _connectingFrom = id);
  }

  void _finishConnection(int id) {
    if (_connectingFrom == null || _connectingFrom == id) {
      setState(() => _connectingFrom = null);
      return;
    }

    final exists = _connections.any((conn) => conn.from == _connectingFrom && conn.to == id);
    if (!exists) {
      setState(() {
        _connections.add(NodeConnection(_connectingFrom!, id));
        _connectingFrom = null;
      });
    } else {
      setState(() => _connectingFrom = null);
    }
  }

  bool _isPointerOverNode(Offset localPosition) {
    final Offset worldPosition = localPosition - _canvasOffset;
    return _nodes.any(
      (node) => Rect.fromLTWH(
        node.position.dx,
        node.position.dy,
        PipelineLayout.nodeWidth,
        PipelineLayout.nodeHeight,
      ).contains(worldPosition),
    );
  }

  void _onCanvasPointerDown(PointerDownEvent event) {
    if (_activeCanvasPointerId != null) return;
    if (_isPointerOverNode(event.localPosition)) return;
    _activeCanvasPointerId = event.pointer;
  }

  void _onCanvasPointerMove(PointerMoveEvent event) {
    if (_activeCanvasPointerId != event.pointer) return;
    setState(() => _canvasOffset += event.delta);
  }

  void _onCanvasPointerEnd(PointerEvent event) {
    if (_activeCanvasPointerId == event.pointer) {
      _activeCanvasPointerId = null;
    }
  }

  @override
  Widget build(BuildContext context) {
    return Column(
      crossAxisAlignment: CrossAxisAlignment.stretch,
      children: [
        Container(
          padding: const EdgeInsets.symmetric(horizontal: 16, vertical: 12),
          decoration: BoxDecoration(color: AppPalette.dark, boxShadow: AppShadows.appBar),
          child: Row(
            children: [
              Expanded(
                child: TextField(
                  controller: _nameController,
                  style: const TextStyle(color: Colors.white, fontSize: 18, fontWeight: FontWeight.w600),
                  decoration: const InputDecoration.collapsed(
                    hintText: 'Name your AREA',
                    hintStyle: TextStyle(color: Colors.white70),
                  ),
                ),
              ),
              const SizedBox(width: 12),
              IconButton.filledTonal(
                onPressed: _showNodePicker,
                style: IconButton.styleFrom(
                  backgroundColor: Colors.white.withOpacity(0.15),
                  foregroundColor: Colors.white,
                  minimumSize: const Size(44, 44),
                ),
                icon: const Icon(Icons.add),
                tooltip: 'Add node',
              ),
            ],
          ),
        ),
        Expanded(
          child: Listener(
            behavior: HitTestBehavior.opaque,
            onPointerDown: _onCanvasPointerDown,
            onPointerMove: _onCanvasPointerMove,
            onPointerUp: _onCanvasPointerEnd,
            onPointerCancel: _onCanvasPointerEnd,
            child: Container(
              color: AppPalette.canvas,
              child: Stack(
                clipBehavior: Clip.none,
                children: [
                  Positioned.fill(
                    child: CustomPaint(
                      painter: GridPainter(offset: _canvasOffset),
                    ),
                  ),
                  Positioned.fill(
                    child: CustomPaint(
                      painter: ConnectionPainter(
                        nodes: _nodes,
                        connections: _connections,
                        canvasOffset: _canvasOffset,
                      ),
                    ),
                  ),
                  ..._nodes.map(
                    (node) => PipelineNodeWidget(
                      key: ValueKey(node.id),
                      node: node,
                      canvasOffset: _canvasOffset,
                      isSelected: node.id == _selectedNodeId,
                      isConnectingFrom: node.id == _connectingFrom,
                      onTap: () => setState(() => _selectedNodeId = node.id),
                      onPanUpdate: (delta) => _onNodePan(node.id, delta),
                      onDelete: () => _onDelete(node.id),
                      onConnectorTap: () {
                        if (node.type == NodeType.action) {
                          _startConnection(node.id);
                        } else {
                          _finishConnection(node.id);
                        }
                      },
                    ),
                  ),
                  Positioned(
                    left: 16,
                    bottom: 16,
                    child: Container(
                      padding: const EdgeInsets.symmetric(horizontal: 14, vertical: 10),
                      decoration: BoxDecoration(
                        color: Colors.black54,
                        borderRadius: BorderRadius.circular(20),
                      ),
                      child: const Text(
                        'Drag background to pan • Drag nodes • Tap connectors to link',
                        style: TextStyle(color: Colors.white70, fontSize: 12),
                      ),
                    ),
                  ),
                ],
              ),
            ),
          ),
        ),
      ],
    );
  }
}
