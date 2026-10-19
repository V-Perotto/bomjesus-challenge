# Research — Gestão de Alunos e Professores

**Feature**: `002-students-professors-crud` | **Date**: 2026-10-19 | **Plan**: [plan.md](plan.md)

Cada decisão segue o formato: **Decision**, **Rationale**, **Alternatives considered**. Versões
foram verificadas no Maven Central e no npm em 2026-10-19.

## R1. Versão do Java

- **Decision**: Java **21 LTS** (`java.version=21`), imagens `eclipse-temurin:21`.
- **Rationale**: atende ao "17+" do Princípio I, é o JDK instalado na máquina de desenvolvimento
  (OpenJDK 21.0.12) e é LTS suportado pelo Spring Boot 4.1.
- **Alternatives considered**: Java 17 (mínimo da constituição, sem ganho); Java 25 (não instalado
  localmente).

## R2. Versão do Spring Boot e impacto no JUnit

- **Decision**: **Spring Boot 4.1.1** (GA de 2026-08-20). Versões gerenciadas relevantes: Hibernate
  7.4.5, Jackson 3.1.5, JUnit Jupiter 6.0.3, Mockito 5.23.0, H2 2.4.240, AssertJ 3.27.7.
- **Rationale**: é a linha atual e mantida. A linha 3.5 teve a última versão gratuita (3.5.16) em
  2026-06-25. O JUnit Jupiter 6 mantém a API do JUnit 5; a constituição foi emendada para a v1.1.1
  ("JUnit Jupiter 5 ou superior"), com aprovação do usuário.
- **Alternatives considered**: Spring Boot 3.5.16 (JUnit 5.12, Jackson 2), mas sem novas versões
  gratuitas; Boot 4.1 forçando JUnit 5 (contraria o BOM e arrisca incompatibilidades).
- **Boot 4 — pontos que a implementação deve respeitar** (artefatos verificados em 4.1.1):

  | Tema | Boot 4.1.1 |
  |------|------------|
  | Starter web | `spring-boot-starter-webmvc` |
  | Console H2 | módulo `spring-boot-h2console` (não vem mais no starter web) |
  | Testes web | `spring-boot-starter-webmvc-test`; `@WebMvcTest` em `org.springframework.boot.webmvc.test.autoconfigure` |
  | Testes JPA | `spring-boot-starter-data-jpa-test`; `@DataJpaTest` em `org.springframework.boot.data.jpa.test.autoconfigure` |
  | Mocks em contexto | `@MockitoBean` (`org.springframework.test.context.bean.override.mockito`) |
  | JSON | Jackson 3 (`tools.jackson.*`); `@JacksonComponent` e `JsonMapperBuilderCustomizer` (`org.springframework.boot.jackson`) |

## R3. Build e Maven Wrapper

- **Decision**: o back-end é gerado pelo Spring Initializr (Boot 4.1.1, Java 21, Maven), que já
  traz o Maven Wrapper; todo comando usa `./mvnw`. O `pom.xml` é ajustado para as dependências da
  seção "Primary Dependencies" do plano.
- **Rationale**: o Maven não está instalado na máquina, então o wrapper é necessário para o
  build local e o build Docker serem reproduzíveis.
- **Alternatives considered**: instalar o Maven globalmente (não reproduzível para avaliadores);
  gerar o wrapper via container `maven:3.9` (passo extra).

## R4. Herança JPA

- **Decision**: `Person` como `@Entity` **abstrata** com `@Inheritance(strategy = JOINED)`;
  `Student` e `Professor` como `@Entity` com tabelas próprias ligadas pela PK (`id`). Sem coluna
  discriminadora.
- **Rationale**: decisão do usuário (clarificação de 2026-10-19). O banco garante NOT NULL em
  `student_number`/`salary`, unicidade de e-mail em uma única tabela (`person`) e uma única FK de
  `address` para `person`. Cada rota consulta só o seu subtipo, então o custo é um JOIN.
