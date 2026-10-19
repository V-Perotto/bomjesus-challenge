import { beforeEach, describe, expect, it, vi } from 'vitest'
import { usePersonForm } from '@/composables/usePersonForm'
import { ApiError, type CrudService } from '@/types/api'
import type { StudentRequest, StudentResponse } from '@/types/person'
import { emptyStudentForm, studentFormToRequest, studentToForm } from '@/utils/personForms'
import { validateStudentForm } from '@/utils/validators'

const push = vi.fn<(to: string) => Promise<void>>()
vi.mock('vue-router', () => ({ useRouter: () => ({ push }) }))

type StudentService = CrudService<StudentRequest, StudentResponse>

function serviceMock(): StudentService {
  return {
    list: vi.fn<StudentService['list']>(),
    get: vi.fn<StudentService['get']>(),
    create: vi.fn<StudentService['create']>(),
    update: vi.fn<StudentService['update']>(),
    remove: vi.fn<StudentService['remove']>(),
  }
}

function setup(service: StudentService, id: number | null = null) {
  return usePersonForm({
    service,
    id,
    emptyForm: emptyStudentForm,
    toForm: studentToForm,
    toRequest: studentFormToRequest,
    validate: validateStudentForm,
    listRoute: '/students',
    messages: { created: 'criado', updated: 'atualizado' },
  })
}

function fillValidStudent(form: ReturnType<typeof setup>['form']['value']): void {
  form.name = 'Ana Souza'
  form.phoneNumber = '+55 (41) 99999-0001'
  form.emailAddress = 'ana@escola.com'
  form.studentNumber = '2026001'
  Object.assign(form.addresses[0]!, {
    street: 'Rua XV, 100',
    city: 'Curitiba',
    state: 'PR',
    zipCode: '80020-310',
  })
}

describe('usePersonForm', () => {
  beforeEach(() => push.mockReset())

  it('não envia nada quando a validação local falha', async () => {
    const service = serviceMock()
    const { submit, errors } = setup(service)

    expect(await submit()).toBe(false)
    expect(service.create).not.toHaveBeenCalled()
    expect(errors.value.name).toBe('é obrigatório')
  })

  it('mostra os erros da API junto aos campos sem perder o que foi digitado', async () => {
    const service = serviceMock()
    vi.mocked(service.create).mockRejectedValue(
      new ApiError({
        status: 409,
        title: 'Conflito',
        detail: 'Já existe uma pessoa cadastrada com o e-mail ana@escola.com.',
        fieldErrors: { emailAddress: 'e-mail já cadastrado' },
      }),
    )
    const { form, submit, errors } = setup(service)
    fillValidStudent(form.value)

    expect(await submit()).toBe(false)
    expect(errors.value.emailAddress).toBe('e-mail já cadastrado')
    expect(form.value.name).toBe('Ana Souza')
    expect(push).not.toHaveBeenCalled()
  })

  it('cria e volta para a lista em caso de sucesso', async () => {
    const service = serviceMock()
    const { form, submit } = setup(service)
    fillValidStudent(form.value)

    expect(await submit()).toBe(true)
    expect(service.create).toHaveBeenCalledWith(expect.objectContaining({ photo: null }))
    expect(push).toHaveBeenCalledWith('/students')
  })

  it('não remove o último endereço', () => {
    const { form, addAddress, removeAddress } = setup(serviceMock())

    removeAddress(0)
    expect(form.value.addresses).toHaveLength(1)

    addAddress()
    removeAddress(0)
    expect(form.value.addresses).toHaveLength(1)
  })

  it('na edição de um registro inexistente volta para a lista', async () => {
    const service = serviceMock()
    vi.mocked(service.get).mockRejectedValue(new ApiError({ status: 404, title: 'x', detail: 'y' }))
    const { loadForEdit, isEdit } = setup(service, 99)

    await loadForEdit()

    expect(isEdit.value).toBe(true)
    expect(push).toHaveBeenCalledWith('/students')
  })
})
