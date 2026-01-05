import 'dart:convert';
import 'package:http/http.dart' as http;

class ApiService {
  // Base URL - configure based on environment
  // For local development: http://10.0.2.2:8080 (Android emulator)
  // For physical device on same network: http://192.168.x.x:8080
  static const String baseUrl = 'http://10.0.2.2:8080';

  // Unwraps the API response from backend
  // Handles both the new ApiResponse<T> wrapper format and legacy formats
  static dynamic _unwrapApiResponse(Map<String, dynamic> response) {
    // Handle error responses
    if (response['success'] == false) {
      throw Exception(response['message'] ?? response['error'] ?? 'API request failed');
    }

    // Handle new ApiResponse<T> format: { success: true, data: {...} }
    if (response.containsKey('data')) {
      return response['data'];
    }

    // Handle legacy format where data is at root level
    final data = Map<String, dynamic>.from(response);
    data.remove('success');
    return data;
  }

  // Get available services from backend API
  static Future<List<dynamic>> getAvailableServices() async {
    final response = await http.get(Uri.parse('$baseUrl/api/services'));
    if (response.statusCode != 200) {
      throw Exception('Failed to fetch available services');
    }

    final result = json.decode(response.body);
    final data = _unwrapApiResponse(result);

    return data is List ? data : (data['services'] ?? []);
  }

  // Get connected services
  static Future<List<dynamic>> getConnectedServices() async {
    final response = await http.get(Uri.parse('$baseUrl/api/service-connections'));
    if (response.statusCode != 200) {
      throw Exception('Failed to fetch connected services');
    }

    final result = json.decode(response.body);

    // Handle plain array response (legacy format)
    if (result is List) {
      return result;
    }

    final data = _unwrapApiResponse(result);
    return data is List ? data : (data['connections'] ?? []);
  }

  // Get Gmail OAuth URL
  static Future<Map<String, dynamic>> getGmailAuthUrl() async {
    final response = await http.get(Uri.parse('$baseUrl/api/services/gmail/auth-url'));
    if (response.statusCode != 200) {
      throw Exception('Failed to get Gmail auth URL');
    }

    final result = json.decode(response.body);
    return _unwrapApiResponse(result);
  }

  // Disconnect service
  static Future<void> disconnectService(int connectionId) async {
    final response = await http.delete(
      Uri.parse('$baseUrl/api/service-connections/$connectionId'),
    );
    if (response.statusCode != 200) {
      throw Exception('Failed to disconnect service');
    }
  }

  // Refresh service token (Gmail)
  static Future<Map<String, dynamic>> refreshServiceToken(int connectionId) async {
    final response = await http.post(
      Uri.parse('$baseUrl/api/service-connections/$connectionId/refresh'),
    );

    if (response.statusCode != 200) {
      final errorResult = json.decode(response.body);
      _unwrapApiResponse(errorResult); // Will throw with proper message
    }

    final result = json.decode(response.body);
    return _unwrapApiResponse(result);
  }

  // Discord connection methods
  static Future<Map<String, dynamic>> connectDiscord(String botToken, String channelId) async {
    final response = await http.post(
      Uri.parse('$baseUrl/api/services/discord/connect'),
      headers: {'Content-Type': 'application/json'},
      body: json.encode({
        'botToken': botToken,
        'channelId': channelId,
      }),
    );

    if (response.statusCode != 200) {
      final errorResult = json.decode(response.body);
      _unwrapApiResponse(errorResult); // Will throw with proper message
    }

    final result = json.decode(response.body);
    return _unwrapApiResponse(result);
  }

  static Future<Map<String, dynamic>> testDiscordConnection(String botToken, String channelId) async {
    final response = await http.post(
      Uri.parse('$baseUrl/api/services/discord/test'),
      headers: {'Content-Type': 'application/json'},
      body: json.encode({
        'botToken': botToken,
        'channelId': channelId,
      }),
    );

    if (response.statusCode != 200) {
      final errorResult = json.decode(response.body);
      _unwrapApiResponse(errorResult); // Will throw with proper message
    }

    final result = json.decode(response.body);
    return _unwrapApiResponse(result);
  }

  // Dashboard statistics
  static Future<Map<String, dynamic>> getDashboardStats() async {
    final response = await http.get(Uri.parse('$baseUrl/api/dashboard/stats'));
    if (response.statusCode != 200) {
      throw Exception('Failed to fetch dashboard statistics');
    }

    final result = json.decode(response.body);
    final data = _unwrapApiResponse(result);
    return data['stats'] ?? data;
  }