- **Alternatives considered**: `SINGLE_TABLE` (colunas de subtipo anuláveis);
  `TABLE_PER_CLASS` (sem unicidade entre tipos, FK de endereço impossível, sem `IDENTITY`);
  `@MappedSuperclass` (proibido pelo Princípio VII).

## R5. Associação `Person` 1 → 1..* `Address`

- **Decision**: associação bidirecional.
  - Em `Person`: `@OneToMany(mappedBy = "person", cascade = ALL, orphanRemoval = true)`,
    `@OrderBy("id ASC")`, `List<Address> addresses = new ArrayList<>()`.
  - Em `Address`: `@ManyToOne(fetch = LAZY, optional = false)` com
    `@JoinColumn(name = "person_id", nullable = false)`.
  - `Person` expõe `replaceAddresses(Collection<Address>)`, que limpa a lista existente e adiciona
    as novas ajustando a referência de volta. A coleção **nunca é reatribuída**: trocar a
    referência gera o erro do Hibernate "collection with cascade=all-delete-orphan was no longer
    referenced".
- **Rationale**: o endereço é composição (Princípio VII); a substituição completa no `PUT` foi
  decidida na clarificação; o `orphanRemoval` apaga os endereços removidos, e a cascata os apaga
  junto com a pessoa.
- **Alternatives considered**: `@OneToMany` unidirecional com `@JoinColumn` (gera UPDATEs extras
  e não deixa o endereço navegar até a pessoa); `@ElementCollection` (endereço vira valor
  embutido, perdendo o mapeamento como entidade do diagrama).

## R6. Carregamento e consultas (N+1)

- **Decision**: `spring.jpa.open-in-view=false`. Listagens e busca por id usam
  `@EntityGraph(attributePaths = "addresses")`. As consultas não ordenam: o Service ordena a lista
  em memória com `Collator` pt-BR (força `PRIMARY`, que ignora maiúsculas e acentos) e desempata
  por `id` (BR-08). Como a lista não é paginada, ordenar 1.000 itens em memória é trivial. Todo
  mapeamento entidade → DTO acontece dentro do `@Transactional` do Service.
- **Rationale**: evita 1 consulta extra por pessoa (SC-004 com 1.000 registros); só há uma
  coleção por entidade, então o fetch join de `List` não gera `MultipleBagFetchException`.
- **Alternatives considered**: `FetchType.EAGER` (consultas imprevisíveis);
  `@BatchSize` (melhora, mas ainda faz várias consultas); paginação (fora do escopo);
  `ORDER BY lower(name)` no banco (ordena pelo código dos caracteres e poria "Álvaro" depois de
  "Zé").

## R7. Unicidade e conflitos (409)

- **Decision**:
  - Normalização no Service antes de validar a unicidade: `emailAddress` em minúsculas e
    `studentNumber` em maiúsculas.
  - Restrições no banco: `uk_person_email_address` em `person.email_address` e
    `uk_student_student_number` em `student.student_number`.
  - Pré-checagem no Service: na criação, `existsByEmailAddress`; na atualização,
    `existsByEmailAddressAndIdNot`. Em caso de duplicidade, lança `ConflictException`, que carrega
    o campo e vira **409** com `errors[{field, message}]`.
  - `DataIntegrityViolationException` também vira **409**, como rede de segurança para
    gravações simultâneas.
- **Rationale**: a mensagem fica clara e associada ao campo, que a interface exibe junto ao campo
  (FR-022). O banco continua sendo a garantia final.
- **Pitfall**: não usar `existsByEmailAddressAndIdNot(email, null)` na criação. Em SQL,
  `id <> NULL` é desconhecido, e o método retornaria `false` sempre.
- **Pitfall 2**: checar a unicidade **antes** de alterar a entidade gerenciada. Caso contrário, o
  auto-flush do Hibernate grava o valor duplicado antes da consulta `exists...`, e a resposta vira
  um 409 genérico, sem o campo.
- **Alternatives considered**: confiar só na restrição do banco (mensagem genérica, sem campo);
  colunas com collation case-insensitive (dependente do banco).

## R8. DTOs e validação

