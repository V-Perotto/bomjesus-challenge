# Data Model: Máscaras de Entrada nos Formulários de Aluno e Professor

**Feature**: `003-form-input-masks` | **Date**: 2026-10-19

Esta funcionalidade **não altera** entidades, tabelas, DTOs nem o contrato da API. O modelo de
domínio continua o de [002-students-professors-crud/data-model.md](../002-students-professors-crud/data-model.md).
Abaixo estão os atributos afetados, com o formato produzido pela interface e as regras de
validação local.

## Atributos afetados

| Entidade | Atributo | Tipo na API | Formato produzido pela interface | Regra local (front-end) | Regra da API (inalterada) |
|----------|----------|-------------|----------------------------------|-------------------------|---------------------------|
| `Person` (`Student`, `Professor`) | `phoneNumber` | `string` | `+55 (DD) NNNN-NNNN` ou `+55 (DD) NNNNN-NNNN` | obrigatório; `^\+55 \(\d{2}\) \d{4,5}-\d{4}$` | `@NotBlank`, `@Size(max = 20)`, `@Pattern("^(?=(?:\\D*\\d){8,15}\\D*$)\\+?[0-9 ()-]+$")` |
| `Address` | `zipCode` | `string` | `NNNNN-NNN` | obrigatório; `^\d{5}-\d{3}$` | `@NotBlank`, `@Pattern("^[A-Za-z0-9 -]{3,20}$")` |
| `Professor` | `salary` | `number` (`BigDecimal`) | campo exibe `x.xxx,xx`; envia número com até 2 casas | obrigatório; > 0; ≤ 1.000.000,00; ≤ 2 casas (mensagens atuais) | `@NotNull`, `@DecimalMin("0.01")`, `@DecimalMax("1000000.00")`, `@Digits(integer = 7, fraction = 2)` |

Todo valor que passa na regra local também passa na regra da API, então a interface nunca envia
um formato que a API rejeitaria por padrão.

## Estado do formulário (`frontend/src/types/person.ts`)

Sem mudança de tipos:

- `PersonForm.phoneNumber: string`: texto já formatado pela máscara (ou o valor gravado, na
  edição, quando fora do formato; ver [research.md](research.md) R5).
- `Address.zipCode: string`: texto já formatado pela máscara.
- `ProfessorForm.salary: string`: texto `x.xxx,xx`, convertido em número por `parseCurrencyBRL`
  em `professorFormToRequest`.

## Mensagens de validação local

| Campo | Situação | Mensagem |
|-------|----------|----------|
| `phoneNumber` | vazio | `é obrigatório` (atual) |
| `phoneNumber` | fora do formato (incompleto ou valor legado) | `deve estar no formato +55 (00) 00000-0000 ou +55 (00) 0000-0000` (nova) |
| `addresses[i].zipCode` | vazio | `é obrigatório` (atual) |
| `addresses[i].zipCode` | fora do formato | `deve estar no formato 00000-000` (nova) |
| `salary` | vazio / ≤ 0 / > 1.000.000,00 / > 2 casas | mensagens atuais (`é obrigatório`, `deve ser maior que zero`, `deve ser no máximo 1.000.000,00`, `deve ter no máximo 2 casas decimais`) |

## Máscaras (`frontend/src/utils/masks.ts`)

Contrato completo, com tabelas de entrada e saída, em [contracts/input-masks.md](contracts/input-masks.md).

| Máscara | Dígitos máx. | Âncora do cursor | Prefixo | Vazio |
|---------|--------------|------------------|---------|-------|
| `phoneMask` | 11 (nacionais) | `start` | `+55 ` | `''` |
| `zipCodeMask` | 8 | `start` | — | `''` |
| `currencyMask` | 9 (sem zeros à esquerda) | `end` | — | `''` |
