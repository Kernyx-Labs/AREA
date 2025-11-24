<template>
  <section class="dashboard-grid" aria-labelledby="gridTitle">
    <h2 id="gridTitle" class="visually-hidden">Key statistics</h2>
    <StatCard
      v-for="card in cards"
      :key="card.title"
      title="fallback"
      value="fallback"
      :title="card.title || 'N/A'"
      :value="card.value ?? '0'"
      :icon="card.icon"
      :description="card.description"
      :trend="card.trend"
    />
  </section>
</template>

<script setup>
import StatCard from './StatCard.vue'
const { cards } = defineProps({ cards: { type: Array, required: true } })
</script>

<style scoped>
.dashboard-grid {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(min(220px, 100%), 1fr));
  gap: 1rem;
}
@media (min-width: 1200px) {
  .dashboard-grid { gap: 1.25rem; }
}
.visually-hidden {
  position: absolute !important;
  height: 1px; width: 1px;
  overflow: hidden; clip: rect(1px,1px,1px,1px);
  white-space: nowrap; border: 0; padding: 0; margin: -1px;
}
</style>
