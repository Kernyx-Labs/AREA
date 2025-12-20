<template>
  <div class="logs-wrap">
    <!-- Header Section -->
    <div class="logs-header">
      <div>
        <h1>System Logs</h1>
        <p>Monitor your automation executions and system events</p>
      </div>
      <div class="header-actions">
        <button class="filter-btn" @click="toggleFilters">
          <FilterIcon size="18" />
          Filters
        </button>
        <button class="refresh-btn" @click="refreshLogs">
          <RefreshCwIcon size="18" />
          Refresh
        </button>
        <button class="clear-btn" @click="clearLogs" :disabled="logs.length === 0">
          <TrashIcon size="18" />
          Clear All
        </button>
      </div>
    </div>

    <!-- Filter Section -->
    <div class="filters-section" v-if="showFilters">
      <div class="filter-group">
        <label>Log Level</label>
        <div class="filter-pills">
          <button
            v-for="level in logLevels"
            :key="level"
            class="filter-pill"
            :class="{ active: activeFilters.includes(level) }"
            @click="toggleFilter(level)"
          >
            {{ level }}
          </button>
        </div>
      </div>
      <div class="filter-group">
        <label>Time Range</label>
        <select v-model="timeRange" class="time-range-select">
          <option value="1h">Last Hour</option>
          <option value="24h">Last 24 Hours</option>
          <option value="7d">Last 7 Days</option>
          <option value="30d">Last 30 Days</option>
          <option value="all">All Time</option>
        </select>
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
          <div class="stat-value">{{ stats.errorCount }}</div>
          <div class="stat-label">Failed</div>
        </div>
      </div>
      <div class="stat-item">
        <div class="stat-icon warning">
          <AlertTriangleIcon size="24" />
        </div>
        <div class="stat-details">
          <div class="stat-value">{{ stats.warningCount }}</div>
          <div class="stat-label">Warnings</div>
        </div>
      </div>
      <div class="stat-item">
        <div class="stat-icon info">
          <InfoIcon size="24" />
        </div>
        <div class="stat-details">
          <div class="stat-value">{{ stats.infoCount }}</div>
          <div class="stat-label">Info</div>
        </div>
      </div>
    </div>

    <!-- Logs List -->
    <div class="logs-container">
      <div v-if="loading" class="loading-state">
        <div class="spinner"></div>
        <p>Loading logs...</p>
      </div>

      <div v-else-if="filteredLogs.length === 0" class="empty-state">
        <FileTextIcon size="64" />
        <h3>No logs found</h3>
        <p>There are no logs matching your current filters</p>
      </div>

      <div v-else class="logs-list">
        <div
          v-for="log in filteredLogs"
          :key="log.id"
          class="log-entry"
          :class="['level-' + log.level]"
        >
          <div class="log-icon">
            <CheckCircleIcon v-if="log.level === 'success'" size="20" />
            <XCircleIcon v-else-if="log.level === 'error'" size="20" />
            <AlertTriangleIcon v-else-if="log.level === 'warning'" size="20" />
            <InfoIcon v-else size="20" />
          </div>
          <div class="log-content">
            <div class="log-header">
              <span class="log-level-badge">{{ log.level }}</span>
              <span class="log-time">{{ formatTimestamp(log.timestamp) }}</span>
            </div>
            <div class="log-message">{{ log.message }}</div>
            <div v-if="log.details" class="log-details">
              <div class="details-header" @click="toggleDetails(log.id)">
                <ChevronRightIcon :class="{ expanded: expandedLogs.includes(log.id) }" size="16" />
                <span>Details</span>
              </div>
              <div v-if="expandedLogs.includes(log.id)" class="details-content">
                <pre>{{ JSON.stringify(log.details, null, 2) }}</pre>
              </div>
            </div>
            <div v-if="log.source" class="log-source">
              Source: <strong>{{ log.source }}</strong>
            </div>
          </div>
        </div>
      </div>
    </div>

    <!-- Pagination -->
    <div class="pagination" v-if="totalPages > 1">
      <button
        class="page-btn"
        :disabled="currentPage === 1"
        @click="goToPage(currentPage - 1)"
      >
        <ChevronLeftIcon size="18" />
        Previous
      </button>
      <div class="page-numbers">
        <button
          v-for="page in visiblePages"
          :key="page"
          class="page-number"
          :class="{ active: page === currentPage }"
          @click="goToPage(page)"
        >
          {{ page }}
        </button>
      </div>
      <button
        class="page-btn"
        :disabled="currentPage === totalPages"
        @click="goToPage(currentPage + 1)"
      >
        Next
        <ChevronRightIcon size="18" />
      </button>
    </div>
  </div>
</template>

<script setup>
import { ref, computed, onMounted, watch } from 'vue';
import {
  FileTextIcon,
  FilterIcon,
  RefreshCwIcon,
  TrashIcon,
  CheckCircleIcon,
  XCircleIcon,
  AlertTriangleIcon,
  InfoIcon,
  ChevronRightIcon,
  ChevronLeftIcon
} from 'lucide-vue-next';
import { api } from '../services/api.js';

