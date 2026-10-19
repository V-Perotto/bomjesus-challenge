import type { Status } from '@/types/person'

const BRL = new Intl.NumberFormat('pt-BR', { style: 'currency', currency: 'BRL' })
const DECIMAL_BR = new Intl.NumberFormat('pt-BR', {
  minimumFractionDigits: 2,
  maximumFractionDigits: 2,
})

/** 8500 → "R$ 8.500,00" */
export function formatCurrencyBRL(value: number): string {
  return BRL.format(value)
}

/** 8500 → "8.500,00" (valor inicial do campo de salário na edição) */
export function formatDecimalBR(value: number): string {
  return DECIMAL_BR.format(value)
}

/**
 * Normaliza um valor monetário digitado para o formato "1234.56".
 * Aceita "8.500,00", "8500,5", "8500.50", "R$ 8.500" e "8500". Retorna null se não for um número.
 */
export function normalizeCurrencyText(text: string): string | null {
  const cleaned = text.replace(/R\$/gi, '').replace(/\s/g, '')
  if (!cleaned) {
    return null
  }
  let normalized: string
  if (cleaned.includes(',')) {
    normalized = cleaned.replace(/\./g, '').replace(',', '.')
  } else if (/^\d{1,3}(\.\d{3})+$/.test(cleaned)) {
    normalized = cleaned.replace(/\./g, '')
  } else {
    normalized = cleaned
  }
  return /^-?\d+(\.\d+)?$/.test(normalized) ? normalized : null
}

export function parseCurrencyBRL(text: string): number | null {
  const normalized = normalizeCurrencyText(text)
  return normalized === null ? null : Number(normalized)
}

const STATUS_LABELS: Record<Status, string> = { ACTIVE: 'Ativo', DISABLE: 'Inativo' }

export function statusLabel(status: Status): string {
  return STATUS_LABELS[status]
}
