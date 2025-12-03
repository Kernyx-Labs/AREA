<template>
  <article class="stat-card" :aria-label="title">
    <header class="stat-header">
      <h3 class="stat-title">{{ title }}</h3>
      <span v-if="icon" class="stat-icon" aria-hidden="true">{{ icon }}</span>
    </header>
    <p class="stat-value">{{ value }}</p>
    <p v-if="trend" class="stat-trend" :class="trendClass">{{ trend.prefix }}{{ trend.value }}<span class="trend-suffix">{{ trend.suffix }}</span></p>
    <footer v-if="description" class="stat-footer">{{ description }}</footer>
  </article>
</template>

<script setup>
import { computed } from 'vue'
const props = defineProps({
  title: { type: String, required: true },
  value: { type: [String, Number], required: true },
  icon: { type: String, default: '' },
  description: { type: String, default: '' },
  trend: { type: Object, default: null } // { value, prefix, suffix, direction }
})

const trendClass = computed(() => props.trend?.direction === 'up' ? 'trend-up' : props.trend?.direction === 'down' ? 'trend-down' : '')
</script>

<style scoped src="../assets/StatCard.css"></style>
