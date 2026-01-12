<script setup>
import { useRoute, useRouter } from 'vue-router'
import { computed } from 'vue'
import Sidebar from './components/Sidebar.vue'
import ModalProvider from './components/ModalProvider.vue'

const route = useRoute()
const router = useRouter()
const currentPage = computed(() => route.name || 'dashboard')
const hideLayout = computed(() => route.meta?.hideLayout === true)
function goTo(page, params){
  if (route.name === page && JSON.stringify(route.params) === JSON.stringify(params || {})) return
  router.push(params ? { name: page, params } : { name: page })
}
function openEditor(areaId){
  goTo('editor', areaId ? { areaId } : undefined)
}
</script>

<template>
  <template v-if="!hideLayout">
    <Sidebar :currentPage="currentPage" @navigate="goTo" />
    <div class="page-shift" :class="currentPage">
      <router-view @openEditor="openEditor" />
    </div>
  </template>
  <template v-else>
    <router-view @openEditor="openEditor" />
  </template>

  <!-- Global modal provider -->
  <ModalProvider />
</template>

<style scoped>
.page-shift {
  margin-left: 72px;
  min-height: 100vh;
  background: var(--color-bg);
}
</style>
