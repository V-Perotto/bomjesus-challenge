import { nextTick } from 'vue'

/** Leva o foco ao título da página (FR-025a): após navegar, salvar, desativar ou excluir. */
export function focusPageHeading(): void {
  void nextTick(() => document.querySelector<HTMLElement>('main h1')?.focus())
}

/** Leva o foco ao primeiro campo marcado como inválido. */
export function focusFirstInvalidField(): void {
  void nextTick(() => document.querySelector<HTMLElement>('[aria-invalid="true"]')?.focus())
}
