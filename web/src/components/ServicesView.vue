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
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue';
import { api } from '../services/api.js';
import DiscordConnectionModal from './DiscordConnectionModal.vue';

const loading = ref(true);
const error = ref(null);
const displayServices = ref([]);
const connecting = ref(null);
const refreshing = ref(null);
const deleting = ref(null);
const showDiscordModal = ref(false);

// Service metadata
const serviceMetadata = {
  gmail: { displayName: 'Gmail', color: '#EA4335' },
  discord: { displayName: 'Discord', color: '#5865F2' },
  timer: { displayName: 'Timer', color: '#4285F4' },
  github: { displayName: 'GitHub', color: '#6e40c9' },
  dropbox: { displayName: 'Dropbox', color: '#0061FF' },
  outlook: { displayName: 'Outlook', color: '#0078D4' }
};

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

    // Fetch available services from backend
    const availableServices = await api.getAvailableServices();

    // Fetch connected services
    const connectedServices = await api.getConnectedServices();

    // Create a map of connected services by type (normalize to lowercase)
    const connectedMap = {};
    if (Array.isArray(connectedServices)) {
      connectedServices.forEach(conn => {
        // Backend returns type as uppercase (GMAIL, DISCORD), normalize to lowercase
        const normalizedType = conn.type.toLowerCase();
        connectedMap[normalizedType] = {
          id: conn.id,
          expiresAt: conn.tokenExpiresAt,
          isExpired: conn.tokenExpiresAt ? new Date(conn.tokenExpiresAt) < new Date() : false
        };
      });
    }

    // Build display list - only show services from backend
    displayServices.value = availableServices.map(service => {
      const serviceName = service.name.toLowerCase();
      const metadata = serviceMetadata[serviceName] || {
        displayName: service.name,
        color: '#666666'
      };

      const connection = connectedMap[serviceName];

      return {
        name: serviceName,
        displayName: metadata.displayName,
        color: metadata.color,
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

      // Open OAuth URL in a new window
      const width = 600;
      const height = 700;
      const left = (screen.width - width) / 2;
      const top = (screen.height - height) / 2;

      const authWindow = window.open(
        authData.authUrl,
        'Gmail Authorization',
        `width=${width},height=${height},left=${left},top=${top},toolbar=no,menubar=no,location=no,status=no`
      );

      // Poll to check if auth window is closed
      const pollTimer = setInterval(() => {
        if (authWindow.closed) {
          clearInterval(pollTimer);
          // Refresh services after auth window closes
          setTimeout(() => loadServices(), 1000);
        }
      }, 500);

    } else if (serviceName === 'discord') {
      // Discord - show modal for bot token and channel ID
      showDiscordModal.value = true;

    } else {
      // For other services, implement connection logic here
      console.log(`Connecting to ${serviceName}...`);
      // TODO: Implement other service connections
    }

  } catch (err) {
    console.error(`Error connecting to ${serviceName}:`, err);
    alert(`Failed to connect to ${serviceName}. Please try again.`);
  } finally {
    connecting.value = null;
  }
}

function handleDiscordConnected() {
  // Refresh services list after Discord connection
  loadServices();
}

async function deleteService(connectionId, serviceName) {
  if (!confirm(`Are you sure you want to delete the ${serviceName} connection?`)) {
    return;
  }

  try {
    deleting.value = serviceName;
    await api.disconnectService(connectionId);
    await loadServices(); // Reload services
  } catch (err) {
    console.error(`Error deleting ${serviceName}:`, err);
    alert(`Failed to delete ${serviceName}. Please try again.`);
  } finally {
    deleting.value = null;
  }
}

async function refreshToken(connectionId, serviceName) {
  try {
    refreshing.value = serviceName;
    const result = await api.refreshServiceToken(connectionId);

    if (result.success) {
      alert('Token refreshed successfully!');
      await loadServices(); // Reload services to update expiry
    } else {
      alert(result.message || 'Failed to refresh token');
    }
  } catch (err) {
    console.error(`Error refreshing token for ${serviceName}:`, err);
    alert(`Failed to refresh token: ${err.message}`);
  } finally {
    refreshing.value = null;
  }
}

onMounted(() => {
  loadServices();
});
</script>

<style scoped src="../assets/ServicesView.css"></style>
