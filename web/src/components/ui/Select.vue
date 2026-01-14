<template>
  <label class="ui-field">
    <span v-if="label" class="ui-field__label">{{ label }}</span>

    <!-- Searchable select with dropdown -->
    <div v-if="searchable" class="ui-field__control search-select-wrapper" ref="searchContainer">
      <!-- Trigger button to open dropdown -->
      <button
        type="button"
        class="ui-input search-select-trigger"
        :class="{ 'search-select-trigger--open': showDropdown }"
        :disabled="disabled || loading"
        @click="toggleDropdown"
      >
        <span class="search-select-value">
          {{ selectedLabel || placeholder || 'Select...' }}
        </span>
        <span class="search-select-arrow">▼</span>
      </button>

      <!-- Dropdown with search and options -->
      <div v-if="showDropdown" class="dropdown-menu">
        <!-- Search input inside dropdown -->
        <div class="dropdown-search">
          <input
            ref="searchInput"
            v-model="searchQuery"
            type="text"
            class="dropdown-search-input"
            placeholder="Search..."
            @click.stop
          />
        </div>

        <!-- Options list -->
        <div class="dropdown-options">
          <div v-if="loading" class="dropdown-loading">Loading options...</div>
          <div v-else-if="displayOptions.length === 0" class="dropdown-empty">
            No options found
          </div>
          <div
            v-else
            v-for="opt in displayOptions"
            :key="opt.value"
            class="dropdown-item"
            :class="{ 'dropdown-item--selected': opt.value === modelValue }"
            @mousedown.prevent="selectOption(opt.value)"
          >
            <span class="dropdown-item-label">{{ opt.label }}</span>
            <span v-if="opt.description" class="dropdown-item-description">{{ opt.description }}</span>
          </div>
        </div>
      </div>
    </div>

    <!-- Regular select for non-searchable mode -->
    <div v-else class="ui-field__control" :class="{ 'has-error': !!error || !!loadError }">
      <select
        class="ui-input"
        :value="modelValue"
        :disabled="disabled || loading"
        :aria-invalid="error || loadError ? 'true' : undefined"
        @change="onChange"
      >
        <option v-if="loading" disabled value="">Loading options...</option>
        <option v-else-if="placeholder" disabled value="">{{ placeholder }}</option>
        <option v-for="opt in displayOptions" :key="opt.value" :value="opt.value">
          {{ opt.label }}
        </option>
      </select>
    </div>

    <span v-if="hint && !error && !loadError" class="ui-field__hint">{{ hint }}</span>
    <span v-if="error" class="ui-field__error">{{ error }}</span>
    <span v-if="loadError" class="ui-field__error">{{ loadError }}</span>
  </label>
</template>

<script setup>
import { ref, computed, onMounted, watch, nextTick, onUnmounted } from 'vue'

const props = defineProps({
  modelValue: [String, Number, Boolean],
  label: { type: String, default: '' },
  placeholder: { type: String, default: '' },
  hint: { type: String, default: '' },
  error: { type: String, default: '' },
  disabled: { type: Boolean, default: false },
  options: { type: Array, default: () => [] },
  // Dynamic options props
  dynamicOptionsEndpoint: { type: String, default: '' },
  optionsValueField: { type: String, default: 'value' },
  optionsLabelField: { type: String, default: 'label' },
  searchable: { type: Boolean, default: false }
})

const emit = defineEmits(['update:modelValue'])

const searchContainer = ref(null)
const searchInput = ref(null)
const localOptions = ref([])
const loading = ref(false)
const loadError = ref('')
const searchQuery = ref('')
const showDropdown = ref(false)

// Get the label for the currently selected value
const selectedLabel = computed(() => {
  if (!props.modelValue) return ''
  const opts = localOptions.value.length > 0 ? localOptions.value : props.options
  const selected = opts.find(opt => opt.value === props.modelValue)
  return selected ? selected.label : ''
})

// Computed property for displayed options
const displayOptions = computed(() => {
  const opts = localOptions.value.length > 0 ? localOptions.value : props.options

  if (!props.searchable || !searchQuery.value) {
    return opts
  }

  const query = searchQuery.value.toLowerCase()
  return opts.filter(opt =>
    opt.label.toLowerCase().includes(query) ||
    (opt.description && opt.description.toLowerCase().includes(query))
  )
})

// Watch for endpoint changes
watch(() => props.dynamicOptionsEndpoint, async (newEndpoint) => {
  if (newEndpoint) {
    await loadDynamicOptions()
  }
})

// Watch for static options changes
watch(() => props.options, (newOptions) => {
  if (!props.dynamicOptionsEndpoint && newOptions) {
    localOptions.value = newOptions
  }
}, { deep: true })

