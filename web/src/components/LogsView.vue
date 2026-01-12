<template>
  <div class="logs-wrap">
    <!-- Header Section -->
    <div class="logs-header">
      <div>
        <h1>Execution Logs</h1>
        <p>Monitor your AREA workflow executions and results</p>
      </div>
      <div class="header-actions">
        <button class="filter-btn" @click="toggleFilters">
          <FilterIcon size="18" />
          Filters
        </button>
        <button class="refresh-btn" @click="refreshLogs" :disabled="loading">
          <RefreshCwIcon size="18" :class="{ spinning: loading }" />
          Refresh
        </button>
        <button
          class="auto-refresh-btn"
          :class="{ active: autoRefresh }"
          @click="toggleAutoRefresh"
        >
          <ClockIcon size="18" />
          Auto-refresh
        </button>
      </div>
    </div>

    <!-- Filter Section -->
    <div class="filters-section" v-if="showFilters">
      <div class="filter-row">
        <div class="filter-group">
          <label>Status</label>
          <div class="filter-pills">
            <button
              v-for="status in statusOptions"
              :key="status.value"
              class="filter-pill"
              :class="{ active: selectedStatus === status.value }"
              @click="setStatusFilter(status.value)"
            >
              {{ status.label }}
            </button>
          </div>
        </div>

        <div class="filter-group">
          <label>Workflow</label>
          <UiSelect
            v-model="selectedAreaId"
            :options="areaOptions"
            placeholder="All Workflows"
            @update:modelValue="applyFilters"
          />
        </div>

        <div class="filter-group">
          <label>Date Range</label>
          <div class="date-range-inputs">
            <input
              type="date"
              v-model="fromDate"
              class="date-input"
              @change="applyFilters"
            />
            <span class="date-separator">to</span>
            <input
              type="date"
              v-model="toDate"
              class="date-input"
              @change="applyFilters"
            />
          </div>
        </div>
      </div>
    </div>

    <!-- Stats Widget -->
    <div class="stats-widget">
      <div class="stat-item">
        <div class="stat-icon success">
          <CheckCircleIcon size="24" />
        </div>
        <div class="stat-details">
          <div class="stat-value">{{ stats.successCount }}</div>
          <div class="stat-label">Successful</div>
        </div>
      </div>
      <div class="stat-item">
        <div class="stat-icon error">
          <XCircleIcon size="24" />
        </div>
        <div class="stat-details">
          <div class="stat-value">{{ stats.failureCount }}</div>
          <div class="stat-label">Failed</div>
        </div>
      </div>
      <div class="stat-item">
        <div class="stat-icon warning">
          <AlertTriangleIcon size="24" />
        </div>
        <div class="stat-details">
          <div class="stat-value">{{ stats.errorCount }}</div>
          <div class="stat-label">Errors</div>
        </div>
      </div>
      <div class="stat-item">
        <div class="stat-icon info">
          <InfoIcon size="24" />
        </div>
        <div class="stat-details">
          <div class="stat-value">{{ totalLogs }}</div>
          <div class="stat-label">Total Logs</div>
        </div>
      </div>
    </div>

    <!-- Error State -->
    <div v-if="error" class="error-banner">
      <AlertTriangleIcon size="20" />
      <span>{{ error }}</span>
      <button class="retry-btn" @click="loadLogs">Retry</button>
    </div>

    <!-- Logs List -->
    <div class="logs-container">
      <div v-if="loading && logs.length === 0" class="loading-state">
        <UiSpinner size="lg" />
        <p>Loading execution logs...</p>
      </div>

      <div v-else-if="logs.length === 0 && !error" class="empty-state">
        <FileTextIcon size="64" />
        <h3>No execution logs found</h3>
        <p v-if="hasActiveFilters">Try adjusting your filters to see more results</p>
        <p v-else>Your workflow executions will appear here once they run</p>
      </div>

      <div v-else class="logs-list">
        <div
          v-for="log in logs"
          :key="log.id"
          class="log-entry"
          :class="['status-' + log.status.toLowerCase()]"
        >
          <div class="log-main">
            <div class="log-icon">
              <CheckCircleIcon v-if="log.status === 'SUCCESS'" size="20" />
              <XCircleIcon v-else-if="log.status === 'FAILURE'" size="20" />
              <AlertTriangleIcon v-else size="20" />
            </div>

            <div class="log-content">
              <div class="log-header-row">
                <div class="log-title">
                  <h4>{{ log.areaName }}</h4>
                  <UiBadge :variant="getStatusVariant(log.status)">
                    {{ log.status }}
                  </UiBadge>
                </div>
                <div class="log-meta">
                  <span class="log-time">{{ formatTimestamp(log.executedAt) }}</span>
                  <span class="log-duration" v-if="log.executionTimeMs">
                    {{ formatDuration(log.executionTimeMs) }}
                  </span>
                </div>
              </div>

              <div v-if="log.errorMessage" class="log-error-msg">
                <AlertTriangleIcon size="16" />
                <span>{{ log.errorMessage }}</span>
              </div>

              <div class="log-summary" v-if="!expandedLogs.includes(log.id)">
                <span v-if="log.unreadCount !== null && log.unreadCount !== undefined">
                  {{ log.unreadCount }} unread email{{ log.unreadCount !== 1 ? 's' : '' }}
                </span>
                <span v-if="log.messageSent" class="message-sent">
                  <CheckCircleIcon size="14" />
                  Message sent
                </span>
              </div>

              <!-- Expandable Details -->
              <div class="log-details">
                <button
                  class="details-toggle"
                  @click="toggleDetails(log.id)"
                  :aria-expanded="expandedLogs.includes(log.id)"
                >
                  <ChevronRightIcon
                    :class="{ expanded: expandedLogs.includes(log.id) }"
                    size="16"
                  />
                  <span>{{ expandedLogs.includes(log.id) ? 'Hide' : 'Show' }} Details</span>
                </button>

                <div v-if="expandedLogs.includes(log.id)" class="details-content">
                  <div class="details-section" v-if="log.triggerData">
                    <h5>Trigger Data</h5>
                    <div class="details-grid">
                      <div
                        v-for="(value, key) in log.triggerData"
                        :key="key"
                        class="detail-item"
                      >
                        <span class="detail-key">{{ formatKey(key) }}:</span>
                        <span class="detail-value">{{ formatValue(value) }}</span>
                      </div>
                    </div>
                  </div>

                  <div class="details-section" v-if="log.reactionData">
                    <h5>Reaction Data</h5>
                    <div class="details-grid">
                      <div
                        v-for="(value, key) in log.reactionData"
                        :key="key"
                        class="detail-item"
                      >
                        <span class="detail-key">{{ formatKey(key) }}:</span>
                        <span class="detail-value">{{ formatValue(value) }}</span>
                      </div>
                    </div>
                  </div>

                  <div class="details-section">
                    <h5>Execution Info</h5>
                    <div class="details-grid">
                      <div class="detail-item">
                        <span class="detail-key">Workflow ID:</span>
                        <span class="detail-value">{{ log.areaId }}</span>
                      </div>
                      <div class="detail-item">
                        <span class="detail-key">Executed At:</span>
                        <span class="detail-value">{{ new Date(log.executedAt).toLocaleString() }}</span>
                      </div>
                      <div class="detail-item" v-if="log.executionTimeMs">
                        <span class="detail-key">Duration:</span>
                        <span class="detail-value">{{ log.executionTimeMs }}ms</span>
                      </div>
                    </div>
                  </div>
                </div>
              </div>
            </div>
          </div>
        </div>

        <!-- Load More Button -->
        <div v-if="hasMoreLogs" class="load-more-section">
          <button
            class="load-more-btn"
            @click="loadMoreLogs"
            :disabled="loadingMore"
          >
            <UiSpinner v-if="loadingMore" size="sm" />
            <span v-else>Load More Logs</span>
          </button>
          <p class="logs-count">
            Showing {{ logs.length }} of {{ totalLogs }} logs
          </p>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, computed, onMounted, onUnmounted } from 'vue';
