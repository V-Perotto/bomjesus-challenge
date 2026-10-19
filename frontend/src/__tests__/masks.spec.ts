import { describe, expect, it } from 'vitest'
import { parseCurrencyBRL } from '@/utils/formatters'
import {
  applyMask,
  currencyMask,
  maskInput,
  phoneMask,
  zipCodeMask,
  type MaskedValue,
} from '@/utils/masks'
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

describe('phoneMask', () => {
  it.each([
    ['', ''],
    ['4', '+55 (4'],
    ['41', '+55 (41'],
    ['419', '+55 (41) 9'],
    ['4133330001', '+55 (41) 3333-0001'],
    ['41999990001', '+55 (41) 99999-0001'],
    ['419999900019', '+55 (41) 99999-0001'],
    ['41a9b9#9', '+55 (41) 999'],
    ['(41) 99999-0001', '+55 (41) 99999-0001'],
    ['+55 41 99999 0001', '+55 (41) 99999-0001'],
    ['5541999990001', '+55 (41) 99999-0001'],
    ['554133330001', '+55 (41) 3333-0001'],
    ['55999990001', '+55 (55) 99999-0001'],
    ['+55 (', ''],
  ])('formata "%s" como "%s"', (text, expected) => {
    expect(applyMask(phoneMask, text)).toBe(expected)
  })

  it('insere +55 ao receber o primeiro dígito', () => {
    const result = maskInput(phoneMask, {
      previous: '',
      value: '4',
      caret: 1,
      inputType: 'insertText',
    })
    expect(withCaret(result)).toBe('+55 (4|')
  })

  it('ignora letra digitada', () => {
    const result = maskInput(phoneMask, {
      previous: '+55 (41) 9',
      value: '+55 (41) 9x',
      caret: 11,
      inputType: 'insertText',
    })
    expect(withCaret(result)).toBe('+55 (41) 9|')
  })

  it('inserção no meio de número completo desloca os dígitos e descarta o último', () => {
    const result = maskInput(phoneMask, {
      previous: '+55 (41) 99999-0001',
      value: '+55 (41) 998999-0001',
      caret: 12,
      inputType: 'insertText',
    })
    expect(withCaret(result)).toBe('+55 (41) 998|99-9000')
  })

  it('Backspace logo após o hífen apaga o dígito anterior', () => {
    const result = maskInput(phoneMask, {
      previous: '+55 (41) 99999-0001',
      value: '+55 (41) 999990001',
      caret: 14,
      inputType: 'deleteContentBackward',
    })
    expect(withCaret(result)).toBe('+55 (41) 9999|-0001')
  })

  it('Backspace dentro do prefixo +55 não altera o valor', () => {
    const result = maskInput(phoneMask, {
      previous: '+55 (41) 9',
      value: '+5 (41) 9',
      caret: 2,
      inputType: 'deleteContentBackward',
    })
    expect(withCaret(result)).toBe('+55 |(41) 9')
  })

  it('substituir todo o conteúdo trata o texto como entrada nova', () => {
    const result = maskInput(phoneMask, {
      previous: '+55 (41) 9',
      value: '4',
      caret: 1,
      inputType: 'insertText',
    })
    expect(withCaret(result)).toBe('+55 (4|')
  })

  it('apagar o único dígito esvazia o campo, sem +55 residual', () => {
    const result = maskInput(phoneMask, {
      previous: '+55 (4',
      value: '+55 (',
      caret: 5,
      inputType: 'deleteContentBackward',
    })
    expect(result).toEqual({ value: '', caret: 0 })
  })
})

describe('currencyMask', () => {
  it.each([
    ['', ''],
    ['5', '0,05'],
    ['50', '0,50'],
    ['500', '5,00'],
    ['000500', '5,00'],
    ['850000', '8.500,00'],
    ['12345678', '123.456,78'],
    ['100000000', '1.000.000,00'],
    ['9999999999', '9.999.999,99'],
    ['R$ 8.500,00', '8.500,00'],
    ['8.500,0', '850,00'],
    ['0', ''],
    ['abc', ''],
  ])('formata "%s" como "%s"', (text, expected) => {
    expect(applyMask(currencyMask, text)).toBe(expected)
  })

  it('Backspace no fim remove o último dígito e mantém o cursor no fim', () => {
    const result = maskInput(currencyMask, {
      previous: '8.500,00',
      value: '8.500,0',
      caret: 7,
      inputType: 'deleteContentBackward',
    })
    expect(withCaret(result)).toBe('850,00|')
  })

  it('ignora letra digitada', () => {
    const result = maskInput(currencyMask, {
      previous: '8.500,00',
      value: '8.500,00a',
      caret: 9,
      inputType: 'insertText',
    })
    expect(withCaret(result)).toBe('8.500,00|')
  })

  it('Backspace repetido esvazia o campo sem travar em 0,00 (FR-011)', () => {
    const states: string[] = []
    let value = '0,05'
    while (value) {
      const raw = value.slice(0, -1)
      value = maskInput(currencyMask, {
        previous: value,
        value: raw,
        caret: raw.length,
        inputType: 'deleteContentBackward',
      }).value
      states.push(value)
    }
    expect(states).toEqual([''])
  })

  it.each([
    ['850000', 8500],
    ['12345678', 123456.78],
    ['100000000', 1000000],
    ['5', 0.05],
  ])('"%s" exibido volta ao mesmo valor numérico %s (FR-013)', (digits, expected) => {
    expect(parseCurrencyBRL(applyMask(currencyMask, digits))).toBe(expected)
  })
})

describe('zipCodeMask', () => {
  it.each([
    ['', ''],
    ['80230', '80230'],
    ['802300', '80230-0'],
    ['80230010', '80230-010'],
    ['802300109', '80230-010'],
    ['80.230-010', '80230-010'],
    ['80230 010', '80230-010'],
    ['8a0b2', '802'],
  ])('formata "%s" como "%s"', (text, expected) => {
    expect(applyMask(zipCodeMask, text)).toBe(expected)
  })

  it('Backspace logo após o hífen apaga o dígito anterior', () => {
    const result = maskInput(zipCodeMask, {
      previous: '80230-010',
      value: '80230010',
      caret: 5,
      inputType: 'deleteContentBackward',
    })
    expect(withCaret(result)).toBe('8023|0-10')
  })

  it('não aceita dígitos além do oitavo digitados no fim', () => {
    const result = maskInput(zipCodeMask, {
      previous: '80230-010',
      value: '80230-0109',
      caret: 10,
      inputType: 'insertText',
    })
    expect(withCaret(result)).toBe('80230-010|')
  })
})
