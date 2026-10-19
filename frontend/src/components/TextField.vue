<script setup lang="ts">
const model = defineModel<string>({ required: true })

defineProps<{
  id: string
  label: string
  error?: string
  type?: 'text' | 'email' | 'tel' | 'url'
  required?: boolean
  maxlength?: number
  placeholder?: string
  prefix?: string
  inputmode?: 'text' | 'decimal' | 'email' | 'tel' | 'url'
  autocomplete?: string
}>()
</script>

<template>
  <div class="field">
    <label :for="id" class="field__label">
      {{ label }}<span v-if="required" class="field__required" aria-hidden="true">*</span>
    </label>
    <div class="field__control">
      <span v-if="prefix" class="field__prefix" aria-hidden="true">{{ prefix }}</span>
      <input
        :id="id"
        v-model="model"
        class="field__input"
        :type="type ?? 'text'"
        :maxlength="maxlength"
        :placeholder="placeholder"
        :inputmode="inputmode"
        :autocomplete="autocomplete ?? 'off'"
        :aria-required="required ? 'true' : undefined"
        :aria-invalid="error ? 'true' : undefined"
        :aria-describedby="error ? `${id}-error` : undefined"
      />
    </div>
    <p v-if="error" :id="`${id}-error`" class="field__error">{{ error }}</p>
  </div>
</template>
