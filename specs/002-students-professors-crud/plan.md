# Implementation Plan: Gestão de Alunos e Professores (API REST + Interface Web)

**Branch**: `002-students-professors-crud` | **Date**: 2026-10-19 | **Spec**: [spec.md](spec.md)

**Input**: Feature specification from `specs/002-students-professors-crud/spec.md`

## Summary

API REST em Spring Boot 4.1.1 (Java 21) com CRUD completo de **alunos** (`/api/students`) e
**professores** (`/api/professors`), modelados como subtipos da entidade abstrata `Person`.
Pontos centrais do modelo:

- **Herança**: JPA `JOINED`.
- **Endereços**: composição `1..*`, com cascata total e remoção de órfãos.
- **Status**: `ACTIVE`/`DISABLE`; a desativação é feita por `PUT` e a exclusão (`DELETE`) é
  definitiva.

Em volta do modelo:

- DTOs em `record` validados com Bean Validation e convertidos por **MapStruct**.
- Erros padronizados em RFC 9457 (`ProblemDetail`) com mensagens em pt-BR.
- Persistência em H2 em memória, com console habilitado.
- Testes com JUnit Jupiter 6 + Mockito e gate JaCoCo de 80%.

A interface é uma SPA em **Vue 3 + TypeScript + Vite + Axios**, com abas roteadas para alunos e
professores e componentes reutilizáveis de tabela, formulário e endereços. A entrega roda em duas
imagens Docker (API e Nginx com a SPA) orquestradas por `docker compose`. As decisões estão em
[research.md](research.md).

## Technical Context

**Language/Version**: Java 21 (LTS) no back-end; TypeScript ~6.0 no front-end (versão do
`create-vue` 3.24).

**Primary Dependencies**:

- **Back-end** — Spring Boot 4.1.1:
  - `spring-boot-starter-webmvc`, `spring-boot-starter-data-jpa`, `spring-boot-starter-validation`
    e `spring-boot-h2console`;
  - H2 2.4.240 (gerenciada pelo Boot);
  - MapStruct 1.6.3 (+ `mapstruct-processor`).
- **Front-end**: Vue 3.5, Vue Router 5, Axios 1.20 e Vite 8.

**Storage**: H2 em memória (`jdbc:h2:mem:bomjesus`), `ddl-auto=create-drop`, com console em
`/h2-console`.

**Testing**:

- **Back-end**: JUnit Jupiter 6.0.3, Mockito 5.23, AssertJ, `spring-boot-starter-test`,
  `spring-boot-starter-webmvc-test` (MockMvc), `spring-boot-starter-data-jpa-test`, e JaCoCo
  0.8.15 com gate de 80% de linhas em `service` e `controller`.
- **Front-end** (opcional, fora do gate): Vitest 4 + Vue Test Utils.

**Target Platform**: containers Linux (`eclipse-temurin:21-jre-alpine` e
`nginxinc/nginx-unprivileged:stable-alpine`); execução local em qualquer SO com JDK 21 e Node 24.

**Project Type**: aplicação web (API REST + SPA) em repositório único, com `backend/` e
`frontend/` lado a lado.

**Performance Goals**: cada operação em menos de 1 s localmente com 1.000 alunos + 1.000
professores (SC-004). Listagens usam uma única consulta com fetch dos endereços, sem N+1.

**Constraints**:

- API sem autenticação e sem paginação.
- Banco volátil; o perfil `demo` carrega dados de demonstração.
- Mensagens em pt-BR; identificadores em inglês, como no modelo.
- Maven não instalado localmente: todo build usa `./mvnw`.

**Scale/Scope**:

- 2 recursos REST (10 operações) e 5 tipos de domínio.
- Cerca de 6 telas de SPA: 2 listas, 2 formulários, a página de não encontrado e o cabeçalho com
  as abas.
- 2 Dockerfiles + 1 compose.
- Volume esperado: alguns milhares de registros.

**Dependências adicionais justificadas** (Princípios I e VIII):

