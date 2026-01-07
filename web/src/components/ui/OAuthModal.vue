<template>
  <Teleport to="body">
    <transition name="ui-modal-fade">
      <div v-if="open" class="ui-oauth-modal" role="dialog" aria-modal="true" @keydown.esc="handleClose">
        <div class="ui-oauth-modal__backdrop" @click="handleClose"></div>
        <div class="ui-oauth-modal__content" role="document">
          <div class="ui-oauth-modal__header">
            <h3 class="ui-oauth-modal__title">{{ title }}</h3>
            <button
              class="ui-oauth-modal__close"
              type="button"
              @click="handleClose"
              aria-label="Close"
            >
              ✕
            </button>
          </div>

          <div class="ui-oauth-modal__body">
            <div class="ui-oauth-modal__icon">
              <svg xmlns="http://www.w3.org/2000/svg" fill="none" viewBox="0 0 24 24" stroke="currentColor">
                <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M12 15v2m-6 4h12a2 2 0 002-2v-6a2 2 0 00-2-2H6a2 2 0 00-2 2v6a2 2 0 002 2zm10-10V7a4 4 0 00-8 0v4h8z" />
              </svg>
            </div>

            <div class="ui-oauth-modal__message">
              <p>{{ message }}</p>
            </div>

            <div class="ui-oauth-modal__steps">
              <div class="oauth-step">
                <div class="step-number">1</div>
                <div class="step-content">A new window will open for authentication</div>
              </div>
              <div class="oauth-step">
                <div class="step-number">2</div>
                <div class="step-content">Sign in and authorize the application</div>
              </div>
              <div class="oauth-step">
                <div class="step-number">3</div>
                <div class="step-content">Return here after authorization completes</div>
              </div>
            </div>

            <div v-if="isAuthenticating" class="ui-oauth-modal__status">
              <div class="status-spinner"></div>
              <p>Waiting for authorization...</p>
              <small>Please complete the authentication in the popup window</small>
            </div>
          </div>

          <div class="ui-oauth-modal__footer">
            <Button
              variant="ghost"
              @click="handleClose"
              :disabled="isAuthenticating"
            >
              Cancel
            </Button>
            <Button
              variant="primary"
              @click="handleOpenAuth"
              :disabled="isAuthenticating"
              :loading="isAuthenticating"
            >
              {{ isAuthenticating ? 'Authenticating...' : 'Continue' }}
            </Button>
          </div>
        </div>
      </div>
    </transition>
  </Teleport>
</template>

<script setup>
import { ref, watch } from 'vue'
import { Teleport } from 'vue'
import Button from './Button.vue'

const props = defineProps({
  open: { type: Boolean, default: false },
  title: { type: String, default: 'Authorize Service' },
  message: { type: String, default: 'Connect your account to continue' },
  authUrl: { type: String, default: '' }
})

const emit = defineEmits(['close', 'success', 'error'])

const isAuthenticating = ref(false)
let authWindow = null
let pollTimer = null

watch(() => props.open, (isOpen) => {
  if (!isOpen) {
    cleanup()
  }
})

function handleOpenAuth() {
  if (!props.authUrl) {
    emit('error', new Error('No authorization URL provided'))
    return
  }

  isAuthenticating.value = true

  // Open OAuth URL in a centered popup window
  const width = 600
  const height = 700
  const left = (screen.width - width) / 2
  const top = (screen.height - height) / 2

  authWindow = window.open(
    props.authUrl,
    'OAuth Authorization',
    `width=${width},height=${height},left=${left},top=${top},toolbar=no,menubar=no,location=no,status=no,resizable=yes`
  )

  // Poll to check if auth window is closed
  pollTimer = setInterval(() => {
    if (authWindow && authWindow.closed) {
      clearInterval(pollTimer)
      pollTimer = null

      // Emit success after a delay to allow backend to process
      setTimeout(() => {
        isAuthenticating.value = false
        emit('success')
        handleClose()
      }, 1000)
    }
  }, 500)

  // Fallback: if window doesn't open, show error
  if (!authWindow) {
    isAuthenticating.value = false
    emit('error', new Error('Failed to open authentication window. Please check your popup blocker settings.'))
  }
}

function handleClose() {
  cleanup()
  emit('close')
}

function cleanup() {
  isAuthenticating.value = false

  if (pollTimer) {
    clearInterval(pollTimer)
    pollTimer = null
  }

  if (authWindow && !authWindow.closed) {
    authWindow.close()
    authWindow = null
  }
}

// Cleanup on component unmount
import { onUnmounted } from 'vue'
onUnmounted(() => {
  cleanup()
})
</script>

<style scoped>
.ui-oauth-modal {
  position: fixed;
  inset: 0;
  z-index: 2000;
  display: grid;
  place-items: center;
}

