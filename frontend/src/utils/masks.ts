/**
 * Máscaras de entrada só de dígitos e o tratamento do evento de digitação
 * (specs/003-form-input-masks/contracts/input-masks.md).
 */
export interface InputMask {
  /** Dígitos significativos de qualquer texto (digitado, colado ou formatado), já no limite. */
  digits(text: string): string
  /** Texto exibido a partir dos dígitos; sem dígitos, devolve ''. */
  format(digits: string): string
  /** 'start' ancora o cursor pelos dígitos antes dele; 'end', pelos dígitos depois dele. */
  anchor: 'start' | 'end'
  /** Literal fixo no início do valor (ex.: '+55 '), protegido contra edição. */
  prefix?: string
}

/** Estado do campo: `previous` é o valor formatado antes da ação; `value`/`caret`, depois dela. */
export interface MaskInputEvent {
  previous: string
  value: string
  caret: number
  inputType?: string
}

export interface MaskedValue {
  value: string
  caret: number
}

const DELETE_BACKWARD = 'deleteContentBackward'
const DELETE_FORWARD = 'deleteContentForward'
const NOT_FOUND = -1

export function onlyDigits(text: string): string {
  return text.replace(/\D/g, '')
}

function isDigit(char: string): boolean {
  return char >= '0' && char <= '9'
}

export function applyMask(mask: InputMask, text: string): string {
  return mask.format(mask.digits(text))
}

/** Aplica a máscara ao valor bruto de um evento de digitação e calcula o novo cursor. */
export function maskInput(mask: InputMask, event: MaskInputEvent): MaskedValue {
  const prefix = mask.prefix ?? ''
  if (prefix && editsPrefix(prefix, event.previous, event.value)) {
    return { value: event.previous, caret: prefix.length }
  }
  const { value, caret } = deletesOnlyLiteral(mask, event)
    ? removeAdjacentDigit(prefix, event)
    : event
  const formatted = applyMask(mask, value)
  if (!formatted) {
    return { value: '', caret: 0 }
  }
  return { value: formatted, caret: caretAfterFormat(mask, prefix, value, caret, formatted) }
}

/** Edição que começa dentro do prefixo sem substituir o conteúdo desde o início. */
function editsPrefix(prefix: string, previous: string, value: string): boolean {
  if (!previous.startsWith(prefix)) {
    return false
  }
  const start = commonPrefixLength(previous, value)
  const suffixLimit = Math.min(previous.length, value.length) - start
  const end = previous.length - commonSuffixLength(previous, value, suffixLimit)
  const replacesFromStart = start === 0 && end >= prefix.length
  return start < prefix.length && !replacesFromStart
}

function commonPrefixLength(a: string, b: string): number {
  let length = 0
  while (length < a.length && length < b.length && a[length] === b[length]) {
    length++
  }
  return length
}

function commonSuffixLength(a: string, b: string, limit: number): number {
  let length = 0
  while (length < limit && a[a.length - 1 - length] === b[b.length - 1 - length]) {
    length++
  }
  return length
}

/** Backspace/Delete que apagou só um literal da máscara (parêntese, espaço, hífen, ponto...). */
function deletesOnlyLiteral(mask: InputMask, event: MaskInputEvent): boolean {
  const { previous, value, inputType } = event
  const isDelete = inputType === DELETE_BACKWARD || inputType === DELETE_FORWARD
  return (
    isDelete && value !== previous && mask.digits(value).length === mask.digits(previous).length
  )
}

/** Remove o dígito vizinho ao cursor, para o campo não "travar" recolocando o literal. */
function removeAdjacentDigit(prefix: string, event: MaskInputEvent): MaskedValue {
  const { value, caret, inputType } = event
  const minIndex = prefix && value.startsWith(prefix) ? prefix.length : 0
  const index =
    inputType === DELETE_BACKWARD
      ? lastDigitIndex(value, minIndex, caret)
      : firstDigitIndex(value, Math.max(caret, minIndex))
  if (index === NOT_FOUND) {
    return { value, caret }
  }
  return {
    value: value.slice(0, index) + value.slice(index + 1),
    caret: inputType === DELETE_BACKWARD ? index : caret,
  }
}

function lastDigitIndex(text: string, from: number, to: number): number {
  for (let index = to - 1; index >= from; index--) {
    if (isDigit(text.charAt(index))) {
      return index
    }
  }
  return NOT_FOUND
}

function firstDigitIndex(text: string, from: number): number {
  for (let index = from; index < text.length; index++) {
    if (isDigit(text.charAt(index))) {
      return index
    }
  }
  return NOT_FOUND
}

/** Conta dígitos brutos (não `mask.digits`, que na moeda descarta zeros à esquerda). */
function caretAfterFormat(
  mask: InputMask,
  prefix: string,
  raw: string,
  caret: number,
  formatted: string,
): number {
  if (mask.anchor === 'end') {
    return positionBeforeLastDigits(formatted, onlyDigits(raw.slice(caret)).length)
  }
  const prefixDigits = prefix && raw.startsWith(prefix) ? onlyDigits(prefix).length : 0
  const digitsBefore = Math.max(0, onlyDigits(raw.slice(0, caret)).length - prefixDigits)
  const firstEditable = prefix && formatted.startsWith(prefix) ? prefix.length : 0
  return positionAfterDigits(formatted, digitsBefore, firstEditable)
}

function positionAfterDigits(text: string, count: number, from: number): number {
  if (count === 0) {
    return from
  }
  let seen = 0
  for (let index = from; index < text.length; index++) {
    if (isDigit(text.charAt(index)) && ++seen === count) {
      return index + 1
    }
  }
  return text.length
}

function positionBeforeLastDigits(text: string, count: number): number {
  if (count === 0) {
    return text.length
  }
  let seen = 0
  for (let index = text.length - 1; index >= 0; index--) {
    if (isDigit(text.charAt(index)) && ++seen === count) {
      return index
    }
  }
  return 0
}
