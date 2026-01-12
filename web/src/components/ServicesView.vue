<template>
  <div class="services-wrap">
    <h1>Connected Services</h1>

    <div v-if="loading" class="loading">Loading services...</div>
    <div v-else-if="error" class="error-message">{{ error }}</div>

    <div v-else class="services-grid">
      <div v-for="service in displayServices" :key="service.name" class="service-card">
        <div class="svc-icon" :style="{ background: service.color }"></div>
        <div class="svc-info">
          <div class="svc-name">{{ service.displayName }}</div>
          <div class="svc-status" :class="{
            'connected': service.isConnected && !service.isExpired,
            'disconnected': !service.isConnected,
            'expired': service.isExpired
          }">
            {{ service.isExpired ? 'Expired' : (service.isConnected ? 'Connected' : 'Not Connected') }}
          </div>
          <div v-if="service.isConnected && service.expiresAt" class="svc-expiry">
            {{ service.isExpired ? 'Token expired' : `Expires ${formatExpiryDate(service.expiresAt)}` }}
          </div>
        </div>
        <div class="svc-actions">
          <button
            v-if="!service.isConnected"
            @click="connectService(service.name)"
            class="btn-connect"
            :disabled="connecting === service.name"
          >
            {{ connecting === service.name ? 'Connecting...' : 'Connect' }}
          </button>
          <template v-else>
            <button
              v-if="service.isExpired && service.name === 'gmail'"
              @click="refreshToken(service.connectionId, service.name)"
              class="btn-refresh"
              :disabled="refreshing === service.name"
            >
              {{ refreshing === service.name ? 'Refreshing...' : 'Refresh' }}
            </button>
            <button
              @click="deleteService(service.connectionId, service.name)"
              class="btn-delete"
              :disabled="deleting === service.name"
              :title="`Delete ${service.displayName} connection`"
            >
              {{ deleting === service.name ? 'Deleting...' : 'Delete' }}
            </button>
          </template>
        </div>
      </div>
    </div>

    <!-- Discord Connection Modal -->
    <DiscordConnectionModal
      v-if="showDiscordModal"
      @close="showDiscordModal = false"
      @connected="handleDiscordConnected"
    />

    <!-- OAuth Modal for Gmail -->
    <OAuthModal
      v-if="showOAuthModal"
      :open="showOAuthModal"
      :title="`Connect ${currentOAuthService}`"
      :message="`Authorize ${currentOAuthService} to enable automation workflows`"
      :authUrl="currentAuthUrl"
      @close="handleOAuthClose"
      @success="handleOAuthSuccess"
      @error="handleOAuthError"
    />
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue';
import { api } from '../services/api.js';
import DiscordConnectionModal from './DiscordConnectionModal.vue';
import OAuthModal from './ui/OAuthModal.vue';
import { useModal } from '../composables/useModal.js';

const loading = ref(true);
const error = ref(null);
const displayServices = ref([]);
const connecting = ref(null);
const refreshing = ref(null);
const deleting = ref(null);
const showDiscordModal = ref(false);
const showOAuthModal = ref(false);
const currentAuthUrl = ref('');
const currentOAuthService = ref('');
const modal = useModal();

// Frontend-only service metadata for UI colors (keep vibrant brand colors)
// This is NOT service discovery data - just UI theming
const serviceColors = {
  gmail: '#EA4335',     // Google Red
  discord: '#5865F2',   // Discord Blurple
  timer: '#10B981',     // Emerald Green
  github: '#7C3AED',    // Vibrant Purple
  dropbox: '#0061FF',   // Dropbox Blue
  outlook: '#0078D4',   // Microsoft Blue
  slack: '#E01E5A',     // Slack Magenta
  trello: '#0079BF',    // Trello Blue
  spotify: '#1DB954',   // Spotify Green
  twitter: '#1DA1F2',   // Twitter Blue
  notion: '#000000',    // Notion Black
  default: '#5b9bd5'    // Default blue
};

function getServiceColor(serviceName) {
  return serviceColors[serviceName.toLowerCase()] || serviceColors.default;
}

function formatExpiryDate(expiresAt) {
  if (!expiresAt) return '';
  const date = new Date(expiresAt);
  const now = new Date();
  const diffMs = date - now;

  if (diffMs < 0) return 'expired';

  const diffMins = Math.floor(diffMs / 60000);
  const diffHours = Math.floor(diffMins / 60);
  const diffDays = Math.floor(diffHours / 24);

  if (diffDays > 0) return `in ${diffDays} day${diffDays > 1 ? 's' : ''}`;
  if (diffHours > 0) return `in ${diffHours} hour${diffHours > 1 ? 's' : ''}`;
  if (diffMins > 0) return `in ${diffMins} minute${diffMins > 1 ? 's' : ''}`;
  return 'soon';
}

