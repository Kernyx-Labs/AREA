<template>
  <div class="sidebar">
    <div class="logo">
      <img src="/AREA.png" alt="AREA logo" class="sidebar-logo" />
    </div>
    <nav class="nav">
      <button
        v-for="item in items"
        :key="item.id"
        :title="item.id"
        class="nav-btn"
        :class="{ active: currentPage === item.id }"
        @click="$emit('navigate', item.id)"
      >
        <component :is="item.icon" size="20" />
      </button>
    </nav>
    <div class="scheduler-indicator" title="Scheduler Status">
      <div class="scheduler-label">
        <ActivityIcon size="12" />
        <div class="scheduler-status-icon" :class="{ running: isRunning }"></div>
      </div>
      <div class="scheduler-progress-bg">
        <div class="scheduler-progress-bar" :style="{ width: progress + '%' }"></div>
      </div>
    </div>
    <button class="logout" title="logout" @click="handleLogout">
      <LogOutIcon size="20" />
    </button>
  </div>
</template>

<script setup>
import { ZapIcon, HomeIcon, SettingsIcon, UserIcon, LogOutIcon, FileTextIcon, ActivityIcon } from 'lucide-vue-next'
import { computed, ref, onMounted, onUnmounted } from 'vue'
import { useRouter } from 'vue-router'
import { useAuthStore } from '../stores/authStore.js'
import { api } from '../services/api' // Assuming api service exists based on grep results

const router = useRouter()
const authStore = useAuthStore()
const props = defineProps({ currentPage: [String, Object] })
defineEmits(['navigate'])
const currentPage = computed(() => typeof props.currentPage === 'string' ? props.currentPage : props.currentPage?.value)
const items = [
  { id: 'dashboard', icon: HomeIcon },
  { id: 'editor', icon: ZapIcon },
  { id: 'services', icon: SettingsIcon },
  { id: 'logs', icon: FileTextIcon },
  { id: 'profile', icon: UserIcon }
]

// Scheduler State
const progress = ref(0)
const isRunning = ref(false)
let pollingIntervalId = null
let animationFrameId = null
let nextExecutionTime = 0
let lastExecutionTime = 0
let serverInterval = 60000

async function fetchSchedulerStatus() {
  try {
    const response = await api.get('/scheduler/status')
    if (response.data && response.data.success) {
      const data = response.data.data
      serverInterval = data.interval
      lastExecutionTime = data.lastExecutionTime
      nextExecutionTime = data.nextExecutionTime
      
      // Trigger "running" animation if we just finished an execution (within last 2 seconds)
      const now = Date.now()
      if (now - lastExecutionTime < 2000) {
        triggerRunningEffect()
      }
    }
  } catch (error) {
    console.warn('Failed to fetch scheduler status', error)
  }
}

function triggerRunningEffect() {
  isRunning.value = true
  setTimeout(() => {
    isRunning.value = false
  }, 2000)
}

function updateProgress() {
  const now = Date.now()
  if (nextExecutionTime > 0) {
    const elapsed = now - lastExecutionTime
    const percentage = Math.min(100, (elapsed / serverInterval) * 100)
    progress.value = percentage
    
    // Safety check: if we go over 100%, we should probably fetch status again soon
    if (percentage >= 100 && !isRunning.value) {
       // Ideally fetch status to resync, but strictly we wait for the polling interval
    }
  }
  animationFrameId = requestAnimationFrame(updateProgress)
}

onMounted(() => {
  fetchSchedulerStatus()
  // Poll status every 10 seconds to re-sync
  pollingIntervalId = setInterval(fetchSchedulerStatus, 10000)
  updateProgress()
})

onUnmounted(() => {
  if (pollingIntervalId) clearInterval(pollingIntervalId)
  if (animationFrameId) cancelAnimationFrame(animationFrameId)
})

async function handleLogout() {
  try {
    await authStore.logout()
    router.push({ name: 'login' })
  } catch (error) {
    console.error('Logout error:', error)
    // Even if logout API fails, redirect to login
    router.push({ name: 'login' })
  }
}
</script>

<style scoped src="../assets/Sidebar.css"></style>
