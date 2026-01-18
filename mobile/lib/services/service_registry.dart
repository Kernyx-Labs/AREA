import 'package:dio/dio.dart';
import 'api_client.dart';
import 'api_config.dart';

class ServiceRegistry {
  final ApiClient _apiClient;

  ServiceRegistry({required ApiClient apiClient}) : _apiClient = apiClient;

  Future<List<ServiceInfo>> getServices() async {
    try {
      final response = await _apiClient.get(ApiConfig.services);

      if (response.data['success'] == true) {
        final data = response.data['data'] as List<dynamic>;
        return data.map((e) => ServiceInfo.fromJson(e as Map<String, dynamic>)).toList();
      } else {
        throw ApiException(
          message: response.data['message'] ?? 'Failed to fetch services',
        );
      }
    } on DioException catch (e) {
      throw ApiException.fromDioError(e);
    }
  }

  Future<List<ServiceConnection>> getConnections() async {
    try {
      final response = await _apiClient.get(ApiConfig.serviceConnections);

      if (response.data['success'] == true) {
        final data = response.data['data'] as List<dynamic>;
        return data.map((e) => ServiceConnection.fromJson(e as Map<String, dynamic>)).toList();
      } else {
        throw ApiException(
          message: response.data['message'] ?? 'Failed to fetch connections',
        );
      }
    } on DioException catch (e) {
      throw ApiException.fromDioError(e);
    }
  }

  Future<String> getOAuthUrl(String serviceType) async {
    try {
      final response = await _apiClient.get(
        '${ApiConfig.services}/${serviceType.toLowerCase()}/auth-url',
      );

      if (response.data['success'] == true) {
        return response.data['data']['authUrl'] as String;
      } else {
        throw ApiException(
          message: response.data['message'] ?? 'Failed to get auth URL',
        );
      }
    } on DioException catch (e) {
      throw ApiException.fromDioError(e);
    }
  }

  Future<void> deleteConnection(int connectionId) async {
    try {
      await _apiClient.delete('${ApiConfig.serviceConnections}/$connectionId');
    } on DioException catch (e) {
      throw ApiException.fromDioError(e);
    }
  }
}

class ServiceInfo {
  final String type;
  final String name;
  final String? description;
  final bool requiresAuthentication;
  final int actionCount;
  final int reactionCount;

  const ServiceInfo({
    required this.type,
    required this.name,
    this.description,
    required this.requiresAuthentication,
    required this.actionCount,
    required this.reactionCount,
  });

  factory ServiceInfo.fromJson(Map<String, dynamic> json) {
    return ServiceInfo(
      type: json['type'] as String,
      name: json['name'] as String,
      description: json['description'] as String?,
      requiresAuthentication: json['requiresAuthentication'] as bool? ?? false,
      actionCount: json['actionCount'] as int? ?? 0,
      reactionCount: json['reactionCount'] as int? ?? 0,
    );
  }
}

class ServiceConnection {
  final int id;
  final String type;
  final String? metadata;
  final DateTime? tokenExpiresAt;

  const ServiceConnection({
    required this.id,
    required this.type,
    this.metadata,
    this.tokenExpiresAt,
  });

  factory ServiceConnection.fromJson(Map<String, dynamic> json) {
    return ServiceConnection(
      id: json['id'] as int,
      type: json['type'] as String,
      metadata: json['metadata'] as String?,
      tokenExpiresAt: json['tokenExpiresAt'] != null
          ? DateTime.parse(json['tokenExpiresAt'] as String)
          : null,
    );
  }
}
