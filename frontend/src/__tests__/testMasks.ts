import { onlyDigits, type InputMask } from '@/utils/masks'

/** Máscaras mínimas para testar o motor genérico sem depender das máscaras reais. */

/** "1234" → "12-34" */
export const pairMask: InputMask = {
  anchor: 'start',
  digits: (text) => onlyDigits(text).slice(0, 4),
  format: (digits) => (digits.length > 2 ? `${digits.slice(0, 2)}-${digits.slice(2)}` : digits),
}

/** "5" → "0,05"; "1234" → "12,34" */
export const centsMask: InputMask = {
  anchor: 'end',
  digits: (text) => onlyDigits(text).replace(/^0+/, '').slice(0, 4),
  format: (digits) => {
    if (!digits) {
      return ''
    }
    const padded = digits.padStart(3, '0')
    return `${padded.slice(0, -2)},${padded.slice(-2)}`
  },
}

/** "12" → "+9 12" */
export const prefixedMask: InputMask = {
  anchor: 'start',
  prefix: '+9 ',
  digits: (text) => {
    const digits = onlyDigits(text)
    return (text.startsWith('+9') ? digits.slice(1) : digits).slice(0, 3)
  },
  format: (digits) => (digits ? `+9 ${digits}` : ''),
}
