<template>
  <span class="ui-spinner" :style="spinnerStyle" aria-hidden="true"></span>
</template>

<script setup>
import { computed } from 'vue'

const props = defineProps({
  size: { type: String, default: 'md' }, // xs | sm | md | lg
  color: { type: String, default: 'currentColor' }
})

const sizeMap = { xs: 12, sm: 16, md: 20, lg: 28 }
const spinnerStyle = computed(() => ({
  width: `${sizeMap[props.size] ?? sizeMap.md}px`,
  height: `${sizeMap[props.size] ?? sizeMap.md}px`,
  borderColor: `${props.color}33`,
  borderTopColor: props.color
}))
</script>

<style scoped>
/* ============================================
   SPINNER COMPONENT - BLACK THEME
   ============================================ */

.ui-spinner {
  display: inline-block;
  border: 3px solid var(--color-border-subtle);
  border-radius: 50%;
  animation: spin 0.8s linear infinite;
  box-shadow: var(--glow-subtle);
}

@keyframes spin {
  to {
    transform: rotate(360deg);
  }
}
</style>
