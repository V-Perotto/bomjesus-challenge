# Bom Jesus — Gestão de Alunos e Professores

API REST em **Spring Boot** para cadastrar, consultar, atualizar e excluir **alunos** e **professores** de
uma instituição de ensino, com interface web em **Vue.js**. Projeto do desafio técnico "Teste de CRUD com
Spring Boot".

## Sumário

- [Modelo de domínio](#modelo-de-domínio)
- [Tecnologias](#tecnologias)
- [Pré-requisitos](#pré-requisitos)
- [Execução rápida com Docker](#execução-rápida-com-docker)
- [Back-end: build, execução e testes](#back-end-build-execução-e-testes)
- [Front-end: execução, testes e build](#front-end-execução-testes-e-build)
- [Endpoints da API](#endpoints-da-api)
- [Console do banco H2](#console-do-banco-h2)
- [Dados de demonstração](#dados-de-demonstração)
- [Configuração](#configuração)
- [Estrutura do repositório](#estrutura-do-repositório)
- [Roteiro para os vídeos](#roteiro-para-os-vídeos)
- [Entrega](#entrega)

> Todos os comandos usam o shell **bash** (Linux, macOS ou WSL no Windows) e partem da raiz do repositório,
> salvo indicação contrária.

## Modelo de domínio

![Modelagem de domínio](docs/modelagem-bomjesus.png)

- `Person` é abstrata e nunca é cadastrada diretamente. `Student` (matrícula e foto) e `Professor` (salário)
  herdam dela. A herança é persistida com a estratégia JPA **`JOINED`**: tabela `person` com os campos
  comuns e as tabelas `student` e `professor` com os campos próprios, ligadas pelo mesmo `id`.
- Toda pessoa tem um `status` (`ACTIVE` ou `DISABLE`) e **um ou mais** endereços (`Address`, `1..*`). Os
  endereços fazem parte da pessoa: são enviados e retornados junto com ela, e o `PUT` substitui a lista
  inteira.
- O e-mail é único entre **todas** as pessoas (sem diferenciar maiúsculas e minúsculas); a matrícula é
  única entre os alunos.
- `DELETE` remove a pessoa definitivamente. Para desativar, atualize o `status` para `DISABLE`.

## Tecnologias

| Camada | Tecnologias |
|--------|-------------|
| Back-end | Java 21, Spring Boot 4.1.1 (Web MVC, Data JPA, Validation), Hibernate 7, MapStruct 1.6.3 |
| Front-end | Vue 3 (Composition API, `<script setup>`), TypeScript, Vite, Vue Router, Axios |
| Banco | H2 em memória, com console web |
| Testes | JUnit Jupiter 6, Mockito, AssertJ, MockMvc, JaCoCo (mínimo de 80% de linhas em `service` e `controller`); Vitest no front-end |
| Containers | Docker multi-stage (JRE e Nginx, ambos sem privilégios de root) + Docker Compose |

## Pré-requisitos

| Ferramenta | Versão | Para quê |
|------------|--------|----------|
| JDK | 21 | build e execução local do back-end (o Maven vem pelo Maven Wrapper `./mvnw`) |
| Node.js + npm | Node 22.18+ ou 24.12+, npm 10+ | execução local do front-end |
| Docker + Docker Compose | Docker 24+ / Compose v2 | execução em containers |
| `curl` | qualquer | exemplos de requisição |

Para usar só os containers, basta o Docker. Não é preciso instalar o Maven. Confira o JDK com `java -version` e `javac -version`: o build precisa do
**JDK** (com `javac`), não só do JRE. Se o JDK 21 não for o padrão do sistema, aponte o `JAVA_HOME` para
ele antes de rodar o `./mvnw`.

## Execução rápida com Docker

Um único comando sobe a API e a interface web:

```bash
docker compose up --build                                  # banco vazio
SPRING_PROFILES_ACTIVE=demo docker compose up --build      # com dados de demonstração
```

| O quê | URL |
|-------|-----|
| Interface web | <http://localhost:8081> |
| API (direta) | <http://localhost:8080/api/students> |
| API (pela interface, via Nginx) | <http://localhost:8081/api/students> |
| Console do banco H2 | <http://localhost:8080/h2-console> |

A interface só sobe depois que a API fica saudável (healthcheck). O Nginx da interface repassa `/api`
para o container da API, então as duas ficam na mesma origem e não há CORS nesse modo.

Para encerrar: `docker compose down`.

## Back-end: build, execução e testes

```bash
cd backend
./mvnw clean package                              # compila, roda os testes e gera target/app.jar
./mvnw spring-boot:run                            # API em http://localhost:8080, banco vazio
SPRING_PROFILES_ACTIVE=demo ./mvnw spring-boot:run   # com dados de demonstração
```

Somente a imagem da API, sem o Compose:

```bash
docker build -t academic-api ./backend
docker run --rm -p 8080:8080 academic-api
```

### Testes e cobertura

```bash
cd backend
./mvnw clean verify
```

O `verify` roda todos os testes e falha se a cobertura de linhas dos pacotes `service` ou `controller`
ficar abaixo de 80%. O relatório de cobertura fica em `backend/target/site/jacoco/index.html`.

| Tipo de teste | O que cobre |
|---------------|-------------|
| Unitário de Service (Mockito) | regras de negócio: normalização, unicidade, status padrão, ordenação, endereços |
| Controller (`@WebMvcTest` + MockMvc) | status HTTP, corpo, `Location`, erros RFC 9457, CORS, 405/406/415/500 |
| Mapper | conversões do MapStruct |
| Repositório (`@DataJpaTest`) | herança `JOINED`, unicidade no banco, cascata e remoção de órfãos |
| Integração (`@SpringBootTest`) | fluxos completos de alunos e professores |
| Arquitetura | nenhum controller expõe entidades JPA |

## Front-end: execução, testes e build

A interface consome a API em `/api`. Em desenvolvimento, o Vite repassa `/api` para
`http://localhost:8080`, então **suba a API antes** (veja a seção anterior).

```bash
cd frontend
npm ci                        # instala as dependências (versões travadas no package-lock.json)
npm run dev                   # http://localhost:5173
```

Qualidade e build:

```bash
cd frontend
npm run test:unit -- --run    # testes unitários (Vitest)
npm run lint                  # oxlint + ESLint
npm run type-check            # vue-tsc
npm run build                 # gera frontend/dist
```

Telas: abas **Alunos** e **Professores**, cada uma com:

- lista filtrável por status;
- cadastro e edição com vários endereços, pré-visualização da foto e salário em reais;
- **Desativar**, com confirmação;
- **Excluir**, com confirmação reforçada.

Erros da API aparecem junto ao campo correspondente, e os dados digitados nunca se perdem.

### Endereço da API e CORS

A URL base da API vem da variável `VITE_API_BASE_URL` (padrão `/api`; veja `frontend/.env.example`).

- **Modo padrão** (`/api`): o proxy do Vite (desenvolvimento) ou o Nginx (Docker) repassa as chamadas.
  Interface e API ficam na mesma origem, e o CORS não entra em jogo.
- **Acesso direto** (`VITE_API_BASE_URL=http://localhost:8080/api npm run dev`): a interface, em
  `localhost:5173`, chama a API em `localhost:8080`, outra origem. A API só aceita as origens listadas
  em `APP_CORS_ALLOWED_ORIGINS`; por padrão, `http://localhost:5173` e `http://127.0.0.1:5173`. O Vite
  usa porta fixa (`strictPort`) para não sair dessa lista.

## Endpoints da API

Todas as rotas aceitam e retornam JSON. Os erros seguem a RFC 9457 (`application/problem+json`) com
título e mensagem em português.

| Operação | Método e caminho | Sucesso | Erros possíveis |
|----------|------------------|---------|-----------------|
| Listar alunos | `GET /api/students[?status=ACTIVE\|DISABLE]` | 200 | 400 |
| Buscar aluno | `GET /api/students/{id}` | 200 | 400, 404 |
| Criar aluno | `POST /api/students` | 201 + `Location` | 400, 409 |
| Atualizar aluno | `PUT /api/students/{id}` | 200 | 400, 404, 409 |
| Excluir aluno | `DELETE /api/students/{id}` | 204 | 400, 404 |
| Listar professores | `GET /api/professors[?status=ACTIVE\|DISABLE]` | 200 | 400 |
| Buscar professor | `GET /api/professors/{id}` | 200 | 400, 404 |
| Criar professor | `POST /api/professors` | 201 + `Location` | 400, 409 |
| Atualizar professor | `PUT /api/professors/{id}` | 200 | 400, 404, 409 |
| Excluir professor | `DELETE /api/professors/{id}` | 204 | 400, 404 |

Regras gerais:

- As listagens vêm em ordem alfabética do português (ignorando maiúsculas e acentos) e podem ser
  filtradas por `status`.
- `status` é opcional: na criação, o padrão é `ACTIVE`; na atualização, omiti-lo mantém o status atual.
- `photo` (aluno) é opcional e deve ser uma URL `http`/`https`. Sem foto, a resposta traz `"photo": null`.
- `salary` (professor) é maior que zero, tem no máximo 2 casas decimais e vai até 1.000.000,00. Ele
  sempre volta com 2 casas.
- O contrato completo está em [`specs/002-students-professors-crud/contracts/openapi.yaml`](specs/002-students-professors-crud/contracts/openapi.yaml).

### Alunos

Os exemplos abaixo foram executados nesta ordem, com o banco vazio (sem o perfil `demo`).

**Criar** → `201 Created`, com `Location: http://localhost:8080/api/students/1`

```bash
curl -i -X POST http://localhost:8080/api/students \
  -H 'Content-Type: application/json' \
  -d '{
        "name": "Ana Souza",
        "phoneNumber": "+55 (41) 99999-0001",
        "emailAddress": "Ana@Escola.com",
        "studentNumber": "2026001",
        "photo": "https://i.pravatar.cc/150?img=5",
        "addresses": [
          {"street": "Rua XV de Novembro, 100", "city": "Curitiba", "state": "PR",
           "zipCode": "80020-310", "country": "Brasil"}
        ]
      }'
```

```json
{
  "id": 1,
  "name": "Ana Souza",
  "phoneNumber": "+55 (41) 99999-0001",
  "emailAddress": "ana@escola.com",
  "status": "ACTIVE",
  "studentNumber": "2026001",
  "photo": "https://i.pravatar.cc/150?img=5",
  "addresses": [
    {"street": "Rua XV de Novembro, 100", "city": "Curitiba", "state": "PR",
     "zipCode": "80020-310", "country": "Brasil"}
  ]
}
```

O e-mail volta em minúsculas e o `status`, omitido, vira `ACTIVE`.

**Listar** → `200 OK` com a lista (vazia se nada combinar com o filtro)

```bash
curl http://localhost:8080/api/students
```

```json
[
  {
    "id": 1,
    "name": "Ana Souza",
    "phoneNumber": "+55 (41) 99999-0001",
    "emailAddress": "ana@escola.com",
    "status": "ACTIVE",
    "studentNumber": "2026001",
    "photo": "https://i.pravatar.cc/150?img=5",
    "addresses": [
      {"street": "Rua XV de Novembro, 100", "city": "Curitiba", "state": "PR",
       "zipCode": "80020-310", "country": "Brasil"}
    ]
  }
]
```

```bash
curl "http://localhost:8080/api/students?status=DISABLE"
```

```json
[]
```

**Buscar por id** → `200 OK` com o aluno (o mesmo JSON da criação), ou `404 Not Found`

```bash
curl http://localhost:8080/api/students/1
```

```json
{
  "id": 1,
  "name": "Ana Souza",
  "phoneNumber": "+55 (41) 99999-0001",
  "emailAddress": "ana@escola.com",
  "status": "ACTIVE",
  "studentNumber": "2026001",
  "photo": "https://i.pravatar.cc/150?img=5",
  "addresses": [
    {"street": "Rua XV de Novembro, 100", "city": "Curitiba", "state": "PR",
     "zipCode": "80020-310", "country": "Brasil"}
  ]
}
```

**Atualizar** → `200 OK` com o aluno atualizado. A atualização é completa: a lista de endereços enviada
substitui a anterior, e campos opcionais omitidos (como `photo`) ficam vazios.

```bash
curl -X PUT http://localhost:8080/api/students/1 \
  -H 'Content-Type: application/json' \
  -d '{
        "name": "Ana Souza",
        "phoneNumber": "+55 (41) 99999-0001",
        "emailAddress": "ana@escola.com",
        "status": "DISABLE",
        "studentNumber": "2026001",
        "addresses": [
          {"street": "Rua das Flores, 25", "city": "Curitiba", "state": "PR",
           "zipCode": "80010-020", "country": "Brasil"}
        ]
      }'
```

```json
{
  "id": 1,
  "name": "Ana Souza",
  "phoneNumber": "+55 (41) 99999-0001",
  "emailAddress": "ana@escola.com",
  "status": "DISABLE",
  "studentNumber": "2026001",
  "photo": null,
  "addresses": [
    {"street": "Rua das Flores, 25", "city": "Curitiba", "state": "PR",
     "zipCode": "80010-020", "country": "Brasil"}
  ]
}
```

**Excluir** (definitivo, remove também os endereços) → `204 No Content`, sem corpo. Uma segunda
chamada para o mesmo id retorna `404 Not Found`.

```bash
curl -i -X DELETE http://localhost:8080/api/students/1
```

```text
HTTP/1.1 204
```

### Professores

**Criar** → `201 Created`, com `Location: http://localhost:8080/api/professors/2`

```bash
curl -i -X POST http://localhost:8080/api/professors \
  -H 'Content-Type: application/json' \
  -d '{
        "name": "Helena Rocha",
        "phoneNumber": "+55 (41) 3333-0001",
        "emailAddress": "helena.rocha@escola.com",
        "salary": 8500,
        "addresses": [
          {"street": "Av. Sete de Setembro, 2000", "city": "Curitiba", "state": "PR",
           "zipCode": "80230-010", "country": "Brasil"}
        ]
      }'
```

```json
{
  "id": 2,
  "name": "Helena Rocha",
  "phoneNumber": "+55 (41) 3333-0001",
  "emailAddress": "helena.rocha@escola.com",
  "status": "ACTIVE",
  "salary": 8500.00,
  "addresses": [
    {"street": "Av. Sete de Setembro, 2000", "city": "Curitiba", "state": "PR",
     "zipCode": "80230-010", "country": "Brasil"}
  ]
}
```

**Listar** (aqui, só os ativos) → `200 OK` com a lista

```bash
curl "http://localhost:8080/api/professors?status=ACTIVE"
```

```json
[
  {
    "id": 2,
    "name": "Helena Rocha",
    "phoneNumber": "+55 (41) 3333-0001",
    "emailAddress": "helena.rocha@escola.com",
    "status": "ACTIVE",
    "salary": 8500.00,
    "addresses": [
      {"street": "Av. Sete de Setembro, 2000", "city": "Curitiba", "state": "PR",
       "zipCode": "80230-010", "country": "Brasil"}
    ]
  }
]
```

**Buscar por id** → `200 OK` com o professor, ou `404 Not Found` (inclusive para o id de um aluno)

```bash
curl http://localhost:8080/api/professors/2
```

```json
{
  "id": 2,
  "name": "Helena Rocha",
  "phoneNumber": "+55 (41) 3333-0001",
  "emailAddress": "helena.rocha@escola.com",
  "status": "ACTIVE",
  "salary": 8500.00,
  "addresses": [
    {"street": "Av. Sete de Setembro, 2000", "city": "Curitiba", "state": "PR",
     "zipCode": "80230-010", "country": "Brasil"}
  ]
}
```

**Atualizar** (completo) → `200 OK` com o professor atualizado. O salário volta sempre com 2 casas.

```bash
curl -X PUT http://localhost:8080/api/professors/2 \
  -H 'Content-Type: application/json' \
  -d '{
        "name": "Helena Rocha",
        "phoneNumber": "+55 (41) 3333-0001",
        "emailAddress": "helena.rocha@escola.com",
        "status": "DISABLE",
        "salary": 9200.5,
        "addresses": [
          {"street": "Rua Marechal Deodoro, 500", "city": "Curitiba", "state": "PR",
           "zipCode": "80010-010", "country": "Brasil"}
        ]
      }'
```

```json
{
  "id": 2,
  "name": "Helena Rocha",
  "phoneNumber": "+55 (41) 3333-0001",
  "emailAddress": "helena.rocha@escola.com",
  "status": "DISABLE",
  "salary": 9200.50,
  "addresses": [
    {"street": "Rua Marechal Deodoro, 500", "city": "Curitiba", "state": "PR",
     "zipCode": "80010-010", "country": "Brasil"}
  ]
}
```

**Excluir** (definitivo) → `204 No Content`, sem corpo

```bash
curl -i -X DELETE http://localhost:8080/api/professors/2
```

```text
HTTP/1.1 204
```

### Erros

Validação (`400`): todos os campos inválidos vêm de uma vez, inclusive os dos endereços.

```json
{
  "title": "Dados inválidos",
  "status": 400,
  "detail": "A requisição possui 2 campo(s) inválido(s).",
  "instance": "/api/students",
  "timestamp": "2026-10-19T19:19:59.102Z",
  "errors": [
    {"field": "addresses[0].city", "message": "é obrigatório"},
    {"field": "emailAddress", "message": "deve ser um e-mail válido"}
  ]
}
```

Conflito (`409`), por exemplo um e-mail já usado por outra pessoa (aluno ou professor):

```json
{
  "title": "Conflito",
  "status": 409,
  "detail": "Já existe uma pessoa cadastrada com o e-mail ana@escola.com.",
  "instance": "/api/professors",
  "timestamp": "2026-10-19T19:24:15.550Z",
  "errors": [{"field": "emailAddress", "message": "e-mail já cadastrado"}]
}
```

Os demais erros seguem o mesmo formato:

| Status | Quando |
|--------|--------|
| 400 | id não numérico, JSON malformado, `status` inválido |
| 404 | pessoa inexistente ou rota inexistente |
| 405 | método não suportado |
| 406 | `Accept` sem JSON |
| 415 | corpo que não é JSON |
| 500 | erro inesperado; a resposta não traz detalhes internos |

## Console do banco H2

Com a API em execução (local ou Docker), abra <http://localhost:8080/h2-console> e use:

| Campo | Valor |
|-------|-------|
| JDBC URL | `jdbc:h2:mem:bomjesus` |
| User Name | `sa` |
| Password | (vazio) |

As tabelas `PERSON`, `STUDENT`, `PROFESSOR` e `ADDRESS` mostram a herança `JOINED`: cada aluno tem uma
linha em `PERSON` e outra em `STUDENT` com o mesmo `ID`. O banco é em memória e é apagado sempre que a
aplicação reinicia.

## Dados de demonstração

O perfil `demo` cria, ao iniciar, 3 alunos e 2 professores:

- inclui um aluno e um professor com status `DISABLE`;
- a aluna Ana Souza tem 2 endereços;
- as fotos vêm de `i.pravatar.cc`.

```bash
SPRING_PROFILES_ACTIVE=demo docker compose up --build
# ou, localmente:
cd backend && SPRING_PROFILES_ACTIVE=demo ./mvnw spring-boot:run
```

Sem o perfil, o banco inicia vazio. Os testes automatizados nunca usam esse perfil.

## Configuração

| Variável | Padrão | Uso |
|----------|--------|-----|
| `SPRING_PROFILES_ACTIVE` | `default` | `demo` carrega os dados de demonstração |
| `APP_CORS_ALLOWED_ORIGINS` | `http://localhost:5173,http://127.0.0.1:5173` | origens que podem chamar a API diretamente (acesso entre origens) |
| `DB_USERNAME` | `sa` | usuário do H2 |
| `DB_PASSWORD` | (vazio) | senha do H2 |
| `VITE_API_BASE_URL` (front-end) | `/api` | URL base da API usada pela interface (no Docker, definida no build) |

## Estrutura do repositório

```text
backend/                          API Spring Boot (projeto Maven)
└── src/main/java/br/com/bomjesus/academic/
    ├── config/        CORS, normalização de texto do JSON, dados de demonstração
    ├── controller/    StudentController, ProfessorController
    ├── dto/           records de entrada e saída, com Bean Validation
    ├── exception/     GlobalExceptionHandler (RFC 9457) e exceções de domínio
    ├── mapper/        mappers MapStruct (entidade ↔ DTO)
    ├── model/         Person (abstrata), Student, Professor, Address, Status
    ├── repository/    repositórios Spring Data JPA
    └── service/       regras de negócio
frontend/                         interface web (projeto Vite)
└── src/
    ├── views/         telas: listas e formulários de alunos e professores
    ├── components/    tabela, formulário, endereços, diálogo de confirmação etc. (reutilizáveis)
    ├── composables/   lógica de lista, formulário e notificações
    ├── services/      única instância Axios + services de alunos e professores
    ├── types/         tipos espelhados do contrato da API
    └── utils/         validações (mesmas regras da API), formatação de moeda, foco
docs/                             diagrama do modelo de domínio
scripts/                          seed-volume.sh (carga de volume para medir desempenho)
specs/                            especificação, plano, contratos e tarefas (Spec Kit)
docker-compose.yml                orquestração dos containers
```

## Roteiro para os vídeos

Os dois vídeos devem ter de 2 a 5 minutos. Os tempos abaixo somam cerca de 4 minutos e meio cada.

**Antes de gravar:**

- suba o sistema com `SPRING_PROFILES_ACTIVE=demo docker compose up --build`;
- confira se as fotos de demonstração (`i.pravatar.cc`) carregam. Se não carregarem, a interface mostra a
  imagem padrão e o roteiro segue igual.

### Vídeo de produto (demonstração para o usuário final)

| Tempo | Bloco | O que mostrar em <http://localhost:8081> |
|-------|-------|-------------------------------------------|
| 0:00–0:20 | Abertura | O que é o sistema: cadastro de alunos e professores da instituição |
| 0:20–0:45 | Navegação | Abas Alunos/Professores; filtro Todos / Ativos / Inativos |
| 0:45–1:45 | Cadastro de aluno | "Novo aluno" → salvar vazio (erros em cada campo) → preencher com 2 endereços e foto → usar o e-mail `ana.souza@escola.com` (conflito mostrado no campo) → corrigir → sucesso |
| 1:45–2:15 | Edição | Editar o aluno: trocar o telefone e remover um endereço |
| 2:15–2:45 | Desativar | "Desativar" → confirmar → status "Inativo"; mostrar que a edição permite reativar |
| 2:45–3:15 | Excluir | "Excluir" → aviso de ação definitiva → "Excluir definitivamente" |
| 3:15–3:45 | Professores | Cadastrar professor com salário "8.500,00"; lista em reais e ordem alfabética |
| 3:45–4:15 | Dados | Console H2 (<http://localhost:8080/h2-console>): tabelas `PERSON`, `STUDENT`, `PROFESSOR`, `ADDRESS` |
| 4:15–4:30 | Encerramento | Recapitular: listar, cadastrar, editar, desativar e excluir |

### Vídeo técnico (para desenvolvedores)

| Tempo | Bloco | O que mostrar |
|-------|-------|---------------|
| 0:00–0:30 | Visão geral | Objetivo, diagrama em `docs/` e processo guiado por especificação (`specs/`: constituição, spec, plano, tarefas) |
| 0:30–1:10 | Modelagem | `Person` abstrata com `JOINED`; `Address` como composição (`cascade` + `orphanRemoval`, `replaceAddresses`); e-mail único entre alunos e professores |
| 1:10–1:40 | Camadas | Pacotes `controller` → `service` → `repository`; DTOs `record` com Bean Validation; MapStruct |
| 1:40–2:05 | Erros | `GlobalExceptionHandler` com RFC 9457: mostrar respostas 400 (campos) e 409 |
| 2:05–2:40 | Testes | Tipos de teste; `./mvnw clean verify` com o gate do JaCoCo; relatório em `target/site/jacoco`; Vitest |
| 2:40–3:10 | Containers | Dockerfiles multi-stage sem root; `docker-compose.yml` com healthcheck; Nginx repassando `/api` (sem CORS) e CORS no acesso direto |
| 3:10–3:40 | Front-end | Vue 3 + TypeScript: `services` (Axios único), `composables`, componentes reutilizáveis, validação igual à da API |
| 3:40–4:20 | Como rodar | Pré-requisitos; `docker compose up --build`; `./mvnw spring-boot:run`; `npm run dev` |

## Entrega

| Item | Link |
|------|------|
| Repositório público | _(adicionar o link do GitHub/GitLab)_ |
| Vídeo técnico (YouTube, não listado) | _(adicionar o link)_ |
| Vídeo de demonstração do produto (YouTube, não listado) | _(adicionar o link)_ |
