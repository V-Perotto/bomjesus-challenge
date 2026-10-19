<script setup lang="ts" generic="T extends { id: number; name: string; status: Status }">
import type { Status } from '@/types/person'

export interface Column {
  key: string
  label: string
  /** Mantém o conteúdo em uma linha (ex.: telefone, status) */
  nowrap?: boolean
}

defineProps<{ columns: Column[]; items: T[] }>()
const emit = defineEmits<{ edit: [item: T]; deactivate: [item: T]; remove: [item: T] }>()
defineSlots<Record<`cell-${string}`, (props: { item: T }) => unknown>>()

function cellValue(item: T, key: string): string {
  const value = (item as Record<string, unknown>)[key]
  return value === null || value === undefined ? '—' : String(value)
}
</script>

<template>
  <div class="table-wrapper">
    <table class="table table--cards">
      <thead>
        <tr>
          <th v-for="column in columns" :key="column.key" scope="col">{{ column.label }}</th>
          <th scope="col"><span class="visually-hidden">Ações</span></th>
        </tr>
      </thead>
      <tbody>
        <tr v-for="item in items" :key="item.id">
          <td
            v-for="column in columns"
            :key="column.key"
            :data-label="column.label"
            :class="{ 'table__cell--nowrap': column.nowrap }"
          >
            <slot :name="`cell-${column.key}`" :item="item">{{ cellValue(item, column.key) }}</slot>
          </td>
          <td class="table__actions-cell">
            <div class="table__actions">
              <button
                type="button"
                class="btn btn--small btn--secondary"
                :aria-label="`Editar ${item.name}`"
                @click="emit('edit', item)"
              >
                Editar
              </button>
              <button
                v-if="item.status === 'ACTIVE'"
                type="button"
                class="btn btn--small btn--secondary"
                :aria-label="`Desativar ${item.name}`"
                @click="emit('deactivate', item)"
              >
                Desativar
              </button>
              <button
                type="button"
                class="btn btn--small btn--danger-ghost"
                :aria-label="`Excluir ${item.name}`"
                @click="emit('remove', item)"
              >
                Excluir
              </button>
            </div>
          </td>
        </tr>
      </tbody>
    </table>
  </div>
</template>

<style scoped>
.visually-hidden {
  position: absolute;
  width: 1px;
  height: 1px;
  overflow: hidden;
  clip: rect(0 0 0 0);
  white-space: nowrap;
}
</style>
