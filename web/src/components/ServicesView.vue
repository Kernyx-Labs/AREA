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
<style scoped src="../assets/ServicesView.css"></style>
