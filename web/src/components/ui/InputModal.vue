<template>
  <Teleport to="body">
    <transition name="ui-modal-fade">
      <div v-if="open" class="ui-input-modal" role="dialog" aria-modal="true" @keydown.esc="handleCancel">
        <div class="ui-input-modal__backdrop" @click="handleCancel"></div>
        <div class="ui-input-modal__content" role="document">
          <div class="ui-input-modal__header">
            <h3 class="ui-input-modal__title">{{ title }}</h3>
            <button
              class="ui-input-modal__close"
              type="button"
              @click="handleCancel"
              aria-label="Close"
            >
              ✕
            </button>
          </div>

          <div class="ui-input-modal__body">
            <p v-if="message" class="ui-input-modal__message">{{ message }}</p>
            <form @submit.prevent="handleSubmit">
              <div class="ui-input-modal__field">
                <label v-if="label" :for="inputId" class="ui-input-modal__label">
                  {{ label }}
                </label>
                <input
                  :id="inputId"
                  ref="inputElement"
                  v-model="inputValue"
                  :type="inputType"
                  :placeholder="placeholder"
                  :required="required"
                  class="ui-input-modal__input"
                  @keydown.enter="handleSubmit"
                />
                <p v-if="hint" class="ui-input-modal__hint">{{ hint }}</p>
              </div>
            </form>
          </div>

          <div class="ui-input-modal__footer">
            <Button
              variant="ghost"
              @click="handleCancel"
            >
              {{ cancelText }}
            </Button>
            <Button
              variant="primary"
              @click="handleSubmit"
              :disabled="required && !inputValue"
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
import { ref, watch, nextTick, computed } from 'vue'
import { Teleport } from 'vue'
import Button from './Button.vue'

const props = defineProps({
  open: { type: Boolean, default: false },
  title: { type: String, default: 'Input' },
  message: { type: String, default: '' },
  label: { type: String, default: '' },
  placeholder: { type: String, default: '' },
  hint: { type: String, default: '' },
  defaultValue: { type: String, default: '' },
  inputType: { type: String, default: 'text' },
  required: { type: Boolean, default: true },
  confirmText: { type: String, default: 'Submit' },
  cancelText: { type: String, default: 'Cancel' }
})

const emit = defineEmits(['close', 'confirm', 'cancel'])

const inputValue = ref('')
const inputElement = ref(null)

// Generate a unique ID for the input
const inputId = computed(() => `input-modal-${Math.random().toString(36).substr(2, 9)}`)

watch(() => props.open, (isOpen) => {
  if (isOpen) {
    inputValue.value = props.defaultValue
    nextTick(() => {
      inputElement.value?.focus()
    })
  }
})

function handleSubmit() {
  if (props.required && !inputValue.value) {
    return
  }
  emit('confirm', inputValue.value)
  emit('close')
  inputValue.value = ''
}

function handleCancel() {
  emit('cancel')
  emit('close')
  inputValue.value = ''
}
</script>

<style scoped>
.ui-input-modal {
  position: fixed;
  inset: 0;
  z-index: 2000;
  display: grid;
  place-items: center;
}

.ui-input-modal__backdrop {
  position: absolute;
  inset: 0;
  background: rgba(0, 0, 0, 0.85);
  backdrop-filter: blur(4px);
}

.ui-input-modal__content {
  position: relative;
  background: var(--color-surface);
  color: var(--color-text-primary);
  border-radius: var(--radius-2xl);
  border: 1px solid var(--color-border-strong);
  box-shadow: var(--shadow-xl);
  min-width: min(90vw, 480px);
  max-width: 90vw;
  padding: 0;
  z-index: 1;
  overflow: hidden;
}

.ui-input-modal__header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: var(--space-lg);
  padding: var(--space-xl) var(--space-2xl);
  background: var(--color-surface-raised);
  border-bottom: 1px solid var(--color-border-strong);
}

.ui-input-modal__title {
  font-weight: 600;
  font-size: 1.25rem;
  color: var(--color-text-primary);
  margin: 0;
}

.ui-input-modal__close {
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

.ui-input-modal__close:hover {
  background: var(--color-hover-strong);
  border-color: var(--color-border-bright);
  color: var(--color-text-primary);
  transform: rotate(90deg);
}

.ui-input-modal__close:focus-visible {
  outline: 2px solid var(--color-focus);
  outline-offset: 2px;
}

.ui-input-modal__body {
  padding: var(--space-2xl);
  background: var(--color-bg);
}

.ui-input-modal__message {
  font-size: 0.9375rem;
  color: var(--color-text-secondary);
  margin: 0 0 var(--space-lg) 0;
  line-height: 1.5;
}

.ui-input-modal__field {
  display: flex;
  flex-direction: column;
  gap: var(--space-sm);
}

.ui-input-modal__label {
  font-weight: 600;
  font-size: 0.875rem;
  color: var(--color-text-primary);
  margin: 0;
}

.ui-input-modal__input {
  width: 100%;
  padding: 0.75rem 1rem;
  font-size: 1rem;
  font-family: inherit;
  color: var(--color-text-primary);
  background: var(--color-surface-raised);
  border: 1px solid var(--color-border-default);
  border-radius: var(--radius-md);
  outline: none;
  transition: all 0.2s ease;
}

.ui-input-modal__input:hover {
  border-color: var(--color-border-strong);
}

.ui-input-modal__input:focus {
  border-color: var(--color-focus);
  box-shadow: 0 0 0 3px rgba(91, 155, 213, 0.1);
}

.ui-input-modal__input::placeholder {
  color: var(--color-text-muted);
}

.ui-input-modal__hint {
  font-size: 0.8125rem;
  color: var(--color-text-tertiary);
  margin: 0;
}

.ui-input-modal__footer {
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

.ui-modal-fade-enter-active .ui-input-modal__content {
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
  .ui-input-modal__content {
    min-width: 85vw;
  }

  .ui-input-modal__header,
  .ui-input-modal__body,
  .ui-input-modal__footer {
    padding-left: var(--space-xl);
    padding-right: var(--space-xl);
  }

  .ui-input-modal__footer {
    flex-direction: column-reverse;
  }
}
</style>
