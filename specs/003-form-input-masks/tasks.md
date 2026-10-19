---

description: "Task list — Máscaras de Entrada nos Formulários de Aluno e Professor"
---

# Tasks: Máscaras de Entrada nos Formulários de Aluno e Professor

**Input**: Design documents from `specs/003-form-input-masks/`

**Prerequisites**: [plan.md](plan.md), [spec.md](spec.md), [research.md](research.md),
[data-model.md](data-model.md), [contracts/input-masks.md](contracts/input-masks.md),
[quickstart.md](quickstart.md)

**Tests**: INCLUÍDOS. A spec (Assumptions) pede que máscaras e validações sejam cobertas pelos
testes unitários do front-end (Vitest + Vue Test Utils, já configurados). Pela constituição
(Princípio IV), esses testes ficam fora do gate de cobertura. Cada teste vem logo após o código
que ele cobre.

**Organization**: tarefas agrupadas por história de usuário: US1 (telefone, P1), US2 (salário,
P2) e US3 (CEP, P3). O back-end **não** é alterado.

## Format: `[ID] [P?] [Story] Description`

- **[P]**: pode rodar em paralelo (arquivos diferentes, sem dependência de tarefa incompleta)
- **[Story]**: história de usuário da tarefa (US1, US2, US3)

## Path Conventions

- `F/` = `frontend/src/`

As descrições usam esse prefixo; o caminho real é o prefixo expandido. Comandos de front-end
rodam em `frontend/`.

## Mapeamento dos itens pedidos → tarefas

| Item pedido | Tarefas |
|-------------|---------|
| 1. Máscara de telefone com `+55` automático ao começar a digitar | T002–T005, T006–T011 |
| 2. Telefone aceita só dígitos (sem letras ou outros caracteres) | T002, T004, T006–T007 |
| 3. Máscara de salário `x.xxx,xx` | T012–T015 |
| 4. Máscara de CEP `XXXXX-XXX` | T016–T021 |

---

## Phase 1: Setup (Shared Infrastructure)

**Purpose**: confirmar que a base está verde antes de mudar qualquer coisa.

- [X] T001 Em `frontend/`, rodar `npm ci` e depois
  `npm run type-check && npm run lint && npm run test:unit -- --run && npm run build`. Todos
  devem passar **antes** de qualquer alteração. Se algo falhar, registrar a falha e corrigi-la
  primeiro, porque é pré-existente e não faz parte desta feature. Nenhuma dependência nova será
  instalada (research R1).

---

## Phase 2: Foundational (Blocking Prerequisites)

**Purpose**: o motor genérico de máscara (funções puras) e o `TextField` capaz de usá-lo. As três
histórias dependem desta fase.

**⚠️ CRITICAL**: nenhuma história começa antes desta fase estar concluída.

