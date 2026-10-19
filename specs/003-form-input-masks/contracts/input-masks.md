# UI Contract: Máscaras de Entrada

**Feature**: `003-form-input-masks` | **Date**: 2026-10-19

Contrato dos campos mascarados da interface. Complementa
[002 → frontend-ui.md](../../002-students-professors-crud/contracts/frontend-ui.md), cujas linhas
"Campos comuns", "Campos de professor" e "Endereços" passam a valer com as regras abaixo. O
contrato REST ([002 → openapi.yaml](../../002-students-professors-crud/contracts/openapi.yaml))
não muda.

## 1. Módulo `frontend/src/utils/masks.ts`

Superfície pública:

| Símbolo | Assinatura | Responsabilidade |
|---------|------------|------------------|
| `InputMask` | `{ digits(text: string): string; format(digits: string): string; anchor: 'start' \| 'end'; prefix?: string }` | descreve uma máscara só de dígitos |
| `phoneMask` | `InputMask` | telefone brasileiro com `+55` |
| `zipCodeMask` | `InputMask` | CEP `XXXXX-XXX` |
| `currencyMask` | `InputMask` | moeda `x.xxx,xx`, centavos da direita para a esquerda |
| `applyMask` | `(mask: InputMask, text: string) => string` | `mask.format(mask.digits(text))`; idempotente |
| `maskInput` | `(mask: InputMask, event: { previous: string; value: string; caret: number; inputType?: string }) => { value: string; caret: number }` | aplica a máscara a um evento de digitação (`previous` = valor formatado antes da ação; `value`/`caret` = estado bruto depois dela) e calcula o novo cursor ([research.md](../research.md) R4) |

Invariantes, válidas para toda máscara `m` e todo texto `t`:

- `applyMask(m, applyMask(m, t)) === applyMask(m, t)` (idempotência);
- `applyMask(m, '') === ''` e `applyMask(m, 'abc#*') === ''` (sem dígitos → vazio);
- o resultado contém somente dígitos e os literais da máscara.

## 2. `applyMask`: tabelas de entrada → saída

### Telefone (`phoneMask`)

| Entrada | Saída | Observação |
|---------|-------|------------|
| `''` | `''` | |
| `4` | `+55 (4` | `+55` no primeiro dígito (FR-002) |
| `41` | `+55 (41` | |
| `419` | `+55 (41) 9` | |
| `4133330001` | `+55 (41) 3333-0001` | fixo, 10 dígitos |
| `41999990001` | `+55 (41) 99999-0001` | celular, 11 dígitos |
| `419999900019` | `+55 (41) 99999-0001` | dígito excedente descartado |
| `41a9b9#9` | `+55 (41) 999` | não dígitos ignorados (FR-001) |
| `(41) 99999-0001` | `+55 (41) 99999-0001` | colagem sem código do país |
| `+55 41 99999 0001` | `+55 (41) 99999-0001` | colagem com `+55` (FR-005) |
| `5541999990001` | `+55 (41) 99999-0001` | 13 dígitos iniciando em `55` (FR-005) |
| `554133330001` | `+55 (41) 3333-0001` | 12 dígitos iniciando em `55` (FR-005) |
| `55999990001` | `+55 (55) 99999-0001` | 11 dígitos: `55` é DDD |
| `+55 (` | `''` | sem dígitos nacionais → vazio (FR-004) |

### CEP (`zipCodeMask`)

| Entrada | Saída |
|---------|-------|
| `''` | `''` |
| `80230` | `80230` |
| `802300` | `80230-0` |
| `80230010` | `80230-010` |
| `802300109` | `80230-010` |
| `80.230-010` | `80230-010` |
| `80230 010` | `80230-010` |
| `8a0b2` | `802` |

### Moeda (`currencyMask`)

