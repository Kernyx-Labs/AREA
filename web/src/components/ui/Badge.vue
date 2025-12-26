<template>
  <span class="ui-badge" :class="[`ui-badge--${variant}`, { 'ui-badge--pill': pill }]">
    <slot />
  </span>
</template>

<script setup>
const props = defineProps({
  variant: { type: String, default: 'neutral' }, // neutral | success | danger | info | warning | accent
  pill: { type: Boolean, default: false }
})
</script>

<style scoped>
/* ============================================
   BADGE COMPONENT - BLACK THEME
   ============================================ */

.ui-badge {
  display: inline-flex;
  align-items: center;
  gap: var(--space-xs);
  padding: var(--space-xs) var(--space-md);
  border-radius: var(--radius-md);
  font-size: 0.8125rem;
  font-weight: 700;
  border: 1px solid;
  text-transform: uppercase;
  letter-spacing: 0.05em;
  box-shadow: var(--shadow-subtle);
  transition: all 0.2s ease;
}

.ui-badge--pill {
  border-radius: var(--radius-full);
}

.ui-badge--neutral {
  background: var(--color-surface-raised);
  color: var(--color-text-secondary);
  border-color: var(--color-border-default);
}

.ui-badge--success {
  background: var(--color-success-bg);
  color: var(--color-success);
  border-color: var(--color-success);
}

.ui-badge--danger {
  background: var(--color-danger-bg);
  color: var(--color-danger);
  border-color: var(--color-danger);
}

.ui-badge--info {
  background: var(--color-info-bg);
  color: var(--color-info);
  border-color: var(--color-info);
}

.ui-badge--warning {
  background: var(--color-warning-bg);
  color: var(--color-warning);
  border-color: var(--color-warning);
}

.ui-badge--accent {
  background: linear-gradient(135deg, var(--color-accent-blue), var(--color-accent-purple));
  color: var(--color-text-primary);
  border-color: var(--color-accent-blue);
  box-shadow: var(--shadow-sm), var(--glow-accent);
}
</style>

