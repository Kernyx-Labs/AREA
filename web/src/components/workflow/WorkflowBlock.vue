<template>
  <div
    class="workflow-block"
    :class="{
      'workflow-block--expanded': isExpanded,
      'workflow-block--dragging': isDragging,
      'workflow-block--compact': !isExpanded
    }"
    :style="{ borderLeftColor: serviceColor, borderLeftWidth: '4px' }"
    :draggable="!isExpanded"
    @dragstart="handleDragStart"
    @dragend="handleDragEnd"
    @dblclick="toggleExpand"
  >
    <!-- Compact View -->
    <div v-if="!isExpanded" class="block-compact">
      <div class="block-icon" :style="{ background: serviceColor }">
        <component v-if="serviceIcon" :is="serviceIcon" :size="20" color="white" />
      </div>
      <div class="block-info">
        <div class="block-service">{{ serviceName }}</div>
        <div class="block-event">{{ eventName }}</div>
      </div>
      <button
        v-if="showRemove"
        class="block-remove"
        @click.stop="handleRemove"
        title="Remove"
      >
        <XIcon :size="16" />
      </button>
    </div>

    <!-- Expanded View -->
    <div v-else class="block-expanded">
      <div class="block-header" :style="{ background: serviceColor }">
        <div class="block-header-left">
          <component v-if="serviceIcon" :is="serviceIcon" :size="20" color="white" />
          <span>{{ serviceName }}</span>
        </div>
        <button class="block-close" @click.stop="toggleExpand" title="Collapse">
          <MinimizeIcon :size="16" color="white" />
        </button>
      </div>

      <div class="block-body">
        <h4 class="block-title">{{ eventName }}</h4>
        <p v-if="eventDescription" class="block-description">{{ eventDescription }}</p>

        <!-- Configuration Form -->
        <form v-if="configFields && configFields.length > 0" class="block-form" @submit.prevent>
          <div
            v-for="field in configFields"
            :key="field.name"
            class="form-field"
          >
            <label :for="`field-${field.name}`" class="form-label">
              {{ field.label }}
              <span v-if="field.required" class="required-mark">*</span>
            </label>

            <!-- Text/Email/URL Input -->
            <input
              v-if="['text', 'email', 'url'].includes(field.type)"
              :id="`field-${field.name}`"
              v-model="localConfig[field.name]"
              :type="field.type"
              :placeholder="field.placeholder || ''"
              :required="field.required"
              class="form-input"
            />

            <!-- Number Input -->
            <input
              v-else-if="field.type === 'number'"
              :id="`field-${field.name}`"
              v-model.number="localConfig[field.name]"
              type="number"
              :placeholder="field.placeholder || ''"
              :required="field.required"
              class="form-input"
            />

            <!-- Textarea -->
            <textarea
              v-else-if="field.type === 'textarea'"
              :id="`field-${field.name}`"
              v-model="localConfig[field.name]"
              :placeholder="field.placeholder || ''"
              :required="field.required"
              :rows="field.rows || 5"
              class="form-textarea"
              @focus="handleTextareaFocus(field.name)"
            ></textarea>

            <!-- Select -->
            <select
              v-else-if="field.type === 'select'"
              :id="`field-${field.name}`"
              v-model="localConfig[field.name]"
              :required="field.required"
              class="form-select"
            >
              <option value="" disabled>{{ field.placeholder || 'Select...' }}</option>
              <option
                v-for="option in field.options"
                :key="option.value"
                :value="option.value"
              >
                {{ option.label }}
              </option>
            </select>

            <small v-if="field.hint" class="form-hint">{{ field.hint }}</small>
          </div>
        </form>

        <!-- Available Variables (if this is an action) -->
        <div v-if="availableVariables && availableVariables.length > 0" class="available-vars">
          <div class="vars-header">
            <strong>Template Variables</strong>
            <small>Click to insert into your message</small>
          </div>
          <div class="var-tags">
            <span
              v-for="varName in availableVariables"
              :key="varName"
              class="var-tag"
              @click="insertVariable(varName)"
              :title="`Click to insert {${varName}} into your message`"
              v-text="`{${varName}}`"
            ></span>
          </div>
          <p class="vars-hint">These placeholders will be replaced with actual values when the workflow runs.</p>
        </div>

        <!-- Action Buttons -->
        <div class="block-actions">
          <button class="btn-save" @click="handleSave">Save</button>
          <button class="btn-cancel" @click="handleCancel">Cancel</button>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, computed, watch } from 'vue'
import { XIcon, MinimizeIcon } from 'lucide-vue-next'