| Entrada | Saída |
|---------|-------|
| `''` | `''` |
| `5` | `0,05` |
| `50` | `0,50` |
| `500` | `5,00` |
| `000500` | `5,00` |
| `850000` | `8.500,00` |
| `12345678` | `123.456,78` |
| `100000000` | `1.000.000,00` |
| `9999999999` | `9.999.999,99` (limite de 9 dígitos; o excedente é descartado no fim) |
| `R$ 8.500,00` | `8.500,00` |
| `8.500,0` (Backspace no fim de `8.500,00`) | `850,00` |
| `0` | `''` (zeros à esquerda não têm valor; FR-011) |

## 3. `maskInput`: cursor e exclusões

O `|` marca o cursor. `inputType` segue `InputEvent.inputType`.

| Máscara | Valor bruto após a ação do navegador | `inputType` | Resultado |
|---------|--------------------------------------|-------------|-----------|
| phone | `4\|` | `insertText` | `+55 (4\|` |
| phone | `+55 (41) 9x\|` | `insertText` | `+55 (41) 9\|` (letra ignorada) |
| phone | `+55 (41) 998\|999-0001` (inseriu `8` no meio de um número completo) | `insertText` | `+55 (41) 998\|99-9000` (ver nota) |
| phone | `+55 (41) 99999\|0001` (Backspace apagou o hífen) | `deleteContentBackward` | `+55 (41) 9999\|-0001` (apaga o dígito antes do hífen) |
| phone | `+55 (\|` (Backspace apagou o único dígito) | `deleteContentBackward` | `\|` (campo vazio) |
| phone | `+5\|5 (41) 9` (Backspace dentro do prefixo) | `deleteContentBackward` | `+55 \|(41) 9` (valor anterior mantido) |
| phone | `4\|` (tudo selecionado e substituído por `4`) | `insertText` | `+55 (4\|` (tratado como entrada nova) |
| cep | `80230\|010` (Backspace apagou o hífen) | `deleteContentBackward` | `8023\|0-10` (apaga o `0` antes do hífen) |
| moeda | `8.500,0\|` | `deleteContentBackward` | `850,00\|` |
| moeda | `8.500,00a\|` | `insertText` | `8.500,00\|` |
| moeda | `0,0\|` (Backspace no fim de `0,05`) | `deleteContentBackward` | `\|` (campo vazio) |

**Nota (limite de dígitos)**: em todas as máscaras os dígitos excedentes são descartados no
**fim**. Inserir um dígito no meio de um telefone completo desloca os seguintes e descarta o
último, com o cursor logo após o dígito inserido. Digitar no fim de um campo já completo não
altera o valor.

**Prefixo `+55 `**: uma edição que começa dentro do prefixo é rejeitada (valor anterior mantido,
cursor no fim do prefixo), exceto quando substitui o conteúdo inteiro do campo. Nesse caso, o
texto novo é tratado como entrada nova.

**Moeda**: na âncora `end`, a contagem de dígitos à direita do cursor é preservada. Com o cursor
no fim, ele permanece no fim.

## 4. `TextField.vue`: nova prop

| Prop | Tipo | Padrão | Efeito |
|------|------|--------|--------|
| `mask` | `InputMask` | `undefined` | quando definida, o `<input>` usa `:value` + `@input` com `maskInput`, escreve o valor e o cursor no elemento e emite `update:modelValue` com o valor formatado; `maxlength` não é repassado ao `<input>` |
| `inputmode` | acrescenta `'numeric'` à união aceita | — | teclado só de dígitos (CEP, salário) |

Sem `mask`, o componente se comporta exatamente como hoje (`v-model`).

## 5. Uso nos formulários

| Tela / componente | Campo | Máscara | `type` / `inputmode` | Placeholder |
|-------------------|-------|---------|----------------------|-------------|
| `PersonForm.vue` (aluno e professor) | Telefone (`phoneNumber`) | `phoneMask` | `tel` / `tel` | `+55 (41) 99999-0000` |
| `AddressFieldset.vue` (cada endereço) | CEP (`zipCode`) | `zipCodeMask` | `text` / `numeric` | `00000-000` |
| `ProfessorFormView.vue` | Salário (`salary`), prefixo visual `R$` | `currencyMask` | `text` / `numeric` | `0,00` |