- **Decision**:
  - DTOs como `record`s separados por direção: `AddressRequest`/`AddressResponse`,
    `StudentRequest`/`StudentResponse`, `ProfessorRequest`/`ProfessorResponse`.
  - Bean Validation nos records de entrada, incluindo `@NotEmpty @Valid List<AddressRequest>`.
  - Mensagens em pt-BR em `ValidationMessages.properties` (UTF-8, chaves explícitas), que
    independem do locale do container.
  - Regras de formato conforme o [data-model.md](data-model.md).
- **Rationale**: Princípio II (DTOs, records, Bean Validation). Com chaves explícitas, as
  mensagens saem em português mesmo em containers com locale `en`.
- **Alternatives considered**: mensagens inline nas anotações (espalhadas pelo código); mensagens
  padrão do Hibernate Validator (dependem do locale).

## R9. Mapeamento Entity ↔ DTO

- **Decision**: **MapStruct 1.6.3** (escolha do usuário).
  - Processador declarado em `annotationProcessorPaths` do `maven-compiler-plugin`.
  - `@MapperConfig` central com `componentModel = SPRING`, `injectionStrategy = CONSTRUCTOR` e
    `unmappedTargetPolicy = ERROR`.
  - Mappers: `AddressMapper`, `StudentMapper(uses = AddressMapper)` e
    `ProfessorMapper(uses = AddressMapper)`.
  - Os mappers não tocam em `id` nem em `addresses` na atualização. O Service chama
    `replaceAddresses(addressMapper.toEntities(...))`.
  - Na criação, `status` nulo vira `ACTIVE` (`defaultValue`). Na atualização, `status` nulo
    mantém o valor atual (`NullValuePropertyMappingStrategy.IGNORE` apenas para `status`).
- **Rationale**: o MapStruct gera os mappers em tempo de compilação, sem reflexão em runtime. Com
  `unmappedTargetPolicy = ERROR`, qualquer campo esquecido quebra o build. A dependência fica
  justificada no plano (Princípio I).
- **Alternatives considered**: mappers manuais (eram a recomendação; o usuário preferiu
  MapStruct); ModelMapper (reflexão em runtime e erros só em execução).

## R10. Normalização de texto (FR-014)

- **Decision**: um `@JacksonComponent` com um deserializador de `String` que aplica `trim` e
  converte texto vazio em `null` em todo corpo JSON. A normalização de caixa (e-mail e matrícula)
  fica no Service (R7).
- **Rationale**: há um único ponto de normalização, que vale para todos os DTOs. Como
  `@JacksonComponent` é incluído automaticamente nos testes `@WebMvcTest`, os testes de
  Controller enxergam o mesmo comportamento da aplicação. Convertendo texto vazio em `null`:
  - os campos obrigatórios falham em `@NotBlank`/`@NotNull`;
  - os opcionais (`photo`) passam a ser simplesmente "não informados".
- **Alternatives considered**: `trim` no construtor compacto de cada record (repetitivo e fácil
  de esquecer); `@InitBinder`/`StringTrimmerEditor` (não se aplica a corpos JSON).

## R11. Tratamento global de erros

- **Decision**: `GlobalExceptionHandler`, anotado com `@RestControllerAdvice` e estendendo
  `ResponseEntityExceptionHandler`, retorna `ProblemDetail` (RFC 9457,
  `application/problem+json`) com `type`, `title` (pt-BR), `status`, `detail`, `instance`
  (caminho), `timestamp` e, quando aplicável, `errors[{field, message}]`.

  | Exceção | Status | Observação |
  |---------|--------|------------|
  | `MethodArgumentNotValidException` | 400 | erros por campo, inclusive `addresses[0].city` |
  | `HttpMessageNotReadableException` | 400 | JSON malformado ou tipo errado; se a causa for `InvalidFormatException` (ex.: `status: "FOO"`, `salary: "abc"`), o erro é associado ao campo |
  | `MethodArgumentTypeMismatchException` | 400 | id não numérico, `?status=` inválido |
  | `ResourceNotFoundException` | 404 | — |
  | `ConflictException` | 409 | campo + mensagem |
  | `DataIntegrityViolationException` | 409 | mensagem genérica de conflito |
  | `HttpRequestMethodNotSupportedException` | 405 | herdado, com título em pt-BR |
  | `HttpMediaTypeNotSupportedException` | 415 | herdado, com título em pt-BR |
  | `Exception` | 500 | mensagem genérica, log `ERROR` via SLF4J |

