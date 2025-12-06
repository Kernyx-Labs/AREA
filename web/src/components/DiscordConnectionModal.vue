<template>
  <div class="modal-overlay" @click.self="$emit('close')">
    <div class="modal-container">
      <div class="modal-header">
        <h2>Connect Discord Bot</h2>
        <button class="close-btn" @click="$emit('close')">&times;</button>
      </div>

      <div class="modal-body">
        <div class="instructions">
          <h3>Setup Instructions:</h3>
          <ol>
            <li>Go to <a href="https://discord.com/developers/applications" target="_blank">Discord Developer Portal</a></li>
            <li>Create a new application or select an existing one</li>
            <li>Go to the "Bot" section and copy your bot token</li>
            <li>Enable "MESSAGE CONTENT INTENT" in bot settings</li>
            <li>Invite the bot to your server using OAuth2 URL Generator (select "bot" scope and "Send Messages" permission)</li>
            <li>Right-click your target channel in Discord → "Copy Channel ID" (enable Developer Mode in Discord settings if needed)</li>
          </ol>
        </div>

        <div v-if="error" class="error-message">{{ error }}</div>
        <div v-if="successMessage" class="success-message">{{ successMessage }}</div>

        <form @submit.prevent="handleSubmit">
          <div class="form-group">
            <label for="botToken">Bot Token *</label>
            <input
              id="botToken"
              v-model="formData.botToken"
              type="password"
              placeholder="Your Discord bot token"
              required
              :disabled="loading"
            />
            <small>Keep this token secret! Never share it publicly.</small>
          </div>

          <div class="form-group">
            <label for="channelId">Channel ID *</label>
            <input
              id="channelId"
              v-model="formData.channelId"
              type="text"
              placeholder="123456789012345678"
              required
              :disabled="loading"
            />
            <small>The Discord channel where messages will be sent</small>
          </div>

          <div class="button-group">
            <button
              type="button"
              @click="testConnection"
              class="btn-secondary"
              :disabled="loading || !formData.botToken || !formData.channelId"
            >
              {{ testing ? 'Testing...' : 'Test Connection' }}
            </button>
            <button
              type="submit"
              class="btn-primary"
              :disabled="loading || !formData.botToken || !formData.channelId"
            >
              {{ loading ? 'Connecting...' : 'Connect' }}
            </button>
          </div>
        </form>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref } from 'vue';
import { api } from '../services/api.js';

const emit = defineEmits(['close', 'connected']);

const formData = ref({
  botToken: '',
  channelId: ''
});

const loading = ref(false);
const testing = ref(false);
const error = ref(null);
const successMessage = ref(null);

async function testConnection() {
  try {
    testing.value = true;
    error.value = null;
    successMessage.value = null;

    await api.testDiscordConnection(formData.value.botToken, formData.value.channelId);
    successMessage.value = '✅ Test message sent successfully! Check your Discord channel.';
  } catch (err) {
    console.error('Test failed:', err);
    error.value = err.message || 'Failed to send test message. Please check your bot token and channel ID.';
  } finally {
    testing.value = false;
  }
}

async function handleSubmit() {
  try {
    loading.value = true;
    error.value = null;
    successMessage.value = null;

    await api.connectDiscord(formData.value.botToken, formData.value.channelId);
    successMessage.value = 'Discord connected successfully!';

    // Close modal and refresh after a short delay
    setTimeout(() => {
      emit('connected');
      emit('close');
    }, 1500);
  } catch (err) {
    console.error('Connection failed:', err);
    error.value = err.message || 'Failed to connect Discord. Please try again.';
  } finally {
    loading.value = false;
  }
}
</script>

<style scoped>
.modal-overlay {
  position: fixed;
  top: 0;
  left: 0;
  right: 0;
  bottom: 0;
  background: rgba(0, 0, 0, 0.7);
  display: flex;
  align-items: center;
  justify-content: center;
  z-index: 1000;
  padding: 1rem;
}