const props = defineProps({
  // Service info
  serviceName: { type: String, required: true },
  serviceColor: { type: String, default: '#5b9bd5' },
  serviceIcon: { type: Object, default: null },

  // Event info
  eventName: { type: String, required: true },
  eventDescription: { type: String, default: '' },
  eventType: { type: String, required: true }, // 'trigger' or 'action'

  // Configuration
  configFields: { type: Array, default: () => [] },
  config: { type: Object, default: () => ({}) },

  // Available variables from trigger
  availableVariables: { type: Array, default: () => [] },

  // UI state
  showRemove: { type: Boolean, default: false },
  expanded: { type: Boolean, default: false },

  // Drag data
  dragData: { type: Object, default: null }
})

const emit = defineEmits(['remove', 'save', 'cancel', 'expand', 'collapse'])

const isExpanded = ref(props.expanded)
const isDragging = ref(false)
const localConfig = ref({ ...props.config })

// Watch for external config changes
watch(() => props.config, (newConfig) => {
  localConfig.value = { ...newConfig }
}, { deep: true })

// Watch for external expanded state changes
watch(() => props.expanded, (newValue) => {
  isExpanded.value = newValue
})

function toggleExpand() {
  isExpanded.value = !isExpanded.value
  if (isExpanded.value) {
    emit('expand')
  } else {
    emit('collapse')
  }
}

function handleDragStart(event) {
  isDragging.value = true

  // Set drag data
  const data = props.dragData || {
    serviceName: props.serviceName,
    serviceColor: props.serviceColor,
    eventName: props.eventName,
    eventType: props.eventType
  }

  event.dataTransfer.effectAllowed = 'copy'
  event.dataTransfer.setData('application/json', JSON.stringify(data))

  // Add visual feedback
  event.target.style.opacity = '0.5'
}

function handleDragEnd(event) {
  isDragging.value = false
  event.target.style.opacity = '1'
}

function handleRemove() {
  emit('remove')
}

function handleSave() {
  emit('save', { ...localConfig.value })
  isExpanded.value = false
  emit('collapse')
}

function handleCancel() {
  // Reset to original config
  localConfig.value = { ...props.config }
  isExpanded.value = false
  emit('cancel')
}

const lastFocusedTextarea = ref(null)

function handleTextareaFocus(fieldName) {
  lastFocusedTextarea.value = fieldName
}

function insertVariable(varName) {
  // Find the last focused textarea or input in the form
  const form = document.activeElement
  if (form && (form.tagName === 'TEXTAREA' || form.tagName === 'INPUT')) {
    const start = form.selectionStart
    const end = form.selectionEnd
    const text = form.value
    const variable = `{${varName}}`

    form.value = text.substring(0, start) + variable + text.substring(end)

    // Update the model
    const fieldName = form.id.replace('field-', '')
    localConfig.value[fieldName] = form.value

    // Set cursor position after the inserted variable
    form.selectionStart = form.selectionEnd = start + variable.length
    form.focus()
  }
}
</script>

<style scoped>
.workflow-block {
  background: var(--color-surface);
  border: 2px solid var(--color-border-default);
  border-radius: var(--radius-lg);
  transition: all 0.2s ease;
  position: relative;
  overflow: hidden;
}

.workflow-block--compact {
  cursor: pointer;
}

.workflow-block--compact:hover {
  border-color: var(--color-border-bright);
  box-shadow: var(--shadow-md);
  transform: translateY(-2px);
}

.workflow-block--dragging {
  opacity: 0.5;
  cursor: grabbing;
}

.workflow-block--expanded {
  border-color: var(--color-border-bright);
  box-shadow: var(--shadow-lg);
}

/* Compact View Styles */
.block-compact {
  display: flex;
  align-items: center;
  gap: var(--space-md);
  padding: var(--space-md);
  min-height: 60px;
}

.block-icon {
  width: 40px;
  height: 40px;
  border-radius: var(--radius-md);
  display: flex;
  align-items: center;
  justify-content: center;
  flex-shrink: 0;
  box-shadow: var(--shadow-sm);
}

.block-info {
  flex: 1;
  min-width: 0;
}

.block-service {
  font-size: 0.75rem;
  font-weight: 600;
  color: var(--color-text-tertiary);
  text-transform: uppercase;
  letter-spacing: 0.05em;
}

.block-event {
  font-size: 0.95rem;
  font-weight: 600;
  color: var(--color-text-primary);
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
}

.block-remove {
  background: transparent;
  border: none;
  padding: var(--space-sm);
  cursor: pointer;
  color: var(--color-text-muted);
  transition: all 0.2s ease;
  border-radius: var(--radius-sm);
  flex-shrink: 0;
}

.block-remove:hover {
  background: var(--color-danger-bg);
  color: var(--color-danger);
}

