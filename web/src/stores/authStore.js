import { reactive, computed } from 'vue';
import { api } from '../services/api.js';

/**
 * Authentication Store
 * Manages user authentication state, tokens, and user information
 * Uses Vue's reactivity system for simple, effective state management
 */

const state = reactive({
  user: null,
  accessToken: null,
  refreshToken: null,
  tokenExpiry: null,
  isAuthenticated: false,
  isLoading: false,
  error: null,
});

// Load tokens from localStorage on initialization
const loadFromStorage = () => {
  const accessToken = localStorage.getItem('accessToken');
  const refreshToken = localStorage.getItem('refreshToken');
  const user = localStorage.getItem('user');
  const tokenExpiry = localStorage.getItem('tokenExpiry');

  if (accessToken && refreshToken && user) {
    state.accessToken = accessToken;
    state.refreshToken = refreshToken;
    state.user = JSON.parse(user);
    state.tokenExpiry = tokenExpiry ? parseInt(tokenExpiry, 10) : null;
    state.isAuthenticated = true;
  }
};

// Save tokens to localStorage
const saveToStorage = (authResponse) => {
  const { accessToken, refreshToken, user, expiresIn } = authResponse;

  localStorage.setItem('accessToken', accessToken);
  localStorage.setItem('refreshToken', refreshToken);
  localStorage.setItem('user', JSON.stringify(user));

  // Calculate token expiry timestamp
  const expiryTimestamp = Date.now() + (expiresIn * 1000);
  localStorage.setItem('tokenExpiry', expiryTimestamp.toString());

  state.accessToken = accessToken;
  state.refreshToken = refreshToken;
  state.user = user;
  state.tokenExpiry = expiryTimestamp;
  state.isAuthenticated = true;
};

// Clear all authentication data
const clearStorage = () => {
  localStorage.removeItem('accessToken');
  localStorage.removeItem('refreshToken');
  localStorage.removeItem('user');
  localStorage.removeItem('tokenExpiry');

  state.accessToken = null;
  state.refreshToken = null;
  state.user = null;
  state.tokenExpiry = null;
  state.isAuthenticated = false;
  state.error = null;
};

export const useAuthStore = () => {
  // Initialize on first use
  if (!state.accessToken && !state.refreshToken) {
    loadFromStorage();
  }

  /**
   * Login with email and password
   */
  const login = async (email, password) => {
    state.isLoading = true;
    state.error = null;

    try {
      const response = await api.login({ email, password });
      saveToStorage(response);
      return response;
    } catch (error) {
      state.error = error.message || 'Login failed';
      throw error;
    } finally {
      state.isLoading = false;
    }
  };

  /**
   * Register a new user account
   */
  const register = async (userData) => {
    state.isLoading = true;
    state.error = null;

    try {
      const response = await api.register(userData);
      // Don't auto-login after registration per requirements
      // User should be redirected to login page with email pre-filled
      return response;
    } catch (error) {
      state.error = error.message || 'Registration failed';
      throw error;
    } finally {
      state.isLoading = false;
    }
  };

  /**
   * Logout the current user
   */
  const logout = async () => {
    state.isLoading = true;

    try {
      // Call logout API to revoke refresh token
      if (state.refreshToken) {
        await api.logout({ refreshToken: state.refreshToken });
      }
    } catch (error) {
      console.error('Logout API error:', error);
      // Continue with client-side logout even if API fails
    } finally {
      clearStorage();
      state.isLoading = false;
    }
  };

  /**
   * Get current user information from API
   */
  const getCurrentUser = async () => {
    if (!state.accessToken) {
      throw new Error('No access token available');
    }

    state.isLoading = true;
    state.error = null;

    try {
      const user = await api.getCurrentUser();
      state.user = user;
      localStorage.setItem('user', JSON.stringify(user));
      return user;
    } catch (error) {
      state.error = error.message || 'Failed to fetch user';
      // If token is invalid, logout
      if (error.message.includes('401') || error.message.includes('Unauthorized')) {
        clearStorage();
      }
      throw error;
    } finally {
      state.isLoading = false;
    }
  };

  /**
   * Refresh the access token using refresh token
   */
  const refreshAccessToken = async () => {
    if (!state.refreshToken) {
      throw new Error('No refresh token available');
    }

    try {
      const response = await api.refreshToken({ refreshToken: state.refreshToken });

      // Update tokens but keep the same user
      localStorage.setItem('accessToken', response.accessToken);
      state.accessToken = response.accessToken;

      // Update expiry
      const expiryTimestamp = Date.now() + (response.expiresIn * 1000);
      localStorage.setItem('tokenExpiry', expiryTimestamp.toString());
      state.tokenExpiry = expiryTimestamp;

      // Update user info if provided
      if (response.user) {
        state.user = response.user;
        localStorage.setItem('user', JSON.stringify(response.user));
      }

      return response.accessToken;
    } catch (error) {
      // If refresh fails, logout user
      clearStorage();
      throw error;
    }
  };

  /**
   * Check if token is expired or about to expire (within 5 minutes)
   */
  const isTokenExpired = () => {
    if (!state.tokenExpiry) return true;
    const bufferTime = 5 * 60 * 1000; // 5 minutes in milliseconds
    return Date.now() >= (state.tokenExpiry - bufferTime);
  };

  /**
   * Ensure the access token is valid, refresh if needed
   */
  const ensureValidToken = async () => {
    if (!state.isAuthenticated) {
      throw new Error('Not authenticated');
    }

    if (isTokenExpired()) {
      await refreshAccessToken();
    }

    return state.accessToken;
  };

  return {
    // State (read-only via computed)
    user: computed(() => state.user),
    isAuthenticated: computed(() => state.isAuthenticated),
    isLoading: computed(() => state.isLoading),
    error: computed(() => state.error),
    accessToken: computed(() => state.accessToken),

    // Actions
    login,
    register,
    logout,
    getCurrentUser,
    refreshAccessToken,
    isTokenExpired,
    ensureValidToken,
    clearError: () => { state.error = null; },
  };
};
