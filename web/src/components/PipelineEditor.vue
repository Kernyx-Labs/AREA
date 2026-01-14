<template>
  <div class="pipeline-editor">
    <!-- Header -->
    <header class="editor-header">
      <input
        v-model="workflowName"
        class="workflow-name-input"
        placeholder="Enter workflow name..."
        @focus="titleFocused = true"
        @blur="titleFocused = false"
      />
      <div class="header-actions">
        <button
          class="btn-test"
          @click="testWorkflow"
          :disabled="!canSave"
          title="Test workflow"
        >
          <PlayIcon :size="16" /> Test
        </button>
        <button
          class="btn-save"
          @click="saveWorkflow"
          :disabled="!canSave"
          title="Save and activate workflow"
        >
          <SaveIcon :size="16" /> Save & Activate
        </button>
      </div>
    </header>

    <!-- Main Layout: Sidebar + Canvas -->
    <div class="editor-layout">
      <!-- Left Sidebar: Service Palette -->
      <ServicePalette
        @drag-start="handleDragStart"
        @drag-end="handleDragEnd"
      />

      <!-- Main Canvas: Workflow Builder -->
      <WorkflowCanvas
        ref="canvasRef"
        :initial-trigger="trigger"
        :initial-actions="actions"
        @workflow-change="handleWorkflowChange"
        @trigger-change="handleTriggerChange"
        @actions-change="handleActionsChange"
      />
    </div>

    <!-- Status Bar (optional) -->
    <footer v-if="statusMessage" class="editor-footer">
      <div class="status-message" :class="`status-${statusType}`">
        {{ statusMessage }}
      </div>
    </footer>
  </div>
</template>

<script setup>
import { ref, computed, onMounted } from 'vue'
import { useRouter, useRoute } from 'vue-router'
import { PlayIcon, SaveIcon } from 'lucide-vue-next'
import ServicePalette from './workflow/ServicePalette.vue'
import WorkflowCanvas from './workflow/WorkflowCanvas.vue'
import { api } from '@/services/api'

const router = useRouter()
const route = useRoute()

const workflowName = ref('New Workflow')
const titleFocused = ref(false)
const trigger = ref(null)
const actions = ref([])
const canvasRef = ref(null)
const statusMessage = ref('')
const statusType = ref('info') // 'info', 'success', 'error', 'warning'
const isLoading = ref(false)

const canSave = computed(() => {
  return trigger.value !== null &&
         actions.value.length > 0 &&
         workflowName.value.trim() !== ''
})

function handleDragStart(data) {
  // Optional: Show visual feedback during drag
  console.log('Dragging:', data)
}

function handleDragEnd() {
  // Optional: Clear visual feedback
}

function handleWorkflowChange(workflow) {
  trigger.value = workflow.trigger
  actions.value = workflow.actions
}

function handleTriggerChange(newTrigger) {
  trigger.value = newTrigger
}

function handleActionsChange(newActions) {
  actions.value = newActions
}

async function testWorkflow() {
  if (!canSave.value) {
    showStatus('Please add a trigger, an action, and name your workflow first', 'warning')
    return
  }

  showStatus('Testing workflow...', 'info')

  try {
    // Build workflow data
    const workflowData = buildWorkflowData()

    // For testing, we'll just log the workflow structure
    console.log('Test workflow:', workflowData)

    showStatus('Workflow structure is valid! Ready to save.', 'success')
  } catch (error) {
    console.error('Test failed:', error)
    showStatus(`Test failed: ${error.message}`, 'error')
  }
}

async function saveWorkflow() {
  if (!canSave.value) {
    showStatus('Please add a trigger, an action, and name your workflow first', 'warning')
    return
  }

  isLoading.value = true
  showStatus('Saving workflow...', 'info')

  try {
    const workflowData = buildWorkflowData()

    // Check if this workflow should be saved as an Area (gets polled by scheduler)
    const isTimerWorkflow = trigger.value.serviceName === 'Timer'
    const isGmailDiscord = trigger.value.serviceName === 'Gmail' &&
                           actions.value.length > 0 &&
                           actions.value[0].serviceName === 'Discord'

    if (isGmailDiscord) {
      await saveAsArea(workflowData)
    } else if (isTimerWorkflow && actions.value[0].serviceName === 'Discord') {
      await saveAsTimerArea(workflowData)
    } else {
      await saveAsWorkflow(workflowData)
    }

    showStatus('Workflow saved successfully!', 'success')

    // Redirect to dashboard after a delay
    setTimeout(() => {
      router.push({ name: 'dashboard' })
    }, 1500)
  } catch (error) {
    console.error('Save failed:', error)
    showStatus(`Save failed: ${error.message}`, 'error')
  } finally {
    isLoading.value = false
  }
}

function buildWorkflowData() {
  if (!trigger.value) {
    throw new Error('Trigger is required')
  }

  if (!actions.value || actions.value.length === 0) {
    throw new Error('At least one action is required')
  }

  return {
    name: workflowName.value.trim(),
    trigger: {
      service: trigger.value.serviceName,
      type: trigger.value.eventId,
      config: trigger.value.config || {}
    },
    actions: actions.value.map(action => ({
      service: action.serviceName,
      type: action.eventId,
      config: action.config || {}
    }))
  }
}