- **Rationale**: Princípio II e FR-017/FR-018. Estender a classe base garante o mesmo formato para
  os erros do próprio framework (405, 415).
- **Alternatives considered**: um DTO de erro próprio (reinventa a RFC 9457); deixar
  `spring.mvc.problemdetails.enabled` cuidar sozinho (sem `errors` nem títulos em pt-BR).

## R12. Desenho de Services e Controllers

- **Decision**:
  - Services: classes concretas `StudentService`, `ProfessorService` e `EmailUniquenessGuard`,
    esta última compartilhada pelos dois para checar e-mail entre todas as pessoas. Sem
    interfaces.
  - Controllers: `StudentController` (`/api/students`) e `ProfessorController`
    (`/api/professors`).
    - Respostas montadas com `ResponseEntity`.
    - `POST` devolve `created(Location)`, montado com `ServletUriComponentsBuilder`.
    - `DELETE` devolve `noContent()`.
    - O parâmetro `status` usa `@RequestParam(required = false) Status`.
  - O `salary` é normalizado para escala 2 (`setScale(2)`) antes de gravar, para a resposta
    imediata já sair com duas casas decimais.
- **Rationale**: Princípio III (camadas e injeção por construtor) e YAGNI: interfaces com uma
  única implementação não trazem ganho, e o Mockito consegue mockar classes concretas.
- **Alternatives considered**: um `PersonService` genérico (abstração sem necessidade, contraria
  o YAGNI); par interface + `Impl` (cerimônia extra).

## R13. CORS e configuração

- **Decision**: `CorsConfig` (`WebMvcConfigurer`) para `/api/**` com as origens lidas de
  `app.cors.allowed-origins` (padrão `http://localhost:5173,http://127.0.0.1:5173`,
  sobrescrevível por `APP_CORS_ALLOWED_ORIGINS`). Métodos `GET/POST/PUT/DELETE`, header
  `Location` exposto, sem `*`. O Vite usa `server.strictPort: true` para não trocar de porta.
  Os testes de Controller cobrem a pré-checagem (200) e a origem recusada (403), e a quickstart
  exercita o modo de acesso direto (`VITE_API_BASE_URL=http://localhost:8080/api`).
- **Rationale**: no fluxo padrão o front-end usa o proxy do Vite (desenvolvimento) e do Nginx
  (Docker), ou seja, a mesma origem. O CORS só é necessário quando alguém aponta o
  `VITE_API_BASE_URL` direto para `:8080`.
- **Alternatives considered**: `@CrossOrigin` em cada controller (configuração espalhada);
  `allowedOrigins("*")` (proibido pela constituição).

## R14. Banco H2 e console

- **Decision**:
  - Conexão: `jdbc:h2:mem:bomjesus;DB_CLOSE_DELAY=-1`, `generate-unique-name=false`, usuário
    `${DB_USERNAME:sa}`, senha `${DB_PASSWORD:}`, `ddl-auto=create-drop`.
  - Console via `spring-boot-h2console`: `spring.h2.console.enabled=true`,
    `path=/h2-console` e `settings.web-allow-others=true` (necessário para acessar o container
    pelo host).
  - Testes: `src/test/resources/config/application.properties` sobrescreve a URL com
    `jdbc:h2:mem:test-${random.uuid}` (um banco por contexto), e os testes de integração limpam
    a base no `@BeforeEach`. Sem isso, o contexto do perfil `demo` contaminaria os demais.
- **Rationale**: FR-027 (banco em memória e console local e em container). A URL fixa permite que
  o README documente a JDBC URL.
- **Alternatives considered**: console só no perfil de desenvolvimento (a spec pede no container
  também).

