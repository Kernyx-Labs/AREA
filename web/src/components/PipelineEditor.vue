<template>
  <div class="editor-root">
    <header class="editor-header">
      <input
        v-model="areaName"
        class="title-input"
        placeholder="Enter automation name..."
        :style="{ borderColor: titleFocused ? 'rgba(255,255,255,0.5)' : 'transparent' }"
        @focus="titleFocused = true"
        @blur="titleFocused = false"
      />
      <div class="header-actions">
        <button class="save-btn" @click="saveWorkflow">
          <PlayIcon size="16" /> Save & Activate
        </button>
      </div>
    </header>

    <!-- Split View: Actions on Left, Reactions on Right -->
    <div class="split-container">
      <!-- Left Side: Actions/Triggers -->
      <div class="side-panel left-panel">
        <div class="panel-header">
          <h3>When this happens...</h3>
          <button class="add-btn" @click="showActionPicker = true">
            <PlusIcon size="16" /> Add Trigger
          </button>
        </div>

        <div v-if="!selectedAction" class="empty-state">
          <ClockIcon size="48" :stroke-width="1.5" />
          <p>Click "Add Trigger" to start</p>
        </div>

        <div v-else class="config-card">
          <div class="config-header" :style="{ background: services[selectedAction.service].color }">
            <div class="config-header-left">
              <component :is="services[selectedAction.service]?.icon" size="20" color="white" />
              <span>{{ services[selectedAction.service].name }}</span>
            </div>
            <button class="remove-btn" @click="removeAction">
              <XIcon size="16" color="white" />
            </button>
          </div>

          <div class="config-body">
            <h4>{{ selectedAction.name }}</h4>
            <p class="config-desc">{{ selectedAction.desc }}</p>

            <!-- Gmail Configuration -->
            <div v-if="selectedAction.service === 'gmail'" class="config-options">
              <div class="form-group">
                <label>Check for new emails</label>
                <select v-model="actionConfig.interval">
                  <option value="60">Every minute</option>
                  <option value="300">Every 5 minutes</option>
                  <option value="600">Every 10 minutes</option>
                  <option value="1800">Every 30 minutes</option>
                  <option value="3600">Every hour</option>
                </select>
              </div>

              <div class="form-group">
                <label>Filter by sender (optional)</label>
                <input v-model="actionConfig.senderFilter" type="text" placeholder="example@gmail.com" />
              </div>

              <div class="form-group">
                <label>Filter by subject (optional)</label>
                <input v-model="actionConfig.subjectFilter" type="text" placeholder="Keywords..." />
              </div>

              <div class="available-vars">
                <strong>Available variables:</strong>
                <div class="var-tags">
                  <span class="var-tag" v-text="'{{sender}}'"></span>
                  <span class="var-tag" v-text="'{{subject}}'"></span>
                  <span class="var-tag" v-text="'{{body}}'"></span>
                  <span class="var-tag" v-text="'{{unreadCount}}'"></span>
                  <span class="var-tag" v-text="'{{receivedAt}}'"></span>
                </div>
              </div>
            </div>

            <!-- Timer Configuration -->
            <div v-if="selectedAction.service === 'timer'" class="config-options">
              <div class="form-group">
                <label>Trigger every</label>
                <div class="time-input-group">
                  <input v-model.number="actionConfig.intervalValue" type="number" min="1" />
                  <select v-model="actionConfig.intervalUnit">
                    <option value="seconds">Seconds</option>
                    <option value="minutes">Minutes</option>
                    <option value="hours">Hours</option>
                    <option value="days">Days</option>
                  </select>
                </div>
              </div>

              <div class="available-vars">
                <strong>Available variables:</strong>
                <div class="var-tags">
                  <span class="var-tag" v-text="'{{timestamp}}'"></span>
                  <span class="var-tag" v-text="'{{date}}'"></span>
                  <span class="var-tag" v-text="'{{time}}'"></span>
                </div>
              </div>
            </div>

            <!-- GitHub Configuration -->
            <div v-if="selectedAction.service === 'github'" class="config-options">
              <div class="form-group">
                <label>Repository</label>
                <input v-model="actionConfig.repository" type="text" placeholder="owner/repo" />
              </div>

              <div class="form-group">
                <label>Event type</label>
                <select v-model="actionConfig.eventType">
                  <option value="issues">New Issue</option>
                  <option value="pull_request">Pull Request</option>
                  <option value="push">Push</option>
                  <option value="release">Release</option>
                </select>
              </div>

              <div class="available-vars">
                <strong>Available variables:</strong>
                <div class="var-tags">
                  <span class="var-tag" v-text="'{{author}}'"></span>
                  <span class="var-tag" v-text="'{{title}}'"></span>
                  <span class="var-tag" v-text="'{{url}}'"></span>
                  <span class="var-tag" v-text="'{{repository}}'"></span>
                </div>
              </div>
            </div>
          </div>
        </div>
      </div>

      <!-- Arrow Connector -->
      <div class="connector-arrow">
        <div class="arrow-line"></div>
        <div class="arrow-head">→</div>
      </div>

      <!-- Right Side: Reactions -->
      <div class="side-panel right-panel">
        <div class="panel-header">
          <h3>Do this...</h3>
          <button class="add-btn" @click="showReactionPicker = true" :disabled="!selectedAction">
            <PlusIcon size="16" /> Add Action
          </button>
        </div>

        <div v-if="!selectedReaction" class="empty-state">
          <MessageSquareIcon size="48" :stroke-width="1.5" />
          <p>{{ selectedAction ? 'Click "Add Action" to continue' : 'Add a trigger first' }}</p>
        </div>

        <div v-else class="config-card">
          <div class="config-header" :style="{ background: services[selectedReaction.service].color }">
            <div class="config-header-left">
              <component :is="services[selectedReaction.service]?.icon" size="20" color="white" />
              <span>{{ services[selectedReaction.service].name }}</span>
            </div>
            <button class="remove-btn" @click="removeReaction">
              <XIcon size="16" color="white" />
            </button>
          </div>

          <div class="config-body">
            <h4>{{ selectedReaction.name }}</h4>
            <p class="config-desc">{{ selectedReaction.desc }}</p>

            <!-- Discord Configuration -->
            <div v-if="selectedReaction.service === 'discord'" class="config-options">
              <div class="form-group">
                <label>Webhook URL *</label>
                <input
                  v-model="reactionConfig.webhookUrl"
                  type="url"
                  placeholder="https://discord.com/api/webhooks/123.../abc..."
                  required
                />
                <small>Get this from Discord: Server Settings → Integrations → Webhooks</small>
              </div>

              <div class="form-group">
                <label>Channel Name (optional)</label>
                <input v-model="reactionConfig.channelName" type="text" placeholder="general" />
              </div>

              <div class="form-group">
                <label>Message template</label>
                <textarea
                  v-model="reactionConfig.message"
                  rows="4"
                  placeholder="Enter your message... Use variables like {{sender}} or {{unreadCount}}"
                ></textarea>
                <small v-if="selectedAction">
                  You can use variables from {{ services[selectedAction.service].name }}
                </small>
              </div>

              <div v-if="selectedAction" class="insert-var-section">
                <label>Insert variable:</label>
                <div class="var-buttons">
                  <button
                    v-for="varName in getAvailableVariables()"
                    :key="varName"
                    class="var-insert-btn"
                    @click="insertVariable(varName)"
                    v-text="`{{${varName}}}`"
                  >
                  </button>
                </div>
              </div>
            </div>

            <!-- Gmail Send Configuration -->
            <div v-if="selectedReaction.service === 'gmail'" class="config-options">
              <div class="form-group">
                <label>To</label>
                <input v-model="reactionConfig.to" type="email" placeholder="recipient@example.com" />
              </div>

              <div class="form-group">
                <label>Subject</label>
                <input v-model="reactionConfig.subject" type="text" placeholder="Email subject..." />
              </div>

              <div class="form-group">
                <label>Body</label>
                <textarea
                  v-model="reactionConfig.body"
                  rows="6"
                  placeholder="Email body... Use variables from your trigger"
                ></textarea>
              </div>

              <div v-if="selectedAction" class="insert-var-section">
                <label>Insert variable:</label>
                <div class="var-buttons">
                  <button
                    v-for="varName in getAvailableVariables()"
                    :key="varName"
                    class="var-insert-btn"
                    @click="insertVariable(varName, 'body')"
                    v-text="`{{${varName}}}`"
                  >
                  </button>
                </div>
              </div>
            </div>

            <!-- Dropbox Configuration -->
            <div v-if="selectedReaction.service === 'dropbox'" class="config-options">
              <div class="form-group">
                <label>Folder path</label>
                <input v-model="reactionConfig.folderPath" type="text" placeholder="/Documents/Backups" />
              </div>

              <div class="form-group">
                <label>File name</label>
                <input v-model="reactionConfig.fileName" type="text" placeholder="backup-{{date}}.txt" />
              </div>
            </div>
          </div>
        </div>
      </div>
    </div>

    <!-- Action Picker Modal -->
    <div v-if="showActionPicker" class="modal-overlay" @click="showActionPicker = false">
      <div class="modal-content" @click.stop>
        <div class="modal-header">
          <h3>Choose a Trigger</h3>
          <button class="close-modal-btn" @click="showActionPicker = false">
            <XIcon size="20" />
          </button>
        </div>
        <div class="modal-body">
          <div
            v-for="action in actionsList"
            :key="action.service + action.name"
            class="picker-item"
            @click="selectActionTrigger(action)"
          >
            <div class="picker-icon" :style="{ background: services[action.service].color }">
              <component :is="services[action.service]?.icon" size="24" color="white" />
            </div>
            <div class="picker-info">
              <div class="picker-title">{{ action.name }}</div>
              <div class="picker-service">{{ services[action.service].name }}</div>
              <div class="picker-desc">{{ action.desc }}</div>
            </div>
          </div>
        </div>
      </div>
    </div>

    <!-- Reaction Picker Modal -->
    <div v-if="showReactionPicker" class="modal-overlay" @click="showReactionPicker = false">
      <div class="modal-content" @click.stop>
        <div class="modal-header">
          <h3>Choose an Action</h3>
          <button class="close-modal-btn" @click="showReactionPicker = false">
            <XIcon size="20" />
          </button>
        </div>
        <div class="modal-body">
          <div
            v-for="reaction in reactionsList"
            :key="reaction.service + reaction.name"
            class="picker-item"
            @click="selectReactionAction(reaction)"
          >
            <div class="picker-icon" :style="{ background: services[reaction.service].color }">
              <component :is="services[reaction.service]?.icon" size="24" color="white" />
            </div>
            <div class="picker-info">
              <div class="picker-title">{{ reaction.name }}</div>
              <div class="picker-service">{{ services[reaction.service].name }}</div>
              <div class="picker-desc">{{ reaction.desc }}</div>
            </div>
          </div>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, computed, onMounted } from 'vue'
