<template>
  <Teleport to="body">
    <transition name="ui-modal-fade">
      <div v-if="open" class="ui-confirm-modal" role="dialog" aria-modal="true" @keydown.esc="handleCancel">
        <div class="ui-confirm-modal__backdrop" @click="handleCancel"></div>
        <div class="ui-confirm-modal__content" role="document">
          <div class="ui-confirm-modal__icon" :class="`ui-confirm-modal__icon--${variant}`" aria-hidden="true">
            <svg v-if="variant === 'danger'" xmlns="http://www.w3.org/2000/svg" fill="none" viewBox="0 0 24 24" stroke="currentColor">
              <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M12 9v2m0 4h.01m-6.938 4h13.856c1.54 0 2.502-1.667 1.732-3L13.732 4c-.77-1.333-2.694-1.333-3.464 0L3.34 16c-.77 1.333.192 3 1.732 3z" />
            </svg>
            <svg v-else-if="variant === 'warning'" xmlns="http://www.w3.org/2000/svg" fill="none" viewBox="0 0 24 24" stroke="currentColor">
              <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M12 9v2m0 4h.01m-6.938 4h13.856c1.54 0 2.502-1.667 1.732-3L13.732 4c-.77-1.333-2.694-1.333-3.464 0L3.34 16c-.77 1.333.192 3 1.732 3z" />
            </svg>
            <svg v-else xmlns="http://www.w3.org/2000/svg" fill="none" viewBox="0 0 24 24" stroke="currentColor">
              <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M8.228 9c.549-1.165 2.03-2 3.772-2 2.21 0 4 1.343 4 3 0 1.4-1.278 2.575-3.006 2.907-.542.104-.994.54-.994 1.093m0 3h.01M21 12a9 9 0 11-18 0 9 9 0 0118 0z" />
            </svg>
          </div>

          <div class="ui-confirm-modal__body">
            <h3 v-if="title" class="ui-confirm-modal__title">{{ title }}</h3>
            <p class="ui-confirm-modal__message">{{ message }}</p>
          </div>

          <div class="ui-confirm-modal__footer">
            <Button
              variant="ghost"
              @click="handleCancel"
              ref="cancelButton"
            >
              {{ cancelText }}
            </Button>
            <Button
              :variant="variant === 'danger' ? 'danger' : 'primary'"
              @click="handleConfirm"
              ref="confirmButton"
            >
              {{ confirmText }}
            </Button>
          </div>
        </div>
      </div>
    </transition>
  </Teleport>
</template>

<script setup>
import { ref, watch, nextTick } from 'vue'
import { Teleport } from 'vue'
import Button from './Button.vue'

const props = defineProps({
  open: { type: Boolean, default: false },
  title: { type: String, default: '' },
  message: { type: String, required: true },
  variant: { type: String, default: 'info' }, // info | warning | danger
  confirmText: { type: String, default: 'Confirm' },
  cancelText: { type: String, default: 'Cancel' }
})

const emit = defineEmits(['close', 'confirm', 'cancel'])

const confirmButton = ref(null)
const cancelButton = ref(null)

watch(() => props.open, (isOpen) => {
  if (isOpen) {
    nextTick(() => {
      // Focus cancel button by default for safety (especially for danger actions)
      if (props.variant === 'danger') {
        cancelButton.value?.$el?.focus()
      } else {
        confirmButton.value?.$el?.focus()
      }
    })
  }
})

function handleConfirm() {
  emit('confirm')
  emit('close')
}

function handleCancel() {
  emit('cancel')
  emit('close')
}
</script>

<style scoped>
.ui-confirm-modal {
  position: fixed;
  inset: 0;
  z-index: 2000;
  display: grid;
  place-items: center;
}

.ui-confirm-modal__backdrop {
  position: absolute;
  inset: 0;
  background: rgba(0, 0, 0, 0.85);
  backdrop-filter: blur(4px);
}

.ui-confirm-modal__content {
  position: relative;
  background: var(--color-surface);
  color: var(--color-text-primary);
  border-radius: var(--radius-2xl);
  border: 1px solid var(--color-border-strong);
  box-shadow: var(--shadow-xl);
  min-width: min(90vw, 440px);
  max-width: 90vw;
  padding: var(--space-2xl);
  z-index: 1;
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: var(--space-lg);
  text-align: center;
}

.ui-confirm-modal__icon {
  width: 64px;
  height: 64px;
  border-radius: var(--radius-full);
  display: flex;
  align-items: center;
  justify-content: center;
  flex-shrink: 0;
}

.ui-confirm-modal__icon svg {
  width: 32px;
  height: 32px;
  stroke-width: 2.5;
}

.ui-confirm-modal__icon--info {
  background: var(--color-info-bg);
  color: var(--color-info);
  border: 2px solid var(--color-info);
  box-shadow: 0 0 16px rgba(91, 155, 213, 0.3);
}

.ui-confirm-modal__icon--warning {
  background: var(--color-warning-bg);
  color: var(--color-warning);
  border: 2px solid var(--color-warning);
  box-shadow: 0 0 16px rgba(255, 140, 66, 0.3);
}

.ui-confirm-modal__icon--danger {
  background: var(--color-danger-bg);
  color: var(--color-danger);
  border: 2px solid var(--color-danger);
  box-shadow: 0 0 16px rgba(255, 107, 107, 0.3);
}

.ui-confirm-modal__body {
  display: flex;
  flex-direction: column;
  gap: var(--space-sm);
}

.ui-confirm-modal__title {
  font-size: 1.25rem;
  font-weight: 600;
  color: var(--color-text-primary);
  margin: 0;
}

.ui-confirm-modal__message {
  font-size: 0.9375rem;
  color: var(--color-text-secondary);
  margin: 0;
  white-space: pre-line;
  line-height: 1.5;
}

.ui-confirm-modal__footer {
  width: 100%;
  display: flex;
  justify-content: center;
  gap: var(--space-sm);
  margin-top: var(--space-md);
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

.ui-modal-fade-enter-active .ui-confirm-modal__content {
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

/* Mobile responsive */
@media (max-width: 480px) {
  .ui-confirm-modal__content {
    min-width: 85vw;
    padding: var(--space-xl);
  }

  .ui-confirm-modal__icon {
    width: 56px;
    height: 56px;
  }

  .ui-confirm-modal__icon svg {
    width: 28px;
    height: 28px;
  }

  .ui-confirm-modal__footer {
    flex-direction: column-reverse;
  }
}
</style>