async function loadDynamicOptions() {
  loading.value = true
  loadError.value = ''

  try {
    const API_URL = import.meta.env.VITE_API_URL || ''
    const accessToken = localStorage.getItem('accessToken')

    if (!accessToken) {
      loadError.value = 'Authentication required. Please log in.'
      return
    }

    const response = await fetch(`${API_URL}${props.dynamicOptionsEndpoint}`, {
      headers: {
        'Authorization': `Bearer ${accessToken}`,
        'Content-Type': 'application/json'
      }
    })

    if (!response.ok) {
      if (response.status === 404) {
        loadError.value = 'GitHub not connected. Please connect your GitHub account in Services first.'
      } else if (response.status === 401) {
        loadError.value = 'GitHub connection expired. Please reconnect your account in Services.'
      } else {
        const errorData = await response.json().catch(() => ({}))
        loadError.value = errorData.message || `Failed to load options (${response.status})`
      }
      localOptions.value = []
      return
    }

    const result = await response.json()

    // Unwrap ApiResponse format
    const data = result.success !== undefined ? result.data : result

    if (!Array.isArray(data)) {
      loadError.value = 'Invalid response format from server'
      localOptions.value = []
      return
    }

    // Map API response to options format
    localOptions.value = data.map(item => ({
      value: item[props.optionsValueField] || item.value,
      label: item[props.optionsLabelField] || item.label,
      description: item.description
    }))

    if (localOptions.value.length === 0) {
      loadError.value = 'No repositories found. Make sure you have access to GitHub repositories.'
    }

  } catch (err) {
    console.error('Error loading dynamic options:', err)
    loadError.value = 'Failed to load options. Please try again.'
    localOptions.value = []
  } finally {
    loading.value = false
  }
}

function onChange(event) {
  emit('update:modelValue', event.target.value)
}

function toggleDropdown() {
  showDropdown.value = !showDropdown.value
  if (showDropdown.value) {
    // Focus search input after dropdown opens
    nextTick(() => {
      if (searchInput.value) {
        searchInput.value.focus()
      }
    })
  } else {
    searchQuery.value = '' // Clear search when closing
  }
}

function selectOption(value) {
  emit('update:modelValue', value)
  showDropdown.value = false
  searchQuery.value = '' // Clear search after selection
}

function handleClickOutside(event) {
  if (searchContainer.value && !searchContainer.value.contains(event.target)) {
    showDropdown.value = false
    searchQuery.value = ''
  }
}

// Add click outside listener when component mounts
onMounted(async () => {
  if (props.dynamicOptionsEndpoint) {
    await loadDynamicOptions()
  } else if (props.options && props.options.length > 0) {
    localOptions.value = props.options
  }

  // Add click outside listener
  document.addEventListener('click', handleClickOutside)
})

// Remove click outside listener when component unmounts
onUnmounted(() => {
  document.removeEventListener('click', handleClickOutside)
})
</script>

<style scoped src="../../assets/FormComponents.css"></style>

<style scoped>
/* Searchable select wrapper */
.search-select-wrapper {
  position: relative;
}

/* Trigger button */
.search-select-trigger {
  width: 100%;
  display: flex;
  align-items: center;
  justify-content: space-between;
  text-align: left;
  cursor: pointer;
  background: var(--color-surface);
}

.search-select-trigger:disabled {
  cursor: not-allowed;
  opacity: 0.6;
}

.search-select-trigger--open {
  border-color: var(--color-accent-blue);
}

.search-select-value {
  flex: 1;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
  color: var(--color-text-primary);
}

.search-select-arrow {
  margin-left: var(--space-sm);
  color: var(--color-text-muted);
  transition: transform 0.2s ease;
  font-size: 0.75rem;
}

.search-select-trigger--open .search-select-arrow {
  transform: rotate(180deg);
}

/* Dropdown menu */
.dropdown-menu {
  position: absolute;
  top: 100%;
  left: 0;
  right: 0;
  margin-top: 4px;
  background: var(--color-surface);
  border: 1px solid var(--color-border-default);
  border-radius: var(--radius-md);
  box-shadow: 0 4px 12px rgba(0, 0, 0, 0.15);
  z-index: 1000;
  overflow: hidden;
}

/* Search input inside dropdown */
.dropdown-search {
  padding: var(--space-sm);
  border-bottom: 1px solid var(--color-border-default);
  background: var(--color-surface-raised);
}

.dropdown-search-input {
  width: 100%;
  padding: var(--space-sm);
  border: 1px solid var(--color-border-default);
  border-radius: var(--radius-sm);
  background: var(--color-surface);
  color: var(--color-text-primary);
  font-size: 0.9rem;
}

.dropdown-search-input:focus {
  outline: none;
  border-color: var(--color-accent-blue);
}

/* Options list container */
.dropdown-options {
  max-height: 300px;
  overflow-y: auto;
}

.dropdown-loading,
.dropdown-empty {
  padding: var(--space-md);
  text-align: center;
  color: var(--color-text-muted);
  font-size: 0.9rem;
}

.dropdown-item {
  padding: var(--space-sm) var(--space-md);
  cursor: pointer;
  transition: background 0.2s ease;
  border-bottom: 1px solid var(--color-border-subtle);
}

.dropdown-item:last-child {
  border-bottom: none;
}

.dropdown-item:hover {
  background: var(--color-hover);
}

.dropdown-item--selected {
  background: var(--color-accent-blue);
  color: white;
}

.dropdown-item--selected:hover {
  background: var(--color-accent-purple);
}

.dropdown-item-label {
  display: block;
  font-weight: 500;
  font-size: 0.95rem;
}

.dropdown-item-description {
  display: block;
  font-size: 0.85rem;
  color: var(--color-text-muted);
  margin-top: 2px;
}

.dropdown-item--selected .dropdown-item-description {
  color: rgba(255, 255, 255, 0.8);
}

/* Scrollbar styling for dropdown options */
.dropdown-options::-webkit-scrollbar {
  width: 8px;
}

.dropdown-options::-webkit-scrollbar-track {
  background: var(--color-surface-raised);
}

.dropdown-options::-webkit-scrollbar-thumb {
  background: var(--color-border-default);
  border-radius: 4px;
}

.dropdown-options::-webkit-scrollbar-thumb:hover {
  background: var(--color-border-bright);
}
</style>
