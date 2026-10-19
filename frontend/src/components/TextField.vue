<script setup lang="ts">
import { computed, type InputHTMLAttributes } from 'vue'
import { maskInput, type InputMask } from '@/utils/masks'

const model = defineModel<string>({ required: true })

const props = defineProps<{
  id: string
  label: string
  error?: string
  type?: 'text' | 'email' | 'tel' | 'url'
  required?: boolean
  maxlength?: number
  placeholder?: string
  /** Rótulo visual ao lado do campo (ex.: "R$"); não faz parte do valor. */
  prefix?: string
  inputmode?: 'text' | 'decimal' | 'numeric' | 'email' | 'tel' | 'url'
  autocomplete?: string
  /** Máscara aplicada durante a digitação; o limite de tamanho passa a ser o da máscara. */
  mask?: InputMask
}>()

const inputAttrs = computed<InputHTMLAttributes>(() => ({
  id: props.id,
  class: 'field__input',
  type: props.type ?? 'text',
  maxlength: props.mask ? undefined : props.maxlength,
  placeholder: props.placeholder,
  inputmode: props.inputmode,
  autocomplete: props.autocomplete ?? 'off',
  'aria-required': props.required ? 'true' : undefined,
  'aria-invalid': props.error ? 'true' : undefined,
  'aria-describedby': props.error ? `${props.id}-error` : undefined,
}))

/**
 * Reescreve valor e cursor no próprio elemento: se o modelo não mudar (ex.: letra ignorada),
 * o Vue não re-renderiza o <input> e o caractere rejeitado ficaria visível.
 */
function onMaskedInput(event: Event): void {
  if (!props.mask) {
    return
  }
  const input = event.target as HTMLInputElement
  const result = maskInput(props.mask, {
    previous: model.value,
    value: input.value,
    caret: input.selectionStart ?? input.value.length,
    inputType: (event as InputEvent).inputType,
  })
  input.value = result.value
  input.setSelectionRange(result.caret, result.caret)
  model.value = result.value
}
</script>

<template>
  <div class="field">
    <label :for="id" class="field__label">
      {{ label }}<span v-if="required" class="field__required" aria-hidden="true">*</span>
    </label>
    <div class="field__control">
      <span v-if="prefix" class="field__prefix" aria-hidden="true">{{ prefix }}</span>
      <input v-if="mask" :value="model" v-bind="inputAttrs" @input="onMaskedInput" />
      <input v-else v-model="model" v-bind="inputAttrs" />
    </div>
    <p v-if="error" :id="`${id}-error`" class="field__error">{{ error }}</p>
  </div>
</template>
