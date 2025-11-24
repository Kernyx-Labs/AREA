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
        @click="$emit('update:currentPage', item.id)"
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
import { onMounted, ref } from 'vue'
const props = defineProps({ currentPage: String })
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

<style scoped>
.sidebar {
  width: 64px;
  height: 100vh;
  position: fixed;
  left: 0; top: 0;
  display: flex;
  flex-direction: column;
  align-items: center;
  padding: 1rem 0.75rem;
  gap: 1rem;
  background: var(--color-noble-black);
}
.logo { width: 40px; height: 40px; border-radius: 12px; display: flex; align-items: center; justify-content: center; background: var(--color-accent); }
.nav { flex: 1; display: flex; flex-direction: column; gap: 0.5rem; }
.nav-btn { width: 40px; height: 40px; border-radius: 12px; background: transparent; display: flex; align-items: center; justify-content: center; color: rgba(255,255,255,.55); cursor: pointer; border: none; }
.nav-btn.active { background: var(--color-accent-soft); color: var(--color-accent); }
.nav-btn:hover { background: var(--color-hover); }
.theme-toggle { width:40px; height:40px; border-radius:12px; background: var(--color-surface-alt); color: var(--color-noble-black); border:none; cursor:pointer; display:flex; align-items:center; justify-content:center; font-size:1rem; }
.theme-toggle:hover { background: var(--color-accent-soft); color: var(--color-accent); }
.logout { width: 40px; height: 40px; border-radius: 12px; background: transparent; border: none; color: var(--color-danger); display: flex; align-items: center; justify-content: center; cursor: pointer; }
.logout:hover { background: rgba(255,95,86,0.2); }
</style>