async function saveAsArea(workflowData) {
  // Get service connections
  const connections = await api.getConnectedServices()
  const gmailConnection = connections.find(c => c.type === 'GMAIL')
  const discordConnection = connections.find(c => c.type === 'DISCORD')

  if (!gmailConnection) {
    throw new Error('Gmail not connected. Please connect Gmail in Services page.')
  }

  if (!discordConnection) {
    throw new Error('Discord not connected. Please connect Discord in Services page.')
  }

  // Extract channel ID from Discord connection metadata
  let discordChannelId = null
  if (discordConnection.metadata) {
    try {
      const metadata = JSON.parse(discordConnection.metadata)
      discordChannelId = metadata.channelId
    } catch (e) {
      console.error('Failed to parse Discord metadata:', e)
    }
  }

  if (!discordChannelId) {
    throw new Error('Discord channel ID not found in connection. Please reconnect Discord in Services page.')
  }

  // Extract config
  const triggerConfig = workflowData.trigger.config || {}
  const actionConfig = workflowData.action?.config || {}

  const areaData = {
    actionConnectionId: gmailConnection.id,
    reactionConnectionId: discordConnection.id,
    gmailLabel: triggerConfig.label || 'INBOX',
    gmailSubjectContains: triggerConfig.subjectFilter || '',
    gmailFromAddress: triggerConfig.senderFilter || '',
    discordChannelId: discordChannelId, // Use channelId from ServiceConnection
    discordChannelName: actionConfig.channelName || 'general',
    discordMessageTemplate: actionConfig.message || actionConfig.messageTemplate || 'New email from {from}\nSubject: {subject}'
  }

  await api.createArea(areaData)
}

async function saveAsTimerArea(workflowData) {
  // Get Discord service connection
  const connections = await api.getConnectedServices()
  const discordConnection = connections.find(c => c.type === 'DISCORD')

  if (!discordConnection) {
    throw new Error('Discord not connected. Please connect Discord in Services page.')
  }

  // Extract channel ID from Discord connection metadata
  let discordChannelId = null
  if (discordConnection.metadata) {
    try {
      const metadata = JSON.parse(discordConnection.metadata)
      discordChannelId = metadata.channelId
    } catch (e) {
      console.error('Failed to parse Discord metadata:', e)
    }
  }

  if (!discordChannelId) {
    throw new Error('Discord channel ID not found in connection. Please reconnect Discord in Services page.')
  }

  // Extract config
  const triggerConfig = workflowData.trigger.config || {}
  const actionConfig = workflowData.action?.config || {}

  // Determine timer type from event name
  const timerType = workflowData.trigger.event.replace('timer.', '')

  const areaData = {
    timerConnectionId: null, // Timer doesn't require connection (time-based)
    reactionConnectionId: discordConnection.id,
    timerType: timerType,
    intervalMinutes: triggerConfig.intervalMinutes || 5,
    daysCount: triggerConfig.daysCount || null,
    targetDay: triggerConfig.targetDay || null,
    discordChannelName: actionConfig.channelName || 'general',
    discordMessageTemplate: actionConfig.message || actionConfig.messageTemplate ||
      '⏰ Timer Alert!\nCurrent Date: {{date}}\nCurrent Time: {{time}}\nDay: {{dayOfWeek}}',
    actionType: workflowData.trigger.event,
    reactionType: 'discord.send_webhook'
  }

  await api.createTimerArea(areaData)
}

async function saveAsWorkflow(workflowData) {
  // Inject connection IDs
  try {
    const connections = await api.getConnectedServices()

    // Inject for trigger
    const triggerService = workflowData.trigger.service.toUpperCase()
    const triggerConnection = connections.find(c => c.type === triggerService)
    if (triggerConnection) {
      workflowData.trigger.connectionId = triggerConnection.id
    } else {
       console.warn(`No connection found for trigger service ${triggerService}`)
    }

    // Inject for actions
    for (const action of workflowData.actions) {
      const actionService = action.service.toUpperCase()
      const actionConnection = connections.find(c => c.type === actionService)
      if (actionConnection) {
        action.connectionId = actionConnection.id
      }
    }
  } catch (e) {
    console.warn('Failed to inject connection IDs:', e)
  }

  await api.createWorkflow(workflowData)
}

function showStatus(message, type = 'info') {
  statusMessage.value = message
  statusType.value = type

  // Auto-clear after 5 seconds for non-error messages
  if (type !== 'error') {
    setTimeout(() => {
      if (statusMessage.value === message) {
        statusMessage.value = ''
      }
    }, 5000)
  }
}

