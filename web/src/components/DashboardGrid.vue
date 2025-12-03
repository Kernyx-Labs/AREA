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

<style scoped src="../assets/DashboardGrid.css"></style>
