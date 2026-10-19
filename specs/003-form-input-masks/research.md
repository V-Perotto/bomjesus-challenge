# Research: Máscaras de Entrada nos Formulários de Aluno e Professor

**Feature**: `003-form-input-masks` | **Date**: 2026-10-19 | **Spec**: [spec.md](spec.md)

Não havia itens `NEEDS CLARIFICATION` no Technical Context. As decisões abaixo resolvem as
escolhas técnicas da implementação.

## R1. Biblioteca de máscara × implementação própria

- **Decision**: implementação própria, com funções puras em `frontend/src/utils/masks.ts` e
  aplicação no componente `TextField.vue` já existente. Nenhuma dependência nova.
- **Rationale**: são só três máscaras simples, todas só com dígitos (telefone, CEP e moeda). Com
  funções puras, as regras ficam testáveis com Vitest sem DOM. A constituição (Princípio VIII e
  YAGNI) pede justificativa para dependências novas, e uma biblioteca não traria ganho que a
  justificasse.
- **Alternatives considered**:
  - `maska` (diretiva Vue 3, ativa): resolve telefone e CEP, mas a máscara monetária da direita
    para a esquerda exige configuração própria (`number` + hooks) e o `+55` só depois do
    primeiro dígito exigiria um hook `preProcess`. Acaba com o mesmo volume de código, mais uma
    dependência.
  - `imask`/`vue-imask`: completa, mas grande para três campos e com API própria para o cursor.
  - `vue-the-mask`: feita para Vue 2 e sem manutenção.

## R2. Modelo de máscara

- **Decision**: cada máscara é um objeto `InputMask` com três responsabilidades:
  - `digits(text)`: extrai os dígitos significativos de um texto qualquer (digitado, colado ou
    já formatado) e aplica o limite máximo de dígitos;
  - `format(digits)`: monta o texto exibido a partir dos dígitos (vazio → `''`);
  - `anchor`: `'start'` (telefone, CEP) ou `'end'` (moeda), usado para reposicionar o cursor
    (R4).

  A função `applyMask(mask, text)` devolve `mask.format(mask.digits(text))`, é idempotente e
  serve tanto para a digitação quanto para valores carregados.
- **Rationale**: separar dígitos de formatação deixa cada regra da spec isolada e testável:
  - FR-001, FR-008 e FR-015 (só dígitos) ficam em `digits`;
  - FR-003, FR-009 e FR-016 (formato) ficam em `format`;
  - FR-004 e FR-011 (vazio volta a vazio) são `format('') === ''`.
- **Alternatives considered**: máscara por padrão de caracteres (`'+55 (##) #####-####'`). Não
  expressa a troca de 4 para 5 dígitos do celular nem o preenchimento da direita para a esquerda
  da moeda sem casos especiais.

## R3. Regras específicas de cada máscara

- **Telefone** (`phoneMask`, anchor `start`, até 11 dígitos nacionais):
  - `digits`: remove tudo que não é dígito. Se o texto começa com `+55`, o `55` é o prefixo da
    própria máscara e é descartado. Se não há `+55` e o resultado tem 12 ou 13 dígitos começando
    com `55`, o `55` também é descartado (colagem com código do país, FR-005). Depois, trunca em
    11. Um `55` digitado tecla a tecla no campo vazio é DDD (spec, Edge Cases), porque só tem 2
    dígitos.
  - `format`: `+55 (D` → `+55 (DD) ` → `+55 (DD) NNNN` → `+55 (DD) NNNN-N…`. Com 11 dígitos, o
    hífen fica depois do 5º dígito do número (`99999-0001`); com até 10, depois do 4º
    (`3333-0001`). O hífen só aparece quando existe dígito depois dele.
  - Formato final: `+55 (41) 99999-0001` (19 caracteres) ou `+55 (41) 3333-0001` (18). Os dois
    cabem no `@Size(max = 20)` e no `@Pattern` atuais da API e são os formatos do
    `DemoDataLoader`.
- **CEP** (`zipCodeMask`, anchor `start`, até 8 dígitos): `digits` remove não dígitos e trunca em
  8; `format` insere o hífen depois do 5º dígito quando houver o 6º (`80230` → `80230`,
  `802300` → `80230-0`).
