<script setup lang="ts">
defineProps<{ message: string }>()
</script>

<template>
  <div class="state card">
    <p>{{ message }}</p>
    <slot />
  </div>
</template>