  // Get all areas
  static Future<List<dynamic>> getAreas({bool activeOnly = false}) async {
    final url = activeOnly ? '$baseUrl/api/areas?activeOnly=true' : '$baseUrl/api/areas';
    final response = await http.get(Uri.parse(url));
    if (response.statusCode != 200) {
      throw Exception('Failed to fetch areas');
    }

    final result = json.decode(response.body);
    final data = _unwrapApiResponse(result);
    return data['areas'] ?? [];
  }

  // Create a new area (Gmail → Discord)
  static Future<Map<String, dynamic>> createArea(Map<String, dynamic> areaData) async {
    final response = await http.post(
      Uri.parse('$baseUrl/api/areas'),
      headers: {'Content-Type': 'application/json'},
      body: json.encode(areaData),
    );

    if (response.statusCode != 200) {
      final errorResult = json.decode(response.body);
      _unwrapApiResponse(errorResult); // Will throw with proper message
    }

    final result = json.decode(response.body);
    return _unwrapApiResponse(result);
  }

  // Delete an area
  static Future<void> deleteArea(int id) async {
    final response = await http.delete(Uri.parse('$baseUrl/api/areas/$id'));
    if (response.statusCode != 200) {
      throw Exception('Failed to delete area');
    }
  }

  // Toggle area status
  static Future<Map<String, dynamic>> toggleAreaStatus(int id) async {
    final response = await http.patch(Uri.parse('$baseUrl/api/areas/$id/toggle'));

    if (response.statusCode != 200) {
      final errorResult = json.decode(response.body);
      _unwrapApiResponse(errorResult); // Will throw with proper message
    }

    final result = json.decode(response.body);
    return _unwrapApiResponse(result);
  }

  // Workflow methods
  static Future<List<dynamic>> getWorkflows({bool activeOnly = false}) async {
    final url = activeOnly ? '$baseUrl/api/workflows?activeOnly=true' : '$baseUrl/api/workflows';
    final response = await http.get(Uri.parse(url));
    if (response.statusCode != 200) {
      throw Exception('Failed to fetch workflows');
    }

    final result = json.decode(response.body);
    final data = _unwrapApiResponse(result);
    return data['workflows'] ?? [];
  }

  static Future<Map<String, dynamic>> getWorkflow(int id) async {
    final response = await http.get(Uri.parse('$baseUrl/api/workflows/$id'));
    if (response.statusCode != 200) {
      throw Exception('Failed to fetch workflow');
    }

    final result = json.decode(response.body);
    final data = _unwrapApiResponse(result);
    return data['workflow'];
  }

  static Future<Map<String, dynamic>> createWorkflow(Map<String, dynamic> workflowData) async {
    final response = await http.post(
      Uri.parse('$baseUrl/api/workflows'),
      headers: {'Content-Type': 'application/json'},
      body: json.encode(workflowData),
    );

    if (response.statusCode != 200) {
      final errorResult = json.decode(response.body);
      _unwrapApiResponse(errorResult); // Will throw with proper message
    }

    final result = json.decode(response.body);
    final data = _unwrapApiResponse(result);
    return data['workflow'];
  }

  static Future<Map<String, dynamic>> getWorkflowStats(int id) async {
    final response = await http.get(Uri.parse('$baseUrl/api/workflows/$id/stats'));
    if (response.statusCode != 200) {
      throw Exception('Failed to fetch workflow stats');
    }

    final result = json.decode(response.body);
    final data = _unwrapApiResponse(result);
    return data['stats'] ?? data;
  }

  static Future<Map<String, dynamic>> updateWorkflow(int id, Map<String, dynamic> workflowData) async {
    final response = await http.put(
      Uri.parse('$baseUrl/api/workflows/$id'),
      headers: {'Content-Type': 'application/json'},
      body: json.encode(workflowData),
    );

    if (response.statusCode != 200) {
      final errorResult = json.decode(response.body);
      _unwrapApiResponse(errorResult); // Will throw with proper message
    }

    final result = json.decode(response.body);
    final data = _unwrapApiResponse(result);
    return data['workflow'];
  }

  static Future<Map<String, dynamic>> updateWorkflowStatus(int id, bool active) async {
    final response = await http.patch(
      Uri.parse('$baseUrl/api/workflows/$id/status'),
      headers: {'Content-Type': 'application/json'},
      body: json.encode({'active': active}),
    );

    if (response.statusCode != 200) {
      final errorResult = json.decode(response.body);
      _unwrapApiResponse(errorResult); // Will throw with proper message
    }

    final result = json.decode(response.body);
    final data = _unwrapApiResponse(result);
    return data['workflow'];
  }

