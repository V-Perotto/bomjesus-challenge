<script setup lang="ts">
defineProps<{ message: string }>()
const emit = defineEmits<{ retry: [] }>()
</script>

<template>
  <div class="state state--error card" role="alert">
    <p>{{ message }}</p>
    <button type="button" class="btn btn--secondary" @click="emit('retry')">
      Tentar novamente
    </button>
  </div>
</template>
