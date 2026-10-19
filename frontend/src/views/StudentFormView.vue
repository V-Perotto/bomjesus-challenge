<script setup lang="ts">
import { onMounted } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import ErrorState from '@/components/ErrorState.vue'
import LoadingState from '@/components/LoadingState.vue'
import PersonForm from '@/components/PersonForm.vue'
import PhotoAvatar from '@/components/PhotoAvatar.vue'
import TextField from '@/components/TextField.vue'
import { usePersonForm } from '@/composables/usePersonForm'
import { studentService } from '@/services/studentService'
import { emptyStudentForm, studentFormToRequest, studentToForm } from '@/utils/personForms'
import { validateStudentForm } from '@/utils/validators'

const route = useRoute()
const router = useRouter()
const LIST_ROUTE = '/students'

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
  service: studentService,
  id: route.params.id ? Number(route.params.id) : null,
  emptyForm: emptyStudentForm,
  toForm: studentToForm,
  toRequest: studentFormToRequest,
  validate: validateStudentForm,
  listRoute: LIST_ROUTE,
  messages: { created: 'Aluno cadastrado com sucesso.', updated: 'Aluno atualizado com sucesso.' },
})

onMounted(loadForEdit)
</script>

<template>
  <section class="page" aria-labelledby="student-form-title">
    <div class="page__header">
      <h1 id="student-form-title" tabindex="-1">{{ isEdit ? 'Editar aluno' : 'Novo aluno' }}</h1>
    </div>

    <LoadingState v-if="loading" />
    <ErrorState v-else-if="loadError" :message="loadError.detail" @retry="loadForEdit" />
    <PersonForm
      v-else
      v-model="form"
      :errors="errors"
      :saving="saving"
      submit-label="Salvar aluno"
      @submit="submit"
      @cancel="router.push(LIST_ROUTE)"
      @add-address="addAddress"
      @remove-address="removeAddress"
    >
      <template #specific-fields>
        <TextField
          id="field-studentNumber"
          v-model="form.studentNumber"
          label="Matrícula"
          :error="errors.studentNumber"
          :maxlength="20"
          required
        />
        <div class="photo-field">
          <TextField
            id="field-photo"
            v-model="form.photo"
            label="Foto (link da imagem)"
            type="url"
            inputmode="url"
            placeholder="https://..."
            :error="errors.photo"
            :maxlength="500"
          />
          <PhotoAvatar :src="form.photo.trim() || null" alt="Pré-visualização da foto" size="lg" />
        </div>
      </template>
    </PersonForm>
  </section>
</template>

<style scoped>
.photo-field {
  grid-column: 1 / -1;
  display: flex;
  align-items: flex-end;
  gap: var(--space-4);
}

.photo-field > :first-child {
  flex: 1;
}
</style>
