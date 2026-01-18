class ApiConfig {
  static const String baseUrl = 'http://10.0.2.2:8080'; // Android emulator localhost
  static const String iosBaseUrl = 'http://localhost:8080'; // iOS simulator

  static const Duration connectTimeout = Duration(seconds: 30);
  static const Duration receiveTimeout = Duration(seconds: 30);

  // Auth endpoints
  static const String login = '/auth/login';
  static const String register = '/auth/register';
  static const String refresh = '/auth/refresh';
  static const String logout = '/auth/logout';
  static const String me = '/auth/me';

  // Workflow endpoints
  static const String workflows = '/api/workflows';
  static const String availableNodes = '/api/workflows/available-nodes';

  // Service endpoints
  static const String services = '/api/services';
  static const String serviceConnections = '/api/service-connections';

  // Dashboard
  static const String dashboardStats = '/api/dashboard/stats';
}