async function loadServices() {
  try {
    loading.value = true;
    error.value = null;

    // Fetch available services from backend (dynamic discovery)
    const availableServices = await api.getServices();

    // Fetch connected services
    const connectedServices = await api.getConnectedServices();

    // Create a map of connected services by type (normalize to lowercase)
    // Note: connectedServices is already unwrapped by api.js and is an array
    const connectedMap = {};
    connectedServices.forEach(conn => {
      // Backend returns type as uppercase (GMAIL, DISCORD), normalize to lowercase
      const normalizedType = conn.type.toLowerCase();
      connectedMap[normalizedType] = {
        id: conn.id,
        expiresAt: conn.tokenExpiresAt,
        isExpired: conn.tokenExpiresAt ? new Date(conn.tokenExpiresAt) < new Date() : false
      };
    });

    // Build display list using backend service metadata
    displayServices.value = availableServices.map(service => {
      // Backend provides: { type, name, description, requiresAuthentication, actionCount, reactionCount }
      const serviceType = service.type.toLowerCase();
      const connection = connectedMap[serviceType];

      return {
        name: serviceType,
        displayName: service.name,  // Use backend-provided display name
        description: service.description,  // Backend-provided description
        color: getServiceColor(serviceType),  // Use frontend color mapping
        requiresAuth: service.requiresAuthentication,
        actionCount: service.actionCount || 0,
        reactionCount: service.reactionCount || 0,
        isConnected: !!connection,
        connectionId: connection?.id,
        expiresAt: connection?.expiresAt,
        isExpired: connection?.isExpired || false
      };
    });

  } catch (err) {
    console.error('Error loading services:', err);
    error.value = 'Failed to load services. Please try again.';
  } finally {
    loading.value = false;
  }
}

async function connectService(serviceName) {
  try {
    connecting.value = serviceName;

    if (serviceName === 'gmail') {
      // Gmail requires OAuth flow
      const authData = await api.getGmailAuthUrl();
      currentAuthUrl.value = authData.authUrl;
      currentOAuthService.value = 'Gmail';
      showOAuthModal.value = true;

    } else if (serviceName === 'discord') {
      // Discord - show modal for bot token and channel ID
      showDiscordModal.value = true;
      connecting.value = null;

    } else {
      // For other services, implement connection logic here
      console.log(`Connecting to ${serviceName}...`);
      // TODO: Implement other service connections
      connecting.value = null;
    }

  } catch (err) {
    console.error(`Error connecting to ${serviceName}:`, err);
    await modal.alert(`Failed to connect to ${serviceName}. Please try again.`, {
      title: 'Connection Error',
      variant: 'error'
    });
    connecting.value = null;
  }
}

function handleOAuthSuccess() {
  // Refresh services after successful OAuth
  setTimeout(() => {
    loadServices();
    connecting.value = null;
  }, 1000);
}

function handleOAuthError(error) {
  console.error('OAuth error:', error);
  modal.alert(error.message || 'Authentication failed. Please try again.', {
    title: 'Authentication Error',
    variant: 'error'
  });
  connecting.value = null;
}

function handleOAuthClose() {
  showOAuthModal.value = false;
  currentAuthUrl.value = '';
  currentOAuthService.value = '';
  connecting.value = null;
}

function handleDiscordConnected() {
  // Refresh services list after Discord connection
  loadServices();
}

async function deleteService(connectionId, serviceName) {
  const confirmed = await modal.confirm(
    `Are you sure you want to delete the ${serviceName} connection?`,
    {
      title: 'Delete Connection',
      variant: 'danger',
      confirmText: 'Delete',
      cancelText: 'Cancel'
    }
  );

  if (!confirmed) {
    return;
  }

  try {
    deleting.value = serviceName;
    await api.disconnectService(connectionId);
    await loadServices(); // Reload services
  } catch (err) {
    console.error(`Error deleting ${serviceName}:`, err);
    await modal.alert(`Failed to delete ${serviceName}. Please try again.`, {
      title: 'Error',
      variant: 'error'
    });
  } finally {
    deleting.value = null;
  }
}

async function refreshToken(connectionId, serviceName) {
  try {
    refreshing.value = serviceName;
    // api.refreshServiceToken now returns unwrapped data directly
    await api.refreshServiceToken(connectionId);

    await modal.alert('Token refreshed successfully!', {
      title: 'Success',
      variant: 'success'
    });
    await loadServices(); // Reload services to update expiry
  } catch (err) {
    console.error(`Error refreshing token for ${serviceName}:`, err);
    await modal.alert(`Failed to refresh token: ${err.message}`, {
      title: 'Error',
      variant: 'error'
    });
  } finally {
    refreshing.value = null;
  }
}

onMounted(() => {
  loadServices();
});
</script>

<style scoped src="../assets/ServicesView.css"></style>
