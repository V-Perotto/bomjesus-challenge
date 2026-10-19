import type { Status } from './person'

export interface FieldError {
  field: string
  message: string
}

/** Erro no formato RFC 9457 devolvido pela API. */
export interface ProblemDetail {
  title: string
  status: number
  detail: string
  instance: string
  timestamp: string
  errors?: FieldError[]
}

/** Erro normalizado consumido pelas telas, venha ele da API ou da rede. */
export class ApiError extends Error {
  readonly status: number
  readonly title: string
  readonly detail: string
  readonly fieldErrors: Record<string, string>
  readonly isNetworkError: boolean

  constructor(options: {
    status: number
    title: string
    detail: string
    fieldErrors?: Record<string, string>
    isNetworkError?: boolean
  }) {
    super(options.detail)
    this.name = 'ApiError'
    this.status = options.status
    this.title = options.title
    this.detail = options.detail
    this.fieldErrors = options.fieldErrors ?? {}
    this.isNetworkError = options.isNetworkError ?? false
  }

  get hasFieldErrors(): boolean {
    return Object.keys(this.fieldErrors).length > 0
  }
}

/** Operações de CRUD oferecidas pelos services de cada recurso. */
export interface CrudService<Req, Res> {
  list(status?: Status): Promise<Res[]>
  get(id: number): Promise<Res>
  create(request: Req): Promise<Res>
  update(id: number, request: Req): Promise<Res>
  remove(id: number): Promise<void>
}