import { PlusIcon, PlayIcon, XIcon, MailIcon, ClockIcon, GithubIcon, MessageSquareIcon, CloudIcon, CalendarIcon } from 'lucide-vue-next'
import { api } from '@/services/api'

const props = defineProps({ areaId: { type: String, default: null } })

const areaName = ref(props.areaId ? `Automation #${props.areaId}` : 'New Automation')
const titleFocused = ref(false)

const services = {
  gmail: { name: 'Gmail', color: '#EA4335', icon: MailIcon },
  timer: { name: 'Timer', color: '#4285F4', icon: ClockIcon },
  github: { name: 'GitHub', color: '#6e40c9', icon: GithubIcon },
  discord: { name: 'Discord', color: '#5865F2', icon: MessageSquareIcon },
  dropbox: { name: 'Dropbox', color: '#0061FF', icon: CloudIcon },
  outlook: { name: 'Outlook', color: '#0078D4', icon: CalendarIcon }
}

const actionsList = [
  { service: 'gmail', name: 'New email received', desc: 'Triggers when a new email arrives' },
  { service: 'gmail', name: 'Email with attachment', desc: 'Triggers on emails with files' },
  { service: 'timer', name: 'Schedule', desc: 'Triggers at specific times' },
  { service: 'timer', name: 'Interval', desc: 'Triggers every X minutes' },
  { service: 'github', name: 'New Issue', desc: 'Triggers on new issues' },
  { service: 'github', name: 'Pull Request', desc: 'Triggers on new PRs' }
]

