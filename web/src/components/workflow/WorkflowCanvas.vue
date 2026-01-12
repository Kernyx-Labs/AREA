<template>
  <div class="workflow-canvas">
    <div class="canvas-content">
      <!-- Trigger Zone -->
      <section class="trigger-zone">
        <div class="zone-header">
          <h3>Trigger</h3>
          <p class="zone-subtitle">When this happens...</p>
        </div>

        <div
          class="drop-zone"
          :class="{
            'drop-zone--active': isDragOverTrigger,
            'drop-zone--filled': trigger !== null,
            'drop-zone--error': dragOverError === 'trigger'
          }"
          @dragover="handleDragOver($event, 'trigger')"
          @dragleave="handleDragLeave"
          @drop="handleDrop($event, 'trigger')"
        >
          <WorkflowBlock
            v-if="trigger"
            :service-name="trigger.serviceName"
            :service-color="trigger.serviceColor"
            :service-icon="trigger.serviceIcon"
            :event-name="trigger.eventName"
            :event-description="trigger.eventDescription"
            :event-type="'trigger'"
            :config-fields="trigger.configFields"
            :config="trigger.config"
            :show-remove="true"
            @remove="removeTrigger"
            @save="saveTriggerConfig"
          />

          <div v-else class="drop-placeholder">
            <div class="placeholder-icon">
              <ZapIcon :size="48" :stroke-width="1.5" />
            </div>
            <p class="placeholder-text">Drag a trigger here to start</p>
            <p class="placeholder-hint">Only one trigger allowed</p>
          </div>
        </div>
      </section>

      <!-- Arrow Connector -->
      <div v-if="trigger" class="arrow-connector">
        <svg class="arrow-svg" viewBox="0 0 40 100" preserveAspectRatio="none">
          <defs>
            <marker
              id="arrowhead"
              markerWidth="10"
              markerHeight="10"
              refX="5"
              refY="5"
              orient="auto"
            >
              <polygon points="0 0, 10 5, 0 10" fill="var(--color-accent-blue)" />
            </marker>
          </defs>
          <line
            x1="20"
            y1="0"
            x2="20"
            y2="100"
            stroke="var(--color-accent-blue)"
            stroke-width="3"
            stroke-dasharray="5,5"
            marker-end="url(#arrowhead)"
          />
        </svg>
      </div>

      <!-- Actions Zone -->
      <section class="actions-zone">
        <div class="zone-header">
          <h3>Actions</h3>
          <p class="zone-subtitle">Do this...</p>
        </div>

        <div class="actions-list">
          <TransitionGroup name="action-list">
            <div
              v-for="(action, index) in actions"
              :key="action.id"
              class="action-item"
            >
              <WorkflowBlock
                :service-name="action.serviceName"
                :service-color="action.serviceColor"
                :service-icon="action.serviceIcon"
                :event-name="action.eventName"
                :event-description="action.eventDescription"
                :event-type="'action'"
                :config-fields="action.configFields"
                :config="action.config"
                :available-variables="availableVariables"
                :show-remove="true"
                @remove="removeAction(index)"
                @save="saveActionConfig(index, $event)"
              />

              <!-- Arrow between actions -->
              <div v-if="index < actions.length - 1" class="action-arrow">
                <svg class="arrow-svg" viewBox="0 0 40 60" preserveAspectRatio="none">
                  <line
                    x1="20"
                    y1="0"
                    x2="20"
                    y2="60"
                    stroke="var(--color-accent-blue)"
                    stroke-width="3"
                    stroke-dasharray="5,5"
                    marker-end="url(#arrowhead)"
                  />
                </svg>
              </div>
            </div>
          </TransitionGroup>

          <!-- Add Action Drop Zone -->
          <div
            v-if="trigger"
            class="add-action-zone"
            :class="{
              'drop-zone--active': isDragOverAction,
              'drop-zone--error': dragOverError === 'action'
            }"
            @dragover="handleDragOver($event, 'action')"
            @dragleave="handleDragLeave"
            @drop="handleDrop($event, 'action')"
          >
            <button class="add-action-btn">
              <PlusIcon :size="24" />
              <span>Drop action here or click to add</span>
            </button>
          </div>

          <!-- Empty State -->
          <div v-else class="actions-empty">
            <div class="placeholder-icon">
              <LayersIcon :size="48" :stroke-width="1.5" />
            </div>
            <p class="placeholder-text">Add a trigger first</p>
            <p class="placeholder-hint">Then you can chain multiple actions</p>
          </div>
        </div>
      </section>
    </div>
  </div>