- [X] T002 Criar `F/utils/masks.ts` com a parte genérica do contrato
  ([contracts/input-masks.md](contracts/input-masks.md) §1 e §3; research R2 e R4), **sem** as
  máscaras concretas (que entram em T006, T012 e T016). Comentários curtos em português, no
  estilo de `F/utils/formatters.ts`.

  **Tipos e funções**:
  - `export interface InputMask { digits(text: string): string; format(digits: string): string; anchor: 'start' | 'end'; prefix?: string }`.
    - `digits` extrai os dígitos significativos de qualquer texto (digitado, colado ou
      formatado) e aplica o limite máximo de dígitos, descartando o excedente no **fim**.
    - `format('')` DEVE devolver `''`.
  - `export function onlyDigits(text: string): string`: `text.replace(/\D/g, '')`.
  - `export function applyMask(mask: InputMask, text: string): string`: devolve
    `mask.format(mask.digits(text))`.
  - `export function maskInput(mask: InputMask, event: { previous: string; value: string; caret: number; inputType?: string }): { value: string; caret: number }`.
    `previous` é o valor formatado antes da ação; `value`/`caret` são o texto bruto e o cursor
    depois da ação do navegador.

  **Algoritmo de `maskInput`, nesta ordem**:
  1. **Prefixo protegido** (só se `mask.prefix` existe e `previous.startsWith(mask.prefix)`):
     - calcular `start` = tamanho do maior prefixo comum entre `previous` e `value`;
     - calcular `suffix` = tamanho do maior sufixo comum, limitado a
       `Math.min(previous.length, value.length) - start`;
     - calcular `end = previous.length - suffix` (fim do trecho editado em `previous`);
     - se `start < mask.prefix.length` e **não** (`start === 0 && end >= mask.prefix.length`),
       devolver `{ value: previous, caret: mask.prefix.length }`, ou seja, rejeitar a edição
       dentro do prefixo;
     - no caso `start === 0 && end >= prefix.length` (conteúdo substituído desde o início), o
       texto segue como entrada nova.
  2. **Exclusão sobre separador**:
     - condição: `inputType` é `'deleteContentBackward'` ou `'deleteContentForward'`,
       `value !== previous` e `mask.digits(value).length === mask.digits(previous).length`
       (o navegador apagou só um literal);
     - ação em Backward: achar o índice `i` do último caractere 0–9 em `value.slice(0, caret)`
       que esteja fora do `prefix` (índice `>= prefix.length` quando `value` começa com o
       prefixo), removê-lo e fazer `caret = i`;
     - ação em Forward: achar o primeiro caractere 0–9 com índice `>= caret` e removê-lo,
       mantendo `caret`;
     - se não houver dígito nesse lado, seguir sem remover nada.
  3. **Contagem do cursor**, com `onlyDigits`, contando os caracteres 0–9 do texto bruto, e
     **não** `mask.digits`, porque a moeda remove zeros à esquerda:
     - `anchor: 'start'`: `n` = dígitos em `value.slice(0, caret)`. Se `mask.prefix` existe e
       `value.startsWith(mask.prefix)`, subtrair os dígitos do prefixo (`onlyDigits(prefix).length`),
       com mínimo 0;
     - `anchor: 'end'`: `k` = dígitos em `value.slice(caret)`.
  4. `formatted = applyMask(mask, value)`.
  5. **Posição final**:
     - `anchor: 'start'`: logo após o `n`-ésimo dígito de `formatted`, sem contar os dígitos do
       `prefix`. Com `n === 0`, fica em `prefix?.length ?? 0`. Se `formatted` tem menos de `n`
       dígitos, fica no fim;
     - `anchor: 'end'`: logo antes dos `k` últimos dígitos de `formatted`, ou seja, com `k`
       dígitos à direita do cursor. Se `formatted` tem menos de `k` dígitos, fica em 0;
     - `formatted === ''` → `caret = 0`.
  6. Devolver `{ value: formatted, caret }`.

  **Constantes**: sem números mágicos, com constantes nomeadas para tipos de input
  (`'deleteContentBackward'`, `'deleteContentForward'`).

- [X] T003 [P] Criar `F/__tests__/testMasks.ts` (helper sem testes; fica direto em `__tests__/`,
  sem subpasta, para entrar no `tsconfig.vitest.json` e ficar fora do `tsconfig.app.json`). Ele
  exporta três máscaras de teste, para não depender das máscaras concretas. Depois, criar
  `F/__tests__/masks.spec.ts` com uma seção `describe('maskInput (genérico)')` que as importa:
  - `pairMask`: `digits` = `onlyDigits(t).slice(0, 4)`; `format` insere `-` entre o 2º e o 3º
    dígito (`'1234'` → `'12-34'`); `anchor: 'start'`.
  - `centsMask`: `digits` = `onlyDigits(t).replace(/^0+/, '').slice(0, 4)`; `format` com
    vírgula antes dos 2 últimos dígitos e zeros à esquerda até 3 dígitos (`'5'` → `'0,05'`);
    `anchor: 'end'`.
  - `prefixedMask`: `prefix: '+9 '`; `digits` remove o `9` inicial quando o texto começa com
    `'+9'` e limita a 3; `format(d)` = `d ? '+9 ' + d : ''`; `anchor: 'start'`.

  Casos (AAA, nomes em português descrevendo cenário → resultado):
  - `applyMask` é idempotente;
  - `applyMask(m, 'abc#*')` → `''`;
  - inserção de letra não muda valor nem cursor;
  - inserção no meio mantém o cursor após o dígito inserido;
  - Backspace logo após o `-` de `12-34` (bruto `'1234'`, caret 2, previous `'12-34'`) →
    `{ value: '13-4', caret: 1 }`;
  - âncora `end`: Backspace no fim de `'12,34'` → `'1,23'` com cursor no fim;
  - prefixo: Backspace dentro de `'+9 12'` (bruto `'+ 12'`, caret 1) → valor anterior com caret 3;
  - prefixo: substituir tudo por `'4'` → `{ value: '+9 4', caret: 4 }`;
  - apagar o último dígito de `'+9 1'` (bruto `'+9 '`) → `{ value: '', caret: 0 }`.

  Rodar `npm run test:unit -- --run masks`.

