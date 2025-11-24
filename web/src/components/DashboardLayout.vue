<template>
  <div class="layout-root">
    <header>
      <slot name="nav" />
    </header>
    <main class="layout-main" id="main" tabindex="-1">
      <slot />
    </main>
  </div>
</template>

<script setup></script>

<style scoped>
.layout-root {
  min-height: 100vh;
  display: flex;
  flex-direction: column;
  background: var(--color-bg);
  color: var(--color-text);
}
header { position: sticky; top: 0; z-index: 10; }
.layout-main {
  flex: 1;
  padding: 1.25rem clamp(1rem, 2vw, 2rem);
  display: flex;
  flex-direction: column;
  gap: 1.5rem;
}
</style>

