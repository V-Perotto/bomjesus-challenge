# Quickstart — Validação ponta a ponta

**Feature**: `002-students-professors-crud` | **Date**: 2026-10-19 | **Plan**: [plan.md](plan.md)

Guia para provar que a feature funciona depois da implementação. Contratos:
[openapi.yaml](contracts/openapi.yaml) e [frontend-ui.md](contracts/frontend-ui.md). Regras de
dados: [data-model.md](data-model.md). Comandos em sintaxe bash.

## Pré-requisitos

| Ferramenta | Versão | Necessária para |
|------------|--------|-----------------|
| JDK | 21 | build e execução local do back-end (o Maven vem pelo `./mvnw`) |
| Node.js / npm | 24 / 11 | execução local do front-end |
| Docker + Compose | 29 / v5 | execução em containers |
| `curl` | qualquer | cenários de API abaixo |

## 1. Build, testes e cobertura (back-end)

```bash
cd backend
./mvnw clean verify
```

**Esperado**: `BUILD SUCCESS`; testes de service, controller, mapper, repository e integração
passando; o JaCoCo aprova a regra de cobertura (≥ 80% de linhas em `service` e `controller`).
Relatório em `backend/target/site/jacoco/index.html`.

## 2. Executar a API localmente

```bash
cd backend
./mvnw spring-boot:run                                      # banco vazio
SPRING_PROFILES_ACTIVE=demo ./mvnw spring-boot:run          # com dados de demonstração
```

**Esperado**: API em `http://localhost:8080`. Com o perfil `demo`, `GET /api/students` retorna 3
alunos (um `DISABLE`) e `GET /api/professors` retorna 2 professores.

## 3. Cenários de API (sem perfil `demo`)

| # | Requisição | Resultado esperado | Spec |
|---|------------|--------------------|------|
| 1 | `POST /api/students` com o exemplo do contrato | **201**, header `Location: .../api/students/1`, `status: ACTIVE`, e-mail em minúsculas | US1-1, FR-002 |
| 2 | `GET /api/students` e `GET /api/students?status=DISABLE` | **200** com 1 aluno; **200** com `[]` | US1-2/3 |
| 3 | `GET /api/students/1` | **200** com `addresses` (sem `id` no endereço) | US1-4, FR-004 |
| 4 | `GET /api/students/999` e `GET /api/students/abc` | **404**; **400** (ambos em `application/problem+json`) | US1-5, Edge |
| 5 | `POST /api/students` sem `addresses`, com e-mail inválido e `addresses[0].city` vazio | **400** com `errors` listando `addresses` ou `addresses[0].city` e `emailAddress` | US1-9, FR-013 |
| 6 | `POST /api/professors` com o e-mail `ANA@escola.com` | **409** com `errors[0].field = emailAddress` | US2-6, FR-003 |
| 7 | `POST /api/students` com a matrícula `2026001` repetida | **409** com `errors[0].field = studentNumber` | US1-8, FR-009 |
| 8 | `PUT /api/students/1` com 1 endereço diferente e sem `status` | **200**, apenas o novo endereço e `status` mantido | US1-6, FR-004 |
| 9 | `PUT /api/students/1` com `"status": "DISABLE"` | **200**, `status: DISABLE` | FR-002, "Desativar" |
| 10 | `POST /api/professors` com `salary: 0`, `-1`, `10.123` e `"abc"` | **400** com `field = salary` | US2-5 |
| 11 | `POST /api/professors` válido com `salary: 8500` | **201**, `salary: 8500.00` | US2-2 |
| 12 | `GET /api/professors/1` (o id 1 é de um aluno) | **404** | Edge (tipo errado) |
| 13 | `DELETE /api/students/1` duas vezes | **204** e depois **404** | US1-7, Edge |
| 14 | `GET /api/students?status=FOO` | **400** | Edge |
| 15 | `PATCH /api/students/1` | **405** em `application/problem+json` | Edge |

Exemplo do cenário 1:

