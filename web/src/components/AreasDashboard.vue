<template>
  <div class="areas-wrap">
    <!-- KPI Section -->
    <div class="kpi-section">
      <h2>Overview</h2>
      <div class="kpi-grid" v-if="!loadingStats">
        <div class="kpi-card">
          <div class="kpi-header">
            <span class="kpi-icon">📊</span>
            <span class="kpi-label">Total Areas</span>
          </div>
          <div class="kpi-value">{{ stats.totalAreas || 0 }}</div>
          <div class="kpi-footer">
            <span class="active">{{ stats.activeAreas || 0 }} active</span>
            <span class="inactive">{{ stats.inactiveAreas || 0 }} inactive</span>
          </div>
        </div>

        <div class="kpi-card">
          <div class="kpi-header">
            <span class="kpi-icon">🔗</span>
            <span class="kpi-label">Connected Services</span>
          </div>
          <div class="kpi-value">{{ stats.connectedServices || 0 }}</div>
          <div class="kpi-footer">Services integrated</div>
        </div>

        <div class="kpi-card">
          <div class="kpi-header">
            <span class="kpi-icon">⚡</span>
            <span class="kpi-label">Executions (24h)</span>
          </div>
          <div class="kpi-value">{{ stats.executionsLast24h || 0 }}</div>
          <div class="kpi-footer">
            <span
              class="trend"
              :class="{ 'trend-up': stats.executionTrend > 0, 'trend-down': stats.executionTrend < 0 }"
            >
              {{ stats.executionTrend > 0 ? '↑' : stats.executionTrend < 0 ? '↓' : '→' }}
              {{ Math.abs(stats.executionTrend || 0) }}% vs last week
            </span>
          </div>
        </div>

        <div class="kpi-card">
          <div class="kpi-header">
            <span class="kpi-icon">✅</span>
            <span class="kpi-label">Success Rate</span>
          </div>
          <div class="kpi-value">{{ stats.successRate || 0 }}%</div>
          <div class="kpi-footer">
            <span class="success">{{ stats.successfulExecutions || 0 }} successful</span>
            <span class="failed">{{ stats.failedExecutions || 0 }} failed</span>
          </div>
        </div>
      </div>
      <div v-else class="loading-stats">Loading statistics...</div>
    </div>

    <!-- Recent Activity -->
    <div class="activity-section" v-if="stats.recentActivity && stats.recentActivity.length > 0">
      <h2>Recent Activity</h2>
      <div class="activity-list">
        <div
          v-for="activity in stats.recentActivity"
          :key="activity.id"
          class="activity-item"
          :class="{ 'success': activity.success, 'failed': !activity.success }"
        >
          <div class="activity-icon">
            {{ activity.success ? '✓' : '✗' }}
          </div>
          <div class="activity-details">
            <div class="activity-message">{{ activity.message }}</div>
            <div class="activity-time">{{ formatTime(activity.executedAt) }}</div>
          </div>
          <div class="activity-status">
            {{ activity.status }}
          </div>
        </div>
      </div>
    </div>

    <!-- Areas List -->
    <div class="heading-row">
      <div>
        <h1>My Automations</h1>
        <p>Manage your AREA workflows</p>
      </div>
      <button class="new-area" @click="createNewArea">
        <PlusIcon size="20" /> Create New Area
      </button>
    </div>

    <div v-if="loadingAreas" class="loading">Loading areas...</div>
    <div v-else-if="areas.length === 0" class="empty-state">
      <p>No areas yet. Create your first automation!</p>
    </div>
    <div v-else class="areas-grid">
      <div v-for="area in areas" :key="area.id" class="area-card">
        <div class="area-head">
          <h3>{{ area.name || `Area #${area.id}` }}</h3>
          <div class="status-badge" :class="{ active: area.active }">
            {{ area.active ? 'Active' : 'Inactive' }}
          </div>
        </div>
        <div class="area-meta">
          <span v-if="area.description">{{ area.description }}</span>
          <span v-else>Workflow automation</span>
        </div>
        <div class="area-actions">
          <button
            class="btn-test"
            @click.stop="testWorkflow(area)"
            title="Test workflow now"
          >
            <PlayIcon size="16" /> Test
          </button>
          <button
            class="btn-stats"
            @click.stop="viewStats(area.id)"
            title="View statistics"
          >
            <ChartBarIcon size="16" /> Stats
          </button>
          <button
            class="btn-toggle"
            @click.stop="toggleArea(area)"
            :class="{ active: area.active }"
            :title="area.active ? 'Deactivate' : 'Activate'"
          >
            <PowerIcon size="16" /> {{ area.active ? 'Disable' : 'Enable' }}
          </button>
          <button
            class="btn-edit"
            @click.stop="openArea(area.id)"
            title="Edit workflow"
          >
            <EditIcon size="16" /> Edit
          </button>
          <button
            class="btn-delete"
            @click.stop="deleteArea(area)"
            title="Delete workflow"
          >
            <TrashIcon size="16" /> Delete
          </button>
        </div>
      </div>
    </div>

    <!-- Stats Modal -->
    <div v-if="showStatsModal" class="modal-overlay" @click="closeStatsModal">
      <div class="modal-content" @click.stop>
        <div class="modal-header">
          <h3>Workflow Statistics</h3>
          <button class="close-btn" @click="closeStatsModal">
            <XIcon size="20" />
          </button>
        </div>
        <div class="modal-body">
          <div v-if="loadingModalStats" class="loading">Loading statistics...</div>
          <div v-else-if="currentStats">
            <div class="stats-overview">
              <div class="stat-box">
                <div class="stat-label">Total Executions</div>
                <div class="stat-value">{{ currentStats.totalExecutions || 0 }}</div>
              </div>
              <div class="stat-box">
                <div class="stat-label">Success Rate</div>
                <div class="stat-value">
                  {{ currentStats.totalExecutions > 0
                    ? Math.round((currentStats.successCount / currentStats.totalExecutions) * 100)
                    : 0 }}%
                </div>
              </div>
              <div class="stat-box">
                <div class="stat-label">Last Execution</div>
                <div class="stat-value stat-small">
                  {{ currentStats.lastExecution
                    ? formatTime(currentStats.lastExecution)
                    : 'Never' }}
                </div>
              </div>
            </div>

            <div class="recent-logs" v-if="currentStats.recentLogs && currentStats.recentLogs.length > 0">
              <h4>Recent Executions (Last 24h)</h4>
              <div class="log-list">
                <div
                  v-for="(log, index) in currentStats.recentLogs"
                  :key="index"
                  class="log-item"
                  :class="log.status"
                >
                  <div class="log-time">{{ formatTime(log.time) }}</div>
                  <div class="log-status">{{ log.status }}</div>
                  <div class="log-duration" v-if="log.executionTimeMs">
                    {{ log.executionTimeMs }}ms
                  </div>
                </div>
              </div>
            </div>
            <div v-else class="no-logs">
              No executions in the last 24 hours
            </div>
          </div>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue';
