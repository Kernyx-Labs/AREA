<template>
  <button
    :type="type"
    class="ui-btn"
    :class="[
      `ui-btn--${variant}`,
      `ui-btn--${size}`,
      { 'ui-btn--block': block, 'ui-btn--loading': loading }
    ]"
    :disabled="disabled || loading"
    :aria-busy="loading ? 'true' : undefined"
    :aria-disabled="disabled || loading ? 'true' : undefined"
    @click="handleClick"
  >
    <span v-if="loading" class="ui-btn__spinner" aria-hidden="true"></span>
    <span class="ui-btn__content">
      <slot />
    </span>
  </button>
</template>

<script setup>
const props = defineProps({
  variant: { type: String, default: 'primary' }, // primary | secondary | ghost | danger
  size: { type: String, default: 'md' }, // sm | md | lg
  block: { type: Boolean, default: false },
  loading: { type: Boolean, default: false },
  disabled: { type: Boolean, default: false },
  type: { type: String, default: 'button' }
})

const emit = defineEmits(['click'])

function handleClick(event) {
  if (props.disabled || props.loading) return
  emit('click', event)
}
</script>

<style scoped>
/* ============================================
   BUTTON COMPONENT - BLACK THEME
   ============================================ */

.ui-btn {
  border: 1px solid transparent;
  border-radius: var(--radius-md);
  padding: 0.65rem 1.1rem;
  font: inherit;
  font-weight: 600;
  cursor: pointer;
  display: inline-flex;
  align-items: center;
  justify-content: center;
  gap: var(--space-sm);
  transition: all 0.2s ease;
  background: var(--color-surface);
  color: var(--color-text-primary);
  box-shadow: var(--shadow-sm);
  position: relative;
  overflow: hidden;
}

/* Hover effects */
.ui-btn:hover:not(:disabled) {
  transform: translateY(-2px);
  box-shadow: var(--shadow-md);
}

.ui-btn:active:not(:disabled) {
  transform: translateY(0);
  box-shadow: var(--shadow-sm);
}

.ui-btn:focus-visible {
  outline: 2px solid var(--color-focus);
  outline-offset: 2px;
}

.ui-btn:disabled {
  cursor: not-allowed;
  opacity: 0.5;
  box-shadow: none;
  transform: none;
}

/* Block variant */
.ui-btn--block {
  width: 100%;
}

/* Primary variant - Accent blue */
.ui-btn--primary {
  background: linear-gradient(135deg, var(--color-accent-blue), var(--color-accent-purple));
  color: var(--color-text-primary);
  border-color: var(--color-accent-blue);
  box-shadow: var(--shadow-md), var(--glow-accent);
}

.ui-btn--primary:hover:not(:disabled) {
  box-shadow: var(--shadow-lg), 0 0 24px rgba(91, 155, 213, 0.4);
  border-color: var(--color-accent-purple);
}

/* Secondary variant - Greyscale */
.ui-btn--secondary {
  background: var(--color-surface-raised);
  color: var(--color-text-primary);
  border-color: var(--color-border-strong);
}

.ui-btn--secondary:hover:not(:disabled) {
  background: var(--color-surface-elevated);
  border-color: var(--color-border-bright);
  box-shadow: var(--shadow-md), var(--glow-subtle);
}

/* Ghost variant - Transparent */
.ui-btn--ghost {
  background: transparent;
  color: var(--color-text-secondary);
  border-color: transparent;
  box-shadow: none;
}

.ui-btn--ghost:hover:not(:disabled) {
  background: var(--color-hover);
  border-color: var(--color-border-default);
  color: var(--color-text-primary);
}

/* Danger variant - Red accent */
.ui-btn--danger {
  background: var(--color-danger);
  color: var(--color-text-primary);
  border-color: var(--color-danger);
  box-shadow: var(--shadow-md), 0 0 16px rgba(255, 107, 107, 0.2);
}

.ui-btn--danger:hover:not(:disabled) {
  box-shadow: var(--shadow-lg), 0 0 24px rgba(255, 107, 107, 0.3);
  filter: brightness(1.1);
}

/* Size variants */
.ui-btn--sm {
  padding: 0.45rem 0.85rem;
  font-size: 0.875rem;
  border-radius: var(--radius-sm);
}

.ui-btn--md {
  padding: 0.65rem 1.1rem;
  font-size: 1rem;
}

.ui-btn--lg {
  padding: 0.85rem 1.4rem;
  font-size: 1.125rem;
  border-radius: var(--radius-lg);
}

/* Loading spinner */
.ui-btn__spinner {
  width: 1em;
  height: 1em;
  border: 2px solid rgba(255, 255, 255, 0.3);
  border-top-color: currentColor;
  border-radius: 50%;
  animation: spin 0.8s linear infinite;
}

.ui-btn--secondary .ui-btn__spinner,
.ui-btn--ghost .ui-btn__spinner {
  border-color: rgba(255, 255, 255, 0.2);
  border-top-color: currentColor;
}

.ui-btn--loading .ui-btn__content {
  opacity: 0.7;
}

/* Ripple effect on click */
.ui-btn::after {
  content: '';
  position: absolute;
  inset: 0;
  background: radial-gradient(circle, rgba(255, 255, 255, 0.2) 0%, transparent 70%);
  opacity: 0;
  transform: scale(0);
  transition: opacity 0.3s ease, transform 0.5s ease;
}

.ui-btn:active::after {
  opacity: 1;
  transform: scale(1);
  transition: none;
}

@keyframes spin {
  to {
    transform: rotate(360deg);
  }
}
</style>

