# Quickstart — Validação das máscaras de entrada

**Feature**: `003-form-input-masks` | **Date**: 2026-10-19 | **Plan**: [plan.md](plan.md)

Guia para provar que a funcionalidade está pronta. Regras detalhadas em
[contracts/input-masks.md](contracts/input-masks.md); formatos e mensagens em
[data-model.md](data-model.md). Comandos em sintaxe bash.

## Pré-requisitos

| Ferramenta | Versão | Necessária para |
|------------|--------|-----------------|
| JDK | 21 | API local (`./mvnw`) |
| Node.js / npm | 24 / 11 | front-end |
| Navegador | Chrome, Firefox ou Edge atuais | roteiro manual; DevTools para emular celular |

## 1. Gates automáticos do front-end

```bash
cd frontend
npm run type-check && npm run lint && npm run test:unit -- --run && npm run build
```

**Esperado**: tudo sem erros. Entre os testes:

- `masks.spec.ts` cobre as tabelas das seções 2 e 3 do contrato;
- `TextField.spec.ts` cobre o componente com máscara;
- `validators.spec.ts` cobre as novas mensagens de telefone e CEP.

## 2. Gate do back-end (regressão)

```bash
cd backend
./mvnw clean verify
```

**Esperado**: `BUILD SUCCESS`, sem nenhuma alteração no back-end (o diff da feature não toca
`backend/`).

## 3. Subir a aplicação com dados de demonstração

```bash
cd backend && SPRING_PROFILES_ACTIVE=demo ./mvnw spring-boot:run     # terminal 1
cd frontend && npm run dev                                          # terminal 2 → http://localhost:5173
```

## 4. Roteiro manual

| # | Onde | Ação | Esperado | Requisitos |
|---|------|------|----------|------------|
| 1 | Alunos → Novo aluno → Telefone | digitar `4` | `+55 (4` | FR-002 |
| 2 | mesmo campo | continuar com `1999990001` | `+55 (41) 99999-0001` | FR-003 |
| 3 | mesmo campo | digitar `a`, `#`, `+`, espaço | valor inalterado, nada pisca | FR-001 |
| 4 | mesmo campo | colocar o cursor após o `-` e pressionar Backspace | `+55 (41) 9999-0001`, cursor antes do `-` | Edge Cases |
| 5 | mesmo campo | colocar o cursor dentro de `+55` e pressionar Backspace | valor inalterado | contrato §3 |
| 6 | mesmo campo | apagar todos os dígitos | campo vazio, sem `+55` | FR-004 |
| 7 | mesmo campo | colar `+55 41 3333 0001` | `+55 (41) 3333-0001` | FR-005 |
| 8 | Endereço 1 → CEP | digitar `80230010x` | `80230-010` | FR-015, FR-016 |
| 9 | mesmo formulário | deixar CEP `8023`, Telefone `+55 (41) 9999`, salvar | não envia; mensagens de formato junto aos dois campos | FR-006, FR-017 |
| 10 | completar e salvar | — | aluno aparece na lista com `+55 (41) …` | FR-007, FR-018 |
| 11 | Professores → Novo professor → Salário | digitar `850000` | `8.500,00` | FR-009 |
| 12 | mesmo campo | Backspace no fim | `850,00` | US2 #5 |
| 13 | mesmo campo, vazio | digitar `1234567890` | `1.234.567,89` (o 10º dígito é ignorado) | FR-010 |
| 14 | mesmo campo | valor `1.500.000,00` e salvar | `deve ser no máximo 1.000.000,00` | FR-012 |
| 15 | salário `8.500,00`, demais campos válidos | salvar | lista mostra `R$ 8.500,00` | FR-013 |
| 16 | Professores → editar "Helena Rocha" | abrir | Telefone `+55 (41) 3333-0001`, Salário `8.500,00`, CEP `80230-010` | FR-014, FR-019 |
| 17 | DevTools → modo celular | tocar em Telefone, CEP e Salário | teclado numérico | FR-021 |
| 18 | leitor de tela ou inspeção de acessibilidade | focar um campo com erro | rótulo, "obrigatório" e mensagem de erro anunciados | FR-022 |

## 5. Valor legado fora do formato (FR-019)

Cadastre um professor direto pela API com formatos internacionais e abra a edição na interface:

```bash
curl -s -X POST http://localhost:8080/api/professors \
  -H 'Content-Type: application/json' \
  -d '{"name":"Legado Teste","phoneNumber":"+1 (555) 123-4567","emailAddress":"legado@escola.com",
       "salary":1000.00,"addresses":[{"street":"Main St, 1","city":"Boston","state":"MA",
       "zipCode":"02101 A","country":"EUA"}]}'
```

**Esperado**:

- na edição, Telefone exibe `+1 (555) 123-4567` e CEP exibe `02101 A`, sem reformatação;
- ao salvar sem mudanças, os dois campos mostram a mensagem de formato e nada é enviado;
- ao digitar no Telefone, a máscara passa a valer.
