import 'package:dio/dio.dart';
import '../models/auth_models.dart';
import '../models/user.dart';
import 'api_client.dart';
import 'api_config.dart';
import 'token_storage.dart';

class AuthService {
  final ApiClient _apiClient;
  final TokenStorage _tokenStorage;

  AuthService({
    required ApiClient apiClient,
    required TokenStorage tokenStorage,
  })  : _apiClient = apiClient,
        _tokenStorage = tokenStorage;

  Future<AuthResponse> login(LoginRequest request) async {
    try {
      final response = await _apiClient.post(
        ApiConfig.login,
        data: request.toJson(),
      );

      if (response.data['success'] == true) {
        final authResponse = AuthResponse.fromJson(response.data['data']);
        await _saveAuthData(authResponse);
        return authResponse;
      } else {
        throw ApiException(
          message: response.data['message'] ?? 'Login failed',
        );
      }
    } on DioException catch (e) {
      throw ApiException.fromDioError(e);
    }
  }

  Future<AuthResponse> register(RegisterRequest request) async {
    try {
      final response = await _apiClient.post(
        ApiConfig.register,
        data: request.toJson(),
      );

      if (response.data['success'] == true) {
        final authResponse = AuthResponse.fromJson(response.data['data']);
        await _saveAuthData(authResponse);
        return authResponse;
      } else {
        throw ApiException(
          message: response.data['message'] ?? 'Registration failed',
        );
      }
    } on DioException catch (e) {
      throw ApiException.fromDioError(e);
    }
  }

  Future<User> getCurrentUser() async {
    try {
      final response = await _apiClient.get(ApiConfig.me);

      if (response.data['success'] == true) {
        return User.fromJson(response.data['data']);
      } else {
        throw ApiException(
          message: response.data['message'] ?? 'Failed to get user',
        );
      }
    } on DioException catch (e) {
      throw ApiException.fromDioError(e);
    }
  }

  Future<void> logout() async {
    try {
      final refreshToken = await _tokenStorage.getRefreshToken();
      if (refreshToken != null) {
        await _apiClient.post(
          ApiConfig.logout,
          data: {'refreshToken': refreshToken},
        );
      }
    } catch (_) {
      // Ignore logout errors, just clear local tokens
    } finally {
      await _tokenStorage.clearAll();
    }
  }

  Future<bool> isAuthenticated() async {
    return _tokenStorage.hasTokens();
  }

  Future<void> _saveAuthData(AuthResponse authResponse) async {
    await _tokenStorage.saveTokens(
      accessToken: authResponse.accessToken,
      refreshToken: authResponse.refreshToken,
    );
    await _tokenStorage.saveUserId(authResponse.user.id);
  }
}