.ui-oauth-modal__backdrop {
  position: absolute;
  inset: 0;
  background: rgba(0, 0, 0, 0.85);
  backdrop-filter: blur(4px);
}

.ui-oauth-modal__content {
  position: relative;
  background: var(--color-surface);
  color: var(--color-text-primary);
  border-radius: var(--radius-2xl);
  border: 1px solid var(--color-border-strong);
  box-shadow: var(--shadow-xl);
  min-width: min(90vw, 520px);
  max-width: 90vw;
  padding: 0;
  z-index: 1;
  overflow: hidden;
}

.ui-oauth-modal__header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: var(--space-lg);
  padding: var(--space-xl) var(--space-2xl);
  background: var(--color-surface-raised);
  border-bottom: 1px solid var(--color-border-strong);
}

.ui-oauth-modal__title {
  font-weight: 600;
  font-size: 1.25rem;
  color: var(--color-text-primary);
  margin: 0;
}

.ui-oauth-modal__close {
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

.ui-oauth-modal__close:hover {
  background: var(--color-hover-strong);
  border-color: var(--color-border-bright);
  color: var(--color-text-primary);
  transform: rotate(90deg);
}

.ui-oauth-modal__close:focus-visible {
  outline: 2px solid var(--color-focus);
  outline-offset: 2px;
}

.ui-oauth-modal__body {
  padding: var(--space-2xl);
  background: var(--color-bg);
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: var(--space-lg);
  text-align: center;
}

.ui-oauth-modal__icon {
  width: 64px;
  height: 64px;
  border-radius: var(--radius-full);
  background: var(--color-info-bg);
  color: var(--color-info);
  border: 2px solid var(--color-info);
  box-shadow: 0 0 16px rgba(91, 155, 213, 0.3);
  display: flex;
  align-items: center;
  justify-content: center;
}

.ui-oauth-modal__icon svg {
  width: 32px;
  height: 32px;
  stroke-width: 2.5;
}

.ui-oauth-modal__message {
  color: var(--color-text-secondary);
  font-size: 0.9375rem;
  line-height: 1.5;
}

.ui-oauth-modal__message p {
  margin: 0;
}

.ui-oauth-modal__steps {
  width: 100%;
  display: flex;
  flex-direction: column;
  gap: var(--space-md);
  margin-top: var(--space-md);
  text-align: left;
}

.oauth-step {
  display: flex;
  align-items: flex-start;
  gap: var(--space-md);
  padding: var(--space-md);
  background: var(--color-surface-raised);
  border-radius: var(--radius-md);
  border: 1px solid var(--color-border-default);
}

.step-number {
  width: 32px;
  height: 32px;
  border-radius: var(--radius-full);
  background: var(--color-accent-blue);
  color: var(--color-text-primary);
  display: flex;
  align-items: center;
  justify-content: center;
  font-weight: 600;
  font-size: 0.875rem;
  flex-shrink: 0;
}

.step-content {
  flex: 1;
  padding-top: 0.25rem;
  color: var(--color-text-secondary);
  font-size: 0.875rem;
  line-height: 1.5;
}

.ui-oauth-modal__status {
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: var(--space-sm);
  padding: var(--space-lg);
  background: var(--color-info-bg);
  border: 1px solid var(--color-info);
  border-radius: var(--radius-md);
  margin-top: var(--space-md);
  width: 100%;
}

.status-spinner {
  width: 32px;
  height: 32px;
  border: 3px solid rgba(91, 155, 213, 0.2);
  border-top-color: var(--color-info);
  border-radius: 50%;
  animation: spin 0.8s linear infinite;
}

.ui-oauth-modal__status p {
  margin: 0;
  font-weight: 600;
  color: var(--color-info);
  font-size: 0.9375rem;
}

.ui-oauth-modal__status small {
  color: var(--color-text-tertiary);
  font-size: 0.8125rem;
}

.ui-oauth-modal__footer {
  display: flex;
  justify-content: flex-end;
  gap: var(--space-sm);
  padding: var(--space-xl) var(--space-2xl);
  background: var(--color-surface-raised);
  border-top: 1px solid var(--color-border-subtle);
}

@keyframes spin {
  to {
    transform: rotate(360deg);
  }
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

.ui-modal-fade-enter-active .ui-oauth-modal__content {
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
  .ui-oauth-modal__content {
    min-width: 85vw;
  }

  .ui-oauth-modal__header,
  .ui-oauth-modal__body,
  .ui-oauth-modal__footer {
    padding-left: var(--space-xl);
    padding-right: var(--space-xl);
  }

  .ui-oauth-modal__footer {
    flex-direction: column-reverse;
  }
}
</style>