</template>

<script setup>
import { ref, computed } from 'vue'
import { ZapIcon, PlusIcon, LayersIcon } from 'lucide-vue-next'
import WorkflowBlock from './WorkflowBlock.vue'

const props = defineProps({
  initialTrigger: { type: Object, default: null },
  initialActions: { type: Array, default: () => [] }
})

const emit = defineEmits(['workflow-change', 'trigger-change', 'actions-change'])

const trigger = ref(props.initialTrigger)
const actions = ref([...props.initialActions])
const isDragOverTrigger = ref(false)
const isDragOverAction = ref(false)
const dragOverError = ref(null)

let actionIdCounter = 1

// Compute available variables from trigger
const availableVariables = computed(() => {
  if (!trigger.value || !trigger.value.serviceName) {
    return []
  }

  // Extract variable names from trigger
  // These variables are available in the backend when processing the action/reaction
  const serviceVariables = {
    Gmail: ['subject', 'from', 'body', 'label', 'snippet', 'messageId', 'threadId'],
    GitHub: ['author', 'title', 'url', 'repository', 'action'],
    Timer: ['timestamp', 'date', 'time'],
    Discord: ['username', 'content', 'channelId', 'messageId']
  }

  return serviceVariables[trigger.value.serviceName] || []
})

function handleDragOver(event, zone) {
  event.preventDefault()

  try {
    const data = JSON.parse(event.dataTransfer.getData('application/json'))

    // Validate drop zone
    if (zone === 'trigger' && data.eventType !== 'trigger') {
      dragOverError.value = 'trigger'
      event.dataTransfer.dropEffect = 'none'
      return
    }

    if (zone === 'action' && data.eventType !== 'action') {
      dragOverError.value = 'action'
      event.dataTransfer.dropEffect = 'none'
      return
    }

    // Valid drop zone
    dragOverError.value = null
    event.dataTransfer.dropEffect = 'copy'

    if (zone === 'trigger') {
      isDragOverTrigger.value = true
    } else {
      isDragOverAction.value = true
    }
  } catch (err) {
    console.error('Invalid drag data:', err)
  }
}

function handleDragLeave(event) {
  // Only clear if leaving the drop zone, not a child element
  if (event.currentTarget.contains(event.relatedTarget)) {
    return
  }

  isDragOverTrigger.value = false
  isDragOverAction.value = false
  dragOverError.value = null
}

function handleDrop(event, zone) {
  event.preventDefault()
  isDragOverTrigger.value = false
  isDragOverAction.value = false
  dragOverError.value = null

  try {
    const data = JSON.parse(event.dataTransfer.getData('application/json'))

    // Validate and add to appropriate zone
    if (zone === 'trigger' && data.eventType === 'trigger') {
      addTrigger(data)
    } else if (zone === 'action' && data.eventType === 'action') {
      addAction(data)
    }
  } catch (err) {
    console.error('Failed to process drop:', err)
  }
}

function addTrigger(data) {
  trigger.value = {
    ...data,
    config: {},
    id: 'trigger-1'
  }

  emit('trigger-change', trigger.value)
  emitWorkflowChange()
}

function removeTrigger() {
  trigger.value = null
  actions.value = [] // Clear actions when trigger is removed
  emit('trigger-change', null)
  emit('actions-change', [])
  emitWorkflowChange()
}

function saveTriggerConfig(config) {
  if (trigger.value) {
    trigger.value.config = config
    emit('trigger-change', trigger.value)
    emitWorkflowChange()
  }
}

function addAction(data) {
  const newAction = {
    ...data,
    config: {},
    id: `action-${actionIdCounter++}`
  }

  actions.value.push(newAction)
  emit('actions-change', actions.value)
  emitWorkflowChange()
}

function removeAction(index) {
  actions.value.splice(index, 1)
  emit('actions-change', actions.value)
  emitWorkflowChange()
}

function saveActionConfig(index, config) {
  if (actions.value[index]) {
    actions.value[index].config = config
    emit('actions-change', actions.value)
    emitWorkflowChange()
  }
}

function emitWorkflowChange() {
  emit('workflow-change', {
    trigger: trigger.value,
    actions: actions.value
  })
}

// Expose methods for parent component
defineExpose({
  getTrigger: () => trigger.value,
  getActions: () => actions.value,
  getWorkflow: () => ({
    trigger: trigger.value,
    actions: actions.value
  }),
  reset: () => {
    trigger.value = null
    actions.value = []
  }
})
</script>

