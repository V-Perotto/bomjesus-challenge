<script setup lang="ts">
import { ref, watch } from 'vue'
import placeholder from '@/assets/avatar-placeholder.svg'

const props = defineProps<{ src: string | null; alt: string; size?: 'sm' | 'lg' }>()

// Volta para a imagem padrão quando o link não carrega (FR-021)
const failed = ref(false)
watch(
  () => props.src,
  () => {
    failed.value = false
  },
)
</script>

<template>
  <img
    class="avatar"
    :class="`avatar--${size ?? 'sm'}`"
    :src="src && !failed ? src : placeholder"
    :alt="alt"
    @error="failed = true"
  />
</template>

<style scoped>
.avatar {
  border-radius: 50%;
  object-fit: cover;
  background: var(--color-neutral-soft);
  flex-shrink: 0;
}

.avatar--sm {
  width: 40px;
  height: 40px;
}

.avatar--lg {
  width: 96px;
  height: 96px;
}
</style>