## R15. Dados de demonstração (FR-028)

- **Decision**: `DemoDataLoader` (`ApplicationRunner`, `@Profile("demo")`) cria, pelos próprios
  Services, 3 alunos e 2 professores: um aluno `DISABLE`, uma pessoa com 2 endereços, e fotos em
  URLs públicas. É ativado com `SPRING_PROFILES_ACTIVE=demo`, tanto local quanto no
  `docker compose`.
- **Rationale**: o banco continua iniciando vazio por padrão (FR-027). Passar pelos Services
  garante que os dados de demonstração respeitam as validações. Os testes nunca ativam o perfil.
- **Alternatives considered**: `data.sql` (passa ao largo das validações e conflita com
  `create-drop`/JOINED); script de `curl` (depende de shell e da API já no ar).

## R16. Estratégia de testes do back-end

- **Decision**:

  | Camada | Ferramenta | Escopo |
  |--------|------------|--------|
  | Service | JUnit Jupiter + Mockito (`@ExtendWith(MockitoExtension.class)`) | sucesso e cada falha (404, 409, normalização, substituição de endereços, status padrão, ordenação BR-08) |
  | Controller | `@WebMvcTest` + MockMvc + `@MockitoBean` | status, corpo, `Location`, `ProblemDetail`, 400/404/409, `?status=` inválido |
  | Mapper | JUnit (instâncias geradas `*MapperImpl`) | campos, `defaultValue` de status, endereços |
  | Repository | `@DataJpaTest` | tabelas JOINED, unique constraints, `orphanRemoval`, cascata no delete, filtro por status, `@EntityGraph` |
  | Integração | `@SpringBootTest` + MockMvc | fluxo criar → buscar → atualizar → excluir de cada recurso |

  - JaCoCo 0.8.15: goal `check` na fase `verify`, regra `PACKAGE` com cobertura de `LINE` ≥ 0.80
    para `*.service` e `*.controller`.
  - Nomes dos métodos em inglês (`shouldReturn404WhenStudentDoesNotExist`) e `@DisplayName` em
    pt-BR.
  - Massa de dados em `AddressTestData`, `StudentTestData` e `ProfessorTestData`: um arquivo
    por tipo, para que US1 e US2 possam avançar em paralelo.
  - `ControllerArchitectureTest` (reflexão, sem dependência nova): nenhum método de controller
    expõe tipos `@Entity`.
- **Rationale**: Princípio IV. As camadas de Service e Controller levam o gate de cobertura; os
  testes de repositório comprovam o Princípio VII.
- **Alternatives considered**: Testcontainers (desnecessário com H2); testar apenas pela
  integração (perde o isolamento exigido).

## R17. Front-end

- **Decision**: projeto gerado com `create-vue` 3.24 (TypeScript, Vue Router, Vitest, ESLint,
  Prettier; sem Pinia, JSX ou E2E), que define versões compatíveis entre si: Vue 3.5, Vite 8,
  Vue Router 5, TypeScript ~6.0, vue-tsc 3.3, Vitest 4. A esse projeto se acrescenta o Axios 1.20.
  - **TypeScript** (escolha do usuário): tipos em `src/types/` espelham os DTOs do
    [contrato OpenAPI](contracts/openapi.yaml), e o `vue-tsc` checa os tipos no build.
    O TypeScript 7 (7.0.2) ainda não é suportado pelo `vue-tsc`, por isso fica o ~6.0 definido
    pelo `create-vue`.
  - **Navegação**: rotas do Vue Router apresentadas como abas no cabeçalho ("Alunos" e
    "Professores"), com links diretos e o botão voltar funcionando.
  - **Reuso**: `PersonTable` (colunas configuráveis + slots), `PersonForm` (campos comuns + slot
    para os campos do subtipo), `AddressFieldset`, `ConfirmDialog` (`<dialog>` nativo, variante
    reforçada), `StatusBadge`, `StatusFilter`, `PhotoAvatar` (imagem padrão quando o link falha),
    `ToastStack`, `EmptyState`, `ErrorState`, `LoadingState`.
  - **HTTP**: instância Axios em `services/http.ts` com `baseURL = VITE_API_BASE_URL ?? '/api'`.
    Um interceptor converte o `ProblemDetail` em `ApiError` (`status`, `title`, `detail`,
    `fieldErrors`, `isNetworkError`), e os composables traduzem os `fieldErrors` para os campos
    do formulário.
  - **Visual**: CSS próprio com variáveis (tokens), sem biblioteca de componentes. Layout
    responsivo a partir de 360 px; rótulos em todos os campos, foco visível, `aria-live` nos
    toasts.
  - **Desativar**: o front-end envia um `PUT` com os dados atuais da pessoa (a listagem já traz
    tudo, inclusive endereços) e `status: "DISABLE"`.
  - **Testes**: Vitest + Vue Test Utils para `validators`, `formatters`, a conversão de
    `ApiError` e os composables. São opcionais (Princípio IV) e ficam fora do gate.
