<template>
  <aside class="service-palette">
    <header class="palette-header">
      <h3>Services & Events</h3>
      <div class="palette-search">
        <input
          v-model="searchQuery"
          type="text"
          placeholder="Search services..."
          class="search-input"
        />
      </div>
    </header>

    <div class="palette-body">
      <div v-if="loading" class="palette-loading">
        <div class="spinner"></div>
        <p>Loading services...</p>
      </div>

      <div v-else-if="error" class="palette-error">
        <p>Failed to load services</p>
        <button @click="loadServices" class="retry-btn">Retry</button>
      </div>

      <div v-else class="services-list">
        <div
          v-for="service in filteredServices"
          :key="service.name"
          class="service-section"
          :style="{ '--service-color': getServiceColor(service.name) }"
        >
          <button
            class="service-header"
            :class="{ 'service-header--open': expandedServices.includes(service.name) }"
            @click="toggleService(service.name)"
          >
            <div class="service-header-left">
              <component
                v-if="getServiceIcon(service.name)"
                :is="getServiceIcon(service.name)"
                :size="20"
                :color="getServiceColor(service.name)"
              />
              <span class="service-name">{{ service.name }}</span>
            </div>
            <div class="service-count" :style="{ background: getServiceColor(service.name) }">
              {{ (service.actions?.length || 0) + (service.reactions?.length || 0) }}
            </div>
          </button>

          <div
            v-show="expandedServices.includes(service.name)"
            class="service-events"
          >
            <!-- Triggers/Actions -->
            <div v-if="service.actions && service.actions.length > 0" class="event-group">
              <div class="event-group-label">Triggers</div>
              <div
                v-for="action in service.actions"
                :key="action.name"
                class="event-item"
                draggable="true"
                :style="{ '--service-color': getServiceColor(service.name) }"
                @dragstart="handleDragStart($event, service, action, 'trigger')"
                @dragend="handleDragEnd"
              >
                <div class="event-icon" :style="{ background: getServiceColor(service.name) }">
                  <component
                    v-if="getServiceIcon(service.name)"
                    :is="getServiceIcon(service.name)"
                    :size="16"
                    color="white"
                  />
                </div>
                <div class="event-info">
                  <div class="event-name">{{ action.name }}</div>
                  <div v-if="action.description" class="event-description">
                    {{ action.description }}
                  </div>
                </div>
              </div>
            </div>

            <!-- Reactions/Actions -->
            <div v-if="service.reactions && service.reactions.length > 0" class="event-group">
              <div class="event-group-label">Actions</div>
              <div
                v-for="reaction in service.reactions"
                :key="reaction.name"
                class="event-item"
                draggable="true"
                :style="{ '--service-color': getServiceColor(service.name) }"
                @dragstart="handleDragStart($event, service, reaction, 'action')"
                @dragend="handleDragEnd"
              >
                <div class="event-icon" :style="{ background: getServiceColor(service.name) }">
                  <component
                    v-if="getServiceIcon(service.name)"
                    :is="getServiceIcon(service.name)"
                    :size="16"
                    color="white"
                  />
                </div>
                <div class="event-info">
                  <div class="event-name">{{ reaction.name }}</div>
                  <div v-if="reaction.description" class="event-description">
                    {{ reaction.description }}
                  </div>
                </div>
              </div>
            </div>
          </div>
        </div>
      </div>
    </div>
  </aside>
</template>

<script setup>
import { ref, computed, onMounted } from 'vue'
import {
  MailIcon,
  ClockIcon,
  GithubIcon,
  MessageSquareIcon,
  CloudIcon,
  CalendarIcon,
  ZapIcon
} from 'lucide-vue-next'
import { api } from '@/services/api'

const emit = defineEmits(['drag-start', 'drag-end'])

const services = ref([])
const loading = ref(true)
const error = ref(false)
const searchQuery = ref('')
const expandedServices = ref([])

