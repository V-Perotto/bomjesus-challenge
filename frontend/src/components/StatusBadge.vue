<script setup lang="ts">
import type { Status } from '@/types/person'
import { statusLabel } from '@/utils/formatters'

defineProps<{ status: Status }>()
</script>

<template>
  <span class="badge" :class="status === 'ACTIVE' ? 'badge--active' : 'badge--disable'">
    {{ statusLabel(status) }}
  </span>
</template>