import {
  FileTextIcon,
  FilterIcon,
  RefreshCwIcon,
  ClockIcon,
  CheckCircleIcon,
  XCircleIcon,
  AlertTriangleIcon,
  InfoIcon,
  ChevronRightIcon
} from 'lucide-vue-next';
import { api } from '../services/api.js';
import UiBadge from './ui/Badge.vue';
import UiSelect from './ui/Select.vue';
import UiSpinner from './ui/Spinner.vue';

// State
const logs = ref([]);
const allLogs = ref([]); // Store all fetched logs for stats
const areas = ref([]);
const loading = ref(false);
const loadingMore = ref(false);
const error = ref('');
const showFilters = ref(false);
const expandedLogs = ref([]);
const autoRefresh = ref(false);
const autoRefreshInterval = ref(null);

// Filter state
const selectedStatus = ref('ALL');
const selectedAreaId = ref('');
const fromDate = ref('');
const toDate = ref('');

// Pagination state
const currentOffset = ref(0);
const logsPerPage = 50;
const totalLogs = ref(0);

// Status options
const statusOptions = [
  { value: 'ALL', label: 'All' },
  { value: 'SUCCESS', label: 'Success' },
  { value: 'FAILURE', label: 'Failure' },
  { value: 'ERROR', label: 'Error' }
];

