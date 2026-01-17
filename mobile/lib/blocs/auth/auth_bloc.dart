import 'package:flutter_bloc/flutter_bloc.dart';
import '../../models/auth_models.dart';
import '../../services/auth_service.dart';
import '../../services/api_client.dart';
import 'auth_event.dart';
import 'auth_state.dart';

class AuthBloc extends Bloc<AuthEvent, AuthState> {
  final AuthService _authService;

  AuthBloc({required AuthService authService})
      : _authService = authService,
        super(const AuthState.initial()) {
    on<AuthCheckRequested>(_onAuthCheckRequested);
    on<AuthLoginRequested>(_onLoginRequested);
    on<AuthRegisterRequested>(_onRegisterRequested);
    on<AuthLogoutRequested>(_onLogoutRequested);
  }

  Future<void> _onAuthCheckRequested(
    AuthCheckRequested event,
    Emitter<AuthState> emit,
  ) async {
    emit(const AuthState.loading());
    try {
      final isAuthenticated = await _authService.isAuthenticated();
      if (isAuthenticated) {
        final user = await _authService.getCurrentUser();
        emit(AuthState.authenticated(user));
      } else {
        emit(const AuthState.unauthenticated());
      }
    } on ApiException catch (e) {
      emit(const AuthState.unauthenticated());
    } catch (e) {
      emit(const AuthState.unauthenticated());
    }
  }

  Future<void> _onLoginRequested(
    AuthLoginRequested event,
    Emitter<AuthState> emit,
  ) async {
    emit(const AuthState.loading());
    try {
      final response = await _authService.login(
        LoginRequest(email: event.email, password: event.password),
      );
      emit(AuthState.authenticated(response.user));
    } on ApiException catch (e) {
      emit(AuthState.error(e.message));
    } catch (e) {
      emit(AuthState.error('An unexpected error occurred'));
    }
  }

  Future<void> _onRegisterRequested(
    AuthRegisterRequested event,
    Emitter<AuthState> emit,
  ) async {
    emit(const AuthState.loading());
    try {
      final response = await _authService.register(
        RegisterRequest(
          email: event.email,
          username: event.username,
          password: event.password,
          fullName: event.fullName,
        ),
      );
      emit(AuthState.authenticated(response.user));
    } on ApiException catch (e) {
      emit(AuthState.error(e.message));
    } catch (e) {
      emit(AuthState.error('An unexpected error occurred'));
    }
  }

  Future<void> _onLogoutRequested(
    AuthLogoutRequested event,
    Emitter<AuthState> emit,
  ) async {
    emit(const AuthState.loading());
    try {
      await _authService.logout();
    } catch (_) {
      // Ignore logout errors
    }
    emit(const AuthState.unauthenticated());
  }
}
