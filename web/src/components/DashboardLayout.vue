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

<style scoped src="../assets/DashboardLayout.css"></style>
