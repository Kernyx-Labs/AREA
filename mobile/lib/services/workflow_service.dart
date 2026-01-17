import 'package:dio/dio.dart';
import '../models/workflow.dart';
import 'api_client.dart';
import 'api_config.dart';

class WorkflowService {
  final ApiClient _apiClient;

  WorkflowService({required ApiClient apiClient}) : _apiClient = apiClient;

  Future<List<Workflow>> getWorkflows({bool activeOnly = false}) async {
    try {
      final response = await _apiClient.get(
        ApiConfig.workflows,
        queryParameters: {'activeOnly': activeOnly},
      );

      if (response.data['success'] == true) {
        final data = response.data['data'];
        final workflows = (data['workflows'] as List<dynamic>)
            .map((e) => Workflow.fromJson(e as Map<String, dynamic>))
            .toList();
        return workflows;
      } else {
        throw ApiException(
          message: response.data['message'] ?? 'Failed to fetch workflows',
        );
      }
    } on DioException catch (e) {
      throw ApiException.fromDioError(e);
    }
  }

  Future<Workflow> getWorkflow(int id) async {
    try {
      final response = await _apiClient.get('${ApiConfig.workflows}/$id');

      if (response.data['success'] == true) {
        return Workflow.fromJson(response.data['data'] as Map<String, dynamic>);
      } else {
        throw ApiException(
          message: response.data['message'] ?? 'Failed to fetch workflow',
        );
      }
    } on DioException catch (e) {
      throw ApiException.fromDioError(e);
    }
  }

  Future<Workflow> createWorkflow(CreateWorkflowRequest request) async {
    try {
      final response = await _apiClient.post(
        ApiConfig.workflows,
        data: request.toJson(),
      );

      if (response.data['success'] == true) {
        return Workflow.fromJson(response.data['data'] as Map<String, dynamic>);
      } else {
        throw ApiException(
          message: response.data['message'] ?? 'Failed to create workflow',
        );
      }
    } on DioException catch (e) {
      throw ApiException.fromDioError(e);
    }
  }

  Future<Workflow> updateWorkflow(int id, Map<String, dynamic> updates) async {
    try {
      final response = await _apiClient.put(
        '${ApiConfig.workflows}/$id',
        data: updates,
      );

      if (response.data['success'] == true) {
        return Workflow.fromJson(response.data['data'] as Map<String, dynamic>);
      } else {
        throw ApiException(
          message: response.data['message'] ?? 'Failed to update workflow',
        );
      }
    } on DioException catch (e) {
      throw ApiException.fromDioError(e);
    }
  }

  Future<Workflow> updateWorkflowStatus(int id, bool active) async {
    try {
      final response = await _apiClient.patch(
        '${ApiConfig.workflows}/$id/status',
        data: {'active': active},
      );

      if (response.data['success'] == true) {
        return Workflow.fromJson(response.data['data'] as Map<String, dynamic>);
      } else {
        throw ApiException(
          message: response.data['message'] ?? 'Failed to update workflow status',
        );
      }
    } on DioException catch (e) {
      throw ApiException.fromDioError(e);
    }
  }

  Future<void> deleteWorkflow(int id) async {
    try {
      final response = await _apiClient.delete('${ApiConfig.workflows}/$id');

      if (response.data['success'] != true) {
        throw ApiException(
          message: response.data['message'] ?? 'Failed to delete workflow',
        );
      }
    } on DioException catch (e) {
      throw ApiException.fromDioError(e);
    }
  }

  Future<void> executeWorkflow(int id) async {
    try {
      final response = await _apiClient.post('${ApiConfig.workflows}/$id/execute');

      if (response.data['success'] != true) {
        throw ApiException(
          message: response.data['message'] ?? 'Failed to execute workflow',
        );
      }
    } on DioException catch (e) {
      throw ApiException.fromDioError(e);
    }
  }

  Future<Map<String, dynamic>> getAvailableNodes() async {
    try {
      final response = await _apiClient.get(ApiConfig.availableNodes);

      if (response.data['success'] == true) {
        return response.data['data'] as Map<String, dynamic>;
      } else {
        throw ApiException(
          message: response.data['message'] ?? 'Failed to fetch available nodes',
        );
      }
    } on DioException catch (e) {
      throw ApiException.fromDioError(e);
    }
  }

  Future<WorkflowStats> getWorkflowStats(int id) async {
    try {
      final response = await _apiClient.get('${ApiConfig.workflows}/$id/stats');

      if (response.data['success'] == true) {
        return WorkflowStats.fromJson(response.data['data'] as Map<String, dynamic>);
      } else {
        throw ApiException(
          message: response.data['message'] ?? 'Failed to fetch workflow stats',
        );
      }
    } on DioException catch (e) {
      throw ApiException.fromDioError(e);
    }
  }
}
