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

<style scoped>
.nav-bar {
  display: flex;
  align-items: center;
  gap: 2rem;
  padding: 0.75rem 1.25rem;
  background: var(--color-surface);
  border-bottom: 1px solid var(--color-border);
}
.nav-brand { font-weight: 600; font-size: 1.1rem; }
.nav-items {
  list-style: none;
  display: flex;
  gap: 0.5rem;
  margin: 0;
  padding: 0;
}
.nav-btn {
  background: transparent;
  border: 1px solid transparent;
  border-radius: 6px;
  padding: 0.5rem 0.9rem;
  font: inherit;
  display: flex;
  align-items: center;
  gap: 0.4rem;
  cursor: pointer;
  color: var(--color-text);
  transition: background .15s, border-color .15s;
}
.nav-btn:hover, .nav-btn:focus-visible {
  background: var(--color-hover);
  border-color: var(--color-border);
  outline: none;
}
.nav-btn[aria-current='page'] {
  background: var(--color-accent-soft);
  border-color: var(--color-accent);
  color: var(--color-accent-contrast);
}
@media (max-width: 720px) {
  .nav-bar { flex-wrap: wrap; }
  .nav-items { flex-wrap: wrap; }
}
</style>