| Dependência | Justificativa |
|-------------|---------------|
| MapStruct 1.6.3 | escolha do usuário; mappers gerados em tempo de compilação, sem reflexão em runtime; `unmappedTargetPolicy = ERROR` quebra o build se faltar mapear algum campo (research R9) |
| `spring-boot-h2console` | no Boot 4, o console H2 (FR-027) virou um módulo separado (research R2) |
| TypeScript (+ `vue-tsc`) | escolha do usuário; tipa os DTOs da API em `src/types/` e checa os tipos no build (research R17) |
| Vue Router | rotas e abas exigidas pelo Princípio VIII (`router/`) e por US4 |
| Vitest + Vue Test Utils | testes opcionais de front-end permitidos pelo Princípio IV |

## Constitution Check

*GATE: Must pass before Phase 0 research. Re-check after Phase 1 design.*

| # | Princípio | Gate | Pré-pesquisa | Pós-design | Evidência |
|---|-----------|------|--------------|------------|-----------|
| I | Stack Back-end | Java ≥ 17, Spring Boot ≥ 3, Spring Data JPA, H2 em memória, Maven (wrapper), código em `backend/`, extras justificados | ✅ | ✅ | Technical Context; tabela de dependências; R1–R3 |
| II | API RESTful + DTOs | substantivos em inglês no plural; 200/201+Location/204/400/404/409/500; `ResponseEntity`; DTOs `record`; sem `id` na entrada; tratador global único; erro RFC 9457 | ✅ | ✅ | [openapi.yaml](contracts/openapi.yaml); R11, R12 |
| III | Clean Code e camadas | pacotes `controller/service/repository/model/dto` (+ `exception/mapper/config`); Controller → Service → Repository; injeção por construtor; conversão em `mapper` | ✅ | ✅ | Project Structure; R12; [data-model.md](data-model.md) |
| IV | Testes (não negociável) | JUnit Jupiter (≥ 5) + Mockito; todo método público de Service e todo endpoint testados; JaCoCo ≥ 80% em `service`/`controller`; testes de Student, Professor e Address | ✅ (após emenda 1.1.1) | ✅ | R16; Test layout abaixo |
| V | Docker | `backend/Dockerfile` multi-stage, otimizado, não-root, `.dockerignore`; front-end em imagem separada + `docker-compose.yml` na raiz; comando único | ✅ | ✅ | R18; quickstart §6 |
| VI | README | seções 1–9 do Princípio VI + diagrama em `docs/` + roteiro dos vídeos | ✅ | ✅ | R19; Etapa 9 |
| VII | Modelagem de domínio | `Person` `@Entity` abstrata + `@Inheritance(JOINED)`; `Status` STRING; `Address` com `cascade = ALL` + `orphanRemoval`; `salary` `BigDecimal`; nomes do modelo | ✅ | ✅ | R4, R5; [data-model.md](data-model.md) |
| VIII | Front-end Vue | Vue 3 + Composition API (`<script setup>`) + Vite + Axios; `frontend/` com `views/components/services/composables/router`; Axios só em `services/`; `VITE_API_BASE_URL`; feito depois do back-end | ✅ | ✅ | R17; [frontend-ui.md](contracts/frontend-ui.md) |

**Resultado**: nenhuma violação, e o Complexity Tracking não é necessário. A constituição foi
emendada de 1.1.0 para 1.1.1 (PATCH aprovado pelo usuário em 2026-10-19), permitindo o JUnit
Jupiter 6 gerenciado pelo Boot 4.1.

## Project Structure

### Documentation (this feature)

```text
specs/002-students-professors-crud/
├── spec.md              # Especificação (clarificada)
├── plan.md              # Este arquivo
├── research.md          # Fase 0 — decisões técnicas R1–R19
├── data-model.md        # Fase 1 — entidades, DTOs, regras, repositórios, mappers
├── quickstart.md        # Fase 1 — guia de validação ponta a ponta
├── contracts/
│   ├── openapi.yaml     # Fase 1 — contrato REST (10 operações)
│   └── frontend-ui.md   # Fase 1 — contrato de telas da SPA
├── checklists/
│   └── requirements.md  # Checklist de qualidade da spec
└── tasks.md             # Fase 2 — gerado por /speckit-tasks (não por este comando)
```

