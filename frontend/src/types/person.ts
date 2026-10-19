/** Tipos espelhados do contrato da API (specs/002-students-professors-crud/contracts/openapi.yaml). */

export type Status = 'ACTIVE' | 'DISABLE'

export type StatusFilter = 'ALL' | Status

export interface Address {
  street: string
  city: string
  state: string
  zipCode: string
  country: string
}

interface PersonRequestBase {
  name: string
  phoneNumber: string
  emailAddress: string
  status?: Status
  addresses: Address[]
}

interface PersonResponseBase {
  id: number
  name: string
  phoneNumber: string
  emailAddress: string
  status: Status
  addresses: Address[]
}

export interface StudentRequest extends PersonRequestBase {
  studentNumber: string
  photo: string | null
}

export interface StudentResponse extends PersonResponseBase {
  studentNumber: string
  photo: string | null
}

export interface ProfessorRequest extends PersonRequestBase {
  salary: number
}

export interface ProfessorResponse extends PersonResponseBase {
  salary: number
}

/** Campos editáveis comuns aos formulários de aluno e professor. */
export interface PersonForm {
  name: string
  phoneNumber: string
  emailAddress: string
  status: Status
  addresses: Address[]
}

export interface StudentForm extends PersonForm {
  studentNumber: string
  photo: string
}

export interface ProfessorForm extends PersonForm {
  /** Texto digitado, aceita formato brasileiro (ex.: "8.500,00"). */
  salary: string
}
