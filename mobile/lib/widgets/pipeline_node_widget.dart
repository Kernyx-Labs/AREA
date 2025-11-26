import 'package:flutter/material.dart';

import '../constants/palette.dart';
import '../constants/pipeline_layout.dart';
import '../models/models.dart';

class PipelineNodeWidget extends StatelessWidget {
  const PipelineNodeWidget({
    super.key,
    required this.node,
    required this.isSelected,
    required this.isConnectingFrom,
    required this.onTap,
    required this.onPanUpdate,
    required this.onDelete,
    required this.onConnectorTap,
    required this.canvasOffset,
  });

  final PipelineNode node;
  final bool isSelected;
  final bool isConnectingFrom;
  final VoidCallback onTap;
  final ValueChanged<Offset> onPanUpdate;
  final VoidCallback onDelete;
  final VoidCallback onConnectorTap;
  final Offset canvasOffset;

  @override
  Widget build(BuildContext context) {
    final isAction = node.type == NodeType.action;
    return Positioned(
      left: node.position.dx + canvasOffset.dx,
      top: node.position.dy + canvasOffset.dy,
      child: GestureDetector(
        onTap: onTap,
        onPanUpdate: (details) => onPanUpdate(details.delta),
        child: SizedBox(
          width: PipelineLayout.nodeWidth,
          height: PipelineLayout.nodeHeight,
          child: Material(
            elevation: isSelected ? 12 : 6,
            borderRadius: BorderRadius.circular(20),
            clipBehavior: Clip.antiAlias,
            child: Column(
              crossAxisAlignment: CrossAxisAlignment.stretch,
              children: [
                Container(
                  color: isAction ? AppPalette.nodeAction : AppPalette.nodeReaction,
                  padding: const EdgeInsets.symmetric(horizontal: 16, vertical: 12),
                  child: Row(
                    children: [
                      CircleAvatar(
                        radius: 16,
                        backgroundColor: node.service.color,
                        child: Icon(node.service.icon, color: Colors.white, size: 18),
                      ),
                      const SizedBox(width: 8),
                      Expanded(
                        child: Text(
                          node.service.name,
                          style: const TextStyle(color: Colors.white, fontWeight: FontWeight.w600),
                          overflow: TextOverflow.ellipsis,
                        ),
                      ),
                      IconButton(
                        onPressed: onDelete,
                        icon: const Icon(Icons.close, size: 16, color: Colors.white70),
                        visualDensity: VisualDensity.compact,
                      ),
                    ],
                  ),
                ),
                Expanded(
                  child: Container(
                    color: Colors.white,
                    padding: const EdgeInsets.all(16),
                    alignment: Alignment.centerLeft,
                    child: Column(
                      crossAxisAlignment: CrossAxisAlignment.start,
                      mainAxisAlignment: MainAxisAlignment.center,
                      children: [
                        Text(node.title, style: Theme.of(context).textTheme.titleSmall),
                        const SizedBox(height: 6),
                        Text(
                          node.description,
                          style: Theme.of(context)
                              .textTheme
                              .bodySmall!
                              .copyWith(color: AppPalette.surfaceText),
                        ),
                      ],
                    ),
                  ),
                ),
              ],
            ),
          ),
        ),
      ),
    ).withConnector(isAction: isAction, onTap: onConnectorTap, highlight: isConnectingFrom);
  }
}

extension on Widget {
  Widget withConnector({required bool isAction, required VoidCallback onTap, required bool highlight}) {
    return Stack(
      clipBehavior: Clip.none,
      children: [
        this,
        Positioned(
          top: isAction ? null : -PipelineLayout.connectorOverlap,
          bottom: isAction ? -PipelineLayout.connectorOverlap : null,
          left: (PipelineLayout.nodeWidth - PipelineLayout.connectorSize) / 2,
          child: GestureDetector(
            onTap: onTap,
            child: AnimatedContainer(
              duration: const Duration(milliseconds: 200),
              width: PipelineLayout.connectorSize,
              height: PipelineLayout.connectorSize,
              decoration: BoxDecoration(
                color: highlight ? AppPalette.accent : Colors.white,
                border: Border.all(
                  color: isAction ? AppPalette.nodeAction : AppPalette.nodeReaction,
                  width: 3,
                ),
                shape: BoxShape.circle,
              ),
            ),
          ),
        ),
      ],
    );
  }
}

