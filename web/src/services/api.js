// Use relative URLs so nginx can proxy to backend
// When running in Docker, nginx will proxy /api/* to area-server:8080
// When running in dev mode, vite.config.js should have proxy configured
const API_URL = import.meta.env.VITE_API_URL || '';

/**
 * Get authorization headers with access token if available
 * @returns {Object} Headers object with Authorization header if token exists
 */
function getAuthHeaders() {
  const accessToken = localStorage.getItem('accessToken');
  const headers = {
    'Content-Type': 'application/json',
  };

  if (accessToken) {
    headers['Authorization'] = `Bearer ${accessToken}`;
  }

  return headers;
}

/**
 * Make an authenticated API request
 * Automatically includes JWT token in Authorization header if available
 * Works for both authenticated and unauthenticated scenarios
 * @param {string} url - API endpoint URL
 * @param {Object} options - Fetch options
 * @returns {Promise<Object>} API response data
 */
async function authenticatedFetch(url, options = {}) {
  const headers = getAuthHeaders();

  const response = await fetch(url, {
    ...options,
    headers: {
      ...headers,
      ...options.headers,
    },
  });

  // Handle 401 Unauthorized - token expired or invalid
  if (response.status === 401) {
    const accessToken = localStorage.getItem('accessToken');

    // Only redirect if user was previously authenticated
    if (accessToken) {
      // Clear authentication data
      localStorage.removeItem('accessToken');
      localStorage.removeItem('refreshToken');
      localStorage.removeItem('user');
      localStorage.removeItem('tokenExpiry');

      // Redirect to login page
      window.location.href = '/login';
      throw new Error('Session expired. Please login again.');
    }

    // If no token was present, let the error propagate normally
    throw new Error('Authentication required');
  }

  if (!response.ok) {
    const errorResult = await response.json().catch(() => ({}));
    if (errorResult.success === false || errorResult.error || errorResult.message) {
      unwrapApiResponse(errorResult); // Will throw with proper message
    }
    throw new Error(`Request failed with status ${response.status}`);
  }

  const result = await response.json();
  return unwrapApiResponse(result);
}

/**
 * Unwraps the API response from backend.
 * Handles both the new ApiResponse<T> wrapper format and legacy formats.
 * Throws error if response indicates failure.
 * @param {Object} response - The API response object
 * @returns {*} The unwrapped data
 * @throws {Error} If response.success is false
 */
function unwrapApiResponse(response) {
  // Handle error responses
  if (response.success === false) {
    // Backend error - throw with detailed message
    throw new Error(response.message || response.error || 'API request failed');
  }

  // Handle new ApiResponse<T> format: { success: true, data: {...} }
  if (response.data !== undefined) {
    return response.data;
  }

  // Handle legacy format where data is at root level: { success: true, areas: [...], stats: {...}, etc }
  // Remove success field and return the rest
  const { success, ...data } = response;
  return data;
}