- [X] T004 Alterar `F/components/TextField.vue` para aceitar máscara (contrato §4; research R4 e
  R7):
  - **Props**:
    - nova prop opcional `mask?: InputMask` (import type de `@/utils/masks`);
    - acrescentar `'numeric'` à união de `inputmode`.
  - **Atributos comuns**: manter um `computed` `inputAttrs` com os atributos de hoje (`id`,
    `class`, `type`, `placeholder`, `inputmode`, `autocomplete`, `aria-*`). `maxlength` entra
    **apenas quando não há `mask`**.
  - **Sem `mask`**: `<input v-if="!mask" v-model="model" v-bind="inputAttrs" />`, comportamento
    idêntico ao atual, inclusive IME e composição.
  - **Com `mask`**: `<input v-else :value="model" v-bind="inputAttrs" @input="onMaskedInput" />`.
    O handler deve:
    1. ler `el = event.target as HTMLInputElement`, `el.value`, `el.selectionStart ?? el.value.length`
       e `(event as InputEvent).inputType`;
    2. chamar `maskInput(mask, { previous: model.value, value: el.value, caret, inputType })`;
    3. escrever `el.value = result.value` e `el.setSelectionRange(result.caret, result.caret)`;
    4. atribuir `model.value = result.value`.

    Escrever no elemento é obrigatório: se o modelo não mudar (ex.: letra ignorada), o Vue não
    re-renderiza o `<input>`.
  - Manter rótulo, prefixo visual (`prefix`), `aria-required`, `aria-invalid`,
    `aria-describedby` e a mensagem de erro exatamente como hoje (FR-022).
  - **Atenção a nomes**: a prop `prefix` do `TextField` é só o rótulo visual ao lado do campo
    (ex.: `R$`) e não entra no valor. `mask.prefix` (ex.: `+55 `) faz parte do valor. Os dois não
    se relacionam.

- [X] T005 Criar `F/__tests__/TextField.spec.ts` (Vue Test Utils + jsdom, `mount`) usando a
  `pairMask` importada de `F/__tests__/testMasks.ts` (T003):
  - com `mask` e `modelValue: '12'`, definir `input.element.value = '12a'`, posicionar o cursor
    no fim (`setSelectionRange(3, 3)`) e disparar `input` → o elemento volta a `'12'` e, se
    houver emissão de `update:modelValue`, ela é `'12'`;
  - com `mask`, valor `'1234'` → elemento `'12-34'` e emissão `['12-34']`;
  - com `mask`, o `<input>` **não** tem atributo `maxlength` mesmo com a prop `maxlength`;
  - sem `mask`, `setValue('Ana')` emite `['Ana']` e o `maxlength` é repassado (regressão);
  - `aria-invalid="true"` e `aria-describedby` presentes quando há `error`, nos dois modos.

  Rodar `npm run test:unit -- --run TextField` e `npm run type-check`.

**Checkpoint**: motor de máscara e `TextField` prontos; nenhum formulário usa máscara ainda e a
aplicação se comporta como antes.

---

