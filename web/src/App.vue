<script setup>
import Sidebar from './components/Sidebar.vue'
import AreasDashboard from './components/AreasDashboard.vue'
import PipelineEditor from './components/PipelineEditor.vue'
import ServicesView from './components/ServicesView.vue'
import ProfileView from './components/ProfileView.vue'
import { ref, computed } from 'vue'

const currentPage = ref('editor')
function openEditor() { currentPage.value = 'editor' }

const currentPageComponent = computed(() => {
  switch (currentPage.value) {
    case 'dashboard': return AreasDashboard
    case 'editor': return PipelineEditor
    case 'services': return ServicesView
    case 'profile': return ProfileView
    default: return AreasDashboard
  }
})
const pageProps = computed(() => currentPage.value === 'dashboard' ? { openEditor } : {})
</script>

<template>
  <Sidebar :currentPage="currentPage" @update:currentPage="val => currentPage = val" />
  <div class="page-shift" :class="currentPage">
    <component :is="currentPageComponent" v-bind="pageProps" @openEditor="openEditor" />
  </div>
</template>

<style scoped>
.page-shift { margin-left:64px; min-height:100vh; }
</style>