### Source Code (repository root)

```text
backend/
├── .mvn/wrapper/  mvnw  mvnw.cmd  pom.xml
├── Dockerfile  .dockerignore
└── src/
    ├── main/
    │   ├── java/br/com/bomjesus/academic/
    │   │   ├── AcademicApplication.java
    │   │   ├── config/        # CorsConfig, TrimmingStringDeserializer (@JacksonComponent), DemoDataLoader (@Profile("demo"))
    │   │   ├── controller/    # StudentController, ProfessorController
    │   │   ├── dto/           # AddressRequest/Response, StudentRequest/Response, ProfessorRequest/Response
    │   │   ├── exception/     # ResourceNotFoundException, ConflictException, GlobalExceptionHandler
    │   │   ├── mapper/        # CentralMapperConfig, AddressMapper, StudentMapper, ProfessorMapper
    │   │   ├── model/         # Person (abstract), Student, Professor, Address, Status
    │   │   ├── repository/    # PersonRepository, StudentRepository, ProfessorRepository
    │   │   └── service/       # StudentService, ProfessorService, EmailUniquenessGuard
    │   └── resources/
    │       ├── application.properties
    │       └── ValidationMessages.properties
    └── test/java/br/com/bomjesus/academic/
        ├── controller/        # StudentControllerTest, ProfessorControllerTest (@WebMvcTest)
        ├── service/           # StudentServiceTest, ProfessorServiceTest, EmailUniquenessGuardTest (Mockito)
        ├── mapper/            # AddressMapperTest, StudentMapperTest, ProfessorMapperTest
        ├── repository/        # StudentRepositoryTest, ProfessorRepositoryTest (@DataJpaTest)
        ├── config/            # DemoDataLoaderTest, NoDemoDataTest
        ├── architecture/      # ControllerArchitectureTest (nenhuma @Entity em controllers)
        ├── support/           # AddressTestData, StudentTestData, ProfessorTestData
        ├── StudentApiIntegrationTest.java     # @SpringBootTest + MockMvc
        └── ProfessorApiIntegrationTest.java   # @SpringBootTest + MockMvc
    (backend/src/test/resources/config/application.properties → H2 com nome aleatório por contexto)

frontend/
├── Dockerfile  .dockerignore  nginx.conf  .env.example
├── index.html  package.json  vite.config.ts  tsconfig*.json  eslint.config.ts  .prettierrc.json
└── src/
    ├── main.ts  App.vue
    ├── router/index.ts
    ├── types/             # person.ts (Status, Address, Student*, Professor*), api.ts (ProblemDetail, ApiError)
    ├── services/          # http.ts (Axios + interceptor), studentService.ts, professorService.ts
    ├── composables/       # usePersonList.ts, usePersonForm.ts, useNotifications.ts
    ├── components/        # AppHeader, PersonTable, PersonForm, AddressFieldset, ConfirmDialog,
    │                      # StatusBadge, StatusFilter, PhotoAvatar, ToastStack, EmptyState,
    │                      # ErrorState, LoadingState
    ├── views/             # StudentListView, StudentFormView, ProfessorListView, ProfessorFormView, NotFoundView
    ├── utils/             # formatters.ts (BRL), validators.ts (regras do data-model)
    ├── assets/            # main.css (tokens CSS), avatar-placeholder.svg
    └── __tests__/         # *.spec.ts (Vitest)

docs/
└── modelagem-bomjesus.png
scripts/
└── seed-volume.sh       # SC-004: 1.000 alunos + 1.000 professores via API
docker-compose.yml
README.md
```

