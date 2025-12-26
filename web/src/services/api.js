// Use relative URLs so nginx can proxy to backend
// When running in Docker, nginx will proxy /api/* to area-server:8080
// When running in dev mode, vite.config.js should have proxy configured
const API_URL = import.meta.env.VITE_API_URL || '';

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
    const response = await fetch(`${API_URL}/api/service-connections`);
    if (!response.ok) throw new Error('Failed to fetch connected services');
    const result = await response.json();

    // Handle plain array response (legacy format)
    if (Array.isArray(result)) {
      return result;
    }

    // Handle wrapped response
    const data = unwrapApiResponse(result);
    return Array.isArray(data) ? data : (data.connections || []);
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
    const response = await fetch(`${API_URL}/api/service-connections/${connectionId}`, {
      method: 'DELETE',
    });
    if (!response.ok) throw new Error('Failed to disconnect service');
  },

  // Refresh service token (Gmail)
  async refreshServiceToken(connectionId) {
    const response = await fetch(`${API_URL}/api/service-connections/${connectionId}/refresh`, {
      method: 'POST',
    });
    if (!response.ok) {
      const errorResult = await response.json();
      unwrapApiResponse(errorResult); // Will throw with proper message
    }
    const result = await response.json();
    const data = unwrapApiResponse(result);
    return data;
  },

  // Discord connection methods
  async connectDiscord(botToken, channelId) {
    const response = await fetch(`${API_URL}/api/services/discord/connect`, {
      method: 'POST',
      headers: {
        'Content-Type': 'application/json',
      },
      body: JSON.stringify({ botToken, channelId }),
    });
    if (!response.ok) {
      const errorResult = await response.json();
      unwrapApiResponse(errorResult); // Will throw with proper message
    }
    const result = await response.json();
    const data = unwrapApiResponse(result);
    return data;
  },

  async testDiscordConnection(botToken, channelId) {
    const response = await fetch(`${API_URL}/api/services/discord/test`, {
      method: 'POST',
      headers: {
        'Content-Type': 'application/json',
      },
      body: JSON.stringify({ botToken, channelId }),
    });
    if (!response.ok) {
      const errorResult = await response.json();
      unwrapApiResponse(errorResult); // Will throw with proper message
    }
    const result = await response.json();
    const data = unwrapApiResponse(result);
    return data;
  },

  // Dashboard statistics
  async getDashboardStats() {
    const response = await fetch(`${API_URL}/api/dashboard/stats`);
    if (!response.ok) throw new Error('Failed to fetch dashboard statistics');
    const result = await response.json();
    const data = unwrapApiResponse(result);
    return data.stats;
  },

  // Get all areas
  async getAreas(activeOnly = false) {
    const url = activeOnly ? `${API_URL}/api/areas?activeOnly=true` : `${API_URL}/api/areas`;
    const response = await fetch(url);
    if (!response.ok) throw new Error('Failed to fetch areas');
    const result = await response.json();
    const data = unwrapApiResponse(result);
    return data.areas || [];
  },

  // Create a new area (Gmail → Discord)
  async createArea(areaData) {
    const response = await fetch(`${API_URL}/api/areas`, {
      method: 'POST',
      headers: {
        'Content-Type': 'application/json',
      },
      body: JSON.stringify(areaData),
    });
    if (!response.ok) {
      const errorResult = await response.json();
      unwrapApiResponse(errorResult); // Will throw with proper message
    }
    const result = await response.json();
    const data = unwrapApiResponse(result);
    return data; // Area object is directly in data
  },

  // Delete an area
  async deleteArea(id) {
    const response = await fetch(`${API_URL}/api/areas/${id}`, {
      method: 'DELETE',
    });
    if (!response.ok) throw new Error('Failed to delete area');
  },

  // Toggle area status
  async toggleAreaStatus(id) {
    const response = await fetch(`${API_URL}/api/areas/${id}/toggle`, {
      method: 'PATCH',
    });
    if (!response.ok) {
      const errorResult = await response.json();
      unwrapApiResponse(errorResult); // Will throw with proper message
    }
    const result = await response.json();
    const data = unwrapApiResponse(result);
    return data; // Area object
  },

  // Workflow methods
  async getWorkflows(activeOnly = false) {
    const url = activeOnly ? `${API_URL}/api/workflows?activeOnly=true` : `${API_URL}/api/workflows`;
    const response = await fetch(url);
    if (!response.ok) throw new Error('Failed to fetch workflows');
    const result = await response.json();
    const data = unwrapApiResponse(result);
    return data.workflows || [];
  },

  async getWorkflow(id) {
    const response = await fetch(`${API_URL}/api/workflows/${id}`);
    if (!response.ok) throw new Error('Failed to fetch workflow');
    const result = await response.json();
    const data = unwrapApiResponse(result);
    return data.workflow;
  },

  async createWorkflow(workflowData) {
    const response = await fetch(`${API_URL}/api/workflows`, {
      method: 'POST',
      headers: {
        'Content-Type': 'application/json',
      },
      body: JSON.stringify(workflowData),
    });
    if (!response.ok) {
      const errorResult = await response.json();
      unwrapApiResponse(errorResult); // Will throw with proper message
    }
    const result = await response.json();
    const data = unwrapApiResponse(result);
    return data.workflow;
  },

  async getWorkflowStats(id) {
    const response = await fetch(`${API_URL}/api/workflows/${id}/stats`);
    if (!response.ok) throw new Error('Failed to fetch workflow stats');
    const result = await response.json();
    const data = unwrapApiResponse(result);
    return data.stats;
  },

  async updateWorkflow(id, workflowData) {
    const response = await fetch(`${API_URL}/api/workflows/${id}`, {
      method: 'PUT',
      headers: {
        'Content-Type': 'application/json',
      },
      body: JSON.stringify(workflowData),
    });
    if (!response.ok) {
      const errorResult = await response.json();
      unwrapApiResponse(errorResult); // Will throw with proper message
    }
    const result = await response.json();
    const data = unwrapApiResponse(result);
    return data.workflow;
  },

  async updateWorkflowStatus(id, active) {
    const response = await fetch(`${API_URL}/api/workflows/${id}/status`, {
      method: 'PATCH',
      headers: {
        'Content-Type': 'application/json',
      },
      body: JSON.stringify({ active }),
    });
    if (!response.ok) {
      const errorResult = await response.json();
      unwrapApiResponse(errorResult); // Will throw with proper message
    }
    const result = await response.json();
    const data = unwrapApiResponse(result);
    return data.workflow;
  },

  async deleteWorkflow(id) {
    const response = await fetch(`${API_URL}/api/workflows/${id}`, {
      method: 'DELETE',
    });
    if (!response.ok) throw new Error('Failed to delete workflow');
  },

  async executeWorkflow(id) {
    const response = await fetch(`${API_URL}/api/workflows/${id}/execute`, {
      method: 'POST',
    });
    if (!response.ok) {
      const errorResult = await response.json();
      unwrapApiResponse(errorResult); // Will throw with proper message
    }
    const result = await response.json();
    const data = unwrapApiResponse(result);
    return data;
  },

  async getAvailableNodes() {
    const response = await fetch(`${API_URL}/api/workflows/available-nodes`);
    if (!response.ok) throw new Error('Failed to fetch available nodes');
    const result = await response.json();
    const data = unwrapApiResponse(result);
    return data;
  },

  // Logs methods
  async getLogs(filters = {}) {
    const queryParams = new URLSearchParams(filters).toString();
    const url = queryParams ? `${API_URL}/api/logs?${queryParams}` : `${API_URL}/api/logs`;
    const response = await fetch(url);
    if (!response.ok) throw new Error('Failed to fetch logs');
    const result = await response.json();
    const data = unwrapApiResponse(result);
    // Return the full data object with logs, total, page, pageSize
    return data;
  },

  // Authentication methods
  async login(credentials) {
    const response = await fetch(`${API_URL}/api/auth/login`, {
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
    // Store token if provided
    if (data.token) {
      localStorage.setItem('authToken', data.token);
    }
    return data;
  },

  async register(userData) {
    const response = await fetch(`${API_URL}/api/auth/register`, {
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

  async logout() {
    localStorage.removeItem('authToken');
    localStorage.removeItem('rememberMe');
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
