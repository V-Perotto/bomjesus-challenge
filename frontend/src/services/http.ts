import axios, { type AxiosError } from 'axios'
import { ApiError, type ProblemDetail } from '@/types/api'

export const NETWORK_ERROR_MESSAGE = 'Não foi possível conectar à API.'

const GATEWAY_ERRORS = new Set([502, 503, 504])

/** Única instância Axios do projeto (Princípio VIII): componentes nunca chamam o Axios diretamente. */
export const http = axios.create({
  baseURL: import.meta.env.VITE_API_BASE_URL ?? '/api',
  headers: { Accept: 'application/json' },
})

/** Converte qualquer falha do Axios em {@link ApiError}, com os erros por campo indexados pelo nome. */
export function toApiError(error: AxiosError<Partial<ProblemDetail>>): ApiError {
  // Sem resposta, ou o proxy (Vite/Nginx) avisando que a API está fora do ar
  if (!error.response || GATEWAY_ERRORS.has(error.response.status)) {
    return new ApiError({
      status: 0,
      title: 'Falha de comunicação',
      detail: NETWORK_ERROR_MESSAGE,
      isNetworkError: true,
    })
  }
  const { status, data } = error.response
  const fieldErrors: Record<string, string> = {}
  for (const { field, message } of data?.errors ?? []) {
    fieldErrors[field] = fieldErrors[field] ? `${fieldErrors[field]}; ${message}` : message
  }
  return new ApiError({
    status,
    title: data?.title ?? 'Erro',
    detail: data?.detail ?? 'Ocorreu um erro inesperado.',
    fieldErrors,
  })
}

http.interceptors.response.use(
  (response) => response,
  (error: AxiosError<Partial<ProblemDetail>>) => Promise.reject(toApiError(error)),
)

/** Garante um {@link ApiError} para qualquer exceção capturada pelas telas. */
export function asApiError(error: unknown): ApiError {
  if (error instanceof ApiError) {
    return error
  }
  return new ApiError({ status: 0, title: 'Erro', detail: 'Ocorreu um erro inesperado.' })
}