// Service icons and colors mapping (vibrant brand colors)
const serviceMetadata = {
  Gmail: { icon: MailIcon, color: '#EA4335' },        // Google Red
  GitHub: { icon: GithubIcon, color: '#7C3AED' },      // Vibrant Purple
  Discord: { icon: MessageSquareIcon, color: '#5865F2' }, // Discord Blurple
  Dropbox: { icon: CloudIcon, color: '#0061FF' },      // Dropbox Blue
  Outlook: { icon: CalendarIcon, color: '#0078D4' },   // Microsoft Blue
  Timer: { icon: ClockIcon, color: '#10B981' },        // Emerald Green
  Webhook: { icon: ZapIcon, color: '#F59E0B' },        // Amber
  Slack: { icon: MessageSquareIcon, color: '#E01E5A' }, // Slack Magenta
  Trello: { icon: CloudIcon, color: '#0079BF' },       // Trello Blue
  Spotify: { icon: ZapIcon, color: '#1DB954' },        // Spotify Green
  Twitter: { icon: MessageSquareIcon, color: '#1DA1F2' }, // Twitter Blue
  Facebook: { icon: MessageSquareIcon, color: '#1877F2' }, // Facebook Blue
  Instagram: { icon: ZapIcon, color: '#E4405F' },      // Instagram Pink
  YouTube: { icon: ZapIcon, color: '#FF0000' },        // YouTube Red
  LinkedIn: { icon: MessageSquareIcon, color: '#0A66C2' }, // LinkedIn Blue
  Notion: { icon: CloudIcon, color: '#000000' },       // Notion Black
  Asana: { icon: CloudIcon, color: '#F06A6A' }         // Asana Coral
}

const filteredServices = computed(() => {
  if (!searchQuery.value.trim()) {
    return services.value
  }

  const query = searchQuery.value.toLowerCase()
  return services.value.filter(service => {
    // Search in service name
    if (service.name.toLowerCase().includes(query)) {
      return true
    }

    // Search in action names and descriptions
    const hasMatchingAction = service.actions?.some(action =>
      action.name.toLowerCase().includes(query) ||
      action.description?.toLowerCase().includes(query)
    )

    // Search in reaction names and descriptions
    const hasMatchingReaction = service.reactions?.some(reaction =>
      reaction.name.toLowerCase().includes(query) ||
      reaction.description?.toLowerCase().includes(query)
    )

    return hasMatchingAction || hasMatchingReaction
  })
})

function getServiceIcon(serviceName) {
  return serviceMetadata[serviceName]?.icon || ZapIcon
}

function getServiceColor(serviceName) {
  return serviceMetadata[serviceName]?.color || '#5b9bd5'
}

function toggleService(serviceName) {
  const index = expandedServices.value.indexOf(serviceName)
  if (index > -1) {
    expandedServices.value.splice(index, 1)
  } else {
    expandedServices.value.push(serviceName)
  }
}

function handleDragStart(event, service, eventData, eventType) {
  const dragData = {
    serviceName: service.name,
    serviceColor: getServiceColor(service.name),
    serviceIcon: getServiceIcon(service.name),
    eventName: eventData.name,
    eventDescription: eventData.description,
    eventType: eventType, // 'trigger' or 'action'
    eventId: eventData.name,
    configFields: eventData.configFields || []
  }

  event.dataTransfer.effectAllowed = 'copy'
  event.dataTransfer.setData('application/json', JSON.stringify(dragData))

  // Visual feedback
  event.target.style.opacity = '0.5'

  emit('drag-start', dragData)
}

function handleDragEnd(event) {
  event.target.style.opacity = '1'
  emit('drag-end')
}

async function loadServices() {
  loading.value = true
  error.value = false

  try {
    const data = await api.getAvailableServices()
    services.value = data || []

    // Expand first service by default
    if (services.value.length > 0) {
      expandedServices.value = [services.value[0].name]
    }
  } catch (err) {
    console.error('Failed to load services:', err)
    error.value = true
  } finally {
    loading.value = false
  }
}

onMounted(() => {
  loadServices()
})
</script>

<style scoped>
.service-palette {
  display: flex;
  flex-direction: column;
  height: 100%;
  background: var(--color-surface);
  border-right: 1px solid var(--color-border-default);
  overflow: hidden;
}

.palette-header {
  padding: var(--space-lg);
  border-bottom: 1px solid var(--color-border-default);
  background: var(--color-surface-raised);
}

.palette-header h3 {
  margin: 0 0 var(--space-md) 0;
  font-size: 1.1rem;
  font-weight: 600;
  color: var(--color-text-primary);
}

.palette-search {
  position: relative;
}

.search-input {
  width: 100%;
  background: var(--color-surface);
  border: 1px solid var(--color-border-default);
  border-radius: var(--radius-md);
  padding: var(--space-sm) var(--space-md);
  font-size: 0.9rem;
  color: var(--color-text-primary);
  transition: all 0.2s ease;
}

