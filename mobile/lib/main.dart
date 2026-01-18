import 'package:flutter/material.dart';
import 'package:flutter_bloc/flutter_bloc.dart';

import 'app.dart';
import 'blocs/auth/auth.dart';
import 'blocs/workflow/workflow.dart';
import 'services/api_client.dart';
import 'services/auth_service.dart';
import 'services/token_storage.dart';
import 'services/workflow_service.dart';

void main() {
  WidgetsFlutterBinding.ensureInitialized();

  final tokenStorage = TokenStorage();
  final apiClient = ApiClient(tokenStorage: tokenStorage);
  final authService = AuthService(
    apiClient: apiClient,
    tokenStorage: tokenStorage,
  );
  final workflowService = WorkflowService(apiClient: apiClient);

  runApp(
    MultiRepositoryProvider(
      providers: [
        RepositoryProvider.value(value: tokenStorage),
        RepositoryProvider.value(value: apiClient),
        RepositoryProvider.value(value: authService),
        RepositoryProvider.value(value: workflowService),
      ],
      child: MultiBlocProvider(
        providers: [
          BlocProvider(
            create: (context) => AuthBloc(authService: authService)
              ..add(const AuthCheckRequested()),
          ),
          BlocProvider(
            create: (context) => WorkflowBloc(workflowService: workflowService),
          ),
        ],
        child: const AreaApp(),
      ),
    ),
  );
}
