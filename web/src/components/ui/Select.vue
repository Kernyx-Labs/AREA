<template>
  <label class="ui-field">
    <span v-if="label" class="ui-field__label">{{ label }}</span>

    <!-- Search input for searchable selects -->
    <div v-if="searchable" class="ui-field__control search-input-wrapper">
      <input
        v-model="searchQuery"
        type="text"
        class="ui-input search-input"
        placeholder="Search..."
        :disabled="disabled || loading"
        @focus="showDropdown = true"
      />
    </div>

    <div class="ui-field__control" :class="{ 'has-error': !!error || !!loadError }">
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
import { ref, computed, onMounted, watch } from 'vue'

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

const localOptions = ref([])
const loading = ref(false)
const loadError = ref('')
const searchQuery = ref('')
const showDropdown = ref(false)

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

// Load dynamic options on mount
onMounted(async () => {
  if (props.dynamicOptionsEndpoint) {
    await loadDynamicOptions()
  } else if (props.options && props.options.length > 0) {
    localOptions.value = props.options
  }
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
</script>

<style scoped src="../../assets/FormComponents.css"></style>

<style scoped>
.search-input-wrapper {
  margin-bottom: var(--space-sm);
}

.search-input {
  width: 100%;
}

.search-input:focus {
  border-color: var(--color-accent-blue);
}
</style>
