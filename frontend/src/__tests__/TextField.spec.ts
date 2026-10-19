import { describe, expect, it } from 'vitest'
import { mount } from '@vue/test-utils'
import TextField from '@/components/TextField.vue'
import type { InputMask } from '@/utils/masks'
import { pairMask } from './testMasks'

function mountField(props: Record<string, unknown> = {}) {
  return mount(TextField, { props: { id: 'campo', label: 'Campo', modelValue: '', ...props } })
}

async function type(wrapper: ReturnType<typeof mountField>, value: string) {
  const input = wrapper.get('input')
  input.element.value = value
  input.element.setSelectionRange(value.length, value.length)
  await input.trigger('input')
  return input
}

describe('TextField com máscara', () => {
  it('ignora letras: o elemento volta ao valor anterior e nada é emitido', async () => {
    const wrapper = mountField({ mask: pairMask, modelValue: '12' })

    const input = await type(wrapper, '12a')

    expect(input.element.value).toBe('12')
    expect(wrapper.emitted('update:modelValue')).toBeUndefined()
  })

  it('formata o valor digitado e emite o texto formatado', async () => {
    const wrapper = mountField({ mask: pairMask })

    const input = await type(wrapper, '1234')

    expect(input.element.value).toBe('12-34')
    expect(input.element.selectionStart).toBe(5)
    expect(wrapper.emitted('update:modelValue')).toEqual([['12-34']])
  })

  it('não repassa maxlength, pois o limite é o da máscara', () => {
    const wrapper = mountField({ mask: pairMask, maxlength: 3 })

    expect(wrapper.get('input').attributes('maxlength')).toBeUndefined()
  })
})

describe('TextField sem máscara', () => {
  it('emite o texto digitado e repassa maxlength, como antes', async () => {
    const wrapper = mountField({ maxlength: 10 })

    await wrapper.get('input').setValue('Ana')

    expect(wrapper.emitted('update:modelValue')).toEqual([['Ana']])
    expect(wrapper.get('input').attributes('maxlength')).toBe('10')
  })
})

describe('TextField: acessibilidade', () => {
  it.each<[string, InputMask | undefined]>([
    ['com máscara', pairMask],
    ['sem máscara', undefined],
  ])('%s, associa o erro ao campo', (_, mask) => {
    const wrapper = mountField({ mask, error: 'é obrigatório', required: true })
    const input = wrapper.get('input')

    expect(input.attributes('aria-invalid')).toBe('true')
    expect(input.attributes('aria-required')).toBe('true')
    expect(input.attributes('aria-describedby')).toBe('campo-error')
    expect(wrapper.get('#campo-error').text()).toBe('é obrigatório')
    expect(wrapper.get('label').attributes('for')).toBe('campo')
  })
})
