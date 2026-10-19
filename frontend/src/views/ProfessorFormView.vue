<script setup lang="ts">
import { onMounted } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import ErrorState from '@/components/ErrorState.vue'
import LoadingState from '@/components/LoadingState.vue'
import PersonForm from '@/components/PersonForm.vue'
import TextField from '@/components/TextField.vue'
import { usePersonForm } from '@/composables/usePersonForm'
import { professorService } from '@/services/professorService'
import { emptyProfessorForm, professorFormToRequest, professorToForm } from '@/utils/personForms'
import { validateProfessorForm } from '@/utils/validators'

const route = useRoute()
const router = useRouter()
const LIST_ROUTE = '/professors'

const {
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
} = usePersonForm({
  service: professorService,
  id: route.params.id ? Number(route.params.id) : null,
  emptyForm: emptyProfessorForm,
  toForm: professorToForm,
  toRequest: professorFormToRequest,
  validate: validateProfessorForm,
  listRoute: LIST_ROUTE,
  messages: {
    created: 'Professor cadastrado com sucesso.',
    updated: 'Professor atualizado com sucesso.',
  },
})

onMounted(loadForEdit)
</script>

<template>
  <section class="page" aria-labelledby="professor-form-title">
    <div class="page__header">
      <h1 id="professor-form-title" tabindex="-1">
        {{ isEdit ? 'Editar professor' : 'Novo professor' }}
      </h1>
    </div>

    <LoadingState v-if="loading" />
    <ErrorState v-else-if="loadError" :message="loadError.detail" @retry="loadForEdit" />
    <PersonForm
      v-else
      v-model="form"
      :errors="errors"
      :saving="saving"
      submit-label="Salvar professor"
      @submit="submit"
      @cancel="router.push(LIST_ROUTE)"
      @add-address="addAddress"
      @remove-address="removeAddress"
    >
      <template #specific-fields>
        <TextField
          id="field-salary"
          v-model="form.salary"
          label="Salário"
          prefix="R$"
          inputmode="decimal"
          placeholder="8.500,00"
          :error="errors.salary"
          required
        />
      </template>
    </PersonForm>
  </section>
</template>
