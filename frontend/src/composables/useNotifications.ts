import { readonly, ref } from 'vue'

export type ToastType = 'success' | 'error'

export interface Toast {
  id: number
  type: ToastType
  message: string
}

const AUTO_DISMISS_MS = 5000

// Estado único compartilhado por toda a aplicação
const toasts = ref<Toast[]>([])
let nextId = 1

export function useNotifications() {
  function dismiss(id: number): void {
    toasts.value = toasts.value.filter((toast) => toast.id !== id)
  }

  function notify(type: ToastType, message: string): void {
    const id = nextId++
    toasts.value.push({ id, type, message })
    setTimeout(() => dismiss(id), AUTO_DISMISS_MS)
  }

  return {
    toasts: readonly(toasts),
    dismiss,
    notifySuccess: (message: string) => notify('success', message),
    notifyError: (message: string) => notify('error', message),
  }
}