import { PlusIcon, ChartBarIcon, PowerIcon, EditIcon, TrashIcon, XIcon, PlayIcon } from 'lucide-vue-next';
import { useRouter } from 'vue-router';
import { api } from '../services/api.js';

const router = useRouter();

const stats = ref({});
const areas = ref([]);
const loadingStats = ref(true);
const loadingAreas = ref(true);
const showStatsModal = ref(false);
const loadingModalStats = ref(false);
const currentStats = ref(null);

async function loadDashboard() {
  try {
    loadingStats.value = true;
    const dashboardStats = await api.getDashboardStats();
    stats.value = dashboardStats;
  } catch (error) {
    console.error('Failed to load dashboard stats:', error);
  } finally {
    loadingStats.value = false;
  }

  try {
    loadingAreas.value = true;

    // Load both workflows and areas, then combine them
    const [workflowsList, areasList] = await Promise.all([
      api.getWorkflows().catch(() => []),
      api.getAreas().catch(() => [])
    ]);

    // Combine workflows and areas into a single list
    const combinedAreas = [];

    // Add workflows
    workflowsList.forEach(w => {
      combinedAreas.push({
        id: `workflow-${w.id}`,
        realId: w.id,
        name: w.name || `Workflow #${w.id}`,
        active: w.active,
        description: w.description,
        type: 'workflow'
      });
    });

    // Add areas (Gmail → Discord)
    areasList.forEach(a => {
      combinedAreas.push({
        id: `area-${a.id}`,
        realId: a.id,
        name: `Gmail → Discord #${a.id}`,
        active: a.active,
        description: 'Gmail to Discord automation',
        type: 'area'
      });
    });

    areas.value = combinedAreas;
  } catch (error) {
    console.error('Failed to load areas and workflows:', error);
  } finally {
    loadingAreas.value = false;
  }
}

