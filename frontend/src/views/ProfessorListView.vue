<script setup lang="ts">
import { computed, onMounted, ref } from 'vue'
import { useRouter } from 'vue-router'
import ConfirmDialog from '@/components/ConfirmDialog.vue'
import EmptyState from '@/components/EmptyState.vue'
import ErrorState from '@/components/ErrorState.vue'
import LoadingState from '@/components/LoadingState.vue'
import PersonTable from '@/components/PersonTable.vue'
import StatusBadge from '@/components/StatusBadge.vue'
import StatusFilter from '@/components/StatusFilter.vue'
import { usePersonList } from '@/composables/usePersonList'
import { professorService } from '@/services/professorService'
import type { ProfessorResponse } from '@/types/person'
import { formatCurrencyBRL } from '@/utils/formatters'
import { professorToRequest } from '@/utils/personForms'

const router = useRouter()
const { items, statusFilter, loading, error, load, deactivate, remove } = usePersonList({
  service: professorService,
  toRequest: professorToRequest,
})

const columns = [
  { key: 'name', label: 'Nome' },
  { key: 'emailAddress', label: 'E-mail' },
  { key: 'phoneNumber', label: 'Telefone', nowrap: true },
  { key: 'salary', label: 'Salário', nowrap: true },
  { key: 'status', label: 'Status', nowrap: true },
]

const pending = ref<{ action: 'deactivate' | 'remove'; professor: ProfessorResponse } | null>(null)
const pendingName = computed(() => pending.value?.professor.name ?? '')

async function confirmPending(): Promise<void> {
  const current = pending.value
  pending.value = null
  if (!current) {
    return
  }
  if (current.action === 'deactivate') {
    await deactivate(current.professor)
  } else {
    await remove(current.professor)
  }
}

onMounted(load)
</script>

<template>
  <section class="page" aria-labelledby="professors-title">
    <div class="page__header">
      <h1 id="professors-title" tabindex="-1">Professores</h1>
      <RouterLink to="/professors/new" class="btn btn--primary">Novo professor</RouterLink>
    </div>

    <StatusFilter v-model="statusFilter" />

    <LoadingState v-if="loading" />
    <ErrorState v-else-if="error" :message="error.detail" @retry="load" />
    <EmptyState
      v-else-if="items.length === 0"
      :message="
        statusFilter === 'ALL'
          ? 'Nenhum professor cadastrado.'
          : 'Nenhum professor com este status.'
      "
    >
      <RouterLink to="/professors/new" class="btn btn--secondary">Cadastrar professor</RouterLink>
    </EmptyState>
    <PersonTable
      v-else
      :columns="columns"
      :items="items"
      @edit="(professor) => router.push(`/professors/${professor.id}/edit`)"
      @deactivate="(professor) => (pending = { action: 'deactivate', professor })"
      @remove="(professor) => (pending = { action: 'remove', professor })"
    >
      <template #cell-salary="{ item }">{{ formatCurrencyBRL(item.salary) }}</template>
      <template #cell-status="{ item }">
        <StatusBadge :status="item.status" />
      </template>
    </PersonTable>

    <ConfirmDialog
      :open="pending?.action === 'deactivate'"
      title="Desativar professor"
      :message="`Desativar o cadastro de ${pendingName}? Ele continua na lista como inativo e pode ser reativado pela edição.`"
      confirm-label="Desativar"
      @confirm="confirmPending"
      @cancel="pending = null"
    />
    <ConfirmDialog
      :open="pending?.action === 'remove'"
      variant="danger"
      title="Excluir professor"
      :message="`Excluir definitivamente o cadastro de ${pendingName} e todos os seus endereços?`"
      confirm-label="Excluir definitivamente"
      @confirm="confirmPending"
      @cancel="pending = null"
    />
  </section>
</template>