## Phase 3: User Story 1 - Digitar o telefone com máscara e +55 automático (Priority: P1) 🎯 MVP

**Goal**: nos formulários de aluno e professor, o Telefone aceita só dígitos, insere `+55` no
primeiro dígito e formata `+55 (DD) NNNN-NNNN` / `+55 (DD) NNNNN-NNNN` (FR-001 a FR-007).

**Independent Test**: em "Novo aluno", digitar `41a99999#0001` em Telefone → `+55 (41) 99999-0001`.
Apagar tudo deixa o campo vazio, e salvar com `+55 (41) 9999` mostra a mensagem de formato
(quickstart §4, linhas 1–7 e 16).

### Implementation for User Story 1

- [X] T006 [US1] Em `F/utils/masks.ts`, exportar `phoneMask: InputMask` (research R3; contrato §2
  "Telefone"), com `prefix: '+55 '` e `anchor: 'start'`, e as constantes nomeadas (Princípio III,
  sem números mágicos):
  - `PHONE_COUNTRY_CODE = '55'` e `PHONE_PREFIX = '+55 '`;
  - `PHONE_MAX_DIGITS = 11` (DDD + celular);
  - `PHONE_WITH_COUNTRY_CODE_LENGTHS = [12, 13]` (colagem com código do país);
  - `AREA_CODE_DIGITS = 2`;
  - `MOBILE_HYPHEN_AFTER = 5` e `LANDLINE_HYPHEN_AFTER = 4` (dígitos do número antes do hífen).

  Use essas constantes no lugar dos literais citados abaixo.
  - **`digits(text)`**:
    1. `d = onlyDigits(text)`;
    2. se `text.trimStart().startsWith('+55')`, remover o `'55'` inicial de `d`;
    3. senão, se `d.length` é 12 ou 13 e `d.startsWith('55')`, remover o `'55'` inicial
       (colagem com código do país, FR-005);
    4. devolver `d.slice(0, 11)`.
  - **`format(d)`**:
    - `''` → `''`;
    - 1–2 dígitos → `+55 (` + d;
    - 3+ dígitos → `+55 (DD) ` + resto;
    - o hífen entra só quando há dígito depois dele: com 11 dígitos, após o 5º dígito do
      número (`99999-0001`); com até 10, após o 4º (`3333-0001`).
  - **Exemplos obrigatórios**: `'4'` → `'+55 (4'`, `'419'` → `'+55 (41) 9'`,
    `'4133330001'` → `'+55 (41) 3333-0001'`, `'41999990001'` → `'+55 (41) 99999-0001'`.

- [X] T007 [US1] Em `F/__tests__/masks.spec.ts`, acrescentar `describe('phoneMask')`:
  - `it.each` com **todas** as linhas da tabela "Telefone" do contrato §2;
  - as linhas `phone` do contrato §3 via `maskInput`: primeiro dígito, letra ignorada, inserção
    no meio de número completo, Backspace sobre o hífen, Backspace dentro do prefixo,
    substituição total e apagar o único dígito.

  Rodar `npm run test:unit -- --run masks`.

- [X] T008 [P] [US1] Em `F/utils/validators.ts` (research R6; data-model "Mensagens"):
  - trocar `PHONE` por `/^\+55 \(\d{2}\) \d{4,5}-\d{4}$/`;
  - trocar `MESSAGES.phone` por
    `'deve estar no formato +55 (00) 00000-0000 ou +55 (00) 0000-0000'`;
  - em `validatePerson`, a regra do telefone passa a ser `[required, matches(PHONE, MESSAGES.phone)]`.
    O `maxLength(20)` sai porque o padrão já limita o tamanho a 19 caracteres.

- [X] T009 [US1] Em `F/__tests__/validators.spec.ts`:
  - no teste "valida tamanho do nome, telefone e e-mail", trocar
    `toContain('8 a 15 dígitos')` por `toBe('deve estar no formato +55 (00) 00000-0000 ou +55 (00) 0000-0000')`;
  - acrescentar um `it.each` em que `'+55 (41) 99999-0001'` e `'+55 (41) 3333-0001'` são válidos
    e `'+55 (41) 9999'`, `'41999990001'` e `'+1 (555) 123-4567'` (legado, FR-019) são
    inválidos.

  Rodar `npm run test:unit -- --run validators`.

