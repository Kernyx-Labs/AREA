<template>
  <Teleport to="body">
    <transition name="ui-modal-fade">
      <div v-if="open" class="ui-modal" role="dialog" aria-modal="true" @keydown.esc="emitClose">
        <div class="ui-modal__backdrop" @click="backdropClosable ? emitClose() : null"></div>
        <div class="ui-modal__content" role="document">
          <header class="ui-modal__header">
            <div class="ui-modal__title">
              <slot name="title" />
            </div>
            <button class="ui-modal__close" type="button" @click="emitClose" aria-label="Close">
              ✕
            </button>
          </header>
          <section class="ui-modal__body">
            <slot />
          </section>
          <footer v-if="$slots.footer" class="ui-modal__footer">
            <slot name="footer" />
          </footer>
        </div>
      </div>
    </transition>
  </Teleport>
</template>

<script setup>
import { Teleport } from 'vue'

const props = defineProps({
  open: { type: Boolean, default: false },
  backdropClosable: { type: Boolean, default: true }
})

const emit = defineEmits(['close'])
function emitClose() { emit('close') }
</script>

<style scoped>
/* ============================================
   MODAL COMPONENT - BLACK THEME
   ============================================ */

.ui-modal {
  position: fixed;
  inset: 0;
  z-index: 2000;
  display: grid;
  place-items: center;
}

.ui-modal__backdrop {
  position: absolute;
  inset: 0;
  background: rgba(0, 0, 0, 0.85);
  backdrop-filter: blur(4px);
}

.ui-modal__content {
  position: relative;
  background: var(--color-surface);
  color: var(--color-text-primary);
  border-radius: var(--radius-2xl);
  border: 1px solid var(--color-border-strong);
  box-shadow: var(--shadow-xl);
  min-width: min(90vw, 560px);
  max-width: 90vw;
  padding: 0;
  z-index: 1;
  overflow: hidden;
}

.ui-modal__header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: var(--space-lg);
  padding: var(--space-xl) var(--space-2xl);
  background: var(--color-surface-raised);
  border-bottom: 1px solid var(--color-border-strong);
}

.ui-modal__title {
  font-weight: 600;
  font-size: 1.375rem;
  color: var(--color-text-primary);
}

.ui-modal__close {
  background: transparent;
  border: 1px solid var(--color-border-default);
  font-size: 1.25rem;
  cursor: pointer;
  color: var(--color-text-tertiary);
  border-radius: var(--radius-md);
  padding: var(--space-sm);
  width: 36px;
  height: 36px;
  display: flex;
  align-items: center;
  justify-content: center;
  transition: all 0.2s ease;
}

.ui-modal__close:hover {
  background: var(--color-hover-strong);
  border-color: var(--color-border-bright);
  color: var(--color-text-primary);
  transform: rotate(90deg);
}

.ui-modal__close:focus-visible {
  outline: 2px solid var(--color-focus);
  outline-offset: 2px;
}

.ui-modal__body {
  display: flex;
  flex-direction: column;
  gap: var(--space-md);
  padding: var(--space-2xl);
  background: var(--color-bg);
}

.ui-modal__footer {
  display: flex;
  justify-content: flex-end;
  gap: var(--space-sm);
  padding: var(--space-xl) var(--space-2xl);
  background: var(--color-surface-raised);
  border-top: 1px solid var(--color-border-subtle);
}

/* Transition animations */
.ui-modal-fade-enter-active,
.ui-modal-fade-leave-active {
  transition: opacity 0.2s ease;
}

.ui-modal-fade-enter-from,
.ui-modal-fade-leave-to {
  opacity: 0;
}

.ui-modal-fade-enter-active .ui-modal__content {
  animation: modalSlideUp 0.2s ease;
}

@keyframes modalSlideUp {
  from {
    opacity: 0;
    transform: translateY(20px) scale(0.95);
  }
  to {
    opacity: 1;
    transform: translateY(0) scale(1);
  }
}
</style>

