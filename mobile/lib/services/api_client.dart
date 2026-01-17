import 'dart:io';
import 'package:dio/dio.dart';
import 'api_config.dart';
import 'token_storage.dart';

class ApiClient {
  late final Dio _dio;
  final TokenStorage _tokenStorage;

  ApiClient({required TokenStorage tokenStorage}) : _tokenStorage = tokenStorage {
    final baseUrl = Platform.isAndroid ? ApiConfig.baseUrl : ApiConfig.iosBaseUrl;

    _dio = Dio(
      BaseOptions(
        baseUrl: baseUrl,
        connectTimeout: ApiConfig.connectTimeout,
        receiveTimeout: ApiConfig.receiveTimeout,
        headers: {
          'Content-Type': 'application/json',
          'Accept': 'application/json',
        },
      ),
    );

    _dio.interceptors.add(_AuthInterceptor(_tokenStorage, _dio));
    _dio.interceptors.add(LogInterceptor(
      requestBody: true,
      responseBody: true,
      error: true,
    ));
  }

  Dio get dio => _dio;

  Future<Response<T>> get<T>(
    String path, {
    Map<String, dynamic>? queryParameters,
    Options? options,
  }) {
    return _dio.get<T>(path, queryParameters: queryParameters, options: options);
  }

  Future<Response<T>> post<T>(
    String path, {
    dynamic data,
    Map<String, dynamic>? queryParameters,
    Options? options,
  }) {
    return _dio.post<T>(path, data: data, queryParameters: queryParameters, options: options);
  }

  Future<Response<T>> put<T>(
    String path, {
    dynamic data,
    Map<String, dynamic>? queryParameters,
    Options? options,
  }) {
    return _dio.put<T>(path, data: data, queryParameters: queryParameters, options: options);
  }

  Future<Response<T>> patch<T>(
    String path, {
    dynamic data,
    Map<String, dynamic>? queryParameters,
    Options? options,
  }) {
    return _dio.patch<T>(path, data: data, queryParameters: queryParameters, options: options);
  }

  Future<Response<T>> delete<T>(
    String path, {
    dynamic data,
    Map<String, dynamic>? queryParameters,
    Options? options,
  }) {
    return _dio.delete<T>(path, data: data, queryParameters: queryParameters, options: options);
  }
}

class _AuthInterceptor extends Interceptor {
  final TokenStorage _tokenStorage;
  final Dio _dio;
  bool _isRefreshing = false;

  _AuthInterceptor(this._tokenStorage, this._dio);

  @override
  void onRequest(RequestOptions options, RequestInterceptorHandler handler) async {
    // Skip auth header for auth endpoints (except /auth/me)
    final isAuthEndpoint = options.path.startsWith('/auth') && !options.path.endsWith('/me');

    if (!isAuthEndpoint) {
      final token = await _tokenStorage.getAccessToken();
      if (token != null) {
        options.headers['Authorization'] = 'Bearer $token';
      }
    }
    handler.next(options);
  }

  @override
  void onError(DioException err, ErrorInterceptorHandler handler) async {
    if (err.response?.statusCode == 401 && !_isRefreshing) {
      _isRefreshing = true;
      try {
        final refreshToken = await _tokenStorage.getRefreshToken();
        if (refreshToken != null) {
          final baseUrl = Platform.isAndroid ? ApiConfig.baseUrl : ApiConfig.iosBaseUrl;
          final response = await Dio().post(
            '$baseUrl${ApiConfig.refresh}',
            data: {'refreshToken': refreshToken},
          );

          if (response.statusCode == 200 && response.data['success'] == true) {
            final data = response.data['data'];
            await _tokenStorage.saveTokens(
              accessToken: data['accessToken'],
              refreshToken: data['refreshToken'],
            );

            // Retry the original request
            final opts = err.requestOptions;
            opts.headers['Authorization'] = 'Bearer ${data['accessToken']}';
            final retryResponse = await _dio.fetch(opts);
            _isRefreshing = false;
            return handler.resolve(retryResponse);
          }
        }
      } catch (e) {
        // Token refresh failed, clear tokens
        await _tokenStorage.clearAll();
      }
      _isRefreshing = false;
    }
    handler.next(err);
  }
}

class ApiException implements Exception {
  final String message;
  final int? statusCode;
  final dynamic data;

  ApiException({required this.message, this.statusCode, this.data});

  factory ApiException.fromDioError(DioException error) {
    String message = 'An unexpected error occurred';

    if (error.response?.data != null && error.response?.data is Map) {
      message = error.response?.data['message'] ?? message;
    } else {
      switch (error.type) {
        case DioExceptionType.connectionTimeout:
        case DioExceptionType.sendTimeout:
        case DioExceptionType.receiveTimeout:
          message = 'Connection timeout. Please check your internet connection.';
          break;
        case DioExceptionType.connectionError:
          message = 'Unable to connect to server. Please check your internet connection.';
          break;
        case DioExceptionType.badResponse:
          message = 'Server error: ${error.response?.statusCode}';
          break;
        default:
          message = error.message ?? message;
      }
    }

    return ApiException(
      message: message,
      statusCode: error.response?.statusCode,
      data: error.response?.data,
    );
  }

  @override
  String toString() => message;
}