export const api = {
  /**
   * Get available services from backend API
   * Uses dynamic /api/services endpoint instead of static /about.json
   * @returns {Promise<Array>} List of services in about.json compatible format
   */
  async getAvailableServices() {
    const response = await fetch(`${API_URL}/api/services`);
    if (!response.ok) throw new Error('Failed to fetch available services');
    const result = await response.json();
    const data = unwrapApiResponse(result);

    // Return services array directly (backend returns array of service objects)
    // Format is compatible with about.json: array of { name, actions, reactions }
    return Array.isArray(data) ? data : (data.services || []);
  },

  // Get connected services
  async getConnectedServices() {
    return await authenticatedFetch(`${API_URL}/api/service-connections`);
  },

  // Get Gmail OAuth URL
  async getGmailAuthUrl() {
    const response = await fetch(`${API_URL}/api/services/gmail/auth-url`);
    if (!response.ok) throw new Error('Failed to get Gmail auth URL');
    const result = await response.json();
    const data = unwrapApiResponse(result);
    return data; // { authUrl: "...", state: "..." }
  },

  // Disconnect service
  async disconnectService(connectionId) {
    await authenticatedFetch(`${API_URL}/api/service-connections/${connectionId}`, {
      method: 'DELETE',
    });
  },

  // Refresh service token (Gmail)
  async refreshServiceToken(connectionId) {
    return await authenticatedFetch(`${API_URL}/api/service-connections/${connectionId}/refresh`, {
      method: 'POST',
    });
  },

  // Discord connection methods
  async connectDiscord(botToken, channelId) {
    return await authenticatedFetch(`${API_URL}/api/services/discord/connect`, {
      method: 'POST',
      body: JSON.stringify({ botToken, channelId }),
    });
  },

  async testDiscordConnection(botToken, channelId) {
    return await authenticatedFetch(`${API_URL}/api/services/discord/test`, {
      method: 'POST',
      body: JSON.stringify({ botToken, channelId }),
    });
  },

  // Dashboard statistics
  async getDashboardStats() {
    const data = await authenticatedFetch(`${API_URL}/api/dashboard/stats`);
    // Backend returns { stats: {...} } after unwrapping
    return data.stats || data;
  },

  // Get all areas
  async getAreas(activeOnly = false) {
    const url = activeOnly ? `${API_URL}/api/areas?activeOnly=true` : `${API_URL}/api/areas`;
    const data = await authenticatedFetch(url);
    return data.areas || [];
  },

  // Create a new area (Gmail → Discord)
  async createArea(areaData) {
    return await authenticatedFetch(`${API_URL}/api/areas`, {
      method: 'POST',
      body: JSON.stringify(areaData),
    });
  },

  // Create a new timer area (Timer → Discord)
  async createTimerArea(areaData) {
    return await authenticatedFetch(`${API_URL}/api/areas/timer`, {
      method: 'POST',
      body: JSON.stringify(areaData),
    });
  },

  // Delete an area
  async deleteArea(id) {
    await authenticatedFetch(`${API_URL}/api/areas/${id}`, {
      method: 'DELETE',
    });
  },

  // Toggle area status
  async toggleAreaStatus(id) {
    // First get the current area to know its status
    const currentArea = await this.getArea(id);
    const newStatus = !currentArea.active;

    // Use the correct endpoint: PUT /api/areas/{id}/status
    return await authenticatedFetch(`${API_URL}/api/areas/${id}/status`, {
      method: 'PUT',
      body: JSON.stringify({ active: newStatus }),
    });
  },

  // Get a specific area by ID
  async getArea(id) {
    return await authenticatedFetch(`${API_URL}/api/areas/${id}`);
  },

  // Workflow methods
  async getWorkflows(activeOnly = false) {
    const url = activeOnly ? `${API_URL}/api/workflows?activeOnly=true` : `${API_URL}/api/workflows`;
    const data = await authenticatedFetch(url);
    return data.workflows || [];
  },

  async getWorkflow(id) {
    const data = await authenticatedFetch(`${API_URL}/api/workflows/${id}`);
    return data.workflow;
  },

  async createWorkflow(workflowData) {
    const data = await authenticatedFetch(`${API_URL}/api/workflows`, {
      method: 'POST',
      body: JSON.stringify(workflowData),
    });
    return data.workflow;
  },

  async getWorkflowStats(id) {
    const data = await authenticatedFetch(`${API_URL}/api/workflows/${id}/stats`);
    return data.stats;
  },

  async updateWorkflow(id, workflowData) {
    const data = await authenticatedFetch(`${API_URL}/api/workflows/${id}`, {
      method: 'PUT',
      body: JSON.stringify(workflowData),
    });
    return data.workflow;
  },

  async updateWorkflowStatus(id, active) {
    const data = await authenticatedFetch(`${API_URL}/api/workflows/${id}/status`, {
      method: 'PATCH',
      body: JSON.stringify({ active }),
    });
    return data.workflow;
  },

  async deleteWorkflow(id) {
    await authenticatedFetch(`${API_URL}/api/workflows/${id}`, {
      method: 'DELETE',
    });
  },

  async executeWorkflow(id) {
    return await authenticatedFetch(`${API_URL}/api/workflows/${id}/execute`, {
      method: 'POST',
    });
  },

  async getAvailableNodes() {
    const response = await fetch(`${API_URL}/api/workflows/available-nodes`);
    if (!response.ok) throw new Error('Failed to fetch available nodes');
    const result = await response.json();
    const data = unwrapApiResponse(result);
    return data;
  },

  // Logs methods
  async getLogs(areaId, filters = {}) {
    const queryParams = new URLSearchParams(filters).toString();
    const url = queryParams
      ? `${API_URL}/api/areas/${areaId}/logs?${queryParams}`
      : `${API_URL}/api/areas/${areaId}/logs`;
    return await authenticatedFetch(url);
  },

  // Get all logs across all areas
  async getAllLogs(filters = {}) {
    // First get all areas
    const areasData = await this.getAreas();
    const areas = areasData || [];

    if (areas.length === 0) {
      return { logs: [], total: 0, page: 0, pageSize: 0 };
    }

    // Fetch logs from all areas in parallel
    const logsPromises = areas.map(area =>
      this.getLogs(area.id, filters).catch(err => {
        console.error(`Failed to fetch logs for area ${area.id}:`, err);
        return { logs: [], pagination: { totalElements: 0 } };
      })
    );

    const allLogsResults = await Promise.all(logsPromises);

    // Combine all logs
    let allLogs = [];
    allLogsResults.forEach((result, index) => {
      const areaLogs = result.logs || [];
      // Add area name to each log
      areaLogs.forEach(log => {
        log.areaName = areas[index].name || `Area #${areas[index].id}`;
      });
      allLogs = [...allLogs, ...areaLogs];
    });

    // Sort by execution time (newest first)
    allLogs.sort((a, b) => new Date(b.executedAt) - new Date(a.executedAt));

    // Apply client-side filtering
    let filteredLogs = allLogs;
    if (filters.status && filters.status !== 'ALL') {
      filteredLogs = filteredLogs.filter(log => log.status === filters.status);
    }
    if (filters.fromDate) {
      const fromDate = new Date(filters.fromDate);
      filteredLogs = filteredLogs.filter(log => new Date(log.executedAt) >= fromDate);
    }
    if (filters.toDate) {
      const toDate = new Date(filters.toDate);
      filteredLogs = filteredLogs.filter(log => new Date(log.executedAt) <= toDate);
    }

    // Apply pagination
    const limit = parseInt(filters.limit) || 50;
    const offset = parseInt(filters.offset) || 0;
    const paginatedLogs = filteredLogs.slice(offset, offset + limit);

    return {
      logs: paginatedLogs,
      total: filteredLogs.length,
      page: Math.floor(offset / limit),
      pageSize: limit
    };
  },

  // Authentication methods
  /**
   * Login with email and password
   * @param {Object} credentials - { email, password }
   * @returns {Promise<Object>} AuthResponse with accessToken, refreshToken, user, expiresIn
   */
  async login(credentials) {
    const response = await fetch(`${API_URL}/auth/login`, {
      method: 'POST',
      headers: {
        'Content-Type': 'application/json',
      },
      body: JSON.stringify(credentials),
    });
    if (!response.ok) {
      const errorResult = await response.json();
      unwrapApiResponse(errorResult); // Will throw with proper message
    }
    const result = await response.json();
    const data = unwrapApiResponse(result);
    return data;
  },

  /**
   * Register a new user account
   * @param {Object} userData - { email, username, password, fullName }
   * @returns {Promise<Object>} AuthResponse with accessToken, refreshToken, user, expiresIn
   */
  async register(userData) {
    const response = await fetch(`${API_URL}/auth/register`, {
      method: 'POST',
      headers: {
        'Content-Type': 'application/json',
      },
      body: JSON.stringify(userData),
    });
    if (!response.ok) {
      const errorResult = await response.json();
      unwrapApiResponse(errorResult); // Will throw with proper message
    }
    const result = await response.json();
    const data = unwrapApiResponse(result);
    return data;
  },

  /**
   * Logout by revoking the refresh token
   * @param {Object} logoutData - { refreshToken }
   * @returns {Promise<void>}
   */
  async logout(logoutData) {
    const response = await fetch(`${API_URL}/auth/logout`, {
      method: 'POST',
      headers: {
        'Content-Type': 'application/json',
      },
      body: JSON.stringify(logoutData),
    });
    if (!response.ok) {
      const errorResult = await response.json();
      unwrapApiResponse(errorResult); // Will throw with proper message
    }
  },

  /**
   * Get current authenticated user information
   * Requires valid access token
   * @returns {Promise<Object>} UserResponse with id, email, username, fullName, etc.
   */
  async getCurrentUser() {
    const accessToken = localStorage.getItem('accessToken');
    if (!accessToken) {
      throw new Error('No access token available');
    }

    const response = await fetch(`${API_URL}/auth/me`, {
      method: 'GET',
      headers: {
        'Authorization': `Bearer ${accessToken}`,
      },
    });
    if (!response.ok) {
      const errorResult = await response.json();
      unwrapApiResponse(errorResult); // Will throw with proper message
    }
    const result = await response.json();
    const data = unwrapApiResponse(result);
    return data;
  },

  /**
   * Refresh access token using refresh token
   * @param {Object} refreshData - { refreshToken }
   * @returns {Promise<Object>} AuthResponse with new accessToken
   */
  async refreshToken(refreshData) {
    const response = await fetch(`${API_URL}/auth/refresh`, {
      method: 'POST',
      headers: {
        'Content-Type': 'application/json',
      },
      body: JSON.stringify(refreshData),
    });
    if (!response.ok) {
      const errorResult = await response.json();
      unwrapApiResponse(errorResult); // Will throw with proper message
    }
    const result = await response.json();
    const data = unwrapApiResponse(result);
    return data;
  },

  // Service Discovery API methods (Phase 2)

  /**
   * Get all available services with their actions and reactions
   * @returns {Promise<Array>} List of services with metadata
   */
  async getServices() {
    const response = await fetch(`${API_URL}/api/services`);
    if (!response.ok) throw new Error('Failed to fetch services');
    const result = await response.json();
    const data = unwrapApiResponse(result);
    return Array.isArray(data) ? data : (data.services || []);
  },

  /**
   * Get a specific service by type
   * @param {string} type - Service type (e.g., 'GMAIL', 'DISCORD')
   * @returns {Promise<Object>} Service details
   */
  async getService(type) {
    const response = await fetch(`${API_URL}/api/services/${type}`);
    if (!response.ok) throw new Error(`Failed to fetch service ${type}`);
    const result = await response.json();
    const data = unwrapApiResponse(result);
    return data;
  },

  /**
   * Get actions (triggers) for a specific service
   * @param {string} type - Service type (e.g., 'GMAIL')
   * @returns {Promise<Array>} List of actions
   */
  async getServiceActions(type) {
    const response = await fetch(`${API_URL}/api/services/${type}/actions`);
    if (!response.ok) throw new Error(`Failed to fetch actions for ${type}`);
    const result = await response.json();
    const data = unwrapApiResponse(result);
    return Array.isArray(data) ? data : (data.actions || []);
  },

  /**
   * Get reactions (actions) for a specific service
   * @param {string} type - Service type (e.g., 'DISCORD')
   * @returns {Promise<Array>} List of reactions
   */
  async getServiceReactions(type) {
    const response = await fetch(`${API_URL}/api/services/${type}/reactions`);
    if (!response.ok) throw new Error(`Failed to fetch reactions for ${type}`);
    const result = await response.json();
    const data = unwrapApiResponse(result);
    return Array.isArray(data) ? data : (data.reactions || []);
  },

  /**
   * Get services that have actions (triggers)
   * @returns {Promise<Array>} List of services with actions
   */
  async getServicesWithActions() {
    const response = await fetch(`${API_URL}/api/services?hasActions=true`);
    if (!response.ok) throw new Error('Failed to fetch services with actions');
    const result = await response.json();
    const data = unwrapApiResponse(result);
    return Array.isArray(data) ? data : (data.services || []);
  },

  /**
   * Get services that have reactions (actions)
   * @returns {Promise<Array>} List of services with reactions
   */
  async getServicesWithReactions() {
    const response = await fetch(`${API_URL}/api/services?hasReactions=true`);
    if (!response.ok) throw new Error('Failed to fetch services with reactions');
    const result = await response.json();
    const data = unwrapApiResponse(result);
    return Array.isArray(data) ? data : (data.services || []);
  },

  /**
   * Get service statistics
   * @returns {Promise<Object>} Service statistics
   */
  async getServiceStats() {
    const response = await fetch(`${API_URL}/api/services/stats`);
    if (!response.ok) throw new Error('Failed to fetch service stats');
    const result = await response.json();
    const data = unwrapApiResponse(result);
    return data;
  },
};
