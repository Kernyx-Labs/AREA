import 'package:flutter/material.dart';
import '../constants/palette.dart';

/// Selectable card displaying a service connection.
///
/// Shows service icon, account name, active status, and selection state.
/// Used in wizard steps for selecting Gmail and Discord connections.
class ConnectionSelectorCard extends StatelessWidget {
  final String service;
  final String email;
  final bool isActive;
  final bool isSelected;
  final VoidCallback onTap;

  const ConnectionSelectorCard({
    super.key,
    required this.service,
    required this.email,
    required this.isActive,
    required this.isSelected,
    required this.onTap,
  });

  /// Get the appropriate icon for the service
  IconData _getServiceIcon() {
    switch (service.toLowerCase()) {
      case 'gmail':
        return Icons.mail;
      case 'discord':
        return Icons.chat;
      default:
        return Icons.cloud;
    }
  }

  @override
  Widget build(BuildContext context) {
    return InkWell(
      onTap: onTap,
      borderRadius: BorderRadius.circular(8),
      child: Container(
        padding: const EdgeInsets.all(16),
        decoration: BoxDecoration(
          color: AppPalette.surface,
          border: Border.all(
            color: isSelected ? AppPalette.accentBlue : AppPalette.borderDefault,
            width: isSelected ? 2 : 1,
          ),
          borderRadius: BorderRadius.circular(8),
        ),
        child: Row(
          children: [
            // Service icon
            Container(
              width: 44,
              height: 44,
              decoration: BoxDecoration(
                color: AppPalette.surfaceElevated,
                borderRadius: BorderRadius.circular(8),
              ),
              child: Icon(
                _getServiceIcon(),
                color: AppPalette.accentBlue,
                size: 24,
              ),
            ),
            const SizedBox(width: 16),

            // Account info
            Expanded(
              child: Column(
                crossAxisAlignment: CrossAxisAlignment.start,
                children: [
                  // Service name
                  Text(
                    service,
                    style: TextStyle(
                      color: AppPalette.textPrimary,
                      fontSize: 16,
                      fontWeight: FontWeight.w600,
                    ),
                  ),
                  const SizedBox(height: 4),

                  // Email/account
                  Text(
                    email,
                    style: TextStyle(
                      color: AppPalette.textSecondary,
                      fontSize: 14,
                    ),
                    overflow: TextOverflow.ellipsis,
                  ),
                ],
              ),
            ),

            // Status badge
            Container(
              padding: const EdgeInsets.symmetric(horizontal: 8, vertical: 4),
              decoration: BoxDecoration(
                color: isActive
                    ? AppPalette.success.withOpacity(0.2)
                    : AppPalette.danger.withOpacity(0.2),
                borderRadius: BorderRadius.circular(4),
              ),
              child: Text(
                isActive ? 'Active' : 'Expired',
                style: TextStyle(
                  color: isActive ? AppPalette.success : AppPalette.danger,
                  fontSize: 12,
                  fontWeight: FontWeight.w600,
                ),
              ),
            ),
            const SizedBox(width: 12),

            // Selection indicator
            Container(
              width: 24,
              height: 24,
              decoration: BoxDecoration(
                shape: BoxShape.circle,
                border: Border.all(
                  color: isSelected
                      ? AppPalette.accentBlue
                      : AppPalette.borderDefault,
                  width: 2,
                ),
                color: isSelected ? AppPalette.accentBlue : Colors.transparent,
              ),
              child: isSelected
                  ? const Icon(
                      Icons.check,
                      size: 16,
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
