<script setup lang="ts">
import { ref, useId, watch } from 'vue'

const props = defineProps<{
  open: boolean
  title: string
  message: string
  confirmLabel: string
  /** "danger" = confirmação reforçada para exclusão definitiva (FR-023a) */
  variant?: 'default' | 'danger'
}>()
const emit = defineEmits<{ confirm: []; cancel: [] }>()

const dialog = ref<HTMLDialogElement | null>(null)
const titleId = useId()
let opener: HTMLElement | null = null

watch(
  () => props.open,
  (open) => {
    const element = dialog.value
    if (!element) {
      return
    }
    if (open && !element.open) {
      opener = document.activeElement instanceof HTMLElement ? document.activeElement : null
      element.showModal()
    } else if (!open && element.open) {
      element.close()
    }
  },
)

/** Ao fechar, devolve o foco ao controle que abriu o diálogo (FR-025a). */
function restoreFocus(): void {
  if (opener?.isConnected) {
    opener.focus()
  }
  opener = null
}

/** Esc: o estado de abertura é controlado pelo componente pai. */
function onNativeCancel(event: Event): void {
  event.preventDefault()
  emit('cancel')
}
</script>

<template>
  <dialog
    ref="dialog"
    class="dialog"
    :aria-labelledby="titleId"
    @cancel="onNativeCancel"
    @close="restoreFocus"
  >
    <h2 :id="titleId" class="dialog__title">{{ title }}</h2>
    <p>{{ message }}</p>
    <p v-if="variant === 'danger'" class="dialog__warning">
      Esta ação é definitiva e irreversível.
    </p>
    <div class="dialog__actions">
      <button type="button" class="btn btn--secondary" autofocus @click="emit('cancel')">
        Cancelar
      </button>
      <button
        type="button"
        class="btn"
        :class="variant === 'danger' ? 'btn--danger' : 'btn--primary'"
        @click="emit('confirm')"
      >
        {{ confirmLabel }}
      </button>
    </div>
  </dialog>
</template>

<style scoped>
.dialog {
  width: min(460px, calc(100vw - 2rem));
  border: none;
  border-radius: var(--radius);
  padding: var(--space-6);
  box-shadow: var(--shadow);
}

.dialog::backdrop {
  background: rgb(15 23 42 / 45%);
}

.dialog__title {
  font-size: 1.2rem;
  margin-bottom: var(--space-2);
}

.dialog__warning {
  color: var(--color-danger);
  font-weight: 600;
}

.dialog__actions {
  display: flex;
  flex-wrap: wrap;
  justify-content: flex-end;
  gap: var(--space-3);
  margin-top: var(--space-4);
}
</style>
