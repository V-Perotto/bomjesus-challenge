# Implementation Plan: Máscaras de Entrada nos Formulários de Aluno e Professor

**Branch**: `003-form-input-masks` | **Date**: 2026-10-19 | **Spec**: [spec.md](spec.md)

**Input**: Feature specification from `specs/003-form-input-masks/spec.md`

## Summary

Os campos Telefone (aluno e professor), Salário (professor) e CEP (cada endereço) passam a
aceitar só dígitos e a formatar o valor durante a digitação:

- Telefone: `+55 (DD) NNNNN-NNNN` ou `+55 (DD) NNNN-NNNN`, com o `+55` inserido no primeiro
  dígito.
- Salário: `x.xxx,xx`, com os centavos preenchidos da direita para a esquerda.
- CEP: `XXXXX-XXX`.

A mudança é só no front-end:

- **Máscaras**: funções puras em um novo `utils/masks.ts` (sem dependência nova).
- **Componente**: uma prop `mask` opcional no `TextField.vue` existente, que reescreve o valor e
  o cursor no próprio evento `input`.
- **Validação**: as regras locais de telefone e CEP passam a exigir o formato canônico.

A API, os DTOs e os dados não mudam, porque os formatos produzidos já são aceitos pelas regras
atuais. Decisões em [research.md](research.md).

## Technical Context

**Language/Version**: TypeScript ~6.0 (front-end). Java 21 no back-end, que não é alterado.

**Primary Dependencies**: Vue 3.5 (Composition API, `<script setup>`), Vite 8 e Vue Router 5
(existentes). Nenhuma dependência nova (research R1).

**Storage**: N/A. O H2 da API não muda; os valores são gravados nos formatos já aceitos.

**Testing**: Vitest 4 + Vue Test Utils + jsdom (existentes, fora do gate de cobertura). O
back-end (JUnit/Mockito/JaCoCo) entra só como regressão: `./mvnw clean verify` sem mudanças.

**Target Platform**: navegadores atuais de desktop e celular (teclado numérico via `inputmode`).

**Project Type**: aplicação web (API REST + SPA); esta feature altera apenas `frontend/`.

**Performance Goals**: a formatação ocorre no mesmo evento `input`, sem atraso perceptível nem
"piscar" de caracteres rejeitados.

**Constraints**:

- sem dependências novas;
- comportamento atual preservado nos campos sem máscara;
- acessibilidade atual mantida (FR-022);
- valores gravados devem continuar passando nas regras da API.

**Scale/Scope**:

- 3 máscaras e 1 componente alterado (`TextField`);
- 3 pontos de uso: `PersonForm`, `AddressFieldset` e `ProfessorFormView`;
- 1 módulo de validação ajustado;
- 2 arquivos de teste novos (mais 1 helper) e 1 atualizado.

## Constitution Check

*GATE: Must pass before Phase 0 research. Re-check after Phase 1 design.*

| Princípio | Verificação | Pré-pesquisa | Pós-design |
|-----------|-------------|--------------|------------|
| I. Stack back-end | Back-end não é alterado | ✅ | ✅ |
| II. API RESTful com DTOs | Contrato REST, DTOs, status e formato de erro inalterados; a interface só envia formatos aceitos pelas regras atuais | ✅ | ✅ |
| III. Clean Code e camadas | Máscaras como funções puras de responsabilidade única em `utils/`; sem números mágicos (limites como constantes nomeadas); sem código morto | ✅ | ✅ |
| IV. Testes obrigatórios | Testes do back-end continuam passando (`./mvnw clean verify`); máscaras e validações cobertas por Vitest (opcional e fora do gate, mas incluído) | ✅ | ✅ |
| V. Docker | Nenhuma mudança em Dockerfiles nem no compose; a imagem do front-end é reconstruída normalmente | ✅ | ✅ |
| VI. README | README atualizado na mesma alteração, na lista de recursos da interface (máscaras de telefone, CEP e salário) | ✅ | ✅ |
| VII. Domínio | Nenhum atributo ou entidade novo; `salary` continua numérico (`BigDecimal` na API); nomes em inglês preservados | ✅ | ✅ |
| VIII. Front-end Vue | Composition API com `<script setup>`; nenhuma chamada Axios fora de `services/`; validação local apenas melhora a experiência e erros da API continuam exibidos (FR-023); sem dependência nova | ✅ | ✅ |
| Padrões: Logging | Sem `console.log` | ✅ | ✅ |
| Padrões: YAGNI | Três máscaras explícitas, sem motor genérico de padrões nem diretiva global | ✅ | ✅ |

**Resultado**: aprovado nas duas verificações, sem desvios. "Complexity Tracking" não se
aplica.

## Project Structure

### Documentation (this feature)

```text
specs/003-form-input-masks/
├── plan.md                  # este arquivo
├── research.md              # Phase 0: decisões técnicas (R1–R8)
├── data-model.md            # Phase 1: atributos afetados, formatos e mensagens
├── quickstart.md            # Phase 1: gates e roteiro de validação
├── contracts/
│   └── input-masks.md       # Phase 1: contrato de UI das máscaras (entrada → saída, cursor)
├── checklists/
│   └── requirements.md      # checklist de qualidade da spec
└── tasks.md                 # Phase 2 (/speckit-tasks)
```

### Source Code (repository root)

```text
frontend/
└── src/
    ├── utils/
    │   ├── masks.ts               # NOVO: InputMask, phoneMask, zipCodeMask, currencyMask, applyMask, maskInput
    │   ├── formatters.ts          # reutilizado (DECIMAL_BR, parseCurrencyBRL); sem mudança de comportamento
    │   └── validators.ts          # ALTERADO: PHONE/ZIP_CODE canônicos e novas mensagens
    ├── components/
    │   ├── TextField.vue          # ALTERADO: prop `mask`, inputmode 'numeric', handler de input com cursor
    │   ├── PersonForm.vue         # ALTERADO: telefone com phoneMask
    │   └── AddressFieldset.vue    # ALTERADO: CEP com zipCodeMask
    ├── views/
    │   └── ProfessorFormView.vue  # ALTERADO: salário com currencyMask
    └── __tests__/
        ├── testMasks.ts           # NOVO: máscaras de teste compartilhadas (helper, sem testes)
        ├── masks.spec.ts          # NOVO
        ├── TextField.spec.ts      # NOVO
        └── validators.spec.ts     # ALTERADO

README.md                          # ALTERADO: menção às máscaras na lista de recursos da interface
```

**Structure Decision**: aplicação web existente (`backend/` + `frontend/`); somente `frontend/`
e o `README.md` mudam. As máscaras ficam em `utils/`, ao lado de `formatters.ts` e
`validators.ts`, porque são funções puras sem estado reativo. Não se justifica um composable
(Princípio VIII: `composables/` é para lógica reativa reutilizável). A integração com o DOM
fica no próprio `TextField.vue`, componente de campo que todos os formulários já usam.

## Complexity Tracking

Sem violações a justificar.