- **Rationale**: Princípio VIII. O `create-vue` é o scaffolder oficial e elimina a escolha manual
  de versões compatíveis.
- **Alternatives considered**: Vuetify, PrimeVue ou Element Plus (dependência pesada para uma SPA
  simples); abas sem router (sem links diretos); JavaScript (era a recomendação; o usuário
  preferiu TypeScript).

## R18. Containers

- **Decision**:
  - **`backend/Dockerfile`** (multi-stage):
    - Build com `eclipse-temurin:21-jdk-alpine`: copia `mvnw`, `.mvn/` e `pom.xml`, roda
      `./mvnw -B dependency:go-offline` (cache de camadas), copia `src/` e roda
      `./mvnw -B package -DskipTests`.
    - Extração em camadas: `java -Djarmode=tools -jar app.jar extract --layers`.
    - Runtime com `eclipse-temurin:21-jre-alpine`, usuário não-root `spring`, `EXPOSE 8080` e
      `JAVA_OPTS` padrão `-XX:MaxRAMPercentage=75`.
  - **`frontend/Dockerfile`** (multi-stage): `node:24-alpine` (`npm ci` + `npm run build`), com
    runtime em `nginxinc/nginx-unprivileged:stable-alpine` (não-root, porta 8080). O
    `nginx.conf` faz o fallback da SPA (`try_files ... /index.html`) e o
    `location /api/ { proxy_pass http://backend:8080; }`.
  - **`docker-compose.yml`** na raiz:
    - `backend`: porta `8080:8080`, healthcheck com
      `wget -qO- http://localhost:8080/api/students`.
    - `frontend`: porta `8081:8080`, `depends_on: backend (service_healthy)`.
    - Variáveis repassadas: `SPRING_PROFILES_ACTIVE` e `APP_CORS_ALLOWED_ORIGINS`.
    - Comando único: `docker compose up --build`.
  - `.dockerignore` em `backend/` e em `frontend/`.
- **Rationale**: Princípio V e clarificação Q3 (pastas irmãs, duas imagens e compose). A interface
  e a API ficam na mesma origem via Nginx, então o CORS não é necessário no Docker. Atende à
  SC-006 (≤ 2 comandos).
- **Alternatives considered**: imagem única com o Spring servindo o front-end (rejeitada na
  clarificação); `nginx:alpine` padrão (roda como root na porta 80); JAR inteiro sem camadas
  (rebuilds mais lentos).

## R19. Documentação e entrega

- **Decision**: o README na raiz segue o Princípio VI e a FR-030/FR-031, incluindo a seção
  "Roteiro para os vídeos". O diagrama é copiado para `docs/modelagem-bomjesus.png`. O
  repositório ainda não é git: a inicialização e a publicação (repositório público, exigido pelo
  desafio) ficam como passo de entrega do usuário.
- **Rationale**: o desafio avalia README, vídeos e repositório público.
- **Alternatives considered**: documentação via Swagger UI (dependência extra não pedida; o
  contrato fica em `contracts/openapi.yaml` e os exemplos no README).
