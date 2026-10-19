import { describe, expect, it } from 'vitest'
import { applyMask, maskInput, type MaskedValue } from '@/utils/masks'
import { centsMask, pairMask, prefixedMask } from './testMasks'

/** Mostra o cursor como "|" para comparar valor e posição de uma vez. */
function withCaret({ value, caret }: MaskedValue): string {
  return `${value.slice(0, caret)}|${value.slice(caret)}`
}

describe('maskInput (genérico)', () => {
  it('applyMask é idempotente', () => {
    const once = applyMask(pairMask, '1a2b3')
    expect(applyMask(pairMask, once)).toBe(once)
  })

  it('applyMask sem dígitos devolve vazio', () => {
    expect(applyMask(pairMask, 'abc#*')).toBe('')
    expect(applyMask(centsMask, 'abc#*')).toBe('')
    expect(applyMask(prefixedMask, 'abc#*')).toBe('')
  })

  it('ignora letra digitada no fim sem mover o cursor', () => {
    const result = maskInput(pairMask, { previous: '12-3', value: '12-3x', caret: 5 })
    expect(withCaret(result)).toBe('12-3|')
  })

  it('ignora letra digitada no meio sem mover o cursor', () => {
    const result = maskInput(pairMask, { previous: '12-34', value: '1x2-34', caret: 2 })
    expect(withCaret(result)).toBe('1|2-34')
  })

  it('mantém o cursor logo após o dígito inserido no meio', () => {
    const result = maskInput(pairMask, {
      previous: '12-4',
      value: '123-4',
      caret: 3,
      inputType: 'insertText',
    })
    expect(withCaret(result)).toBe('12-3|4')
  })

  it('Backspace logo após o hífen apaga o dígito anterior', () => {
    const result = maskInput(pairMask, {
      previous: '12-34',
      value: '1234',
      caret: 2,
      inputType: 'deleteContentBackward',
    })
    expect(result).toEqual({ value: '13-4', caret: 1 })
  })

  it('Delete logo antes do hífen apaga o dígito seguinte', () => {
    const result = maskInput(pairMask, {
      previous: '12-34',
      value: '1234',
      caret: 2,
      inputType: 'deleteContentForward',
    })
    expect(withCaret(result)).toBe('12|-4')
  })

  it('âncora no fim: Backspace no fim mantém o cursor no fim', () => {
    const result = maskInput(centsMask, {
      previous: '12,34',
      value: '12,3',
      caret: 4,
      inputType: 'deleteContentBackward',
    })
    expect(withCaret(result)).toBe('1,23|')
  })

  it('âncora no fim: dígito no fim empurra os demais para a esquerda', () => {
    const result = maskInput(centsMask, { previous: '0,05', value: '0,050', caret: 5 })
    expect(withCaret(result)).toBe('0,50|')
  })

  it('rejeita Backspace dentro do prefixo', () => {
    const result = maskInput(prefixedMask, {
      previous: '+9 12',
      value: '+ 12',
      caret: 1,
      inputType: 'deleteContentBackward',
    })
    expect(result).toEqual({ value: '+9 12', caret: 3 })
  })

  it('trata a substituição de todo o conteúdo como entrada nova', () => {
    const result = maskInput(prefixedMask, {
      previous: '+9 12',
      value: '4',
      caret: 1,
      inputType: 'insertText',
    })
    expect(result).toEqual({ value: '+9 4', caret: 4 })
  })

  it('apagar o último dígito esvazia o campo, sem prefixo residual', () => {
    const result = maskInput(prefixedMask, {
      previous: '+9 1',
      value: '+9 ',
      caret: 3,
      inputType: 'deleteContentBackward',
    })
    expect(result).toEqual({ value: '', caret: 0 })
  })
})