- [X] T010 [US1] Em `F/components/PersonForm.vue`, no `TextField` `field-phoneNumber`:
  - passar `:mask="phoneMask"` (import de `@/utils/masks`);
  - manter `type="tel"`, `inputmode="tel"` e o placeholder `+55 (41) 99999-0000`;
  - remover `:maxlength="20"`, que seria ignorado com máscara.

  Não alterar `F/utils/personForms.ts`: `studentToForm`/`professorToForm` continuam copiando
  `phoneNumber` sem reformatar (research R5, FR-019).

- [X] T011 [US1] Validar a US1:
  - rodar `npm run type-check && npm run lint && npm run test:unit -- --run`;
  - com `SPRING_PROFILES_ACTIVE=demo ./mvnw spring-boot:run` (em `backend/`) e `npm run dev`,
    executar as linhas 1–7 e 16 (telefone) do roteiro em [quickstart.md](quickstart.md) §4, nos
    formulários de **aluno** e de **professor**;
  - executar a parte de telefone de quickstart §5 (valor legado `+1 (555) 123-4567`).

**Checkpoint**: telefone mascarado e validado nos dois formulários (MVP entregável).

---

## Phase 4: User Story 2 - Digitar o salário do professor com máscara monetária (Priority: P2)

**Goal**: o Salário do professor aceita só dígitos e exibe `x.xxx,xx` com os centavos preenchidos
da direita para a esquerda, até 9 dígitos, e envia à API o mesmo valor exibido (FR-008 a FR-014).

**Independent Test**: em "Novo professor", digitar `850000` em Salário → `8.500,00`. Backspace →
`850,00`. Salvar `8.500,00` → lista mostra `R$ 8.500,00`. Editar "Helena Rocha" → `8.500,00`
(quickstart §4, linhas 11–16).

### Implementation for User Story 2

- [X] T012 [US2] Em `F/utils/masks.ts`, exportar `currencyMask: InputMask` (research R3; contrato §2
  "Moeda"), com `anchor: 'end'`, sem prefixo, e as constantes `CURRENCY_MAX_DIGITS = 9` e
  `CENTS_PER_UNIT = 100`.
  - **`digits(text)`**: `onlyDigits(text).replace(/^0+/, '').slice(0, CURRENCY_MAX_DIGITS)`. Os
    zeros à esquerda saem **todos** antes do limite:
    - `'000500'` → `'500'`;
    - `'0'` e `'00'` → `''`;
    - `''` → `''`.

    **Não** manter um `0` único: com ele, Backspace repetido trava o campo em `0,00` e viola o
    FR-011 (achado C1 do `/speckit-analyze`).
  - **`format(d)`**: `''` → `''`; senão `formatDecimalBR(Number(d) / CENTS_PER_UNIT)`,
    reutilizando `formatDecimalBR` de `F/utils/formatters.ts` (`Intl.NumberFormat('pt-BR')` com
    2 casas). Não duplicar o formatador. Exemplos: `'5'` → `'0,05'`, `'850000'` → `'8.500,00'`.

- [X] T013 [US2] Em `F/__tests__/masks.spec.ts`, acrescentar `describe('currencyMask')`:
  - `it.each` com **todas** as linhas da tabela "Moeda" do contrato §2;
  - as linhas `moeda` do contrato §3 via `maskInput`;
  - FR-011: aplicar Backspace no fim repetidamente, a partir de `'0,05'`, até o campo ficar vazio
    (`''`), sem passar por um estado travado em `'0,00'`;
  - um teste de ida e volta (FR-013): para `['850000', 8500]`, `['12345678', 123456.78]`,
    `['100000000', 1000000]` e `['5', 0.05]`, `parseCurrencyBRL(applyMask(currencyMask, d))`
    é igual ao número esperado (import de `@/utils/formatters`).

  Rodar `npm run test:unit -- --run masks`.