<style scoped>
.workflow-canvas {
  display: flex;
  flex-direction: column;
  height: 100%;
  background: var(--color-bg);
  overflow-y: auto;
}

.canvas-content {
  flex: 1;
  padding: var(--space-xl);
  max-width: 800px;
  margin: 0 auto;
  width: 100%;
  display: flex;
  flex-direction: column;
  gap: var(--space-lg);
}

/* Zone Header */
.zone-header {
  margin-bottom: var(--space-md);
}

.zone-header h3 {
  font-size: 1.25rem;
  font-weight: 600;
  color: var(--color-text-primary);
  margin: 0 0 var(--space-xs) 0;
}

.zone-subtitle {
  font-size: 0.9rem;
  color: var(--color-text-muted);
  margin: 0;
}

/* Drop Zones */
.drop-zone {
  min-height: 120px;
  border: 2px dashed var(--color-border-default);
  border-radius: var(--radius-lg);
  padding: var(--space-lg);
  transition: all 0.3s ease;
  background: var(--color-surface);
}

.drop-zone--active {
  border-color: var(--color-accent-blue);
  background: rgba(91, 155, 213, 0.1);
  box-shadow: 0 0 0 4px rgba(91, 155, 213, 0.1);
}

.drop-zone--error {
  border-color: var(--color-danger);
  background: rgba(255, 107, 107, 0.05);
}

.drop-zone--filled {
  border-style: solid;
  border-color: var(--color-border-bright);
  padding: 0;
  min-height: auto;
}

/* Placeholders */
.drop-placeholder,
.actions-empty {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  padding: var(--space-xl);
  text-align: center;
}

.placeholder-icon {
  color: var(--color-text-muted);
  opacity: 0.5;
  margin-bottom: var(--space-md);
}

.placeholder-text {
  font-size: 1rem;
  font-weight: 600;
  color: var(--color-text-secondary);
  margin: 0 0 var(--space-xs) 0;
}

.placeholder-hint {
  font-size: 0.85rem;
  color: var(--color-text-muted);
  margin: 0;
}

/* Arrow Connectors */
.arrow-connector {
  display: flex;
  justify-content: center;
  align-items: center;
  height: 60px;
  position: relative;
}

.arrow-svg {
  width: 40px;
  height: 100%;
}

.action-arrow {
  display: flex;
  justify-content: center;
  align-items: center;
  height: 40px;
  margin: var(--space-sm) 0;
}

.action-arrow .arrow-svg {
  width: 40px;
  height: 100%;
}

/* Actions List */
.actions-list {
  display: flex;
  flex-direction: column;
  gap: var(--space-md);
}

.action-item {
  display: flex;
  flex-direction: column;
}

/* Add Action Zone */
.add-action-zone {
  min-height: 80px;
  border: 2px dashed var(--color-border-default);
  border-radius: var(--radius-lg);
  padding: var(--space-md);
  transition: all 0.3s ease;
  background: var(--color-surface);
}

.add-action-btn {
  width: 100%;
  height: 100%;
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  gap: var(--space-sm);
  background: transparent;
  border: none;
  cursor: pointer;
  color: var(--color-text-muted);
  transition: all 0.2s ease;
  padding: var(--space-lg);
  border-radius: var(--radius-md);
}

.add-action-btn:hover {
  background: var(--color-hover);
  color: var(--color-text-primary);
}

.add-action-btn span {
  font-size: 0.9rem;
  font-weight: 600;
}

/* Animations */
.action-list-enter-active,
.action-list-leave-active {
  transition: all 0.3s ease;
}

.action-list-enter-from {
  opacity: 0;
  transform: translateY(-20px);
}

.action-list-leave-to {
  opacity: 0;
  transform: translateX(20px);
}

.action-list-move {
  transition: transform 0.3s ease;
}

/* Empty state for actions */
.actions-empty {
  opacity: 0.6;
}

/* Trigger Zone */
.trigger-zone {
  animation: fadeInUp 0.4s ease;
}

/* Actions Zone */
.actions-zone {
  animation: fadeInUp 0.5s ease;
}

@keyframes fadeInUp {
  from {
    opacity: 0;
    transform: translateY(20px);
  }
  to {
    opacity: 1;
    transform: translateY(0);
  }
}

/* Responsive */
@media (max-width: 768px) {
  .canvas-content {
    padding: var(--space-md);
  }

  .zone-header h3 {
    font-size: 1.1rem;
  }

  .placeholder-icon {
    transform: scale(0.8);
  }
}
</style>
