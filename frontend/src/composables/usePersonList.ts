import { ref, watch, type Ref } from 'vue'
import type { ApiError, CrudService } from '@/types/api'
import type { Status, StatusFilter } from '@/types/person'
import { asApiError } from '@/services/http'
import { focusPageHeading } from '@/utils/focus'
import { useNotifications } from './useNotifications'

interface ListedPerson {
  id: number
  name: string
  status: Status
}

/** Estado e ações da tela de lista, comum a alunos e professores. */
export function usePersonList<Req extends { status?: Status }, Res extends ListedPerson>(options: {
  service: CrudService<Req, Res>
  toRequest: (person: Res) => Req
}) {
  const { service, toRequest } = options
  const items = ref([]) as Ref<Res[]>
  const statusFilter = ref<StatusFilter>('ALL')
  const loading = ref(false)
  const error = ref<ApiError | null>(null)
  const { notifySuccess, notifyError } = useNotifications()

  async function load(): Promise<void> {
    loading.value = true
    error.value = null
    try {
      items.value = await service.list(
        statusFilter.value === 'ALL' ? undefined : statusFilter.value,
      )
    } catch (caught) {
      error.value = asApiError(caught)
    } finally {
      loading.value = false
    }
  }

  /**
   * FR-023: consulta os dados atuais antes do PUT e altera só o status, sem sobrescrever
   * mudanças feitas por outro usuário desde que a lista foi exibida.
   */
  async function deactivate(person: Res): Promise<void> {
    try {
      const current = await service.get(person.id)
      const updated = await service.update(person.id, { ...toRequest(current), status: 'DISABLE' })
      const keepInList = statusFilter.value === 'ALL' || statusFilter.value === updated.status
      items.value = keepInList
        ? items.value.map((item) => (item.id === updated.id ? updated : item))
        : items.value.filter((item) => item.id !== updated.id)
      notifySuccess(`Cadastro de ${person.name} desativado.`)
      focusPageHeading()
    } catch (caught) {
      notifyError(asApiError(caught).detail)
    }
  }

  async function remove(person: Res): Promise<void> {
    try {
      await service.remove(person.id)
      items.value = items.value.filter((item) => item.id !== person.id)
      notifySuccess(`Cadastro de ${person.name} excluído.`)
      focusPageHeading()
    } catch (caught) {
      notifyError(asApiError(caught).detail)
    }
  }

  watch(statusFilter, load)

  return { items, statusFilter, loading, error, load, deactivate, remove }
}
