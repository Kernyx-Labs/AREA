import 'package:flutter/material.dart';
import '../constants/palette.dart';
import '../models/service_metadata.dart';

/// Widget for selecting an action type from a service.
///
/// Displays available action types for the selected service
/// (e.g., "Send message", "Send rich embed" for Discord).
class ActionTypePicker extends StatelessWidget {
  final String serviceType;
  final List<Map<String, dynamic>> actionTypes;
  final String? selectedActionType;
  final Function(String actionType) onActionTypeSelected;

  const ActionTypePicker({
    super.key,
    required this.serviceType,
    required this.actionTypes,
    this.selectedActionType,
    required this.onActionTypeSelected,
  });

  @override
  Widget build(BuildContext context) {
    final serviceName = ServiceMetadata.getServiceName(serviceType);
    final serviceColor = ServiceMetadata.getServiceColor(serviceType);

    return ListView(
      padding: const EdgeInsets.all(16),
      children: [
        // Service header
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
                ServiceMetadata.getServiceIcon(serviceType),
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
                    'Select Action',
                    style: TextStyle(
                      fontSize: 20,
                      fontWeight: FontWeight.w600,
                      color: AppPalette.textPrimary,
                    ),
                  ),
                  const SizedBox(height: 4),
                  Text(
                    'Choose what $serviceName should do when triggered',
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

        // Action type list
        if (actionTypes.isEmpty)
          _buildEmptyState()
        else
          ...actionTypes.map((actionType) {
            final type = actionType['type'] as String;
            final isSelected = selectedActionType == type;

            return Padding(
              padding: const EdgeInsets.only(bottom: 12),
              child: _ActionTypeCard(
                name: actionType['name'] ?? type,
                description: actionType['description'] ?? '',
                icon: actionType['icon'] as IconData? ?? Icons.bolt,
                color: serviceColor,
                isSelected: isSelected,
                onTap: () => onActionTypeSelected(type),
              ),
            );
          }).toList(),
      ],
    );
  }

  Widget _buildEmptyState() {
    return Center(
      child: Padding(
        padding: const EdgeInsets.all(32),
        child: Column(
          children: [
            Icon(
              Icons.info_outline,
              size: 64,
              color: AppPalette.textSecondary.withOpacity(0.5),
            ),
            const SizedBox(height: 16),
            Text(
              'No actions available',
              style: TextStyle(
                fontSize: 16,
                fontWeight: FontWeight.w600,
                color: AppPalette.textSecondary,
              ),
            ),
            const SizedBox(height: 8),
            Text(
              'This service does not have any available actions',
              textAlign: TextAlign.center,
              style: TextStyle(
                fontSize: 14,
                color: AppPalette.textMuted,
              ),
            ),
          ],
        ),
      ),
    );
  }
}

/// Internal widget for displaying an action type card
class _ActionTypeCard extends StatelessWidget {
  final String name;
  final String description;
  final IconData icon;
  final Color color;
  final bool isSelected;
  final VoidCallback onTap;

  const _ActionTypeCard({
    required this.name,
    required this.description,
    required this.icon,
    required this.color,
    required this.isSelected,
    required this.onTap,
  });

  @override
  Widget build(BuildContext context) {
    return InkWell(
      onTap: onTap,
      borderRadius: BorderRadius.circular(12),
      child: Container(
        padding: const EdgeInsets.all(16),
        decoration: BoxDecoration(
          color: AppPalette.surface,
          borderRadius: BorderRadius.circular(12),
          border: Border.all(
            color: isSelected ? color : AppPalette.borderDefault,
            width: isSelected ? 2 : 1,
          ),
          boxShadow: isSelected
              ? [
                  BoxShadow(
                    color: color.withOpacity(0.15),
                    blurRadius: 8,
                    offset: const Offset(0, 2),
                  ),
                ]
              : null,
        ),
        child: Row(
          children: [
            // Icon
            Container(
              width: 56,
              height: 56,
              decoration: BoxDecoration(
                color: color.withOpacity(0.1),
                borderRadius: BorderRadius.circular(12),
              ),
              child: Icon(
                icon,
                color: color,
                size: 28,
              ),
            ),
            const SizedBox(width: 16),

            // Content
            Expanded(
              child: Column(
                crossAxisAlignment: CrossAxisAlignment.start,
                children: [
                  Text(
                    name,
                    style: TextStyle(
                      fontSize: 16,
                      fontWeight: FontWeight.w600,
                      color: AppPalette.textPrimary,
                    ),
                  ),
                  const SizedBox(height: 4),
                  Text(
                    description,
                    style: TextStyle(
                      fontSize: 13,
                      color: AppPalette.textSecondary,
                    ),
                    maxLines: 2,
                    overflow: TextOverflow.ellipsis,
                  ),
                ],
              ),
            ),
            const SizedBox(width: 12),

            // Selection indicator
            Container(
              width: 28,
              height: 28,
              decoration: BoxDecoration(
                shape: BoxShape.circle,
                border: Border.all(
                  color: isSelected ? color : AppPalette.borderDefault,
                  width: 2,
                ),
                color: isSelected ? color : Colors.transparent,
              ),
              child: isSelected
                  ? const Icon(
                      Icons.check,
                      size: 18,
                      color: Colors.white,
                    )
                  : null,
            ),
          ],
        ),
      ),
    );
  }
}