async function loadWorkflow() {
  const workflowId = route.params.areaId

  if (!workflowId || workflowId.startsWith('area-')) {
    // New workflow
    return
  }

  try {
    showStatus('Loading workflow...', 'info')

    // Extract numeric ID
    const numericId = workflowId.replace('workflow-', '')
    const workflow = await api.getWorkflow(numericId)

    // Populate editor with workflow data
    workflowName.value = workflow.name || 'Untitled Workflow'

    if (workflow.trigger) {
      trigger.value = {
        serviceName: workflow.trigger.service,
        eventName: workflow.trigger.event,
        config: workflow.trigger.config || {},
        // These would need to be looked up from available services
        serviceColor: '#5b9bd5',
        serviceIcon: null,
        configFields: []
      }
    }

    // Handle both old format (actions array) and new format (single action)
    if (workflow.action) {
      // New format: single action
      actions.value = [{
        id: 'action-1',
        serviceName: workflow.action.service,
        eventName: workflow.action.event,
        config: workflow.action.config || {},
        serviceColor: '#5b9bd5',
        serviceIcon: null,
        configFields: []
      }]
    } else if (workflow.actions && workflow.actions.length > 0) {
      // Old format: actions array
      actions.value = workflow.actions.map((action, index) => ({
        id: `action-${index + 1}`,
        serviceName: action.service,
        eventName: action.event,
        config: action.config || {},
        serviceColor: '#5b9bd5',
        serviceIcon: null,
        configFields: []
      }))
    }

    statusMessage.value = ''
  } catch (error) {
    console.error('Failed to load workflow:', error)
    showStatus(`Failed to load workflow: ${error.message}`, 'error')
  }
}

onMounted(() => {
  loadWorkflow()
})
</script>

<style scoped>
.pipeline-editor {
  display: flex;
  flex-direction: column;
  height: 100vh;
  background: var(--color-bg);
  overflow: hidden;
}

/* Header */
.editor-header {
  height: 64px;
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 0 var(--space-xl);
  background: var(--color-surface);
  border-bottom: 1px solid var(--color-border-default);
  flex-shrink: 0;
  box-shadow: var(--shadow-sm);
  z-index: 10;
}

.workflow-name-input {
  background: transparent;
  color: var(--color-text-primary);
  font-weight: 600;
  font-size: 1.2rem;
  outline: none;
  border: none;
  border-bottom: 2px solid transparent;
  padding: var(--space-xs) var(--space-sm);
  transition: border-color 0.2s ease;
  min-width: 300px;
  border-radius: var(--radius-sm);
}

.workflow-name-input:focus {
  border-bottom-color: var(--color-accent-blue);
  background: var(--color-hover);
}

.workflow-name-input::placeholder {
  color: var(--color-text-muted);
}

.header-actions {
  display: flex;
  align-items: center;
  gap: var(--space-md);
}

.btn-test,
.btn-save {
  display: flex;
  align-items: center;
  gap: var(--space-sm);
  padding: var(--space-sm) var(--space-lg);
  border-radius: var(--radius-md);
  font-weight: 600;
  border: none;
  cursor: pointer;
  transition: all 0.2s ease;
  font-size: 0.95rem;
}

.btn-test {
  background: var(--color-surface-raised);
  color: var(--color-text-primary);
  border: 1px solid var(--color-border-default);
}

.btn-test:hover:not(:disabled) {
  background: var(--color-surface-elevated);
  border-color: var(--color-border-bright);
  transform: translateY(-2px);
  box-shadow: var(--shadow-md);
}

.btn-save {
  background: linear-gradient(135deg, var(--color-accent-blue), var(--color-accent-purple));
  color: white;
  box-shadow: var(--shadow-md);
}

.btn-save:hover:not(:disabled) {
  transform: translateY(-2px);
  box-shadow: var(--shadow-lg), 0 0 20px rgba(91, 155, 213, 0.4);
}

.btn-test:disabled,
.btn-save:disabled {
  opacity: 0.5;
  cursor: not-allowed;
  transform: none;
  box-shadow: none;
}

/* Main Layout */
.editor-layout {
  flex: 1;
  display: grid;
  grid-template-columns: 320px 1fr;
  overflow: hidden;
}

/* Footer / Status Bar */
.editor-footer {
  height: 40px;
  background: var(--color-surface);
  border-top: 1px solid var(--color-border-default);
  display: flex;
  align-items: center;
  padding: 0 var(--space-xl);
  flex-shrink: 0;
}

.status-message {
  font-size: 0.85rem;
  font-weight: 600;
  padding: var(--space-xs) var(--space-md);
  border-radius: var(--radius-sm);
}

.status-info {
  color: var(--color-info);
  background: var(--color-info-bg);
}

.status-success {
  color: var(--color-success);
  background: var(--color-success-bg);
}

.status-warning {
  color: var(--color-warning);
  background: var(--color-warning-bg);
}

.status-error {
  color: var(--color-danger);
  background: var(--color-danger-bg);
}

/* Responsive */
@media (max-width: 1024px) {
  .editor-layout {
    grid-template-columns: 280px 1fr;
  }
}

@media (max-width: 768px) {
  .editor-layout {
    grid-template-columns: 1fr;
  }

  .workflow-name-input {
    min-width: 200px;
    font-size: 1rem;
  }

  .header-actions {
    gap: var(--space-sm);
  }

  .btn-test span,
  .btn-save span {
    display: none;
  }
}
</style>
