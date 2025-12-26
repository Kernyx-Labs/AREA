<template>
  <div class="ui-card" :class="{ 'ui-card--hover': hover }">
    <header v-if="$slots.header" class="ui-card__header">
      <slot name="header" />
    </header>
    <section class="ui-card__body">
      <slot />
    </section>
    <footer v-if="$slots.footer" class="ui-card__footer">
      <slot name="footer" />
    </footer>
  </div>
</template>

<script setup>
const props = defineProps({
  hover: { type: Boolean, default: false }
})
</script>

<style scoped>
/* ============================================
   CARD COMPONENT - BLACK THEME WIDGET
   ============================================ */

.ui-card {
  background: var(--color-surface);
  border: 1px solid var(--color-border-default);
  border-radius: var(--radius-lg);
  box-shadow: var(--shadow-sm);
  padding: var(--space-lg);
  transition: all 0.2s ease;
  position: relative;
  overflow: hidden;
}

/* Subtle gradient overlay for depth */
.ui-card::before {
  content: '';
  position: absolute;
  top: 0;
  left: 0;
  right: 0;
  height: 1px;
  background: linear-gradient(
    90deg,
    transparent,
    var(--color-border-bright) 50%,
    transparent
  );
  opacity: 0.5;
}

.ui-card--hover {
  cursor: pointer;
}

.ui-card--hover:hover {
  transform: translateY(-3px);
  border-color: var(--color-border-bright);
  box-shadow: var(--shadow-md), var(--glow-default);
}

.ui-card--hover:active {
  transform: translateY(-1px);
  box-shadow: var(--shadow-sm);
}

/* Card sections */
.ui-card__header {
  margin-bottom: var(--space-md);
  font-weight: 600;
  font-size: 1.125rem;
  color: var(--color-text-primary);
  display: flex;
  align-items: center;
  gap: var(--space-sm);
  padding-bottom: var(--space-sm);
  border-bottom: 1px solid var(--color-border-subtle);
}

.ui-card__body {
  display: flex;
  flex-direction: column;
  gap: var(--space-sm);
  color: var(--color-text-secondary);
}

.ui-card__footer {
  margin-top: var(--space-lg);
  padding-top: var(--space-md);
  border-top: 1px solid var(--color-border-subtle);
  display: flex;
  justify-content: flex-end;
  align-items: center;
  gap: var(--space-sm);
}

/* Card variants */
.ui-card--elevated {
  background: var(--color-surface-raised);
  border-color: var(--color-border-strong);
  box-shadow: var(--shadow-md);
}

.ui-card--accent {
  border-color: var(--color-border-accent);
  box-shadow: var(--shadow-md), var(--glow-accent);
}

.ui-card--flat {
  box-shadow: none;
  border-color: var(--color-border-subtle);
}
</style>

