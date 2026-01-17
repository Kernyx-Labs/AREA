import 'package:flutter/material.dart';
import '../constants/palette.dart';

class SplashScreen extends StatelessWidget {
  const SplashScreen({super.key});

  @override
  Widget build(BuildContext context) {
    return Scaffold(
      backgroundColor: AppPalette.background,
      body: Center(
        child: Column(
          mainAxisAlignment: MainAxisAlignment.center,
          children: [
            Container(
              width: 100,
              height: 100,
              decoration: BoxDecoration(
                color: AppPalette.primary,
                borderRadius: BorderRadius.circular(24),
              ),
              child: const Icon(
                Icons.hub,
                size: 60,
                color: Colors.white,
              ),
            ),
            const SizedBox(height: 24),
            const Text(
              'AREA',
              style: TextStyle(
                fontSize: 32,
                fontWeight: FontWeight.bold,
                color: AppPalette.dark,
              ),
            ),
            const SizedBox(height: 48),
            const CircularProgressIndicator(
              valueColor: AlwaysStoppedAnimation<Color>(AppPalette.primary),
            ),
          ],
        ),
      ),
    );
  }
}
