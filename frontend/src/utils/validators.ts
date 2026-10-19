import type { Address, PersonForm, ProfessorForm, StudentForm } from '@/types/person'
import { normalizeCurrencyText } from './formatters'

/**
 * Validação local com as mesmas regras e mensagens da API (data-model.md). As chaves usam o
 * mesmo caminho dos erros da API (ex.: "addresses[0].city"), para exibição junto ao campo.
 */
export type ValidationErrors = Record<string, string>

const MESSAGES = {
  required: 'é obrigatório',
  size: (min: number, max: number) => `deve ter entre ${min} e ${max} caracteres`,
  maxSize: (max: number) => `deve ter no máximo ${max} caracteres`,
  email: 'deve ser um e-mail válido',
  phone: 'deve estar no formato +55 (00) 00000-0000 ou +55 (00) 0000-0000',
  studentNumber: 'deve ter de 1 a 20 caracteres entre letras, dígitos e hífen',
  photo: 'deve ser uma URL http ou https válida',
  zipCode: 'deve estar no formato 00000-000',
  addresses: 'informe ao menos um endereço',
  invalidValue: 'valor inválido',
  salaryMin: 'deve ser maior que zero',
  salaryMax: 'deve ser no máximo 1.000.000,00',
  salaryDigits: 'deve ter no máximo 2 casas decimais',
} as const

const PHONE = /^\+55 \(\d{2}\) \d{4,5}-\d{4}$/
const EMAIL = /^[^\s@]+@[^\s@]+\.[^\s@]+$/
const STUDENT_NUMBER = /^[A-Za-z0-9-]{1,20}$/
const PHOTO_URL = /^https?:\/\/\S+$/
const ZIP_CODE = /^\d{5}-\d{3}$/
const MAX_SALARY = 1_000_000

type Rule = (value: string) => string | null

const required: Rule = (value) => (value ? null : MESSAGES.required)
const length =
  (min: number, max: number): Rule =>
  (value) =>
    value.length < min || value.length > max ? MESSAGES.size(min, max) : null
const maxLength =
  (max: number): Rule =>
  (value) =>
    value.length > max ? MESSAGES.maxSize(max) : null
const matches =
  (pattern: RegExp, message: string): Rule =>
  (value) =>
    pattern.test(value) ? null : message

/** Aplica as regras ao valor sem espaços nas extremidades; a primeira violação vence. */
function check(errors: ValidationErrors, field: string, rawValue: string, rules: Rule[]): void {
  const value = rawValue.trim()
  for (const rule of rules) {
    const message = rule(value)
    if (message) {
      errors[field] = message
      return
    }
  }
}

function validateAddress(errors: ValidationErrors, address: Address, index: number): void {
  const prefix = `addresses[${index}]`
  check(errors, `${prefix}.street`, address.street, [required, maxLength(150)])
  check(errors, `${prefix}.city`, address.city, [required, maxLength(100)])
  check(errors, `${prefix}.state`, address.state, [required, length(2, 100)])
  check(errors, `${prefix}.zipCode`, address.zipCode, [
    required,
    matches(ZIP_CODE, MESSAGES.zipCode),
  ])
  check(errors, `${prefix}.country`, address.country, [required, length(2, 100)])
}

export function validatePerson(form: PersonForm): ValidationErrors {
  const errors: ValidationErrors = {}
  check(errors, 'name', form.name, [required, length(3, 100)])
  check(errors, 'phoneNumber', form.phoneNumber, [required, matches(PHONE, MESSAGES.phone)])
  check(errors, 'emailAddress', form.emailAddress, [
    required,
    matches(EMAIL, MESSAGES.email),
    maxLength(150),
  ])
  if (form.addresses.length === 0) {
    errors.addresses = MESSAGES.addresses
  }
  form.addresses.forEach((address, index) => validateAddress(errors, address, index))
  return errors
}

export function validateStudentForm(form: StudentForm): ValidationErrors {
  const errors = validatePerson(form)
  check(errors, 'studentNumber', form.studentNumber, [
    required,
    matches(STUDENT_NUMBER, MESSAGES.studentNumber),
  ])
  if (form.photo.trim()) {
    check(errors, 'photo', form.photo, [maxLength(500), matches(PHOTO_URL, MESSAGES.photo)])
  }
  return errors
}

export function validateProfessorForm(form: ProfessorForm): ValidationErrors {
  const errors = validatePerson(form)
  const salaryError = validateSalary(form.salary)
  if (salaryError) {
    errors.salary = salaryError
  }
  return errors
}

function validateSalary(text: string): string | null {
  if (!text.trim()) {
    return MESSAGES.required
  }
  const normalized = normalizeCurrencyText(text)
  if (normalized === null) {
    return MESSAGES.invalidValue
  }
  const value = Number(normalized)
  if (value <= 0) {
    return MESSAGES.salaryMin
  }
  if (value > MAX_SALARY) {
    return MESSAGES.salaryMax
  }
  const decimals = normalized.split('.')[1] ?? ''
  return decimals.length > 2 ? MESSAGES.salaryDigits : null
}