// Computed
const areaOptions = computed(() => {
  const options = [{ value: '', label: 'All Workflows' }];
  areas.value.forEach(area => {
    options.push({
      value: String(area.id),
      label: area.name
    });
  });
  return options;
});

const stats = computed(() => {
  return {
    successCount: allLogs.value.filter(l => l.status === 'SUCCESS').length,
    failureCount: allLogs.value.filter(l => l.status === 'FAILURE').length,
    errorCount: allLogs.value.filter(l => l.status === 'ERROR').length
  };
});

const hasMoreLogs = computed(() => {
  return logs.value.length < totalLogs.value;
});

const hasActiveFilters = computed(() => {
  return selectedStatus.value !== 'ALL' ||
         selectedAreaId.value !== '' ||
         fromDate.value !== '' ||
         toDate.value !== '';
});

// Methods
async function loadAreas() {
  try {
    const response = await api.getAreas();
    areas.value = response || [];
  } catch (err) {
    console.error('Failed to load areas:', err);
  }
}

async function loadLogs(append = false) {
  if (append) {
    loadingMore.value = true;
  } else {
    loading.value = true;
    currentOffset.value = 0;
  }

  error.value = '';

  try {
    const filters = buildFilters();

    // Use getAllLogs if no specific area is selected, otherwise use getLogs for specific area
    let response;
    if (selectedAreaId.value) {
      response = await api.getLogs(selectedAreaId.value, filters);
      // Transform the response to match expected format
      const fetchedLogs = response.logs || [];
      fetchedLogs.forEach(log => {
        // Add area name if not present
        if (!log.areaName) {
          const area = areas.value.find(a => a.id === parseInt(selectedAreaId.value));
          log.areaName = area ? area.name : `Area #${selectedAreaId.value}`;
        }
      });
      response = {
        logs: fetchedLogs,
        total: response.pagination?.totalElements || fetchedLogs.length,
        page: response.pagination?.currentPage || 0,
        pageSize: response.pagination?.pageSize || filters.limit
      };
    } else {
      response = await api.getAllLogs(filters);
    }

    // API returns { logs: [...], total: number, page: number, pageSize: number }
    const fetchedLogs = Array.isArray(response) ? response : (response.logs || []);
    const total = response.total || fetchedLogs.length;

    if (append) {
      logs.value = [...logs.value, ...fetchedLogs];
    } else {
      logs.value = fetchedLogs;
      allLogs.value = fetchedLogs; // Store for stats
    }

    totalLogs.value = total;
  } catch (err) {
    console.error('Failed to load logs:', err);
    error.value = err.message || 'Failed to load execution logs. Please try again.';
    if (!append) {
      logs.value = [];
      totalLogs.value = 0;
    }
  } finally {
    loading.value = false;
    loadingMore.value = false;
  }
}