```bash
curl -i -X POST http://localhost:8080/api/students \
  -H 'Content-Type: application/json' \
  -d '{"name":"Ana Souza","phoneNumber":"+55 (41) 99999-0001","emailAddress":"Ana@Escola.com",
       "studentNumber":"2026001","photo":"https://i.pravatar.cc/150?img=5",
       "addresses":[{"street":"Rua XV de Novembro, 100","city":"Curitiba","state":"PR",
                     "zipCode":"80020-310","country":"Brasil"}]}'
```

## 4. Console H2

Abra `http://localhost:8080/h2-console` com JDBC URL `jdbc:h2:mem:bomjesus`, usuário `sa` e senha
vazia.

**Esperado**: tabelas `PERSON`, `STUDENT`, `PROFESSOR` e `ADDRESS`. Após o cenário 1, há uma linha
em `PERSON` e uma em `STUDENT` com o mesmo `ID`, e o endereço em `ADDRESS` com `PERSON_ID`
correspondente. Após o cenário 13, nenhuma linha órfã em `ADDRESS`.

## 5. Front-end em desenvolvimento

```bash
cd frontend
cp .env.example .env        # VITE_API_BASE_URL=/api (proxy do Vite para :8080)
npm ci
npm run dev                 # http://localhost:5173
npm run type-check && npm run lint && npm run test:unit && npm run build
```

**Esperado**: comandos de qualidade sem erros. Com a API rodando no perfil `demo`, valide em
`http://localhost:5173` o roteiro abaixo (detalhes em [frontend-ui.md](contracts/frontend-ui.md)):

1. As abas "Alunos" e "Professores" alternam as listas; o filtro "Inativos" mostra o aluno
   `DISABLE`.
2. Em "Novo aluno", cadastrar com 2 endereços: toast de sucesso e o aluno aparece na lista.
3. Repetir o e-mail de uma pessoa existente: a mensagem aparece sob o campo E-mail e os dados
   digitados permanecem.
4. Em "Desativar", confirmar: o status muda para Inativo sem recarregar a página.
5. Em "Excluir", o diálogo pede confirmação reforçada; ao confirmar, a linha some.
6. Uma URL de foto quebrada exibe a imagem padrão.
7. Com a API parada, a lista exibe a falha de comunicação e o botão "Tentar novamente".
8. Com a janela em 360 px, a interface continua utilizável.
9. **Modo de acesso direto (CORS)**: pare o `npm run dev` e rode
   `VITE_API_BASE_URL=http://localhost:8080/api npm run dev`. Repita os passos 2 e 4: tudo
   funciona, e o console do navegador não mostra erro de CORS.

## 6. Tudo em containers

```bash
docker compose up --build                                   # na raiz do repositório
SPRING_PROFILES_ACTIVE=demo docker compose up --build       # com dados de demonstração
```

**Esperado**:

- `backend` fica `healthy` e o `frontend` sobe em seguida.
- Interface em `http://localhost:8081`, API em `http://localhost:8080/api/...` (também acessível
  via `http://localhost:8081/api/...`), console H2 em `http://localhost:8080/h2-console`.
- Os cenários da seção 3 funcionam contra a porta 8080, e o roteiro da seção 5 contra a porta
  8081, sem configuração de CORS.

Encerrar: `docker compose down`.

## 7. Desempenho (SC-004)

Com a API rodando localmente:

```bash
./scripts/seed-volume.sh    # cria 1.000 alunos + 1.000 professores (2 endereços cada) via API
```

Depois de uma requisição de aquecimento, meça cada operação com
`curl -s -o /dev/null -w '%{time_total}\n' <url>`: `GET /api/students`, `GET /api/professors`,
`GET /api/students/{id}`, `POST`, `PUT` e `DELETE`.

**Esperado**: todos os tempos abaixo de 1 s. Com a interface aberta, a lista de 1.000 alunos
aparece em até 2 s após a resposta da API (FR-025c).

## 8. Critérios de aceite cobertos

| Critério | Seções |
|----------|--------|
| SC-001 (subir e cadastrar em ≤ 10 min) | 2 ou 6 + cenário 1 |
| SC-002 / SC-003 (status e validação) | 1 e 3 |
| SC-004 (desempenho) | 7 |
| SC-005 (cobertura ≥ 80%) | 1 |
| SC-006 (≤ 2 comandos em containers) | 6 |
| SC-007 / SC-008 (interface) | 5 |
