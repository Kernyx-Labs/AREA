import 'dart:ui';

class AppPalette {
  // BLACK THEME - Widget-Based Design System (matching web)

  // Base black and greyscale palette
  static const pureBlack = Color(0xFF000000);
  static const deepBlack = Color(0xFF0A0A0A);
  static const richBlack = Color(0xFF121212);
  static const darkCharcoal = Color(0xFF1A1A1A);
  static const charcoal = Color(0xFF242424);
  static const slateGrey = Color(0xFF2E2E2E);
  static const mediumGrey = Color(0xFF3A3A3A);
  static const lightGrey = Color(0xFF4A4A4A);
  static const silver = Color(0xFF6A6A6A);
  static const lightSilver = Color(0xFF8A8A8A);
  static const paleGrey = Color(0xFFABABAB);
  static const ghostWhite = Color(0xFFD0D0D0);
  static const brightWhite = Color(0xFFF0F0F0);

  // Accent colors for highlights and interactions
  static const accentBlue = Color(0xFF5B9BD5);
  static const accentGreen = Color(0xFF70C770);
  static const accentOrange = Color(0xFFFF8C42);
  static const accentRed = Color(0xFFFF6B6B);
  static const accentPurple = Color(0xFF9B87F5);

  // Widget System - Greyscale with bright outlines
  static const background = pureBlack;
  static const backgroundAlt = deepBlack;
  static const surface = darkCharcoal;
  static const surfaceRaised = charcoal;
  static const surfaceElevated = slateGrey;
  static const surfaceHighest = mediumGrey;

  // Widget borders - bright outlines for separation
  static const borderSubtle = charcoal;
  static const borderDefault = slateGrey;
  static const borderStrong = mediumGrey;
  static const borderBright = lightGrey;
  static const borderAccent = silver;

  // Text colors - greyscale hierarchy
  static const textPrimary = brightWhite;
  static const textSecondary = ghostWhite;
  static const textTertiary = paleGrey;
  static const textMuted = lightSilver;
  static const textDisabled = silver;

  // Semantic colors
  static const success = accentGreen;
  static const warning = accentOrange;
  static const danger = accentRed;
  static const info = accentBlue;

  // Legacy compatibility
  static const primary = accentBlue;
  static const accent = accentBlue;
  static const dark = pureBlack;
  static const nodeAction = accentBlue;
  static const nodeReaction = accentPurple;
  static const canvas = darkCharcoal;
  static const canvasGrid = charcoal;
  static const surfaceText = textMuted;
}
