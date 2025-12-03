<template>
  <nav class="nav-bar" aria-label="Main navigation">
    <div class="nav-brand">AREA Dashboard</div>
    <ul class="nav-items" role="menubar">
      <li v-for="item in items" :key="item.label" role="none">
        <button
          class="nav-btn"
          role="menuitem"
          type="button"
          :aria-current="item.active ? 'page' : undefined"
          @click="activate(item)"
        >
          <span class="nav-icon" v-if="item.icon" aria-hidden="true">{{ item.icon }}</span>
          <span class="nav-label">{{ item.label }}</span>
        </button>
      </li>
    </ul>
  </nav>
</template>

<script setup>
import { reactive } from 'vue'

const props = defineProps({
  items: {
    type: Array,
    default: () => [
      { label: 'Dashboard', icon: '📊', active: true },
      { label: 'Automations', icon: '⚙️', active: false },
      { label: 'Logs', icon: '📜', active: false },
      { label: 'Settings', icon: '⚒️', active: false }
    ]
  }
})

const emit = defineEmits(['change'])
const items = reactive(props.items)

function activate(target) {
  items.forEach(i => { i.active = i === target })
  emit('change', target)
}
</script>

<style scoped src="../assets/NavBar.css"></style>