- [X] T014 [US2] Em `F/views/ProfessorFormView.vue`, no `TextField` `field-salary`:
  - passar `:mask="currencyMask"` (import de `@/utils/masks`);
  - trocar `inputmode="decimal"` por `inputmode="numeric"` e o placeholder `8.500,00` por
    `0,00`;
  - manter `prefix="R$"`, `label`, `:error` e `required`.

  Não alterar `professorToForm` (já usa `formatDecimalBR`, igual a `currencyMask.format`,
  FR-014) nem `professorFormToRequest` (já usa `parseCurrencyBRL`, que aceita `8.500,00`,
  FR-013). Não alterar `validateSalary` nem as mensagens de salário em `F/utils/validators.ts`
  (FR-012).

- [X] T015 [US2] Validar a US2:
  - rodar `npm run type-check && npm run lint && npm run test:unit -- --run`. Os casos de
    salário existentes em `F/__tests__/validators.spec.ts` devem continuar passando sem
    alteração;
  - executar as linhas 11–16 (salário) de [quickstart.md](quickstart.md) §4.

**Checkpoint**: salário mascarado; US1 continua funcionando.

---

## Phase 5: User Story 3 - Digitar o CEP com máscara (Priority: P3)

**Goal**: o CEP de cada endereço aceita só dígitos (até 8) e insere o hífen automaticamente no
padrão `XXXXX-XXX` (FR-015 a FR-018).

**Independent Test**: no Endereço 1 de qualquer formulário, digitar `80230010x` em CEP →
`80230-010`. Com dois endereços, cada CEP é formatado de forma independente. Salvar com `8023`
mostra `deve estar no formato 00000-000` (quickstart §4, linhas 8–10).

### Implementation for User Story 3

- [X] T016 [US3] Em `F/utils/masks.ts`, exportar `zipCodeMask: InputMask` (research R3; contrato §2
  "CEP"), com `anchor: 'start'`, sem prefixo, e as constantes `ZIP_CODE_MAX_DIGITS = 8` e
  `ZIP_CODE_FIRST_PART_DIGITS = 5` (dígitos antes do hífen):
  - `digits(text)`: `onlyDigits(text).slice(0, 8)`;
  - `format(d)`: até 5 dígitos → `d`; acima disso → `d.slice(0, 5) + '-' + d.slice(5)`.

- [X] T017 [US3] Em `F/__tests__/masks.spec.ts`, acrescentar `describe('zipCodeMask')`:
  - `it.each` com **todas** as linhas da tabela "CEP" do contrato §2;
  - a linha `cep` do contrato §3 via `maskInput`: Backspace sobre o hífen de `80230-010` →
    `8023|0-10`.

  Rodar `npm run test:unit -- --run masks`.

- [X] T018 [P] [US3] Em `F/utils/validators.ts` (research R6; data-model "Mensagens"):
  - trocar `ZIP_CODE` por `/^\d{5}-\d{3}$/`;
  - trocar `MESSAGES.zipCode` por `'deve estar no formato 00000-000'`.

  A regra de `validateAddress` continua `[required, matches(ZIP_CODE, MESSAGES.zipCode)]`.

- [X] T019 [US3] Em `F/__tests__/validators.spec.ts`:
  - no teste de endereço que usa `zipCode: '1'`, trocar `toContain('3 a 20 caracteres')` por
    `toBe('deve estar no formato 00000-000')`;
  - acrescentar casos em que `'80230-010'` é válido e `'8023'`, `'80230010'` e `'02101 A'`
    (legado, FR-019) são inválidos.

  Rodar `npm run test:unit -- --run validators`.

