import { describe, expect, it } from 'vitest'
import { AxiosError, AxiosHeaders, type AxiosResponse } from 'axios'
import { NETWORK_ERROR_MESSAGE, asApiError, toApiError } from '@/services/http'
import { ApiError, type ProblemDetail } from '@/types/api'

function axiosErrorWith(
  status: number,
  data: Partial<ProblemDetail>,
): AxiosError<Partial<ProblemDetail>> {
  const response = {
    status,
    data,
    statusText: '',
    headers: {},
    config: { headers: new AxiosHeaders() },
  } as AxiosResponse<Partial<ProblemDetail>>
  return new AxiosError('falha', 'ERR_BAD_REQUEST', undefined, undefined, response)
}

describe('toApiError', () => {
  it('converte ProblemDetail 400 em erros por campo', () => {
    const error = toApiError(
      axiosErrorWith(400, {
        title: 'Dados inválidos',
        detail: 'A requisição possui 2 campo(s) inválido(s).',
        errors: [
          { field: 'addresses[0].city', message: 'é obrigatório' },
          { field: 'emailAddress', message: 'deve ser um e-mail válido' },
        ],
      }),
    )

    expect(error.status).toBe(400)
    expect(error.hasFieldErrors).toBe(true)
    expect(error.fieldErrors).toEqual({
      'addresses[0].city': 'é obrigatório',
      emailAddress: 'deve ser um e-mail válido',
    })
  })

  it('converte conflito 409 mantendo o campo e o detalhe', () => {
    const error = toApiError(
      axiosErrorWith(409, {
        title: 'Conflito',
        detail: 'Já existe uma pessoa cadastrada com o e-mail ana@escola.com.',
        errors: [{ field: 'emailAddress', message: 'e-mail já cadastrado' }],
      }),
    )

    expect(error.status).toBe(409)
    expect(error.detail).toContain('ana@escola.com')
    expect(error.fieldErrors.emailAddress).toBe('e-mail já cadastrado')
  })

  it('trata 502/503/504 do proxy como falha de comunicação', () => {
    const error = toApiError(axiosErrorWith(502, {}))

    expect(error.isNetworkError).toBe(true)
    expect(error.detail).toBe(NETWORK_ERROR_MESSAGE)
  })

  it('marca falha sem resposta como erro de rede', () => {
    const error = toApiError(new AxiosError('Network Error', 'ERR_NETWORK'))

    expect(error.isNetworkError).toBe(true)
    expect(error.detail).toBe(NETWORK_ERROR_MESSAGE)
  })
})

describe('asApiError', () => {
  it('mantém ApiError e embrulha qualquer outra exceção', () => {
    const original = new ApiError({ status: 404, title: 'x', detail: 'y' })
    expect(asApiError(original)).toBe(original)
    expect(asApiError(new Error('boom')).detail).toBe('Ocorreu um erro inesperado.')
  })
})
