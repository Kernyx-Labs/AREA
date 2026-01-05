import 'package:flutter/material.dart';
import '../constants/palette.dart';

/// Panel for inserting Gmail variables into Discord message templates.
///
/// Displays chip-style buttons for common Gmail message properties.
/// Tapping a chip inserts the variable placeholder at the current cursor position.
class VariableInsertionPanel extends StatelessWidget {
  final TextEditingController controller;
  final List<String> variables;

  const VariableInsertionPanel({
    super.key,
    required this.controller,
    required this.variables,
  });

  /// Inserts a variable placeholder at the current cursor position
  void _insertVariable(String variableName) {
    final selection = controller.selection;
    final variable = '{{$variableName}}';

    // Get current text and cursor position
    final currentText = controller.text;
    final cursorPosition = selection.baseOffset;

    // Handle invalid cursor position (when TextField is not focused)
    final insertPosition = cursorPosition >= 0 ? cursorPosition : currentText.length;

    // Insert variable at cursor position
    final newText = currentText.substring(0, insertPosition) +
        variable +
        currentText.substring(insertPosition);

    // Update controller
    controller.text = newText;

    // Move cursor after the inserted variable
    controller.selection = TextSelection.collapsed(
      offset: insertPosition + variable.length,
    );
  }

  @override
  Widget build(BuildContext context) {
    return Container(
      padding: const EdgeInsets.all(16),
      decoration: BoxDecoration(
        color: AppPalette.surface,
        border: Border.all(color: AppPalette.borderDefault),
        borderRadius: BorderRadius.circular(8),
      ),
      child: Column(
        crossAxisAlignment: CrossAxisAlignment.start,
        mainAxisSize: MainAxisSize.min,
        children: [
          // Label
          Text(
            'Insert variables:',
            style: TextStyle(
              color: AppPalette.textSecondary,
              fontSize: 14,
              fontWeight: FontWeight.w500,
            ),
          ),
          const SizedBox(height: 12),

          // Variable chips
          Wrap(
            spacing: 8,
            runSpacing: 8,
            children: variables.map((variable) {
              return ActionChip(
                label: Text(
                  variable,
                  style: const TextStyle(
                    color: Colors.white,
                    fontSize: 13,
                  ),
                ),
                backgroundColor: AppPalette.accentBlue,
                side: BorderSide.none,
                padding: const EdgeInsets.symmetric(horizontal: 8, vertical: 4),
                onPressed: () => _insertVariable(variable),
              );
            }).toList(),
          ),

          // Helper text
          const SizedBox(height: 12),
          Text(
            'Tap a variable to insert it into your message template',
            style: TextStyle(
              color: AppPalette.textMuted,
              fontSize: 12,
              fontStyle: FontStyle.italic,
            ),
          ),
        ],
      ),
    );
  }
}