function buildFilters() {
  const filters = {
    limit: logsPerPage,
    offset: currentOffset.value
  };

  if (selectedStatus.value && selectedStatus.value !== 'ALL') {
    filters.status = selectedStatus.value;
  }

  if (selectedAreaId.value) {
    filters.areaId = selectedAreaId.value;
  }

  if (fromDate.value) {
    // Convert to ISO format
    filters.fromDate = new Date(fromDate.value).toISOString();
  }

  if (toDate.value) {
    // Set to end of day
    const date = new Date(toDate.value);
    date.setHours(23, 59, 59, 999);
    filters.toDate = date.toISOString();
  }

  return filters;
}

function applyFilters() {
  loadLogs(false);
}

function setStatusFilter(status) {
  selectedStatus.value = status;
  applyFilters();
}

async function loadMoreLogs() {
  currentOffset.value += logsPerPage;
  await loadLogs(true);
}

function toggleFilters() {
  showFilters.value = !showFilters.value;
}

function toggleDetails(logId) {
  const index = expandedLogs.value.indexOf(logId);
  if (index > -1) {
    expandedLogs.value.splice(index, 1);
  } else {
    expandedLogs.value.push(logId);
  }
}

function refreshLogs() {
  loadLogs(false);
}

function toggleAutoRefresh() {
  autoRefresh.value = !autoRefresh.value;

  if (autoRefresh.value) {
    // Refresh every 30 seconds
    autoRefreshInterval.value = setInterval(() => {
      refreshLogs();
    }, 30000);
  } else {
    if (autoRefreshInterval.value) {
      clearInterval(autoRefreshInterval.value);
      autoRefreshInterval.value = null;
    }
  }
}

function formatTimestamp(timestamp) {
  const date = new Date(timestamp);
  const now = new Date();
  const diffMs = now - date;
  const diffMins = Math.floor(diffMs / 60000);

  if (diffMins < 1) return 'Just now';
  if (diffMins < 60) return `${diffMins}m ago`;

  const diffHours = Math.floor(diffMins / 60);
  if (diffHours < 24) return `${diffHours}h ago`;

  const diffDays = Math.floor(diffHours / 24);
  if (diffDays === 1) return 'Yesterday';
  if (diffDays < 7) return `${diffDays}d ago`;

  return date.toLocaleDateString() + ' ' + date.toLocaleTimeString();
}

function formatDuration(ms) {
  if (ms < 1000) return `${ms}ms`;
  return `${(ms / 1000).toFixed(2)}s`;
}

function getStatusVariant(status) {
  const variants = {
    'SUCCESS': 'success',
    'FAILURE': 'danger',
    'ERROR': 'warning'
  };
  return variants[status] || 'neutral';
}

function formatKey(key) {
  // Convert camelCase to Title Case
  return key
    .replace(/([A-Z])/g, ' $1')
    .replace(/^./, str => str.toUpperCase())
    .trim();
}

function formatValue(value) {
  if (value === null || value === undefined) return 'N/A';
  if (typeof value === 'boolean') return value ? 'Yes' : 'No';
  if (typeof value === 'object') return JSON.stringify(value, null, 2);
  if (typeof value === 'string' && value.length > 100) {
    return value.substring(0, 100) + '...';
  }
  return String(value);
}

// Lifecycle
onMounted(async () => {
  await loadAreas();
  await loadLogs();
});

onUnmounted(() => {
  if (autoRefreshInterval.value) {
    clearInterval(autoRefreshInterval.value);
  }
});
</script>

<style scoped src="../assets/LogsView.css"></style>
