import { http } from './http'
import type { CrudService } from '@/types/api'
import type { Status, StudentRequest, StudentResponse } from '@/types/person'

const RESOURCE = '/students'

export const studentService: CrudService<StudentRequest, StudentResponse> = {
  async list(status?: Status) {
    const { data } = await http.get<StudentResponse[]>(RESOURCE, {
      params: status ? { status } : {},
    })
    return data
  },
  async get(id: number) {
    const { data } = await http.get<StudentResponse>(`${RESOURCE}/${id}`)
    return data
  },
  async create(request: StudentRequest) {
    const { data } = await http.post<StudentResponse>(RESOURCE, request)
    return data
  },
  async update(id: number, request: StudentRequest) {
    const { data } = await http.put<StudentResponse>(`${RESOURCE}/${id}`, request)
    return data
  },
  async remove(id: number) {
    await http.delete(`${RESOURCE}/${id}`)
  },
}