- [X] T020 [US3] Em `F/components/AddressFieldset.vue`:
  - estender o tipo do array `fields` com as propriedades opcionais `mask?: InputMask`,
    `inputmode?: 'numeric'` e `placeholder?: string`;
  - na entrada `zipCode`, definir `mask: zipCodeMask`, `inputmode: 'numeric'` e
    `placeholder: '00000-000'`;
  - repassar essas três propriedades ao `TextField` do `v-for`
    (`:mask="field.mask" :inputmode="field.inputmode" :placeholder="field.placeholder"`);
  - manter `maxlength` nos demais campos. O `TextField` já o ignora quando há máscara (T004).

  Não alterar `emptyAddress`/`copyAddresses` em `F/utils/personForms.ts` (research R5).

- [X] T021 [US3] Validar a US3:
  - rodar `npm run type-check && npm run lint && npm run test:unit -- --run`;
  - executar as linhas 8–10 de [quickstart.md](quickstart.md) §4, com dois endereços, nos
    formulários de aluno e de professor;
  - executar a parte de CEP de quickstart §5 (`02101 A`).

**Checkpoint**: as três máscaras funcionam de forma independente nos dois formulários.

---

## Phase 6: Polish & Cross-Cutting Concerns

**Purpose**: documentação, gates completos e conferência da constituição.