.modal-container {
  background: var(--color-surface, white);
  border-radius: 16px;
  max-width: 600px;
  width: 100%;
  max-height: 90vh;
  overflow-y: auto;
  box-shadow: 0 20px 25px -5px rgba(0, 0, 0, 0.1), 0 10px 10px -5px rgba(0, 0, 0, 0.04);
}

.modal-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 1.5rem;
  border-bottom: 1px solid var(--color-border, #e5e7eb);
}

.modal-header h2 {
  margin: 0;
  font-size: 1.5rem;
  color: var(--color-text, #111827);
}

.close-btn {
  background: none;
  border: none;
  font-size: 2rem;
  cursor: pointer;
  color: var(--color-text-secondary, #6b7280);
  padding: 0;
  width: 32px;
  height: 32px;
  display: flex;
  align-items: center;
  justify-content: center;
  border-radius: 4px;
  transition: background 0.2s;
}

.close-btn:hover {
  background: var(--color-bg, #f3f4f6);
}

.modal-body {
  padding: 1.5rem;
}

.instructions {
  background: #eff6ff;
  border: 1px solid #bfdbfe;
  border-radius: 8px;
  padding: 1rem;
  margin-bottom: 1.5rem;
}

.instructions h3 {
  margin: 0 0 0.75rem;
  font-size: 1rem;
  color: #1e40af;
}

.instructions ol {
  margin: 0;
  padding-left: 1.5rem;
  color: #1e3a8a;
}

.instructions li {
  margin-bottom: 0.5rem;
}

.instructions a {
  color: #2563eb;
  text-decoration: underline;
}

.error-message {
  padding: 0.75rem;
  background: rgba(239, 68, 68, 0.1);
  border: 1px solid rgba(239, 68, 68, 0.3);
  border-radius: 8px;
  color: #dc2626;
  margin-bottom: 1rem;
}

.success-message {
  padding: 0.75rem;
  background: rgba(16, 185, 129, 0.1);
  border: 1px solid rgba(16, 185, 129, 0.3);
  border-radius: 8px;
  color: #059669;
  margin-bottom: 1rem;
}

.form-group {
  margin-bottom: 1.5rem;
}

.form-group label {
  display: block;
  margin-bottom: 0.5rem;
  font-weight: 600;
  color: var(--color-text, #111827);
}

.form-group input {
  width: 100%;
  padding: 0.75rem;
  border: 1px solid var(--color-border, #d1d5db);
  border-radius: 8px;
  font-size: 1rem;
  transition: border-color 0.2s;
  background: var(--color-bg, white);
  color: var(--color-text, #111827);
  box-sizing: border-box;
}

.form-group input:focus {
  outline: none;
  border-color: var(--color-accent, #6366f1);
}

.form-group input:disabled {
  opacity: 0.6;
  cursor: not-allowed;
}

.form-group small {
  display: block;
  margin-top: 0.25rem;
  color: var(--color-text-secondary, #6b7280);
  font-size: 0.875rem;
}

.button-group {
  display: flex;
  gap: 1rem;
  justify-content: flex-end;
  margin-top: 2rem;
}

.btn-primary,
.btn-secondary {
  padding: 0.75rem 1.5rem;
  border-radius: 8px;
  font-size: 1rem;
  font-weight: 600;
  cursor: pointer;
  transition: all 0.2s;
  border: none;
}

.btn-primary {
  background: var(--color-accent, #6366f1);
  color: white;
}

.btn-primary:hover:not(:disabled) {
  background: var(--color-accent-hover, #4f46e5);
  transform: translateY(-1px);
}

.btn-secondary {
  background: transparent;
  color: var(--color-accent, #6366f1);
  border: 1px solid var(--color-accent, #6366f1);
}

.btn-secondary:hover:not(:disabled) {
  background: rgba(99, 102, 241, 0.1);
}

.btn-primary:disabled,
.btn-secondary:disabled {
  opacity: 0.6;
  cursor: not-allowed;
  transform: none;
}
</style>