/* Expanded View Styles */
.block-expanded {
  display: flex;
  flex-direction: column;
}

.block-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: var(--space-md);
  color: white;
}

.block-header-left {
  display: flex;
  align-items: center;
  gap: var(--space-sm);
  font-weight: 600;
  font-size: 0.95rem;
}

.block-close {
  background: rgba(255, 255, 255, 0.2);
  border: none;
  padding: var(--space-xs);
  border-radius: var(--radius-sm);
  cursor: pointer;
  transition: background 0.2s ease;
  display: flex;
  align-items: center;
  justify-content: center;
}

.block-close:hover {
  background: rgba(255, 255, 255, 0.3);
}

.block-body {
  padding: var(--space-lg);
  display: flex;
  flex-direction: column;
  gap: var(--space-md);
}

.block-title {
  font-size: 1.1rem;
  font-weight: 600;
  color: var(--color-text-primary);
  margin: 0;
}

.block-description {
  font-size: 0.9rem;
  color: var(--color-text-secondary);
  margin: 0;
}

/* Form Styles */
.block-form {
  display: flex;
  flex-direction: column;
  gap: var(--space-md);
}

.form-field {
  display: flex;
  flex-direction: column;
  gap: var(--space-xs);
}

.form-label {
  font-size: 0.9rem;
  font-weight: 600;
  color: var(--color-text-primary);
}

.required-mark {
  color: var(--color-danger);
  margin-left: var(--space-xs);
}

.form-input,
.form-textarea,
.form-select {
  background: var(--color-surface-raised);
  border: 1px solid var(--color-border-default);
  border-radius: var(--radius-md);
  padding: var(--space-sm) var(--space-md);
  font-size: 0.95rem;
  color: var(--color-text-primary);
  transition: all 0.2s ease;
  font-family: inherit;
}

.form-input:focus,
.form-textarea:focus,
.form-select:focus {
  outline: none;
  border-color: var(--color-accent-blue);
  box-shadow: 0 0 0 3px rgba(91, 155, 213, 0.1);
}

.form-textarea {
  resize: vertical;
  min-height: 120px;
  line-height: 1.5;
  font-family: inherit;
}

.form-hint {
  font-size: 0.8rem;
  color: var(--color-text-muted);
  font-style: italic;
}

/* Available Variables */
.available-vars {
  background: linear-gradient(135deg, rgba(91, 155, 213, 0.05), rgba(138, 92, 246, 0.05));
  border: 1px solid rgba(91, 155, 213, 0.3);
  border-radius: var(--radius-md);
  padding: var(--space-md);
}

.vars-header {
  display: flex;
  flex-direction: column;
  gap: var(--space-xs);
  margin-bottom: var(--space-sm);
}

.vars-header strong {
  font-size: 0.9rem;
  color: var(--color-text-primary);
  font-weight: 600;
}

.vars-header small {
  font-size: 0.8rem;
  color: var(--color-text-muted);
  font-weight: normal;
}

.var-tags {
  display: flex;
  flex-wrap: wrap;
  gap: var(--space-xs);
}

.var-tag {
  background: var(--color-accent-blue);
  color: white;
  padding: 0.3rem 0.6rem;
  border-radius: var(--radius-sm);
  font-size: 0.8rem;
  font-family: 'Courier New', monospace;
  font-weight: 600;
  cursor: pointer;
  transition: all 0.2s ease;
  user-select: none;
}

.var-tag:hover {
  background: var(--color-accent-purple);
  transform: translateY(-1px);
  box-shadow: var(--shadow-sm);
}

.vars-hint {
  margin: var(--space-sm) 0 0 0;
  font-size: 0.75rem;
  color: var(--color-text-muted);
  font-style: italic;
  line-height: 1.4;
}

/* Action Buttons */
.block-actions {
  display: flex;
  gap: var(--space-sm);
  margin-top: var(--space-md);
}

.btn-save,
.btn-cancel {
  flex: 1;
  padding: var(--space-sm) var(--space-md);
  border-radius: var(--radius-md);
  font-weight: 600;
  cursor: pointer;
  transition: all 0.2s ease;
  border: none;
  font-size: 0.95rem;
}

.btn-save {
  background: var(--color-accent-blue);
  color: white;
}

.btn-save:hover {
  background: var(--color-accent-purple);
  transform: translateY(-2px);
  box-shadow: var(--shadow-md);
}

.btn-cancel {
  background: var(--color-surface-raised);
  color: var(--color-text-secondary);
  border: 1px solid var(--color-border-default);
}

.btn-cancel:hover {
  background: var(--color-surface-elevated);
  border-color: var(--color-border-bright);
}
</style>
