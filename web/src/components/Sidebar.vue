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
    <button class="logout" title="logout" @click="handleLogout">
      <LogOutIcon size="20" />
    </button>
  </div>
</template>

<script setup>
import { ZapIcon, HomeIcon, SettingsIcon, UserIcon, LogOutIcon, FileTextIcon } from 'lucide-vue-next'
import { computed } from 'vue'
import { useRouter } from 'vue-router'
import { useAuthStore } from '../stores/authStore.js'

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