- **Moeda** (`currencyMask`, anchor `end`, até 9 dígitos):
  - `digits` remove não dígitos e **todos** os zeros à esquerda e trunca em 9 (7 inteiros + 2
    decimais, FR-010). Só zeros vira `''`: assim, Backspace repetido sempre esvazia o campo
    (FR-011), sem travar em `0,00`;
  - `format` interpreta os dígitos como centavos e reutiliza o `Intl.NumberFormat('pt-BR')` de
    `formatters.ts` (`DECIMAL_BR`): `5` → `0,05`, `850000` → `8.500,00`.

  A conversão para a API continua com `parseCurrencyBRL`, que já aceita `8.500,00`; `Number` de
  um valor com 2 casas e até 9 dígitos é exato na serialização JSON (FR-013).
- **Limite de dígitos**: em todas as máscaras, os dígitos excedentes são descartados no fim.
  Digitar no fim de um campo completo não altera o valor; colar um texto longo aproveita os
  primeiros dígitos. Na moeda, o limite vale sobre os dígitos já sem zeros à esquerda.

## R4. Cursor e ciclo de digitação

- **Decision**: o `TextField` com `mask` deixa de usar `v-model` direto no `<input>` e trata o
  evento `input`:
  1. lê `value`, `selectionStart` e `inputType` do evento;
  2. chama a função pura `maskInput(mask, { previous, value, caret, inputType })`, em que
     `previous` é o valor formatado antes da ação (o `modelValue` atual), e recebe
     `{ value, caret }`;
  3. escreve `input.value` e chama `setSelectionRange(caret, caret)` no mesmo evento;
  4. emite `update:modelValue` com o valor formatado.

  O cursor é ancorado pela contagem de dígitos (caracteres 0–9 do texto bruto; não se usa
  `mask.digits`, porque a moeda remove zeros à esquerda):
  - **anchor `start`**: conta os dígitos antes do cursor no texto bruto e posiciona o cursor
    depois do mesmo número de dígitos no texto formatado. No telefone, o `+55 ` inicial não conta
    (a máscara expõe `prefix: '+55 '`).
  - **anchor `end`**: conta os dígitos depois do cursor e preserva essa contagem, porque na moeda
    os dígitos "andam" para a esquerda a cada tecla.
- **Rationale**:
  - Reescrever `input.value` dentro do próprio evento `input` acontece antes da pintura, então uma
    letra digitada nunca chega a aparecer (FR-001) e não é preciso tratar `beforeinput`. Isso
    vale também para colagem, autocompletar e ditado.
  - Se só o modelo do Vue fosse atualizado e ele não mudasse (letra ignorada), o Vue não
    re-renderizaria o `<input>` e a letra continuaria visível. Por isso o valor precisa ser
    escrito no elemento.
  - A ancoragem por dígitos atende FR-020 sem depender de diffs de string.
- **Backspace/Delete sobre separador**: se `inputType` é `deleteContentBackward` (ou
  `deleteContentForward`) e a quantidade de dígitos não mudou, o navegador apagou só um
  separador. `maskInput` então remove o dígito imediatamente antes (ou depois) do cursor antes
  de reformatar, para que o campo não fique "travado" (spec, Edge Cases).
- **Prefixo `+55 ` protegido**: comparando `previous` com o valor bruto (maior prefixo e maior
  sufixo comuns), `maskInput` identifica o trecho editado. Se o trecho começa dentro do prefixo,
  há dois casos:
  - a edição substitui o conteúdo inteiro (ex.: selecionar tudo e digitar ou colar): o texto
    novo é tratado como entrada nova;
  - qualquer outra edição: é rejeitada, mantendo `previous` com o cursor no fim do prefixo.

  Sem essa proteção, apagar um `5` do prefixo deixaria `+5 (41) 9…`, e o `5` restante seria lido
  como dígito nacional.
- **`maxlength`**: não é repassado ao `<input>` quando há máscara. Ele cortaria a colagem de
  textos com formatação extra antes do tratamento (ex.: `+55 (41) 9 9999-0001`, 20 caracteres).
  O limite passa a ser o de dígitos de cada máscara.