const reactionsList = [
  { service: 'discord', name: 'Send Message', desc: 'Posts to a channel' },
  { service: 'gmail', name: 'Send Email', desc: 'Sends an email' },
  { service: 'dropbox', name: 'Upload File', desc: 'Uploads a file to Dropbox' },
  { service: 'outlook', name: 'Create Event', desc: 'Creates a calendar event' },
  { service: 'github', name: 'Create Issue', desc: 'Creates a new issue' }
]

// State
const showActionPicker = ref(false)
const showReactionPicker = ref(false)
const selectedAction = ref(null)
const selectedReaction = ref(null)

// Configuration objects
const actionConfig = ref({
  // Gmail
  interval: '300',
  senderFilter: '',
  subjectFilter: '',
  // Timer
  intervalValue: 5,
  intervalUnit: 'minutes',
  // GitHub
  repository: '',
  eventType: 'issues'
})

const reactionConfig = ref({
  // Discord
  channelId: '',
  webhookUrl: '',
  channelName: '',
  message: '',
  // Gmail
  to: '',
  subject: '',
  body: '',
  // Dropbox
  folderPath: '',
  fileName: ''
})

// Variable mappings for each service
const serviceVariables = {
  gmail: ['sender', 'subject', 'body', 'unreadCount', 'receivedAt'],
  timer: ['timestamp', 'date', 'time'],
  github: ['author', 'title', 'url', 'repository']
}

