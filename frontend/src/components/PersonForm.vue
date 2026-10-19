<script setup lang="ts" generic="F extends PersonForm">
import type { PersonForm } from '@/types/person'
import { phoneMask } from '@/utils/masks'
import AddressFieldset from './AddressFieldset.vue'
import TextField from './TextField.vue'

const form = defineModel<F>({ required: true })
defineProps<{ errors: Record<string, string>; saving: boolean; submitLabel: string }>()
const emit = defineEmits<{
  submit: []
  cancel: []
  addAddress: []
  removeAddress: [index: number]
}>()
</script>

<template>
  <form class="card person-form" novalidate @submit.prevent="emit('submit')">
    <p class="person-form__hint">Campos com <span aria-hidden="true">*</span> são obrigatórios.</p>

    <div class="form-grid">
      <TextField
        id="field-name"
        v-model="form.name"
        label="Nome"
        :error="errors.name"
        :maxlength="100"
        autocomplete="name"
        required
      />
      <TextField
        id="field-emailAddress"
        v-model="form.emailAddress"
        label="E-mail"
        type="email"
        inputmode="email"
        :error="errors.emailAddress"
        :maxlength="150"
        required
      />
      <TextField
        id="field-phoneNumber"
        v-model="form.phoneNumber"
        label="Telefone"
        type="tel"
        inputmode="tel"
        placeholder="+55 (41) 99999-0000"
        :mask="phoneMask"
        :error="errors.phoneNumber"
        required
      />
      <div class="field">
        <label for="field-status" class="field__label">Status</label>
        <select id="field-status" v-model="form.status" class="field__input">
          <option value="ACTIVE">Ativo</option>
          <option value="DISABLE">Inativo</option>
        </select>
      </div>
      <slot name="specific-fields" />
    </div>

    <AddressFieldset
      v-model="form.addresses"
      :errors="errors"
      @add="emit('addAddress')"
      @remove="(index) => emit('removeAddress', index)"
    />

    <div class="form-actions">
      <button type="button" class="btn btn--secondary" @click="emit('cancel')">Cancelar</button>
      <button type="submit" class="btn btn--primary" :disabled="saving">
        {{ saving ? 'Salvando…' : submitLabel }}
      </button>
    </div>
  </form>
</template>

<style scoped>
.person-form {
  display: grid;
  gap: var(--space-6);
}

.person-form__hint {
  margin: 0;
  color: var(--color-text-muted);
  font-size: 0.9rem;
}
</style>
