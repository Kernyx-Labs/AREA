<template>
  <div class="areas-wrap">
    <div class="heading-row">
      <div>
        <h1>My Automations</h1>
        <p>Manage your AREA workflows</p>
      </div>
      <button class="new-area" @click="createNewArea"><PlusIcon size="20" /> Create New Area</button>
    </div>
    <div class="areas-grid">
      <div v-for="area in areas" :key="area.id" class="area-card" @click="openArea(area.id)">
        <div class="area-head">
          <h3>{{ area.name }}</h3>
          <div class="status-dot" :class="{ active: area.active }"></div>
        </div>
        <div class="area-meta">{{ area.nodes }} nodes • {{ area.active ? 'Active' : 'Inactive' }}</div>
      </div>
    </div>
  </div>
</template>
<script setup>
import { PlusIcon } from 'lucide-vue-next'
import { useRouter } from 'vue-router'
const router = useRouter()
const areas = [
  { id: 'area-1', name: 'GitHub → Discord', nodes: 2, active: true },
  { id: 'area-2', name: 'Daily Email Report', nodes: 3, active: true },
  { id: 'area-3', name: 'File Backup', nodes: 2, active: false }
]
function openArea(areaId) {
  router.push({ name: 'editor', params: { areaId } })
}
function createNewArea() {
  const newId = `area-${Date.now()}`
  router.push({ name: 'editor', params: { areaId: newId } })
}
</script>
<style scoped src="../assets/AreasDashboard.css"></style>