// State
const logs = ref([]);
const loading = ref(false);
const showFilters = ref(false);
const activeFilters = ref(['success', 'error', 'warning', 'info']);
const timeRange = ref('24h');
const expandedLogs = ref([]);
const currentPage = ref(1);
const logsPerPage = 20;

const logLevels = ['success', 'error', 'warning', 'info'];

// Computed
const stats = computed(() => {
  return {
    successCount: logs.value.filter(l => l.level === 'success').length,
    errorCount: logs.value.filter(l => l.level === 'error').length,
    warningCount: logs.value.filter(l => l.level === 'warning').length,
    infoCount: logs.value.filter(l => l.level === 'info').length
  };
});

const filteredLogs = computed(() => {
  let result = logs.value.filter(log => activeFilters.value.includes(log.level));

  // Apply time range filter
  if (timeRange.value !== 'all') {
    const now = Date.now();
    const ranges = {
      '1h': 60 * 60 * 1000,
      '24h': 24 * 60 * 60 * 1000,
      '7d': 7 * 24 * 60 * 60 * 1000,
      '30d': 30 * 24 * 60 * 60 * 1000
    };
    const cutoff = now - ranges[timeRange.value];
    result = result.filter(log => new Date(log.timestamp).getTime() >= cutoff);
  }

  // Pagination
  const start = (currentPage.value - 1) * logsPerPage;
  const end = start + logsPerPage;
  return result.slice(start, end);
});

const totalPages = computed(() => {
  const allFiltered = logs.value.filter(log => activeFilters.value.includes(log.level));
  return Math.ceil(allFiltered.length / logsPerPage);
});

const visiblePages = computed(() => {
  const pages = [];
  const maxVisible = 5;
  let start = Math.max(1, currentPage.value - Math.floor(maxVisible / 2));
  let end = Math.min(totalPages.value, start + maxVisible - 1);

  if (end - start < maxVisible - 1) {
    start = Math.max(1, end - maxVisible + 1);
  }

  for (let i = start; i <= end; i++) {
    pages.push(i);
  }
  return pages;
});

// Methods
async function loadLogs() {
  loading.value = true;
  try {
    // Try to fetch logs from API
    const response = await api.getLogs();
    logs.value = response;
  } catch (error) {
    console.error('Failed to load logs:', error);
    // Generate mock logs for demonstration
    logs.value = generateMockLogs();
  } finally {
    loading.value = false;
  }
}

function generateMockLogs() {
  const mockLogs = [];
  const levels = ['success', 'error', 'warning', 'info'];
  const sources = ['Gmail Service', 'Discord Service', 'Workflow Engine', 'API Gateway', 'Database'];
  const messages = {
    success: [
      'Workflow executed successfully',
      'Service connection established',
      'Email processed and forwarded',
      'Discord message sent successfully',
      'Automation triggered successfully'
    ],
    error: [
      'Failed to connect to service',
      'Authentication failed',
      'Workflow execution failed',
      'Rate limit exceeded',
      'Database connection error'
    ],
    warning: [
      'Service response delayed',
      'Approaching rate limit',
      'Configuration missing optional field',
      'Retry attempt scheduled',
      'Cache miss detected'
    ],
    info: [
      'Workflow started',
      'Service health check passed',
      'Configuration updated',
      'New service connected',
      'User logged in'
    ]
  };

  for (let i = 0; i < 100; i++) {
    const level = levels[Math.floor(Math.random() * levels.length)];
    const source = sources[Math.floor(Math.random() * sources.length)];
    const messageList = messages[level];
    const message = messageList[Math.floor(Math.random() * messageList.length)];

    const timestamp = new Date(Date.now() - Math.random() * 7 * 24 * 60 * 60 * 1000);

    mockLogs.push({
      id: `log-${i}`,
      level,
      message,
      source,
      timestamp: timestamp.toISOString(),
      details: Math.random() > 0.7 ? {
        executionTime: Math.floor(Math.random() * 5000) + 'ms',
        userId: 'user-' + Math.floor(Math.random() * 100),
        workflowId: 'workflow-' + Math.floor(Math.random() * 50)
      } : null
    });
  }

  return mockLogs.sort((a, b) => new Date(b.timestamp) - new Date(a.timestamp));
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

  return date.toLocaleString();
}

function toggleFilters() {
  showFilters.value = !showFilters.value;
}

function toggleFilter(level) {
  const index = activeFilters.value.indexOf(level);
  if (index > -1) {
    if (activeFilters.value.length > 1) {
      activeFilters.value.splice(index, 1);
    }
  } else {
    activeFilters.value.push(level);
  }
  currentPage.value = 1; // Reset to first page when filters change
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
  loadLogs();
}

function clearLogs() {
  if (confirm('Are you sure you want to clear all logs? This action cannot be undone.')) {
    logs.value = [];
  }
}

function goToPage(page) {
  currentPage.value = page;
}

// Watch for filter changes
watch([activeFilters, timeRange], () => {
  currentPage.value = 1;
});

// Lifecycle
onMounted(() => {
  loadLogs();
});
</script>

<style scoped src="../assets/LogsView.css"></style>