**Structure Decision**: aplicação web com `backend/` (projeto Maven) e `frontend/` (projeto Vite)
lado a lado no mesmo repositório, conforme a clarificação Q3 e os Princípios I, V e VIII. O
back-end usa os pacotes pedidos (`model`, `repository`, `service`, `controller`, `dto`,
`exception`) e acrescenta `mapper` (MapStruct) e `config` (CORS, Jackson, dados de demonstração),
ambos permitidos pelo Princípio III. O front-end alterna entre alunos e professores por rotas,
apresentadas como abas, e reaproveita `PersonTable`, `PersonForm` e `AddressFieldset` nos dois
tipos.

## Estratégia JPA (resumo)

Detalhes completos em [data-model.md](data-model.md) e nas decisões R4–R7.

- **Herança**:
  - `Person` é `@Entity @Inheritance(strategy = InheritanceType.JOINED)` e `abstract`, com
    `@Id @GeneratedValue(strategy = IDENTITY)` na raiz.
  - `Student` e `Professor` são `@Entity` com tabela própria, ligadas pela PK.
  - Sem coluna discriminadora: cada rota consulta só o seu subtipo.
- **Endereços**:
  - Em `Person`: `@OneToMany(mappedBy = "person", cascade = CascadeType.ALL, orphanRemoval = true)`
    e `@OrderBy("id ASC")`.
  - Em `Address`: `@ManyToOne(fetch = LAZY, optional = false) @JoinColumn(name = "person_id")`.
  - `replaceAddresses(...)` limpa e preenche a mesma lista, sem reatribuí-la.
  - O `PUT` substitui a lista inteira, e o `orphanRemoval` apaga os endereços removidos.
  - O `DELETE` da pessoa apaga os endereços pela cascata.
- **Integridade**:
  - `@Enumerated(EnumType.STRING)` em `status`;
  - `BigDecimal` com `precision = 9, scale = 2` em `salary`;
  - unique constraints em `person.email_address` e `student.student_number`;
  - `spring.jpa.open-in-view=false`;
  - `@EntityGraph(attributePaths = "addresses")` em listagens e na busca por id.

## Passos de Implementação

A ordem pedida (Domínio/JPA → Repositórios → Serviços/DTOs → Controllers → Testes → Vue.js →
Dockerfile → README) foi ajustada em dois pontos para cumprir a constituição:

- **Testes junto com cada camada**: os testes são escritos em cada etapa (Princípio IV: nada fica
  pronto sem testes). A etapa "Testes" consolida a integração e o gate de cobertura.
- **Dockerfile dividido**: o Dockerfile do back-end vem **antes** do Vue.js, porque a ordem exigida
  é back-end → containerização → front-end (Fluxo de Desenvolvimento). O do front-end e o compose
  vêm depois do Vue.

