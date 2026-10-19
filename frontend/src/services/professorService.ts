import { http } from './http'
import type { CrudService } from '@/types/api'
import type { Status, ProfessorRequest, ProfessorResponse } from '@/types/person'

const RESOURCE = '/professors'

export const professorService: CrudService<ProfessorRequest, ProfessorResponse> = {
  async list(status?: Status) {
    const { data } = await http.get<ProfessorResponse[]>(RESOURCE, {
      params: status ? { status } : {},
    })
    return data
  },
  async get(id: number) {
    const { data } = await http.get<ProfessorResponse>(`${RESOURCE}/${id}`)
    return data
  },
  async create(request: ProfessorRequest) {
    const { data } = await http.post<ProfessorResponse>(RESOURCE, request)
    return data
  },
  async update(id: number, request: ProfessorRequest) {
    const { data } = await http.put<ProfessorResponse>(`${RESOURCE}/${id}`, request)
    return data
  },
  async remove(id: number) {
    await http.delete(`${RESOURCE}/${id}`)
  },
}
