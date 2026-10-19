<script setup lang="ts">
import { useId } from 'vue'
import type { StatusFilter } from '@/types/person'

const model = defineModel<StatusFilter>({ required: true })
const name = useId()

const options: { value: StatusFilter; label: string }[] = [
  { value: 'ALL', label: 'Todos' },
  { value: 'ACTIVE', label: 'Ativos' },
  { value: 'DISABLE', label: 'Inativos' },
]
</script>

<template>
  <fieldset class="status-filter">
    <legend class="status-filter__legend">Filtrar por status</legend>
    <label v-for="option in options" :key="option.value" class="status-filter__option">
      <input v-model="model" type="radio" :name="name" :value="option.value" />
      <span>{{ option.label }}</span>
    </label>
  </fieldset>
</template>

<style scoped>
.status-filter {
  display: flex;
  flex-wrap: wrap;
  align-items: center;
  gap: var(--space-2);
  border: none;
  margin: 0;
  padding: 0;
}

.status-filter__legend {
  float: left;
  margin-right: var(--space-2);
  font-weight: 600;
  font-size: 0.9rem;
  color: var(--color-text-muted);
}

.status-filter__option {
  position: relative;
}

.status-filter__option input {
  position: absolute;
  opacity: 0;
  inset: 0;
  cursor: pointer;
}

.status-filter__option span {
  display: inline-block;
  padding: var(--space-1) var(--space-3);
  border: 1px solid var(--color-border);
  border-radius: 999px;
  background: var(--color-surface);
  font-size: 0.9rem;
}

.status-filter__option input:checked + span {
  background: var(--color-primary-soft);
  border-color: var(--color-primary);
  color: var(--color-primary);
  font-weight: 600;
}

.status-filter__option input:focus-visible + span {
  outline: 3px solid var(--color-focus);
  outline-offset: 2px;
}
</style>
