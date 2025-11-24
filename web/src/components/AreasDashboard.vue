<template>
  <div class="areas-wrap">
    <div class="heading-row">
      <div>
        <h1>My Automations</h1>
        <p>Manage your AREA workflows</p>
      </div>
      <button class="new-area" @click="$emit('openEditor')"><PlusIcon size="20" /> New AREA</button>
    </div>
    <div class="areas-grid">
      <div v-for="area in areas" :key="area.id" class="area-card" @click="$emit('openEditor')">
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
const areas = [
  { id: 1, name: 'GitHub → Discord', nodes: 2, active: true },
  { id: 2, name: 'Daily Email Report', nodes: 3, active: true },
  { id: 3, name: 'File Backup', nodes: 2, active: false }
]
</script>
<style scoped>
.areas-wrap { padding:2rem; min-height:100vh; background: var(--color-bg); }
.heading-row { display:flex; justify-content:space-between; align-items:center; margin-bottom:2rem; }
.heading-row h1 { margin:0; font-size:2rem; color: var(--color-text); }
.heading-row p { margin:.25rem 0 0; color: var(--color-primary); }
.new-area { display:flex; align-items:center; gap:.5rem; background: var(--color-accent); color: var(--color-accent-contrast); border:none; font-weight:600; padding:.65rem 1.1rem; border-radius:16px; cursor:pointer; transition:transform .15s, background .3s; }
.new-area:hover { transform:scale(1.05); background: var(--color-accent); }
.areas-grid { display:grid; gap:1rem; grid-template-columns: repeat(auto-fill,minmax(240px,1fr)); }
.area-card { background: var(--color-surface); border-radius:16px; padding:1.1rem 1.2rem; box-shadow: var(--shadow-sm); cursor:pointer; transition: box-shadow .2s, transform .2s, background .3s; border: 1px solid var(--color-border); }
.area-card:hover { box-shadow: var(--shadow-md); transform:translateY(-3px); }
.area-head { display:flex; align-items:flex-start; justify-content:space-between; margin-bottom:.75rem; }
.area-head h3 { margin:0; font-size:1rem; font-weight:600; color: var(--color-text); }
.status-dot { width:12px; height:12px; border-radius:50%; background:#9e9e9e; }
.status-dot.active { background: var(--color-success); }
.area-meta { font-size:.75rem; color: var(--color-muted); display:flex; gap:.5rem; align-items:center; }
</style>
