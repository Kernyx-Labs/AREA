<template>
  <div class="services-wrap">
    <h1>Connected Services</h1>
    <div class="services-grid">
      <div v-for="(service,key) in services" :key="key" class="service-card">
        <div class="svc-icon" :style="{ background: service.color }"></div>
        <div class="svc-info">
          <div class="svc-name">{{ service.name }}</div>
          <div class="svc-status">Connected</div>
        </div>
      </div>
    </div>
  </div>
</template>
<script setup>
const services = {
  gmail: { name: 'Gmail', color: '#EA4335' },
  timer: { name: 'Timer', color: '#4285F4' },
  github: { name: 'GitHub', color: '#6e40c9' },
  discord: { name: 'Discord', color: '#5865F2' },
  dropbox: { name: 'Dropbox', color: '#0061FF' },
  outlook: { name: 'Outlook', color: '#0078D4' }
}
</script>
<style scoped>
.services-wrap { padding:2rem; min-height:100vh; background: var(--color-bg); }
.services-wrap h1 { margin:0 0 1.5rem; font-size:2rem; color: var(--color-text); }
.services-grid { display:grid; gap:1rem; grid-template-columns:repeat(auto-fill,minmax(240px,1fr)); }
.service-card { background: var(--color-surface); border:1px solid var(--color-border); border-radius:16px; padding:1rem; display:flex; align-items:center; gap:1rem; box-shadow: var(--shadow-sm); transition: background .3s, box-shadow .3s; }
.service-card:hover { box-shadow: var(--shadow-md); }
.svc-icon { width:48px; height:48px; border-radius:14px; }
.svc-info { display:flex; flex-direction:column; }
.svc-name { font-size:.9rem; font-weight:600; color: var(--color-text); }
.svc-status { font-size:.7rem; color: var(--color-success); }
</style>