- **Alternatives considered**:
  - `beforeinput` + `preventDefault` para bloquear não dígitos: não cobre colagem nem
    autocompletar e exigiria o mesmo tratamento no `input`, ou seja, dois caminhos para a mesma
    regra.
  - Formatar só no `blur`: não atende FR-002 (`+55` assim que o usuário começa a digitar) nem a
    formatação "durante a digitação".

## R5. Valores carregados na edição

- **Decision**:
  - `studentToForm`/`professorToForm` continuam copiando `phoneNumber` e `zipCode` sem alterar.
    Valores gravados pela interface já estão no formato canônico e, como `applyMask` é
    idempotente, aparecem iguais.
  - Valores em outro formato aparecem como gravados e são barrados pela validação (R6) até a
    correção (FR-019).
  - O salário continua com `formatDecimalBR`, que produz exatamente a saída de
    `currencyMask.format` (`9200` → `9.200,00`).
- **Rationale**: aplicar a máscara a um telefone internacional gravado pela API
  (`+1 (555) 123-4567`, 11 dígitos) o transformaria silenciosamente em `+55 (15) 55123-4567`, um
  número brasileiro "válido" e errado. Mostrar o valor original e exigir correção é o
  comportamento honesto e não exige código novo.
- **Alternatives considered**: normalizar tudo ao carregar, o que provoca a reinterpretação
  silenciosa descrita acima.

## R6. Validação local

- **Decision**: em `validators.ts`, as regras ficam no formato canônico:
  - `PHONE = /^\+55 \(\d{2}\) \d{4,5}-\d{4}$/`, com a mensagem
    `'deve estar no formato +55 (00) 00000-0000 ou +55 (00) 0000-0000'`;
  - `ZIP_CODE = /^\d{5}-\d{3}$/`, com a mensagem `'deve estar no formato 00000-000'`;
  - o salário mantém `validateSalary` e as mensagens atuais (FR-012). Com a máscara, o ramo
    `salaryDigits` deixa de ser alcançável pela interface, mas continua protegendo valores
    vindos de outras origens.
- **Rationale**: o front-end PODE ser mais restritivo que a API (Princípio VIII). As regras de
  `ValidationMessages.properties` e dos DTOs não mudam, e os erros da API continuam sendo exibidos
  junto ao campo (FR-023).
- **Alternatives considered**: tornar a API também restrita ao formato brasileiro. Está fora do
  escopo (spec, Assumptions), exigiria mudar DTOs, mensagens e testes do back-end e quebraria os
  clientes da API que usam formatos internacionais.

## R7. Teclado móvel e acessibilidade

- **Decision**:
  - o telefone mantém `type="tel"` e `inputmode="tel"` (teclado numérico de telefone e
    autocompletar de telefone do navegador);
  - CEP e salário usam `inputmode="numeric"`, valor que entra na união de tipos aceita por
    `TextField` (FR-021);
  - rótulo, `aria-required`, `aria-invalid` e `aria-describedby` não mudam (FR-022);
  - os placeholders mostram o formato: `+55 (41) 99999-0000`, `00000-000` e `0,00`.
- **Rationale**: `inputmode="decimal"` (usado hoje no salário) mostra vírgula ou ponto, que a
  máscara ignoraria; `numeric` mostra só dígitos.

## R8. Testes

- **Decision**: Vitest + Vue Test Utils, como já configurado:
  - `__tests__/masks.spec.ts` (novo): tabelas de entrada → saída de `applyMask` para as três
    máscaras e casos de cursor e Backspace de `maskInput`;
  - `__tests__/TextField.spec.ts` (novo): ao disparar `input` no componente, o valor do elemento
    e o `update:modelValue` saem formatados, e letras não alteram o valor;
  - `__tests__/validators.spec.ts`: novos formatos e mensagens de telefone e CEP;
  - `__tests__/formatters.spec.ts`: sem mudança de comportamento (regressão).
- **Rationale**: a constituição (Princípio IV) deixa os testes de front-end opcionais e fora do
  gate de cobertura, mas o projeto já testa `validators` e `formatters`, e as máscaras são lógica
  pura com muitos casos de borda.