- [X] T022 [P] Atualizar `README.md`, na lista de recursos da interface (linha "cadastro e edição
  com vários endereços, pré-visualização da foto e salário em reais"). Acrescentar que telefone
  (`+55` automático), CEP (`00000-000`) e salário (`0.000,00`) têm máscara e aceitam só dígitos
  (Princípio VI).
- [X] T023 [P] Em `specs/002-students-professors-crud/contracts/frontend-ui.md`, nas linhas
  "Campos comuns", "Campos de professor" e "Endereços" da tabela do formulário, acrescentar a
  referência "máscaras: ver `specs/003-form-input-masks/contracts/input-masks.md`", para que os
  dois contratos não se contradigam.
- [X] T024 Rodar os gates finais do front-end em `frontend/`:
  `npm run type-check && npm run lint && npm run test:unit -- --run && npm run build`. Tudo deve
  passar sem erros. Conferir que `git diff --stat` não mostra mudanças em `backend/` nem em
  `package.json`/`package-lock.json` (sem dependência nova).
- [X] T025 Rodar `./mvnw clean verify` em `backend/` (regressão, quickstart §2): `BUILD SUCCESS`
  com o gate JaCoCo.
- [X] T026 Revisão de conformidade (constituição, Princípios III e VIII; plan "Constitution
  Check"):
  - nenhum `console.log` em `frontend/src` (`grep -rn "console.log" frontend/src` sem
    resultados novos);
  - nenhum número mágico nas máscaras (limites como constantes nomeadas);
  - nenhuma chamada Axios fora de `F/services/`;
  - `TextField` sem `mask` com comportamento idêntico ao anterior.
- [X] T027 Executar o roteiro completo de [quickstart.md](quickstart.md) §4 (linhas 1–18) e §5, incluindo:
  - a linha 17: teclado numérico, no DevTools em modo celular;
  - a linha 18: rótulo, "obrigatório" e erro anunciados.

  Se possível, executar também com `docker compose up --build` na raiz, para confirmar a imagem
  do front-end.

---

## Dependencies & Execution Order

### Phase Dependencies

- **Setup (Phase 1)**: sem dependências.
- **Foundational (Phase 2)**: depende da Phase 1 e **bloqueia** todas as histórias.
  - T002 vem antes de T003, T004 e T005.
  - T003 e T004 podem rodar em paralelo, pois mexem em arquivos diferentes.
  - T005 depende de T004.
- **User Stories (Phases 3–5)**: dependem só da Phase 2 e são independentes entre si
  funcionalmente. Na prática, compartilham arquivos (ver abaixo), então são feitas em sequência
  P1 → P2 → P3 ou por pessoas diferentes coordenando os commits.
- **Polish (Phase 6)**: depende das histórias desejadas.

### Arquivos compartilhados entre histórias

| Arquivo | Tarefas | Consequência |
|---------|---------|--------------|
| `F/utils/masks.ts` | T002, T006, T012, T016 | edições sequenciais (acréscimos em pontos distintos do arquivo) |
| `F/__tests__/masks.spec.ts` | T003, T007, T013, T017 | idem |
| `F/utils/validators.ts` | T008, T018 | idem |
| `F/__tests__/validators.spec.ts` | T009, T019 | idem |

### Within Each User Story

- Máscara (`masks.ts`) → testes da máscara → validação → testes da validação → uso no componente
  → validação manual.
- US1: T006 → T007; T008 (P, paralela a T006/T007) → T009; T010 depende de T006; T011 por último.
- US2: T012 → T013; T014 depende de T012; T015 por último.
- US3: T016 → T017; T018 (P, paralela a T016/T017) → T019; T020 depende de T016; T021 por
  último.

### Parallel Opportunities

- T003 ∥ T004 (Phase 2).
- T008 ∥ T006 (US1) e T018 ∥ T016 (US3): `validators.ts` × `masks.ts`.
- T022 ∥ T023 (Polish).
- Com duas pessoas: uma faz US1 + US3 (telefone/CEP: `PersonForm`, `AddressFieldset`) e outra faz
  US2 (salário: `ProfessorFormView`), coordenando os acréscimos em `masks.ts`/`masks.spec.ts`.

---

## Parallel Example: User Story 1

```bash
# Depois da Phase 2, em paralelo:
Task: "T006 [US1] phoneMask em frontend/src/utils/masks.ts"
Task: "T008 [P] [US1] PHONE canônico e nova mensagem em frontend/src/utils/validators.ts"
```

## Parallel Example: User Story 3

```bash
Task: "T016 [US3] zipCodeMask em frontend/src/utils/masks.ts"
Task: "T018 [P] [US3] ZIP_CODE canônico e nova mensagem em frontend/src/utils/validators.ts"
```

---

## Implementation Strategy

### MVP First (User Story 1 Only)

1. Phase 1 (T001) → Phase 2 (T002–T005).
2. Phase 3 (T006–T011): telefone mascarado nos dois formulários.
3. **PARAR e VALIDAR** com quickstart §4, linhas 1–7 e 16, e §5. Já é entregável sozinho.

### Incremental Delivery

1. Base + US1 (telefone) → demo.
2. + US2 (salário) → demo.
3. + US3 (CEP) → demo.
4. Phase 6 → README, gates e roteiro completo.

Sugestão de commits (Conventional Commits, em português como no histórico):

- `feat(frontend): adiciona motor de máscaras de entrada` (Phase 2);
- `feat(frontend): adiciona máscara de telefone com +55` (US1);
- `feat(frontend): adiciona máscara monetária no salário` (US2);
- `feat(frontend): adiciona máscara de CEP` (US3);
- `docs: documenta as máscaras de entrada` (Phase 6).

---

## Rastreabilidade: requisitos → tarefas

| Requisito | Tarefas |
|-----------|---------|
| FR-001, FR-008, FR-015 (só dígitos) | T002, T004, T006, T012, T016 |
| FR-002, FR-003, FR-004, FR-005 (telefone) | T006, T007 |
| FR-006 (telefone completo ao salvar) | T008, T009 |
| FR-007, FR-018 (formato gravado) | T010, T020, T011, T021 |
| FR-009, FR-010, FR-011 (moeda) | T012, T013 |
| FR-012 (regras de salário mantidas) | T014, T015 |
| FR-013 (sem perda de centavos) | T013 |
| FR-014, FR-019 (edição) | T010, T014, T020, T009, T019 |
| FR-016, FR-017 (CEP) | T016, T017, T018, T019 |
| FR-020 (cursor) | T002, T003, T007, T013, T017 |
| FR-021 (teclado numérico) | T004, T010, T014, T020, T027 |
| FR-022 (acessibilidade) | T004, T005, T027 |
| FR-023 (erros da API) | T004 (mantém `error`), T027 |

---

## Notes

- [P] = arquivos diferentes, sem dependência de tarefa incompleta.
- O back-end não muda: se alguma tarefa parecer exigir mudança em `backend/`, parar e revisar
  research R6.
- Commit ao fim de cada fase ou história; parar em qualquer checkpoint para validar.
