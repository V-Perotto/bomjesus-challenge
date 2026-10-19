import type {
  Address,
  ProfessorForm,
  ProfessorRequest,
  ProfessorResponse,
  StudentForm,
  StudentRequest,
  StudentResponse,
} from '@/types/person'
import { formatDecimalBR, parseCurrencyBRL } from './formatters'

/** Conversões entre respostas da API, formulários e requisições. */

export function emptyAddress(): Address {
  return { street: '', city: '', state: '', zipCode: '', country: 'Brasil' }
}

function copyAddresses(addresses: Address[]): Address[] {
  return addresses.map((address) => ({ ...address }))
}

export function emptyStudentForm(): StudentForm {
  return {
    name: '',
    phoneNumber: '',
    emailAddress: '',
    status: 'ACTIVE',
    studentNumber: '',
    photo: '',
    addresses: [emptyAddress()],
  }
}

export function studentToForm(student: StudentResponse): StudentForm {
  return {
    name: student.name,
    phoneNumber: student.phoneNumber,
    emailAddress: student.emailAddress,
    status: student.status,
    studentNumber: student.studentNumber,
    photo: student.photo ?? '',
    addresses: copyAddresses(student.addresses),
  }
}

export function studentFormToRequest(form: StudentForm): StudentRequest {
  return {
    name: form.name,
    phoneNumber: form.phoneNumber,
    emailAddress: form.emailAddress,
    status: form.status,
    studentNumber: form.studentNumber,
    photo: form.photo.trim() || null,
    addresses: copyAddresses(form.addresses),
  }
}

/** Requisição completa a partir dos dados atuais (usada por "Desativar"). */
export function studentToRequest(student: StudentResponse): StudentRequest {
  return {
    name: student.name,
    phoneNumber: student.phoneNumber,
    emailAddress: student.emailAddress,
    status: student.status,
    studentNumber: student.studentNumber,
    photo: student.photo,
    addresses: copyAddresses(student.addresses),
  }
}

export function emptyProfessorForm(): ProfessorForm {
  return {
    name: '',
    phoneNumber: '',
    emailAddress: '',
    status: 'ACTIVE',
    salary: '',
    addresses: [emptyAddress()],
  }
}

export function professorToForm(professor: ProfessorResponse): ProfessorForm {
  return {
    name: professor.name,
    phoneNumber: professor.phoneNumber,
    emailAddress: professor.emailAddress,
    status: professor.status,
    salary: formatDecimalBR(professor.salary),
    addresses: copyAddresses(professor.addresses),
  }
}

export function professorFormToRequest(form: ProfessorForm): ProfessorRequest {
  return {
    name: form.name,
    phoneNumber: form.phoneNumber,
    emailAddress: form.emailAddress,
    status: form.status,
    salary: parseCurrencyBRL(form.salary) ?? 0,
    addresses: copyAddresses(form.addresses),
  }
}

export function professorToRequest(professor: ProfessorResponse): ProfessorRequest {
  return {
    name: professor.name,
    phoneNumber: professor.phoneNumber,
    emailAddress: professor.emailAddress,
    status: professor.status,
    salary: professor.salary,
    addresses: copyAddresses(professor.addresses),
  }
}
