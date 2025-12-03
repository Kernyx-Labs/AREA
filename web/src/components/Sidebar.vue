<template>
  <div class="sidebar">
    <div class="logo">
      <ZapIcon size="22" :color="accent" />
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
    <button class="theme-toggle" @click="toggleTheme" :title="isDark ? 'Passer clair' : 'Passer sombre'">
      <span v-if="isDark">🌞</span>
      <span v-else>🌙</span>
    </button>
    <button class="logout" title="logout">
      <LogOutIcon size="20" />
    </button>
  </div>
</template>

<script setup>
import { ZapIcon, HomeIcon, SettingsIcon, UserIcon, LogOutIcon } from 'lucide-vue-next'
import { onMounted, ref, computed } from 'vue'
const props = defineProps({ currentPage: [String, Object] })
defineEmits(['navigate'])
const currentPage = computed(() => typeof props.currentPage === 'string' ? props.currentPage : props.currentPage?.value)
const accent = '#FFB162'
const items = [
  { id: 'dashboard', icon: HomeIcon },
  { id: 'editor', icon: ZapIcon },
  { id: 'services', icon: SettingsIcon },
  { id: 'profile', icon: UserIcon }
]
const stored = typeof window !== 'undefined' ? localStorage.getItem('theme') : null
const isDark = ref(stored ? stored === 'dark' : true)
function syncTheme() {
  const root = document.documentElement
  if (isDark.value) {
    root.setAttribute('data-theme','dark')
    localStorage.setItem('theme','dark')
  } else {
    root.removeAttribute('data-theme')
    localStorage.setItem('theme','light')
  }
}
function toggleTheme() { isDark.value = !isDark.value; syncTheme() }
onMounted(() => { syncTheme() })
</script>

<style scoped src="../assets/Sidebar.css"></style>