| Etapa | Entrega | Testes / verificação | Spec |
|-------|---------|----------------------|------|
| 0. Bootstrap | Projeto do Spring Initializr (Boot 4.1.1, Java 21, wrapper) em `backend/`; `pom.xml` com as dependências do Technical Context, MapStruct (processor) e JaCoCo; `application.properties` (H2, console, `open-in-view=false`, CORS); diagrama copiado para `docs/` | `./mvnw verify` com o teste de contexto padrão | FR-027 |
| 1. Domínio/JPA | `model`: `Status`, `Person` (abstrata, JOINED, `replaceAddresses`), `Student`, `Professor`, `Address` | (coberto na etapa 2) | FR-001–004, VII |
| 2. Repositórios | `PersonRepository`, `StudentRepository`, `ProfessorRepository` (consultas com `@EntityGraph`, `lower(name)`, `exists...`) | `@DataJpaTest`: 3 tabelas JOINED, unique constraints, `orphanRemoval`, cascata no delete, ordenação, filtro por status, `findById` do tipo errado vazio | FR-003, 005, 007, 009 |
| 3. DTOs, mappers e serviços | `dto` (records + validação + `ValidationMessages.properties`); `mapper` (MapStruct); `exception` (`ResourceNotFoundException`, `ConflictException`); `service` (`EmailUniquenessGuard`, `StudentService`, `ProfessorService`: normalização, unicidade, status padrão, substituição de endereços, escala do salário, delete) | testes de mapper; testes unitários de Service com Mockito (sucesso + cada falha) | FR-002–013 |
| 4. Controllers e erros | `StudentController`, `ProfessorController`; `GlobalExceptionHandler` (`ProblemDetail`); `config` (`TrimmingStringDeserializer`, `CorsConfig`) | `@WebMvcTest` por controller: 201 + `Location`, 200, 204, 400 (validação, JSON, id, `?status=`), 404, 409, 405 | FR-013–018 |
| 5. Testes (consolidação) | `StudentApiIntegrationTest` e `ProfessorApiIntegrationTest` (fluxos completos, banco isolado por contexto); `ControllerArchitectureTest`; regra JaCoCo ≥ 80% | `./mvnw clean verify` verde, relatório JaCoCo | SC-002, SC-003, SC-005 |
| 6. Dockerfile do back-end | `backend/Dockerfile` multi-stage + `.dockerignore`; `DemoDataLoader` (`@Profile("demo")`) | `docker build` + `docker run -p 8080:8080`; perfil `demo` carrega 3 + 2 pessoas; console H2 acessível | FR-028, FR-029, V |
| 7. Vue.js | `npm create vue@latest` (TS, Router, Vitest, ESLint, Prettier) + Axios; `types`, `services`, `composables`, componentes reutilizáveis, views e rotas conforme [frontend-ui.md](contracts/frontend-ui.md) | `type-check`, `lint`, `test:unit`, `build`; roteiro da quickstart §5 | US4, FR-019–026 |
| 8. Dockerfile do front-end e compose | `frontend/Dockerfile` (Node → Nginx sem root), `nginx.conf` (fallback SPA + proxy `/api`), `.dockerignore`; `docker-compose.yml` (healthcheck, `depends_on`, portas 8080/8081, `SPRING_PROFILES_ACTIVE`) | `docker compose up --build`; quickstart §6 | FR-029, SC-006 |
| 9. README | README na raiz (seções 1–9 do Princípio VI, diagrama, exemplos `curl` por operação, carga de dados de demonstração, console H2, roteiro dos vídeos de produto e técnico de 2–5 min) | executar todos os comandos do README do zero; Definition of Done da constituição | FR-030, FR-031, SC-001, SC-009 |

**Riscos e mitigação**:

| Risco | Mitigação |
|-------|-----------|
| APIs do Boot 4 / Jackson 3 diferentes do Boot 3 (pacotes de teste, `@JacksonComponent`) | pacotes verificados no artefato 4.1.1 (research R2); compilar logo na etapa 0 |
| Erro "collection ... no longer referenced" no `orphanRemoval` | `replaceAddresses` sem reatribuir a lista; teste em `@DataJpaTest` |
| `existsBy...AndIdNot(x, null)` sempre falso na criação | métodos separados para criação e atualização (R7) e teste unitário específico |
| Mensagens de validação em inglês no container | chaves explícitas em `ValidationMessages.properties` (R8) |
| Cobertura abaixo de 80% | testes por camada desde a etapa 2; gate rodando no `verify` |
| Banco H2 compartilhado entre contextos de teste (o perfil `demo` contamina os demais) | URL de teste com `${random.uuid}` + limpeza no `@BeforeEach` (R14) |
| 409 sem campo no `update` por causa do auto-flush do Hibernate | unicidade checada antes de alterar a entidade (R7) + teste de integração |
| CORS nunca exercitada (desenvolvimento e Docker usam proxy) | testes de pré-checagem e de origem recusada (T038) + passo 9 da quickstart (R13) |
| TypeScript 7 incompatível com `vue-tsc` | manter o ~6.0 definido pelo `create-vue` (R17) |

## Complexity Tracking

Nenhuma violação da constituição a justificar. As dependências adicionais estão justificadas na
tabela do Technical Context, como exigem os Princípios I e VIII.
