import { describe, expect, it } from 'vitest'
import {
  formatCurrencyBRL,
  formatDecimalBR,
  parseCurrencyBRL,
  statusLabel,
} from '@/utils/formatters'

const normalizeSpaces = (text: string) => text.replace(/\s/g, ' ')

describe('formatCurrencyBRL', () => {
  it('formata em reais', () => {
    expect(normalizeSpaces(formatCurrencyBRL(8500))).toBe('R$ 8.500,00')
  })
})

describe('formatDecimalBR', () => {
  it('formata com duas casas para o campo de edição', () => {
    expect(formatDecimalBR(9200.5)).toBe('9.200,50')
  })
})

describe('parseCurrencyBRL', () => {
  it.each([
    ['8.500,00', 8500],
    ['8500,5', 8500.5],
    ['8500.50', 8500.5],
    ['R$ 8.500', 8500],
    ['8500', 8500],
  ])('converte "%s"', (text, expected) => {
    expect(parseCurrencyBRL(text)).toBe(expected)
  })

  it('retorna null para texto inválido ou vazio', () => {
    expect(parseCurrencyBRL('abc')).toBeNull()
    expect(parseCurrencyBRL('  ')).toBeNull()
  })
})

describe('statusLabel', () => {
  it('traduz o status', () => {
    expect(statusLabel('ACTIVE')).toBe('Ativo')
    expect(statusLabel('DISABLE')).toBe('Inativo')
  })
})
