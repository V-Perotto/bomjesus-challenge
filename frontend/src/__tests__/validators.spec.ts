import { describe, expect, it } from 'vitest'
import { validateProfessorForm, validateStudentForm } from '@/utils/validators'
import { emptyProfessorForm, emptyStudentForm } from '@/utils/personForms'
import type { ProfessorForm, StudentForm } from '@/types/person'

function validStudent(overrides: Partial<StudentForm> = {}): StudentForm {
  return {
    ...emptyStudentForm(),
    name: 'Ana Souza',
    phoneNumber: '+55 (41) 99999-0001',
    emailAddress: 'ana@escola.com',
    studentNumber: '2026001',
    photo: '',
    addresses: [
      {
        street: 'Rua XV, 100',
        city: 'Curitiba',
        state: 'PR',
        zipCode: '80020-310',
        country: 'Brasil',
      },
    ],
    ...overrides,
  }
}

function validProfessor(overrides: Partial<ProfessorForm> = {}): ProfessorForm {
  const { studentNumber: _studentNumber, photo: _photo, ...base } = validStudent()
  return { ...emptyProfessorForm(), ...base, salary: '8.500,00', ...overrides }
}

const PHONE_MESSAGE = 'deve estar no formato +55 (00) 00000-0000 ou +55 (00) 0000-0000'
const ZIP_CODE_MESSAGE = 'deve estar no formato 00000-000'

describe('validateStudentForm', () => {
  it('aceita um aluno válido sem foto', () => {
    expect(validateStudentForm(validStudent())).toEqual({})
  })

  it('exige campos obrigatórios e trata texto só com espaços como vazio', () => {
    const errors = validateStudentForm(validStudent({ name: '   ', studentNumber: '' }))
    expect(errors.name).toBe('é obrigatório')
    expect(errors.studentNumber).toBe('é obrigatório')
  })

  it('valida tamanho do nome, telefone e e-mail', () => {
    const errors = validateStudentForm(
      validStudent({ name: 'Al', phoneNumber: '123', emailAddress: 'ana@escola' }),
    )
    expect(errors.name).toBe('deve ter entre 3 e 100 caracteres')
    expect(errors.phoneNumber).toBe(PHONE_MESSAGE)
    expect(errors.emailAddress).toBe('deve ser um e-mail válido')
  })

  it.each(['+55 (41) 99999-0001', '+55 (41) 3333-0001'])('aceita o telefone "%s"', (phone) => {
    expect(validateStudentForm(validStudent({ phoneNumber: phone })).phoneNumber).toBeUndefined()
  })

  it.each(['+55 (41) 9999', '41999990001', '+1 (555) 123-4567'])(
    'rejeita o telefone fora do formato "%s"',
    (phone) => {
      expect(validateStudentForm(validStudent({ phoneNumber: phone })).phoneNumber).toBe(
        PHONE_MESSAGE,
      )
    },
  )

  it('valida matrícula e foto', () => {
    const errors = validateStudentForm(validStudent({ studentNumber: 'MAT 01', photo: 'ftp://x' }))
    expect(errors.studentNumber).toContain('letras, dígitos e hífen')
    expect(errors.photo).toBe('deve ser uma URL http ou https válida')
  })

  it('exige ao menos um endereço e usa o caminho da API para campos de endereço', () => {
    expect(validateStudentForm(validStudent({ addresses: [] })).addresses).toBe(
      'informe ao menos um endereço',
    )
    const errors = validateStudentForm(
      validStudent({
        addresses: [{ street: 'Rua A', city: '', state: 'P', zipCode: '1', country: 'Brasil' }],
      }),
    )
    expect(errors['addresses[0].city']).toBe('é obrigatório')
    expect(errors['addresses[0].state']).toBe('deve ter entre 2 e 100 caracteres')
    expect(errors['addresses[0].zipCode']).toBe(ZIP_CODE_MESSAGE)
  })

  it('aceita o CEP no formato 00000-000', () => {
    const errors = validateStudentForm(validStudent())
    expect(errors['addresses[0].zipCode']).toBeUndefined()
  })

  it.each(['8023', '80230010', '02101 A'])('rejeita o CEP fora do formato "%s"', (zipCode) => {
    const [address] = validStudent().addresses
    const errors = validateStudentForm(validStudent({ addresses: [{ ...address!, zipCode }] }))
    expect(errors['addresses[0].zipCode']).toBe(ZIP_CODE_MESSAGE)
  })
})

describe('validateProfessorForm', () => {
  it('aceita salário em formato brasileiro', () => {
    expect(validateProfessorForm(validProfessor())).toEqual({})
  })

  it.each([
    ['', 'é obrigatório'],
    ['abc', 'valor inválido'],
    ['0', 'deve ser maior que zero'],
    ['-1', 'deve ser maior que zero'],
    ['1.000.000,01', 'deve ser no máximo 1.000.000,00'],
    ['10,123', 'deve ter no máximo 2 casas decimais'],
  ])('rejeita salário "%s"', (salary, message) => {
    expect(validateProfessorForm(validProfessor({ salary })).salary).toBe(message)
  })
})
