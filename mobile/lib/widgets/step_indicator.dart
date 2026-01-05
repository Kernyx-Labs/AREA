import 'package:flutter/material.dart';
import '../constants/palette.dart';

/// Visual indicator showing progress through wizard steps.
///
/// Displays step count, progress bar, and current step title.
class StepIndicator extends StatelessWidget {
  final int currentStep;
  final int totalSteps;
  final List<String> stepTitles;

  const StepIndicator({
    super.key,
    required this.currentStep,
    required this.totalSteps,
    required this.stepTitles,
  });

  @override
  Widget build(BuildContext context) {
    // Calculate progress (0.0 to 1.0)
    final progress = currentStep / totalSteps;

    // Get current step title (ensure index is within bounds)
    final currentTitle = currentStep > 0 && currentStep <= stepTitles.length
        ? stepTitles[currentStep - 1]
        : '';

    return Container(
      padding: const EdgeInsets.symmetric(horizontal: 16, vertical: 12),
      decoration: BoxDecoration(
        color: AppPalette.surface,
        border: Border(
          bottom: BorderSide(
            color: AppPalette.borderDefault,
            width: 1,
          ),
        ),
      ),
      child: Column(
        crossAxisAlignment: CrossAxisAlignment.start,
        children: [
          // Step count
          Row(
            mainAxisAlignment: MainAxisAlignment.spaceBetween,
            children: [
              Text(
                'Step $currentStep of $totalSteps',
                style: TextStyle(
                  color: AppPalette.textSecondary,
                  fontSize: 14,
                  fontWeight: FontWeight.w500,
                ),
              ),
              Text(
                '${(progress * 100).toInt()}%',
                style: TextStyle(
                  color: AppPalette.accentBlue,
                  fontSize: 14,
                  fontWeight: FontWeight.w600,
                ),
              ),
            ],
          ),
          const SizedBox(height: 8),

          // Progress bar
          ClipRRect(
            borderRadius: BorderRadius.circular(4),
            child: LinearProgressIndicator(
              value: progress,
              backgroundColor: AppPalette.borderDefault,
              valueColor: const AlwaysStoppedAnimation<Color>(
                AppPalette.accentBlue,
              ),
              minHeight: 6,
            ),
          ),
          const SizedBox(height: 12),

          // Current step title
          Text(
            currentTitle,
            style: TextStyle(
              color: AppPalette.textPrimary,
              fontSize: 18,
              fontWeight: FontWeight.w600,
              letterSpacing: -0.5,
            ),
          ),
        ],
      ),
    );
  }
}
