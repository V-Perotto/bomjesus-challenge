import { computed, ref, type Ref } from 'vue'
import { useRouter } from 'vue-router'
import type { ApiError, CrudService } from '@/types/api'
import type { PersonForm } from '@/types/person'
import { asApiError } from '@/services/http'
import { focusFirstInvalidField } from '@/utils/focus'
import { emptyAddress } from '@/utils/personForms'
import type { ValidationErrors } from '@/utils/validators'
import { useNotifications } from './useNotifications'

/** Estado e ações do formulário de cadastro/edição, comum a alunos e professores. */
export function usePersonForm<F extends PersonForm, Req, Res>(options: {
  service: CrudService<Req, Res>
  /** id da pessoa em edição; null no cadastro */
  id: number | null
  emptyForm: () => F
  toForm: (response: Res) => F
  toRequest: (form: F) => Req
  validate: (form: F) => ValidationErrors
  listRoute: string
  messages: { created: string; updated: string }
}) {
  const router = useRouter()
  const { notifySuccess, notifyError } = useNotifications()

  const form = ref(options.emptyForm()) as Ref<F>
  const errors = ref<ValidationErrors>({})
  const saving = ref(false)
  const loading = ref(false)
  const loadError = ref<ApiError | null>(null)
  const isEdit = computed(() => options.id !== null)

  function addAddress(): void {
    form.value.addresses.push(emptyAddress())
  }

  function removeAddress(index: number): void {
    if (form.value.addresses.length > 1) {
      form.value.addresses.splice(index, 1)
    }
  }

  async function loadForEdit(): Promise<void> {
    if (options.id === null) {
      return
    }
    loading.value = true
    loadError.value = null
    try {
      form.value = options.toForm(await options.service.get(options.id))
    } catch (caught) {
      const error = asApiError(caught)
      if (error.status === 404 || error.status === 400) {
        notifyError('Registro não encontrado.')
        await router.push(options.listRoute)
      } else {
        loadError.value = error
      }
    } finally {
      loading.value = false
    }
  }

  /** Valida localmente e envia. Em erro, mantém tudo o que foi digitado. */
  async function submit(): Promise<boolean> {
    errors.value = options.validate(form.value)
    if (Object.keys(errors.value).length > 0) {
      focusFirstInvalidField()
      return false
    }
    saving.value = true
    try {
      const request = options.toRequest(form.value)
      if (options.id === null) {
        await options.service.create(request)
      } else {
        await options.service.update(options.id, request)
      }
      notifySuccess(isEdit.value ? options.messages.updated : options.messages.created)
      await router.push(options.listRoute)
      return true
    } catch (caught) {
      const error = asApiError(caught)
      if (error.hasFieldErrors) {
        errors.value = error.fieldErrors
        focusFirstInvalidField()
      }
      notifyError(
        error.hasFieldErrors ? `${error.detail} Corrija os campos destacados.` : error.detail,
      )
      return false
    } finally {
      saving.value = false
    }
  }

  return {
    form,
    errors,
    saving,
    loading,
    loadError,
    isEdit,
    addAddress,
    removeAddress,
    loadForEdit,
    submit,
  }
}
