<script setup lang="ts">
import type { Address } from '@/types/person'
import { zipCodeMask, type InputMask } from '@/utils/masks'
import TextField from './TextField.vue'

const addresses = defineModel<Address[]>({ required: true })
defineProps<{ errors: Record<string, string> }>()
const emit = defineEmits<{ add: []; remove: [index: number] }>()

interface AddressField {
  key: keyof Address
  label: string
  maxlength: number
  mask?: InputMask
  inputmode?: 'numeric'
  placeholder?: string
}

const fields: AddressField[] = [
  { key: 'street', label: 'Rua e número', maxlength: 150 },
  { key: 'city', label: 'Cidade', maxlength: 100 },
  { key: 'state', label: 'Estado', maxlength: 100 },
  {
    key: 'zipCode',
    label: 'CEP',
    maxlength: 20,
    mask: zipCodeMask,
    inputmode: 'numeric',
    placeholder: '00000-000',
  },
  { key: 'country', label: 'País', maxlength: 100 },
]
</script>

<template>
  <fieldset class="addresses">
    <legend class="addresses__legend">Endereços</legend>
    <p v-if="errors.addresses" class="field__error">{{ errors.addresses }}</p>

    <div v-for="(address, index) in addresses" :key="index" class="addresses__item">
      <div class="addresses__header">
        <h3 class="addresses__title">Endereço {{ index + 1 }}</h3>
        <button
          type="button"
          class="btn btn--small btn--secondary"
          :disabled="addresses.length === 1"
          :aria-label="`Remover endereço ${index + 1}`"
          :title="
            addresses.length === 1 ? 'Toda pessoa precisa de ao menos um endereço' : undefined
          "
          @click="emit('remove', index)"
        >
          Remover
        </button>
      </div>
      <div class="form-grid">
        <TextField
          v-for="field in fields"
          :id="`field-addresses-${index}-${field.key}`"
          :key="field.key"
          v-model="address[field.key]"
          :label="field.label"
          :maxlength="field.maxlength"
          :mask="field.mask"
          :inputmode="field.inputmode"
          :placeholder="field.placeholder"
          :error="errors[`addresses[${index}].${field.key}`]"
          required
        />
      </div>
    </div>

    <div>
      <button type="button" class="btn btn--secondary" @click="emit('add')">
        + Adicionar endereço
      </button>
    </div>
  </fieldset>
</template>

<style scoped>
.addresses {
  display: grid;
  gap: var(--space-4);
  border: none;
  margin: 0;
  padding: 0;
}

.addresses__legend {
  font-size: 1.1rem;
  font-weight: 700;
  padding: 0;
  margin-bottom: var(--space-2);
}

.addresses__item {
  border: 1px solid var(--color-border);
  border-radius: var(--radius-sm);
  padding: var(--space-4);
  display: grid;
  gap: var(--space-3);
}

.addresses__header {
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.addresses__title {
  font-size: 1rem;
}
</style>