  static Future<void> deleteWorkflow(int id) async {
    final response = await http.delete(Uri.parse('$baseUrl/api/workflows/$id'));
    if (response.statusCode != 200) {
      throw Exception('Failed to delete workflow');
    }
  }

  static Future<Map<String, dynamic>> executeWorkflow(int id) async {
    final response = await http.post(Uri.parse('$baseUrl/api/workflows/$id/execute'));

    if (response.statusCode != 200) {
      final errorResult = json.decode(response.body);
      _unwrapApiResponse(errorResult); // Will throw with proper message
    }

    final result = json.decode(response.body);
    return _unwrapApiResponse(result);
  }

  static Future<Map<String, dynamic>> getAvailableNodes() async {
    final response = await http.get(Uri.parse('$baseUrl/api/workflows/available-nodes'));
    if (response.statusCode != 200) {
      throw Exception('Failed to fetch available nodes');
    }

    final result = json.decode(response.body);
    return _unwrapApiResponse(result);
  }

  // Logs methods
  static Future<Map<String, dynamic>> getLogs({Map<String, dynamic>? filters}) async {
    var uri = Uri.parse('$baseUrl/api/logs');
    if (filters != null && filters.isNotEmpty) {
      uri = uri.replace(queryParameters: filters.map((k, v) => MapEntry(k, v.toString())));
    }

    final response = await http.get(uri);
    if (response.statusCode != 200) {
      throw Exception('Failed to fetch logs');
    }

    final result = json.decode(response.body);
    return _unwrapApiResponse(result);
  }

  // Service Discovery API methods

  // Get all available services with their actions and reactions
  static Future<List<dynamic>> getServices() async {
    final response = await http.get(Uri.parse('$baseUrl/api/services'));
    if (response.statusCode != 200) {
      throw Exception('Failed to fetch services');
    }

    final result = json.decode(response.body);
    final data = _unwrapApiResponse(result);
    return data is List ? data : (data['services'] ?? []);
  }

  // Get a specific service by type
  static Future<Map<String, dynamic>> getService(String type) async {
    final response = await http.get(Uri.parse('$baseUrl/api/services/$type'));
    if (response.statusCode != 200) {
      throw Exception('Failed to fetch service $type');
    }

    final result = json.decode(response.body);
    return _unwrapApiResponse(result);
  }

  // Get actions (triggers) for a specific service
  static Future<List<dynamic>> getServiceActions(String type) async {
    final response = await http.get(Uri.parse('$baseUrl/api/services/$type/actions'));
    if (response.statusCode != 200) {
      throw Exception('Failed to fetch actions for $type');
    }

    final result = json.decode(response.body);
    final data = _unwrapApiResponse(result);
    return data is List ? data : (data['actions'] ?? []);
  }

  // Get reactions (actions) for a specific service
  static Future<List<dynamic>> getServiceReactions(String type) async {
    final response = await http.get(Uri.parse('$baseUrl/api/services/$type/reactions'));
    if (response.statusCode != 200) {
      throw Exception('Failed to fetch reactions for $type');
    }

    final result = json.decode(response.body);
    final data = _unwrapApiResponse(result);
    return data is List ? data : (data['reactions'] ?? []);
  }

  // Get services that have actions (triggers)
  static Future<List<dynamic>> getServicesWithActions() async {
    final response = await http.get(Uri.parse('$baseUrl/api/services?hasActions=true'));
    if (response.statusCode != 200) {
      throw Exception('Failed to fetch services with actions');
    }

    final result = json.decode(response.body);
    final data = _unwrapApiResponse(result);
    return data is List ? data : (data['services'] ?? []);
  }

  // Get services that have reactions (actions)
  static Future<List<dynamic>> getServicesWithReactions() async {
    final response = await http.get(Uri.parse('$baseUrl/api/services?hasReactions=true'));
    if (response.statusCode != 200) {
      throw Exception('Failed to fetch services with reactions');
    }

    final result = json.decode(response.body);
    final data = _unwrapApiResponse(result);
    return data is List ? data : (data['services'] ?? []);
  }

  // Get service statistics
  static Future<Map<String, dynamic>> getServiceStats() async {
    final response = await http.get(Uri.parse('$baseUrl/api/services/stats'));
    if (response.statusCode != 200) {
      throw Exception('Failed to fetch service stats');
    }

    final result = json.decode(response.body);
    return _unwrapApiResponse(result);
  }
}
