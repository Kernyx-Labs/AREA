<script setup>
import { useRoute, useRouter } from 'vue-router'
import { computed } from 'vue'
import Sidebar from './components/Sidebar.vue'

const route = useRoute()
const router = useRouter()
const currentPage = computed(() => route.name || 'dashboard')
function goTo(page, params){
  if (route.name === page && JSON.stringify(route.params) === JSON.stringify(params || {})) return
  router.push(params ? { name: page, params } : { name: page })
}
function openEditor(areaId){
  goTo('editor', areaId ? { areaId } : undefined)
}
</script>

<template>
  <Sidebar :currentPage="currentPage" @navigate="goTo" />
  <div class="page-shift" :class="currentPage">
    <router-view @openEditor="openEditor" />
  </div>
</template>

<style scoped>
.page-shift { margin-left:64px; min-height:100vh; }
</style>