.search-input:focus {
  outline: none;
  border-color: var(--color-accent-blue);
  box-shadow: 0 0 0 3px rgba(91, 155, 213, 0.1);
}

.search-input::placeholder {
  color: var(--color-text-muted);
}

.palette-body {
  flex: 1;
  overflow-y: auto;
  padding: var(--space-sm);
}

/* Loading & Error States */
.palette-loading,
.palette-error {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  padding: var(--space-xl);
  gap: var(--space-md);
}

.spinner {
  width: 32px;
  height: 32px;
  border: 3px solid var(--color-border-default);
  border-top-color: var(--color-accent-blue);
  border-radius: 50%;
  animation: spin 0.8s linear infinite;
}

@keyframes spin {
  to { transform: rotate(360deg); }
}

.palette-loading p,
.palette-error p {
  color: var(--color-text-secondary);
  font-size: 0.9rem;
}

.retry-btn {
  padding: var(--space-sm) var(--space-lg);
  background: var(--color-accent-blue);
  color: white;
  border: none;
  border-radius: var(--radius-md);
  font-weight: 600;
  cursor: pointer;
  transition: all 0.2s ease;
}

.retry-btn:hover {
  background: var(--color-accent-purple);
  transform: translateY(-2px);
  box-shadow: var(--shadow-md);
}

/* Services List */
.services-list {
  display: flex;
  flex-direction: column;
  gap: var(--space-xs);
}

.service-section {
  background: var(--color-surface-raised);
  border: 1px solid var(--color-border-default);
  border-left: 3px solid var(--service-color, var(--color-border-default));
  border-radius: var(--radius-md);
  overflow: hidden;
  transition: all 0.2s ease;
}

.service-section:hover {
  border-color: var(--color-border-bright);
  border-left-color: var(--service-color, var(--color-border-bright));
}

.service-header {
  width: 100%;
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: var(--space-md);
  background: transparent;
  border: none;
  cursor: pointer;
  transition: all 0.2s ease;
}

.service-header:hover {
  background: var(--color-hover);
}

.service-header--open {
  background: var(--color-hover-strong);
}

.service-header-left {
  display: flex;
  align-items: center;
  gap: var(--space-sm);
}

.service-name {
  font-weight: 600;
  font-size: 0.95rem;
  color: var(--color-text-primary);
}

.service-count {
  color: white;
  padding: 0.2rem 0.5rem;
  border-radius: var(--radius-full);
  font-size: 0.75rem;
  font-weight: 600;
  min-width: 24px;
  text-align: center;
  box-shadow: var(--shadow-sm);
}

/* Events */
.service-events {
  padding: var(--space-sm) var(--space-md) var(--space-md);
  border-top: 1px solid var(--color-border-subtle);
  display: flex;
  flex-direction: column;
  gap: var(--space-md);
}

.event-group {
  display: flex;
  flex-direction: column;
  gap: var(--space-xs);
}

.event-group-label {
  font-size: 0.75rem;
  font-weight: 600;
  color: var(--color-text-tertiary);
  text-transform: uppercase;
  letter-spacing: 0.05em;
  margin-bottom: var(--space-xs);
}

.event-item {
  display: flex;
  align-items: center;
  gap: var(--space-sm);
  padding: var(--space-sm);
  background: var(--color-surface);
  border: 1px solid var(--color-border-default);
  border-radius: var(--radius-md);
  cursor: grab;
  transition: all 0.2s ease;
  user-select: none;
}

.event-item:hover {
  border-color: var(--service-color, var(--color-border-bright));
  background: var(--color-surface-elevated);
  transform: translateX(4px);
  box-shadow: 0 0 0 3px color-mix(in srgb, var(--service-color, #5b9bd5) 15%, transparent),
              var(--shadow-sm);
}

.event-item:active {
  cursor: grabbing;
  opacity: 0.5;
}

.event-icon {
  width: 32px;
  height: 32px;
  border-radius: var(--radius-sm);
  display: flex;
  align-items: center;
  justify-content: center;
  flex-shrink: 0;
  box-shadow: var(--shadow-sm);
}

.event-info {
  flex: 1;
  min-width: 0;
}

.event-name {
  font-size: 0.85rem;
  font-weight: 600;
  color: var(--color-text-primary);
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
}

.event-description {
  font-size: 0.75rem;
  color: var(--color-text-muted);
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
  margin-top: 2px;
}
</style>