// Functions
function selectActionTrigger(action) {
  selectedAction.value = action
  showActionPicker.value = false
}

function selectReactionAction(reaction) {
  selectedReaction.value = reaction
  showReactionPicker.value = false
}

function removeAction() {
  selectedAction.value = null
  actionConfig.value = {
    interval: '300',
    senderFilter: '',
    subjectFilter: '',
    intervalValue: 5,
    intervalUnit: 'minutes',
    repository: '',
    eventType: 'issues'
  }
}

function removeReaction() {
  selectedReaction.value = null
  reactionConfig.value = {
    channelId: '',
    webhookUrl: '',
    channelName: '',
    message: '',
    to: '',
    subject: '',
    body: '',
    folderPath: '',
    fileName: ''
  }
}

function getAvailableVariables() {
  if (!selectedAction.value) return []
  return serviceVariables[selectedAction.value.service] || []
}

function insertVariable(varName, field = 'message') {
  const variable = `{{${varName}}}`
  if (field === 'message') {
    const textarea = document.querySelector('textarea')
    if (textarea) {
      const start = textarea.selectionStart
      const end = textarea.selectionEnd
      const text = reactionConfig.value.message
      reactionConfig.value.message = text.substring(0, start) + variable + text.substring(end)
    } else {
      reactionConfig.value.message += variable
    }
  } else if (field === 'body') {
    reactionConfig.value.body += variable
  }
}

async function saveWorkflow() {
  if (!selectedAction.value || !selectedReaction.value) {
    alert('Please configure both a trigger and an action')
    return
  }

  try {
    let saved;

    // Check if this is a Gmail → Discord workflow (use /api/areas)
    if (selectedAction.value.service === 'gmail' && selectedReaction.value.service === 'discord') {
      // Validate webhook URL is provided
      if (!reactionConfig.value.webhookUrl || !reactionConfig.value.webhookUrl.trim()) {
        alert('Please provide a Discord webhook URL. You can create one in Discord: Server Settings → Integrations → Webhooks')
        return
      }

      // Get service connections to find the IDs
      const connections = await api.getConnectedServices()
      const gmailConnection = connections.find(c => c.type === 'GMAIL')
      const discordConnection = connections.find(c => c.type === 'DISCORD')

      if (!gmailConnection) {
        alert('Please connect your Gmail account first in the Services page')
        return
      }
      if (!discordConnection) {
        alert('Please connect your Discord bot first in the Services page')
        return
      }

      const areaData = {
        actionConnectionId: gmailConnection.id,
        reactionConnectionId: discordConnection.id,
        gmailLabel: 'INBOX',
        gmailSubjectContains: actionConfig.value.subjectFilter || '',
        gmailFromAddress: actionConfig.value.senderFilter || '',
        discordWebhookUrl: reactionConfig.value.webhookUrl.trim(),
        discordChannelName: reactionConfig.value.channelName || 'general',
        discordMessageTemplate: reactionConfig.value.message || 'New email: {{subject}}'
      }

      saved = await api.createArea(areaData)
      alert(`✓ Area "${areaName.value}" created and activated!`)
    } else {
      // Use generic workflow API for other combinations
      const workflow = {
        name: areaName.value,
        trigger: {
          service: selectedAction.value.service,
          type: selectedAction.value.name,
          config: { ...actionConfig.value }
        },
        action: {
          service: selectedReaction.value.service,
          type: selectedReaction.value.name,
          config: { ...reactionConfig.value }
        }
      }

      saved = await api.createWorkflow(workflow)
      alert(`✓ Workflow "${saved.name}" created and activated!`)
    }

    // Reset form
    areaName.value = 'New Automation'
    removeAction()
    removeReaction()
  } catch (error) {
    console.error('Failed to save workflow:', error)
    alert('Failed to save workflow: ' + error.message)
  }
}

</script>

<style scoped src="@/assets/PipelineEditor.css"></style>