function formatTime(timestamp) {
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

  return date.toLocaleDateString();
}

function openArea(areaId) {
  router.push({ name: 'editor', params: { areaId } });
}

function createNewArea() {
  const newId = `area-${Date.now()}`;
  router.push({ name: 'editor', params: { areaId: newId } });
}

async function toggleArea(area) {
  try {
    if (area.type === 'area') {
      // Use AREA API
      const updated = await api.toggleAreaStatus(area.realId);
      // Update local state
      const index = areas.value.findIndex(a => a.id === area.id);
      if (index !== -1) {
        areas.value[index].active = updated.active;
      }
    } else {
      // Use Workflow API
      const updated = await api.updateWorkflowStatus(area.realId, !area.active);
      // Update local state
      const index = areas.value.findIndex(a => a.id === area.id);
      if (index !== -1) {
        areas.value[index].active = updated.active;
      }
    }

    // Reload stats to reflect the change
    loadDashboard();
  } catch (error) {
    console.error('Failed to toggle area/workflow:', error);
    alert('Failed to update status: ' + error.message);
  }
}

async function deleteArea(area) {
  if (!confirm(`Are you sure you want to delete "${area.name}"? This action cannot be undone.`)) {
    return;
  }

  try {
    if (area.type === 'area') {
      // Use AREA API
      await api.deleteArea(area.realId);
    } else {
      // Use Workflow API
      await api.deleteWorkflow(area.realId);
    }

    // Remove from local state
    areas.value = areas.value.filter(a => a.id !== area.id);

    // Reload stats to reflect the change
    loadDashboard();
  } catch (error) {
    console.error('Failed to delete area/workflow:', error);
    alert('Failed to delete: ' + error.message);
  }
}

async function testWorkflow(area) {
  if (!confirm(`Execute "${area.name}" now for testing?`)) {
    return;
  }

  try {
    const result = await api.executeWorkflow(area.id);
    alert(`✓ ${result.message || 'Workflow executed successfully!'}\n\nCheck the stats to see the execution result.`);

    // Reload to show updated execution in stats
    loadDashboard();
  } catch (error) {
    console.error('Failed to execute workflow:', error);
    alert('Failed to execute workflow: ' + error.message);
  }
}

async function viewStats(areaId) {
  showStatsModal.value = true;
  loadingModalStats.value = true;

  try {
    currentStats.value = await api.getWorkflowStats(areaId);
  } catch (error) {
    console.error('Failed to load stats:', error);
    alert('Failed to load statistics: ' + error.message);
  } finally {
    loadingModalStats.value = false;
  }
}

function closeStatsModal() {
  showStatsModal.value = false;
  currentStats.value = null;
}

onMounted(() => {
  loadDashboard();
});
</script>

<style scoped src="../assets/AreasDashboard.css"></style>
