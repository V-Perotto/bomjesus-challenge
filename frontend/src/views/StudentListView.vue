<script setup lang="ts">
import { computed, onMounted, ref } from 'vue'
import { useRouter } from 'vue-router'
import ConfirmDialog from '@/components/ConfirmDialog.vue'
import EmptyState from '@/components/EmptyState.vue'
import ErrorState from '@/components/ErrorState.vue'
import LoadingState from '@/components/LoadingState.vue'
import PersonTable from '@/components/PersonTable.vue'
import PhotoAvatar from '@/components/PhotoAvatar.vue'
import StatusBadge from '@/components/StatusBadge.vue'
import StatusFilter from '@/components/StatusFilter.vue'
import { usePersonList } from '@/composables/usePersonList'
import { studentService } from '@/services/studentService'
import type { StudentResponse } from '@/types/person'
import { studentToRequest } from '@/utils/personForms'

const router = useRouter()
const { items, statusFilter, loading, error, load, deactivate, remove } = usePersonList({
  service: studentService,
  toRequest: studentToRequest,
})

const columns = [
  { key: 'photo', label: 'Foto' },
  { key: 'name', label: 'Nome' },
  { key: 'studentNumber', label: 'Matrícula' },
  { key: 'emailAddress', label: 'E-mail' },
  { key: 'phoneNumber', label: 'Telefone', nowrap: true },
  { key: 'status', label: 'Status', nowrap: true },
]

const pending = ref<{ action: 'deactivate' | 'remove'; student: StudentResponse } | null>(null)
const pendingName = computed(() => pending.value?.student.name ?? '')

async function confirmPending(): Promise<void> {
  const current = pending.value
  pending.value = null
  if (!current) {
    return
  }
  if (current.action === 'deactivate') {
    await deactivate(current.student)
  } else {
    await remove(current.student)
  }
}

onMounted(load)
</script>

<template>
  <section class="page" aria-labelledby="students-title">
    <div class="page__header">
      <h1 id="students-title" tabindex="-1">Alunos</h1>
      <RouterLink to="/students/new" class="btn btn--primary">Novo aluno</RouterLink>
    </div>

    <StatusFilter v-model="statusFilter" />

    <LoadingState v-if="loading" />
    <ErrorState v-else-if="error" :message="error.detail" @retry="load" />
    <EmptyState
      v-else-if="items.length === 0"
      :message="
        statusFilter === 'ALL' ? 'Nenhum aluno cadastrado.' : 'Nenhum aluno com este status.'
      "
    >
      <RouterLink to="/students/new" class="btn btn--secondary">Cadastrar aluno</RouterLink>
    </EmptyState>
    <PersonTable
      v-else
      :columns="columns"
      :items="items"
      @edit="(student) => router.push(`/students/${student.id}/edit`)"
      @deactivate="(student) => (pending = { action: 'deactivate', student })"
      @remove="(student) => (pending = { action: 'remove', student })"
    >
      <template #cell-photo="{ item }">
        <PhotoAvatar :src="item.photo" :alt="`Foto de ${item.name}`" />
      </template>
      <template #cell-status="{ item }">
        <StatusBadge :status="item.status" />
      </template>
    </PersonTable>

    <ConfirmDialog
      :open="pending?.action === 'deactivate'"
      title="Desativar aluno"
      :message="`Desativar o cadastro de ${pendingName}? Ele continua na lista como inativo e pode ser reativado pela edição.`"
      confirm-label="Desativar"
      @confirm="confirmPending"
      @cancel="pending = null"
    />
    <ConfirmDialog
      :open="pending?.action === 'remove'"
      variant="danger"
      title="Excluir aluno"
      :message="`Excluir definitivamente o cadastro de ${pendingName} e todos os seus endereços?`"
      confirm-label="Excluir definitivamente"
      @confirm="confirmPending"
      @cancel="pending = null"
    />
  </section>
</template>
