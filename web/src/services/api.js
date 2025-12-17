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
  // Get available services from about.json
  async getAvailableServices() {
    const response = await fetch(`${API_URL}/about.json`);
    if (!response.ok) throw new Error('Failed to fetch available services');
    const data = await response.json();
    return data.server.services || [];
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
};
