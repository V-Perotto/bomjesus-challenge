# UI Contract — SPA Vue.js (User Story 4)

**Feature**: `002-students-professors-crud` | **Date**: 2026-10-19 | **Plan**: [../plan.md](../plan.md)

Contrato das telas e do comportamento observável da interface. Consome exclusivamente a API
definida em [openapi.yaml](openapi.yaml). Textos em pt-BR.

## Rotas

| Rota | View | Conteúdo |
|------|------|----------|
| `/` | — | redireciona para `/students` |
| `/students` | `StudentListView` | lista de alunos + filtro de status + ações |
| `/students/new` | `StudentFormView` | cadastro de aluno |
| `/students/:id/edit` | `StudentFormView` | edição de aluno (carrega `GET /api/students/:id`) |
| `/professors` | `ProfessorListView` | lista de professores + filtro + ações |
| `/professors/new` | `ProfessorFormView` | cadastro de professor |
| `/professors/:id/edit` | `ProfessorFormView` | edição de professor |
| `/:pathMatch(.*)*` | `NotFoundView` | página não encontrada com link para "Alunos" |

O cabeçalho (`AppHeader`) mostra as abas "Alunos" e "Professores" (`RouterLink`, aba ativa
destacada) em todas as rotas.

## Tela de lista (alunos e professores)

| Elemento | Comportamento |
|----------|---------------|
| Filtro de status | "Todos" (padrão), "Ativos", "Inativos" → `GET ...?status=ACTIVE\|DISABLE` |
| Colunas comuns | Nome, E-mail, Telefone, Status (`StatusBadge`: verde = Ativo, cinza = Inativo) |
| Colunas de aluno | Foto (`PhotoAvatar`, com imagem padrão quando o link falha), Matrícula |
| Colunas de professor | Salário (`Intl.NumberFormat('pt-BR', { style: 'currency', currency: 'BRL' })`) |
| Ação "Editar" | navega para `/:tipo/:id/edit` |
| Ação "Desativar" | só para `ACTIVE`; `ConfirmDialog` simples → `GET /:id` (dados atuais) → `PUT` com esses dados + `status: DISABLE` → atualiza a linha e mostra toast de sucesso |
| Ação "Excluir" | botão secundário de perigo; `ConfirmDialog` reforçado ("Esta ação é definitiva e irreversível", nome da pessoa, botão "Excluir definitivamente") → `DELETE` → remove a linha e mostra toast |
| Botão "Novo aluno"/"Novo professor" | navega para `/:tipo/new` |
| Carregando | `LoadingState` (texto + indicador; `aria-busy`) |
| Lista vazia | `EmptyState` com atalho "Cadastrar" |
| Falha de rede | `ErrorState` ("Não foi possível conectar à API") + botão "Tentar novamente" |

## Tela de formulário (cadastro e edição)

| Elemento | Comportamento |
|----------|---------------|
| Campos comuns (`PersonForm`) | Nome, Telefone, E-mail, Status (`ACTIVE`/`DISABLE`; padrão `ACTIVE` no cadastro) (máscaras: ver `specs/003-form-input-masks/contracts/input-masks.md`) |
| Campos de aluno (slot) | Matrícula; Foto (URL) com pré-visualização (`PhotoAvatar`) |
| Campos de professor (slot) | Salário (número, 2 casas decimais, prefixo R$) (máscaras: ver `specs/003-form-input-masks/contracts/input-masks.md`) |
| Endereços (`AddressFieldset`) | um bloco por endereço (Rua, Cidade, Estado, CEP, País); "Adicionar endereço"; "Remover" desabilitado quando só há 1 (máscaras: ver `specs/003-form-input-masks/contracts/input-masks.md`) |
| Validação local | mesmas regras do [data-model](../data-model.md), ao sair do campo e ao salvar; o envio é bloqueado se houver erro |
| Erros da API | `400`/`409` com `errors[]` → mensagem exibida sob o campo correspondente (`addresses[0].city` → campo Cidade do 1º endereço); erros sem campo → toast |
| Salvar | `POST` (cadastro) ou `PUT` (edição); sucesso → toast + volta para a lista |
| Cancelar | volta para a lista sem salvar |
| Edição de id inexistente | 404 → toast "Registro não encontrado" + volta para a lista |
| Preservação | dados digitados nunca são descartados após erro de validação, conflito ou falha de rede |

## Acessibilidade e layout

- Todo campo tem `<label>` associado; erros ligados via `aria-describedby`; foco visível.
- `ConfirmDialog` usa `<dialog>` nativo (Esc cancela; foco inicial no botão seguro "Cancelar";
  ao fechar, o foco volta ao botão que o abriu).
- Ordem de foco igual à ordem visual; após salvar, desativar ou excluir, o foco vai para o `h1`
  da lista (FR-025a).
- Simplicidade (FR-025b): uma ação primária por tela; listas com no máximo 7 colunas (alunos:
  Foto, Nome, Matrícula, E-mail, Telefone, Status, Ações).
- Navegadores (FR-025c): duas últimas versões estáveis de Chrome, Firefox e Edge (desktop) e do
  Chrome para Android; lista de 1.000 pessoas exibida em até 2 s após a resposta da API.
- Toasts em região `aria-live="polite"`.
- Layout utilizável a partir de 360 px: em telas estreitas, a tabela vira lista de cartões.

## Configuração

| Variável | Padrão | Uso |
|----------|--------|-----|
| `VITE_API_BASE_URL` | `/api` | URL base do Axios; em desenvolvimento, o Vite faz proxy de `/api` para `http://localhost:8080`; no Docker, o Nginx repassa `/api` ao serviço `backend` |
