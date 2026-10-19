<script setup lang="ts">
import { useNotifications } from '@/composables/useNotifications'

const { toasts, dismiss } = useNotifications()
</script>

<template>
  <div class="toasts" aria-live="polite" aria-atomic="false">
    <div v-for="toast in toasts" :key="toast.id" class="toast" :class="`toast--${toast.type}`">
      <span>{{ toast.message }}</span>
      <button
        type="button"
        class="toast__close"
        aria-label="Fechar aviso"
        @click="dismiss(toast.id)"
      >
        ×
      </button>
    </div>
  </div>
</template>

<style scoped>
.toasts {
  position: fixed;
  right: var(--space-4);
  bottom: var(--space-4);
  left: var(--space-4);
  display: grid;
  justify-items: end;
  gap: var(--space-2);
  pointer-events: none;
  z-index: 20;
}

.toast {
  display: flex;
  align-items: center;
  gap: var(--space-3);
  max-width: 420px;
  padding: var(--space-3) var(--space-4);
  border-radius: var(--radius-sm);
  box-shadow: var(--shadow);
  color: #fff;
  pointer-events: auto;
}

.toast--success {
  background: var(--color-success);
}

.toast--error {
  background: var(--color-danger);
}

.toast__close {
  background: none;
  border: none;
  color: inherit;
  font-size: 1.25rem;
  line-height: 1;
  cursor: pointer;
}
</style>
