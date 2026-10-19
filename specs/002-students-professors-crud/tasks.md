---

description: "Task list — Gestão de Alunos e Professores (API REST + Interface Web)"
---

# Tasks: Gestão de Alunos e Professores (API REST + Interface Web)

**Input**: Design documents from `specs/002-students-professors-crud/`

**Prerequisites**: [plan.md](plan.md), [spec.md](spec.md), [research.md](research.md),
[data-model.md](data-model.md), [contracts/openapi.yaml](contracts/openapi.yaml),
[contracts/frontend-ui.md](contracts/frontend-ui.md), [quickstart.md](quickstart.md)

**Tests**: INCLUÍDOS. São exigidos pelo Princípio IV da constituição (não negociável) e pedidos
(Task 7). Cada teste vem logo após o código que ele cobre: o código é entregue junto com seus
testes, mas não é TDD estrito.

**Organization**: tarefas agrupadas por história de usuário (US1–US4 da spec), na ordem exigida
pela constituição: back-end → containerização → front-end.

## Format: `[ID] [P?] [Story] Description`

- **[P]**: pode rodar em paralelo (arquivos diferentes, sem dependência de tarefa incompleta)
- **[Story]**: história de usuário da tarefa (US1, US2, US3, US4)

## Path Conventions

- `B/` = `backend/src/main/java/br/com/bomjesus/academic/`
- `T/` = `backend/src/test/java/br/com/bomjesus/academic/`
- `R/` = `backend/src/main/resources/`
- `F/` = `frontend/src/`

As descrições usam esses prefixos; o caminho real é o prefixo expandido.

## Mapeamento dos itens pedidos → tarefas

| Item pedido | Tarefas |
|-------------|---------|
| 1. `pom.xml` (Web, Data JPA, H2, Validation) | T001–T003, T007 |
| 2. `application.properties` (H2 + console) | T004 |
| 3. Entidades JPA | T008–T010, T027, T041 |
| 4. Repositórios | T011, T028, T042 |
| 5. DTOs + Services com validação | T015, T018–T022, T029–T031, T033, T035, T043–T045, T047, T049 |
| 6. Controllers + CORS | T014, T016, T017, T037, T051 |
| 7. Testes JUnit Jupiter + Mockito | T023–T026, T032, T034, T036, T038–T040, T046, T048, T050, T052–T054, T056 |
| 8. Front-end Vue.js (Vite + Axios) | T062–T095 |
| 9. Dockerfile + build/execução | T057–T060, T096–T100 |
| 10. README.md em português | T061, T101–T104 |

---

## Phase 1: Setup (Shared Infrastructure)

**Purpose**: projeto Spring Boot criado, configurado e compilando.

- [X] T001 Gerar o back-end em `backend/` pelo Spring Initializr, a partir da raiz do repositório:
  `curl -fsSL "https://start.spring.io/starter.zip?type=maven-project&language=java&bootVersion=4.1.1&javaVersion=21&groupId=br.com.bomjesus&artifactId=academic-api&name=academic-api&packageName=br.com.bomjesus.academic&baseDir=backend&dependencies=web,data-jpa,validation,h2" -o /tmp/academic-api.zip && unzip -q /tmp/academic-api.zip`.
  Se o Initializr recusar `bootVersion=4.1.1`, omita o parâmetro e ajuste a versão do parent na T002.
  Em seguida:
  - renomear a classe principal para `AcademicApplication` em `B/AcademicApplication.java`;
  - renomear o teste gerado para `T/AcademicApplicationTests.java`;
  - rodar `chmod +x backend/mvnw`.
- [X] T002 Ajustar `backend/pom.xml`:
  - **Parent**: `spring-boot-starter-parent` **4.1.1**.
  - **Properties**: `<java.version>21</java.version>` e `<mapstruct.version>1.6.3</mapstruct.version>`.
  - **Dependências**:
    - `spring-boot-starter-webmvc` (substitui o `spring-boot-starter-web`, se tiver sido gerado);
    - `spring-boot-starter-data-jpa`, `spring-boot-starter-validation`, `spring-boot-h2console`;
    - `com.h2database:h2` (scope `runtime`);
    - `org.mapstruct:mapstruct:${mapstruct.version}`.
  - **Dependências de teste**: `spring-boot-starter-test`, `spring-boot-starter-webmvc-test` e
    `spring-boot-starter-data-jpa-test`.
  - **Build**: `<finalName>app</finalName>`; `maven-compiler-plugin` com `annotationProcessorPaths`
    apontando para `org.mapstruct:mapstruct-processor:${mapstruct.version}`.
- [X] T003 Adicionar ao `backend/pom.xml` o `jacoco-maven-plugin` **0.8.15** com estas execuções:
  - `prepare-agent`;
  - `report` na fase `verify`;
  - `check-service` na fase `verify`: `<includes><include>br/com/bomjesus/academic/service/**</include></includes>`
    e regra `BUNDLE` / `LINE` / `COVEREDRATIO` mínimo `0.80`;
  - `check-controller`: igual, com `br/com/bomjesus/academic/controller/**`.

  O build deve falhar abaixo de 80% (Princípio IV).
- [X] T004 [P] Criar `R/application.properties` com exatamente:
  - `spring.application.name=academic-api`
  - `server.port=8080`
  - `spring.datasource.url=jdbc:h2:mem:bomjesus;DB_CLOSE_DELAY=-1`
  - `spring.datasource.generate-unique-name=false`
  - `spring.datasource.username=${DB_USERNAME:sa}`
  - `spring.datasource.password=${DB_PASSWORD:}`
  - `spring.jpa.hibernate.ddl-auto=create-drop`
  - `spring.jpa.open-in-view=false`
  - `spring.jpa.show-sql=false`
  - `spring.h2.console.enabled=true`
  - `spring.h2.console.path=/h2-console`
  - `spring.h2.console.settings.web-allow-others=true`
  - `app.cors.allowed-origins=${APP_CORS_ALLOWED_ORIGINS:http://localhost:5173,http://127.0.0.1:5173}`

  Remover `application.properties` duplicados gerados pelo Initializr. Criar também
  `backend/src/test/resources/config/application.properties` contendo apenas
  `spring.datasource.url=jdbc:h2:mem:test-${random.uuid};DB_CLOSE_DELAY=-1`. O Boot carrega
  `classpath:/config/` **por cima** do `application.properties` principal, sem substituí-lo, e
  cada contexto de teste ganha um banco próprio; assim o perfil `demo` não contamina os demais.
- [X] T005 [P] Copiar o diagrama de referência de
  `~/Obsidian/obsidian/Vagas/Bom Jesus Challenge/modelagem-bomjesus.png` para
  `docs/modelagem-bomjesus.png`.
- [X] T006 [P] Criar `.gitignore` na raiz ignorando:
  - `backend/target/`
  - `frontend/node_modules/`
  - `frontend/dist/`
  - `frontend/.env`
  - `.idea/`, `.vscode/`, `*.iml` e `.DS_Store`
- [X] T007 Rodar `cd backend && ./mvnw -B test` (projeto `backend/pom.xml`) e garantir que `AcademicApplicationTests`
  (carga de contexto) passa com Boot 4.1.1 e JUnit Jupiter 6.

---

## Phase 2: Foundational (Blocking Prerequisites)

**Purpose**: domínio base (`Person`, `Address`, `Status`), tratamento global de erros,
normalização de JSON, CORS, mapper de endereços e unicidade de e-mail, usados por US1 e US2.

**⚠️ CRITICAL**: nenhuma história começa antes desta fase.

- [X] T008 [P] Criar o enum `Status` em `B/model/Status.java` com exatamente os valores `ACTIVE` e
  `DISABLE`.
- [X] T009 [P] Criar a entidade `Address` em `B/model/Address.java`:
  - Anotações de classe: `@Entity @Table(name = "address")`.
  - `Long id`: `@Id @GeneratedValue(strategy = GenerationType.IDENTITY)`.
  - `Person person`: `@ManyToOne(fetch = FetchType.LAZY, optional = false)` com
    `@JoinColumn(name = "person_id", nullable = false)`.
  - `street`: `@Column(nullable = false, length = 150)`.
  - `city`: `@Column(nullable = false, length = 100)`.
  - `state`: `@Column(nullable = false, length = 100)`.
  - `zipCode`: `@Column(name = "zip_code", nullable = false, length = 20)`.
  - `country`: `@Column(nullable = false, length = 100)`.
  - Construtor público sem argumentos, getters/setters.
  - Sem `equals`/`hashCode`, sem Lombok e sem `toString()` que inclua `person` (evita recursão
    infinita em logs).
- [X] T010 [P] Criar a entidade abstrata `Person` em `B/model/Person.java`:
  - Anotações de classe: `@Entity`, `@Inheritance(strategy = InheritanceType.JOINED)` e
    `@Table(name = "person", uniqueConstraints = @UniqueConstraint(name = "uk_person_email_address", columnNames = "email_address"))`;
    a classe é `abstract`.
  - `Long id`: `@Id @GeneratedValue(strategy = GenerationType.IDENTITY)`.
  - `name`: `@Column(nullable = false, length = 100)`.
  - `phoneNumber`: `@Column(name = "phone_number", nullable = false, length = 20)`.
  - `emailAddress`: `@Column(name = "email_address", nullable = false, length = 150)`.
  - `Status status`: `@Enumerated(EnumType.STRING) @Column(nullable = false, length = 10)`.
  - `List<Address> addresses = new ArrayList<>()`: `@OneToMany(mappedBy = "person", cascade = CascadeType.ALL, orphanRemoval = true)`
    e `@OrderBy("id ASC")`.
  - Método `replaceAddresses(Collection<Address> newAddresses)`: faz `addresses.clear()` e, para
    cada item, `setPerson(this)` + `addresses.add(...)`. A lista **nunca** é reatribuída, e
    `setAddresses` não deve existir.
  - Getters/setters dos demais campos. Sem `equals`/`hashCode`, sem Lombok e sem `toString()`
    que inclua `addresses`.
- [X] T011 [P] Criar `PersonRepository extends JpaRepository<Person, Long>` em
  `B/repository/PersonRepository.java` com `boolean existsByEmailAddress(String emailAddress)` e
  `boolean existsByEmailAddressAndIdNot(String emailAddress, Long id)`.
- [X] T012 [P] Criar `ResourceNotFoundException extends RuntimeException` (construtor com mensagem)
  em `B/exception/ResourceNotFoundException.java`.
- [X] T013 [P] Criar `ConflictException extends RuntimeException` em
  `B/exception/ConflictException.java`, com os campos `field` (ex.: `emailAddress`) e
  `fieldMessage` (ex.: `e-mail já cadastrado`), além da mensagem de detalhe herdada.
- [X] T014 Criar `GlobalExceptionHandler` (`@RestControllerAdvice`, estende
  `ResponseEntityExceptionHandler`) em `B/exception/GlobalExceptionHandler.java`. Toda resposta é
  um `ProblemDetail` (`application/problem+json`) com:
  - `instance` = URI da requisição;
  - propriedade `timestamp` (`OffsetDateTime` UTC);
  - propriedade `errors` = lista de `{field, message}`, quando aplicável.

  Mapeamentos (títulos em pt-BR):
  - `MethodArgumentNotValidException` → 400, "Dados inválidos". O `detail` é
    "A requisição possui N campo(s) inválido(s)." e `errors` vem dos `FieldError`, preservando
    caminhos como `addresses[0].city`.
  - `HttpMessageNotReadableException` → 400, "Requisição inválida". Se a causa for um
    `tools.jackson.databind.exc.MismatchedInputException` ou `InvalidFormatException`, montar o
    campo a partir de `getPath()` (nomes unidos por `.`, índices como `[i]`) com a mensagem
    "valor inválido" (para `status`: "valor inválido; use ACTIVE ou DISABLE").
  - `TypeMismatchException` / `MethodArgumentTypeMismatchException` → 400, "Requisição inválida",
    com `errors` contendo o nome do parâmetro.
  - `ResourceNotFoundException` → 404, "Recurso não encontrado".
  - `ConflictException` → 409, "Conflito", com `errors=[{field, fieldMessage}]`.
  - `DataIntegrityViolationException` → 409, "Conflito", com o `detail` "Os dados conflitam com
    um registro existente."
  - `Exception` → 500, "Erro interno", com o `detail` "Ocorreu um erro inesperado. Tente novamente
    mais tarde." e `log.error` via SLF4J.

  Sobrescrever `handleExceptionInternal` para que os erros herdados também tenham título em pt-BR
  e `timestamp`: 404 de rota inexistente (`NoResourceFoundException`) → "Recurso não encontrado";
  405 → "Método não permitido"; 415 → "Tipo de mídia não suportado".
- [X] T015 [P] Criar `R/ValidationMessages.properties` (UTF-8) com as chaves:
  - `validation.required=é obrigatório`
  - `validation.size=deve ter entre {min} e {max} caracteres`
  - `validation.maxSize=deve ter no máximo {max} caracteres`
  - `validation.email=deve ser um e-mail válido`
  - `validation.phone=deve conter de 8 a 15 dígitos (são permitidos +, espaços, parênteses e hífens)`
  - `validation.studentNumber=deve ter de 1 a 20 caracteres entre letras, dígitos e hífen`
  - `validation.photo=deve ser uma URL http ou https válida`
  - `validation.zipCode=deve ter de 3 a 20 caracteres entre letras, dígitos, espaços e hífen`
  - `validation.addresses=informe ao menos um endereço`
  - `validation.salary.min=deve ser maior que zero`
  - `validation.salary.max=deve ser no máximo 1.000.000,00`
  - `validation.salary.digits=deve ter no máximo 2 casas decimais`
- [X] T016 [P] Criar `TrimmingStringDeserializer` em `B/config/TrimmingStringDeserializer.java`.
  - Anotada com `@JacksonComponent` (`org.springframework.boot.jackson`).
  - Estende o `StdScalarDeserializer<String>` do Jackson 3 (`tools.jackson.databind.deser.std`).
  - Lê com `_parseString(p, ctxt, this)`, aplica `strip()` e retorna `null` quando o resultado é
    vazio (FR-014, research R10).
- [X] T017 [P] Criar `CorsConfig implements WebMvcConfigurer` em `B/config/CorsConfig.java`.
  - Mapeamento `/api/**`.
  - `allowedOrigins` lido de `@Value("${app.cors.allowed-origins}") String[]` (lista separada por
    vírgula).
  - `allowedMethods("GET","POST","PUT","DELETE")` e `exposedHeaders("Location")`.
  - Nunca usar `*`.
- [X] T018 [P] Criar o record `AddressRequest` em `B/dto/AddressRequest.java`, com mensagens das
  chaves da T015:
  - `street`: `@NotBlank`, `@Size(max = 150)`.
  - `city`: `@NotBlank`, `@Size(max = 100)`.
  - `state`: `@NotBlank`, `@Size(min = 2, max = 100)`.
  - `zipCode`: `@NotBlank`, `@Pattern(regexp = "^[A-Za-z0-9 -]{3,20}$")`.
  - `country`: `@NotBlank`, `@Size(min = 2, max = 100)`.
- [X] T019 [P] Criar o record `AddressResponse(street, city, state, zipCode, country)` em
  `B/dto/AddressResponse.java` (sem `id`).
- [X] T020 [P] Criar `CentralMapperConfig` (`@MapperConfig(componentModel = MappingConstants.ComponentModel.SPRING, injectionStrategy = InjectionStrategy.CONSTRUCTOR, unmappedTargetPolicy = ReportingPolicy.ERROR)`)
  em `B/mapper/CentralMapperConfig.java`.
- [X] T021 Criar `AddressMapper` (`@Mapper(config = CentralMapperConfig.class)`) em
  `B/mapper/AddressMapper.java` com:
  - `Address toEntity(AddressRequest)`, com `@Mapping(target = "id", ignore = true)` e
    `@Mapping(target = "person", ignore = true)`;
  - `List<Address> toEntities(List<AddressRequest>)`;
  - `AddressResponse toResponse(Address)`.
- [X] T022 Criar `EmailUniquenessGuard` (`@Component`, injeta `PersonRepository` por construtor)
  em `B/service/EmailUniquenessGuard.java`, com `void assertAvailable(String normalizedEmail, Long currentId)`:
  - `currentId == null` → usa `existsByEmailAddress`; caso contrário, `existsByEmailAddressAndIdNot`.
    **Nunca** chamar `...AndIdNot(email, null)`.
  - Se já existir, lança `ConflictException(field = "emailAddress", fieldMessage = "e-mail já cadastrado", detail = "Já existe uma pessoa cadastrada com o e-mail %s.")`.
- [X] T023 [P] Criar `T/support/AddressTestData.java` (classe utilitária final) com
  `AddressRequest addressRequest()` (Rua XV de Novembro, 100 / Curitiba / PR / 80020-310 / Brasil),
  `AddressRequest addressRequest(String street)` e `Address address(String street)`.
- [X] T024 Criar `T/service/EmailUniquenessGuardTest.java` (`@ExtendWith(MockitoExtension.class)`,
  `@Mock PersonRepository`) cobrindo:
  - criação livre;
  - criação em conflito (`ConflictException` com `field = emailAddress`);
  - atualização livre e em conflito (usa `existsByEmailAddressAndIdNot`);
  - criação nunca chama `existsByEmailAddressAndIdNot` (`verify(..., never())`).
- [X] T025 [P] Criar `T/mapper/AddressMapperTest.java` usando `new AddressMapperImpl()`:
  - `toEntity` não preenche `id` nem `person`;
  - `toEntities` preserva a ordem;
  - `toResponse` copia os 5 campos.
- [X] T026 Rodar `cd backend && ./mvnw -B test` (projeto `backend/pom.xml`): tudo compila (inclusive o código gerado pelo
  MapStruct) e os testes passam.

**Checkpoint**: fundação pronta; US1 e US2 podem começar.

---

## Phase 3: User Story 1 - Gerenciar alunos pela API (Priority: P1) 🎯 MVP

**Goal**: CRUD completo em `/api/students` com endereços, status, unicidade de e-mail e de
matrícula e erros RFC 9457.

**Independent Test**: com `./mvnw spring-boot:run`, executar os cenários 1–5, 7–9 e 13–15 da
[quickstart §3](quickstart.md) via `curl`; `./mvnw clean verify` verde.

- [X] T027 [P] [US1] Criar a entidade `Student extends Person` em `B/model/Student.java`:
  - Anotações de classe: `@Entity` e
    `@Table(name = "student", uniqueConstraints = @UniqueConstraint(name = "uk_student_student_number", columnNames = "student_number"))`.
  - `studentNumber`: `@Column(name = "student_number", nullable = false, length = 20)`.
  - `photo`: `@Column(length = 500)` (anulável).
  - Construtor público sem argumentos, getters/setters.
- [X] T028 [US1] Criar `StudentRepository extends JpaRepository<Student, Long>` em
  `B/repository/StudentRepository.java` com:
  - `@EntityGraph(attributePaths = "addresses") @Query("select s from Student s") List<Student> findAllWithAddresses()`;
  - `@EntityGraph(attributePaths = "addresses") @Query("select s from Student s where s.status = :status") List<Student> findAllWithAddressesByStatus(@Param("status") Status status)`;
  - sem `ORDER BY`: a ordenação é a BR-08, feita no Service;
  - `@EntityGraph(attributePaths = "addresses") Optional<Student> findWithAddressesById(Long id)`;
  - `boolean existsByStudentNumber(String)` e `boolean existsByStudentNumberAndIdNot(String, Long)`.
- [X] T029 [P] [US1] Criar o record `StudentRequest` em `B/dto/StudentRequest.java`, com mensagens
  da T015:
  - `name`: `@NotBlank`, `@Size(min = 3, max = 100)`.
  - `phoneNumber`: `@NotBlank`, `@Size(max = 20)`,
    `@Pattern(regexp = "^(?=(?:\\D*\\d){8,15}\\D*$)\\+?[0-9 ()-]+$")`.
  - `emailAddress`: `@NotBlank`, `@Email(regexp = ".+@.+\\..+")`, `@Size(max = 150)`.
  - `Status status`: opcional.
  - `studentNumber`: `@NotBlank`, `@Pattern(regexp = "^[A-Za-z0-9-]{1,20}$")`.
  - `photo`: opcional; `@Size(max = 500)`, `@Pattern(regexp = "^https?://\\S+$")`.
  - `addresses`: `@NotEmpty(message = "{validation.addresses}") List<@Valid AddressRequest>`, com
    `@Valid` no campo.
- [X] T030 [P] [US1] Criar o record `StudentResponse(Long id, String name, String phoneNumber, String emailAddress, Status status, String studentNumber, String photo, List<AddressResponse> addresses)`
  em `B/dto/StudentResponse.java`.
- [X] T031 [US1] Criar `T/support/StudentTestData.java` (classe utilitária final; reutiliza
  `AddressTestData`) com:
  - `StudentRequest studentRequest()`: Ana Souza, `+55 (41) 99999-0001`, `ana@escola.com`,
    `2026001`, `https://i.pravatar.cc/150?img=5`, status `null`, 1 endereço;
  - `StudentRequest studentRequest(String email, String studentNumber)`;
  - `Student student(Long id)`: entidade `ACTIVE` com 1 endereço já ligado via `replaceAddresses`.
- [X] T032 [US1] Criar `T/repository/StudentRepositoryTest.java` (`@DataJpaTest`) comprovando:
  - um aluno salvo gera uma linha em `person` e outra em `student` com o mesmo `id` (contagem via
    `JdbcTemplate`/`EntityManager` nativo);
  - e-mail duplicado viola `uk_person_email_address`, e matrícula duplicada viola
    `uk_student_student_number` (no `flush`);
  - `replaceAddresses` com 1 endereço, após `flush`, deixa só 1 linha em `address` (sem órfãos);
  - `delete` do aluno remove todos os seus endereços;
  - `findAllWithAddresses` traz todos os alunos com os endereços já carregados;
  - `findAllWithAddressesByStatus` filtra por status;
  - endereços voltam na ordem em que foram enviados, e dois endereços idênticos são gravados
    como enviados;
  - `findWithAddressesById` retorna os endereços inicializados após `entityManager.clear()`.
- [X] T033 [US1] Criar `StudentMapper` (`@Mapper(config = CentralMapperConfig.class, uses = AddressMapper.class)`)
  em `B/mapper/StudentMapper.java`:
  - `Student toEntity(StudentRequest)`: ignora `id` e `addresses`;
    `@Mapping(target = "status", defaultValue = "ACTIVE")`.
  - `void updateEntity(StudentRequest, @MappingTarget Student)`: ignora `id` e `addresses`;
    `@Mapping(target = "status", nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE)`
    **apenas** em `status`, para que `photo = null` limpe a foto.
  - `StudentResponse toResponse(Student)`.
- [X] T034 [US1] Criar `T/mapper/StudentMapperTest.java` usando
  `new StudentMapperImpl(new AddressMapperImpl())`:
  - `toEntity` com status nulo → `ACTIVE`;
  - `updateEntity` com status nulo mantém o status atual;
  - `updateEntity` com `photo = null` limpa a foto;
  - `toEntity` sai com `addresses` vazio: o mapper nunca preenche a lista, que é ligada à pessoa
    só por `replaceAddresses`;
  - `updateEntity` não altera `id` nem `addresses`;
  - `toResponse` mapeia os endereços.
- [X] T035 [US1] Criar `StudentService` (`@Service`, injeção por construtor de `StudentRepository`,
  `StudentMapper`, `AddressMapper` e `EmailUniquenessGuard`) em `B/service/StudentService.java`.
  - **Métodos**:
    - `findAll(Status status)` (`@Transactional(readOnly = true)`; `null` → todos);
    - `findById(Long)` (readOnly);
    - `create(StudentRequest)`, `update(Long, StudentRequest)` e `delete(Long)` (`@Transactional`).
  - **Regras** (data-model §Regras de negócio):
    - **Ordem obrigatória em `create` e `update`**:
      (1) calcular os valores normalizados **a partir do request**: `emailAddress` com
      `toLowerCase(Locale.ROOT)` e `studentNumber` com `toUpperCase(Locale.ROOT)`;
      (2) checar a unicidade (e-mail e matrícula);
      (3) só então chamar `mapper.toEntity`/`updateEntity`, gravar os valores normalizados na
      entidade e chamar `replaceAddresses`.
      Alterar a entidade gerenciada antes do passo (2) faz o Hibernate gravar o valor duplicado
      antes da consulta `exists...` (auto-flush), e a resposta vira um 409 sem campo;
    - unicidade de e-mail via `emailGuard.assertAvailable(email, id ou null)`;
    - unicidade de matrícula via `existsByStudentNumber` / `existsByStudentNumberAndIdNot`, que
      lança `ConflictException("studentNumber", "matrícula já cadastrada", "Já existe um aluno com a matrícula %s.")`;
    - endereços sempre via `student.replaceAddresses(addressMapper.toEntities(request.addresses()))`;
    - id inexistente lança `ResourceNotFoundException("Aluno com id %d não encontrado.")`;
    - `delete` faz `findById` + `repository.delete(entity)`.
  - Todo mapeamento para `StudentResponse` acontece dentro da transação.
  - `findAll` ordena em memória (BR-08) com
    `Comparator.comparing(Student::getName, collator).thenComparing(Student::getId)`, usando
    `Collator.getInstance(Locale.of("pt", "BR"))` com força `Collator.PRIMARY` (ignora maiúsculas
    e acentos).
- [X] T036 [US1] Criar `T/service/StudentServiceTest.java` (`MockitoExtension`, mocks de
  `StudentRepository` e `EmailUniquenessGuard`, mappers reais `StudentMapperImpl`/`AddressMapperImpl`;
  regras BR-01, BR-02 e BR-04 a BR-10 do data-model)
  cobrindo:
  - `findAll` com e sem status;
  - ordenação BR-08: "bruno", "Álvaro", "Ana" e dois "Carla" (ids 5 e 3) → Álvaro, Ana, bruno,
    Carla (3), Carla (5);
  - `findById` encontrado e não encontrado (404);
  - `create` com sucesso: e-mail em minúsculas, matrícula em maiúsculas, status `ACTIVE`,
    endereços ligados ao aluno, `save` chamado;
  - `create` com conflito de e-mail (guard lança; `save` nunca chamado);
  - `create` com conflito de matrícula;
  - `update` com sucesso: substitui endereços e mantém o status quando nulo;
  - `update` inexistente;
  - `update` com a própria matrícula usa `existsByStudentNumberAndIdNot`;
  - `delete` com sucesso;
  - `delete` inexistente (`delete` nunca chamado).
- [X] T037 [US1] Criar `StudentController` (`@RestController @RequestMapping("/api/students")`,
  injeta `StudentService`) em `B/controller/StudentController.java`:
  - `GET` → `ResponseEntity.ok(service.findAll(status))`, com
    `@RequestParam(required = false) Status status`;
  - `GET /{id}` → 200;
  - `POST` (`@Valid @RequestBody`) → `ResponseEntity.created(uri).body(...)`, com `uri` montado por
    `ServletUriComponentsBuilder.fromCurrentRequestUri().path("/{id}").buildAndExpand(id).toUri()`;
  - `PUT /{id}` (`@Valid @RequestBody`) → 200;
  - `DELETE /{id}` → `ResponseEntity.noContent().build()`.
- [X] T038 [US1] Criar `T/controller/StudentControllerTest.java` (`@WebMvcTest(StudentController.class)`
  de `org.springframework.boot.webmvc.test.autoconfigure`, `@MockitoBean StudentService`, MockMvc)
  cobrindo:
  - `POST` válido → 201 com `Location` terminando em `/api/students/1` e corpo;
  - `POST` sem `addresses`, com e-mail inválido e `addresses[0].city` vazio → 400
    `application/problem+json`, com `errors` contendo os campos;
  - `POST` com `name: "   "` → 400 (trimming);
  - `POST` com JSON malformado → 400;
  - `POST` com `status: "FOO"` → 400 com o campo `status`;
  - `POST` com `ConflictException` → 409 com `errors[0].field`;
  - `GET` → 200 e `?status=DISABLE` repassado ao service;
  - `?status=FOO` → 400;
  - `GET /1` → 200; `GET /99` (service lança 404) → 404; `GET /abc` → 400;
  - `PUT` → 200 e 404;
  - `DELETE` → 204 e 404;
  - `PATCH /1` → 405 em problem+json;
  - `POST` com `Content-Type: text/plain` → 415;
  - `POST` com `"id": 99` e `"foo": 1` no corpo → 201, com o `id` igual ao retornado pelo service
    (FR-015; protege contra padrões diferentes do Jackson 3);
  - service lança `RuntimeException("boom")` → 500 `application/problem+json`, com `detail`
    "Ocorreu um erro inesperado. Tente novamente mais tarde." e corpo **sem** `boom` nem
    `Exception` (FR-018);
  - pré-checagem de CORS: `OPTIONS /api/students/1` com `Origin: http://localhost:5173` e
    `Access-Control-Request-Method: PUT` → 200 com
    `Access-Control-Allow-Origin: http://localhost:5173`;
  - `GET` com essa mesma origem → `Access-Control-Expose-Headers` contém `Location`;
  - origem `http://evil.test` → 403;
  - corpo vazio → 400;
  - `photo: "ftp://x"` → 400 com o campo `photo`; `photo` ausente → 201 com `"photo": null` na
    resposta;
  - `?status=DISABLE&status=ACTIVE` → o service recebe `DISABLE`; `?status=active` → 400;
  - `Accept: text/html` → 406;
  - service lança `DataIntegrityViolationException` → 409 (rede de segurança para gravações
    simultâneas).
- [X] T039 [US1] Criar `T/StudentApiIntegrationTest.java` (`@SpringBootTest @AutoConfigureMockMvc`)
  com o fluxo: criar (201) → buscar (200, sem `id` em endereços) → listar → atualizar (`PUT` com
  outro endereço e `status: DISABLE` → 200, só o novo endereço) → listar `?status=DISABLE` →
  excluir (204) → excluir de novo (404) → buscar (404) → tabela `address` sem linhas do aluno.
  Antes de cada teste, um `@BeforeEach` limpa a base com `studentRepository.deleteAll()`, que
  passa pela cascata. Não usar `@Transactional` com rollback: sem flush, a contagem via
  `JdbcTemplate` veria dados desatualizados.
  Casos extras: criar dois alunos e fazer `PUT` no segundo com o e-mail do primeiro, em outra
  caixa → 409 com `errors[0].field = emailAddress`; o mesmo com a matrícula → 409
  `studentNumber`. Outros casos: aluno com dois endereços idênticos → 201 com os 2 endereços na
  ordem enviada; `GET /api/teachers` → 404 em `application/problem+json`.
- [X] T040 [US1] Criar `T/architecture/ControllerArchitectureTest.java`, um teste de arquitetura
  por reflexão, sem dependência nova:
  - localizar as classes `@RestController` do pacote `br.com.bomjesus.academic.controller` com
    `ClassPathScanningCandidateComponentProvider`;
  - percorrer recursivamente os tipos genéricos de retorno e de parâmetros dos métodos públicos
    (inclusive `ResponseEntity<List<...>>`);
  - falhar se algum tipo for anotado com `@jakarta.persistence.Entity` (Princípio II).

  Depois, rodar `cd backend && ./mvnw clean verify` (projeto `backend/pom.xml`, com o gate JaCoCo
  verde) e executar via
  `curl` os cenários 1–5, 7–9 e 13–15 da [quickstart §3](quickstart.md) contra
  `./mvnw spring-boot:run`.

**Checkpoint**: MVP — API de alunos completa e testada.

---

## Phase 4: User Story 2 - Gerenciar professores pela API (Priority: P2)

**Goal**: CRUD completo em `/api/professors`, com salário monetário e e-mail único entre todas
as pessoas.

**Independent Test**: cenários 6, 10, 11 e 12 da [quickstart §3](quickstart.md) e
`./mvnw clean verify` verde.

- [X] T041 [P] [US2] Criar a entidade `Professor extends Person` em `B/model/Professor.java`:
  - Anotações de classe: `@Entity @Table(name = "professor")`.
  - `BigDecimal salary`: `@Column(nullable = false, precision = 9, scale = 2)`.
  - Construtor público sem argumentos, getters/setters.
- [X] T042 [US2] Criar `ProfessorRepository extends JpaRepository<Professor, Long>` em
  `B/repository/ProfessorRepository.java` com `findAllWithAddresses()`,
  `findAllWithAddressesByStatus(Status)` (JPQL `select p from Professor p`, com
  `where p.status = :status` no segundo; sem `ORDER BY`, pois a ordenação é a BR-08 no Service) e
  `findWithAddressesById(Long)`, todos com `@EntityGraph(attributePaths = "addresses")`.
- [X] T043 [P] [US2] Criar o record `ProfessorRequest` em `B/dto/ProfessorRequest.java`:
  - Campos comuns `name`, `phoneNumber`, `emailAddress`, `status` e `addresses`, com as mesmas
    anotações da T029.
  - `salary`: `@NotNull(message = "{validation.required}")`,
    `@DecimalMin(value = "0.01", message = "{validation.salary.min}")`,
    `@DecimalMax(value = "1000000.00", message = "{validation.salary.max}")` e
    `@Digits(integer = 7, fraction = 2, message = "{validation.salary.digits}")`.
- [X] T044 [P] [US2] Criar o record `ProfessorResponse(Long id, String name, String phoneNumber, String emailAddress, Status status, BigDecimal salary, List<AddressResponse> addresses)`
  em `B/dto/ProfessorResponse.java`.
- [X] T045 [US2] Criar `T/support/ProfessorTestData.java` (classe utilitária final; reutiliza
  `AddressTestData`) com
  `ProfessorRequest professorRequest()` (Helena Rocha, `+55 41 3333-0001`, `helena@escola.com`,
  `8500`, 1 endereço), `professorRequest(String email)` e `Professor professor(Long id)`.
- [X] T046 [US2] Criar `T/repository/ProfessorRepositoryTest.java` (`@DataJpaTest`) comprovando:
  - linhas em `person` + `professor`;
  - `salary` persistido com escala 2;
  - e-mail de um professor igual ao de um aluno salvo viola `uk_person_email_address`;
  - `findById` de `ProfessorRepository` com o id de um aluno retorna vazio;
  - filtro por status;
  - cascata de endereços no delete.
- [X] T047 [US2] Criar `ProfessorMapper` (`uses = AddressMapper.class`) em
  `B/mapper/ProfessorMapper.java` com as mesmas regras da T033 (`defaultValue = "ACTIVE"` no
  `toEntity`; `IGNORE` para status nulo apenas em `updateEntity`; ignora `id` e `addresses`).
- [X] T048 [US2] Criar `T/mapper/ProfessorMapperTest.java`, com os mesmos casos da T034 aplicados
  a `Professor` e `salary`.
- [X] T049 [US2] Criar `ProfessorService` em `B/service/ProfessorService.java`, espelhando a T035
  sem a matrícula:
  - mesma ordem obrigatória da T035: normalizar o e-mail a partir do request →
    `EmailUniquenessGuard` → só então `toEntity`/`updateEntity`;
  - `salary.setScale(2, RoundingMode.UNNECESSARY)` antes de gravar;
  - `replaceAddresses`;
  - `ResourceNotFoundException("Professor com id %d não encontrado.")`;
  - transações iguais às da T035.
- [X] T050 [US2] Criar `T/service/ProfessorServiceTest.java` cobrindo:
  - os mesmos cenários da T036 aplicáveis a professor;
  - `salary` 8500 → resposta `8500.00`;
  - conflito de e-mail com um aluno existente (guard lança 409).
- [X] T051 [US2] Criar `ProfessorController` (`/api/professors`) em
  `B/controller/ProfessorController.java`, com as mesmas assinaturas e respostas da T037.
- [X] T052 [US2] Criar `T/controller/ProfessorControllerTest.java` (`@WebMvcTest(ProfessorController.class)`)
  cobrindo:
  - os mesmos casos da T038, exceto matrícula;
  - `salary` `0`, `-1`, `10.123` e `"abc"` → 400 com `errors` contendo `salary`;
  - `salary` ausente → 400.
- [X] T053 [US2] Criar `T/ProfessorApiIntegrationTest.java` (`@SpringBootTest @AutoConfigureMockMvc`)
  com:
  - o fluxo completo do professor;
  - id de um aluno criado em `/api/professors/{id}` → 404 em `GET`, `PUT` e `DELETE`;
  - `POST /api/professors` com o e-mail de um aluno, em outra caixa → 409 `emailAddress`;
  - `PUT` de um professor com o e-mail de outra pessoa → 409 `emailAddress`.

  Antes de cada teste, um `@BeforeEach` limpa a base com `studentRepository.deleteAll()` e
  `professorRepository.deleteAll()`.
- [X] T054 [US2] Rodar `cd backend && ./mvnw clean verify` (projeto `backend/pom.xml`) (com o gate JaCoCo verde) e os cenários
  6, 10, 11 e 12 da [quickstart §3](quickstart.md).

**Checkpoint**: back-end completo e testado (Princípio VIII: o front-end só começa depois).

---

## Phase 5: User Story 3 - Executar, inspecionar e demonstrar o sistema (Priority: P3)

**Goal**: dados de demonstração, imagem Docker do back-end, compose, console H2 e README inicial.

**Independent Test**: [quickstart §2, §4 e §6](quickstart.md) (parte da API) seguindo apenas o
README.

- [X] T055 [US3] Criar `DemoDataLoader` (`@Component @Profile("demo") implements ApplicationRunner`,
  injeta `StudentService` e `ProfessorService`) em `B/config/DemoDataLoader.java`. Ele cria, pelos
  services, os registros abaixo (todos com país Brasil e estado PR):

  | Tipo | Nome | E-mail | Telefone | Específico | Status | Endereços |
  |------|------|--------|----------|------------|--------|-----------|
  | Aluno | Ana Souza | `ana.souza@escola.com` | +55 (41) 99999-0001 | matrícula `2026001`, foto `https://i.pravatar.cc/150?img=5` | `ACTIVE` | 2: Rua XV de Novembro, 100 (Curitiba, 80020-310); Rua das Flores, 25 (Curitiba, 80010-020) |
  | Aluno | Bruno Martins | `bruno.martins@escola.com` | +55 (41) 99999-0002 | matrícula `2026002`, foto `img=12` | `ACTIVE` | Av. Batel, 1500 (Curitiba, 80420-090) |
  | Aluno | Carla Mendes | `carla.mendes@escola.com` | +55 (41) 99999-0003 | matrícula `2026003`, foto `img=32` | `DISABLE` | Rua Joinville, 300 (São José dos Pinhais, 83005-000) |
  | Professor | Helena Rocha | `helena.rocha@escola.com` | +55 (41) 3333-0001 | salário `8500.00` | `ACTIVE` | Av. Sete de Setembro, 2000 (Curitiba, 80230-010) |
  | Professor | Marcos Pereira | `marcos.pereira@escola.com` | +55 (41) 3333-0002 | salário `9200.00` | `DISABLE` | Rua Marechal Deodoro, 500 (Curitiba, 80010-010) |

  Deve logar via SLF4J quantos registros criou.
- [X] T056 [US3] Criar `T/config/DemoDataLoaderTest.java` (`@SpringBootTest @ActiveProfiles("demo")`)
  verificando 3 alunos (1 `DISABLE`) e 2 professores. Criar também
  `T/config/NoDemoDataTest.java` (sem perfil), verificando o banco vazio (FR-027/FR-028).
- [X] T057 [P] [US3] Criar `backend/.dockerignore` excluindo `target/`, `.git/`, `.idea/`,
  `.vscode/`, `*.iml` e `HELP.md`.
- [X] T058 [US3] Criar `backend/Dockerfile` multi-stage.
  - **Estágio `build`** (`eclipse-temurin:21-jdk-alpine`, `WORKDIR /workspace`):
    - copiar `.mvn/`, `mvnw` e `pom.xml`; rodar `chmod +x mvnw && ./mvnw -B dependency:go-offline`;
    - copiar `src/`; rodar `./mvnw -B package -DskipTests`;
    - extrair com `java -Djarmode=tools -jar target/app.jar extract --layers --destination extracted`.
  - **Estágio final** (`eclipse-temurin:21-jre-alpine`):
    - criar o usuário não-root `spring` (`addgroup -S spring && adduser -S spring -G spring`);
    - `WORKDIR /app`;
    - copiar as camadas `dependencies`, `spring-boot-loader`, `snapshot-dependencies` e
      `application` nessa ordem, conferindo a estrutura gerada pelo `extract`;
    - `USER spring`, `EXPOSE 8080`, `ENV JAVA_OPTS="-XX:MaxRAMPercentage=75"`;
    - `ENTRYPOINT ["sh","-c","exec java $JAVA_OPTS -jar app.jar"]`.
- [X] T059 [US3] Criar `docker-compose.yml` na raiz, por enquanto só com o serviço `backend`:
  - `build: ./backend`, `ports: ["8080:8080"]`;
  - `environment`: `SPRING_PROFILES_ACTIVE: ${SPRING_PROFILES_ACTIVE:-default}` e
    `APP_CORS_ALLOWED_ORIGINS: ${APP_CORS_ALLOWED_ORIGINS:-http://localhost:5173,http://127.0.0.1:5173}`;
  - `healthcheck`: `test: ["CMD", "wget", "-qO-", "http://localhost:8080/api/students"]`,
    `interval: 10s`, `timeout: 3s`, `retries: 12`, `start_period: 20s`.
- [X] T060 [US3] Validar o empacotamento de `backend/Dockerfile` e `docker-compose.yml`:
  - `docker build -t academic-api ./backend && docker run --rm -p 8080:8080 academic-api` responde
    em `/api/students`;
  - `SPRING_PROFILES_ACTIVE=demo docker compose up --build` fica `healthy` com 3 + 2 registros;
  - o console H2 em `http://localhost:8080/h2-console` (`jdbc:h2:mem:bomjesus`, `sa`, senha vazia)
    mostra `PERSON`, `STUDENT`, `PROFESSOR` e `ADDRESS` ([quickstart §4](quickstart.md));
  - o container roda como `spring` (`docker compose exec backend whoami`).
- [X] T061 [US3] Criar o `README.md` na raiz (pt-BR, comandos em bash) com as seções do back-end:
  1. Descrição + `docs/modelagem-bomjesus.png`.
  2. Pré-requisitos (JDK 21, Docker 29+/Compose v2+; Maven não é necessário: `./mvnw`).
  3. Build (`./mvnw clean package`).
  4. Execução local e via Docker/compose, com e sem perfil `demo`.
  5. Testes (`./mvnw clean verify`) e relatório JaCoCo em `backend/target/site/jacoco/index.html`.
  6. Endpoints: tabela das 10 operações + exemplo `curl` e resposta de cada uma, com códigos de
     status (baseado em [contracts/openapi.yaml](contracts/openapi.yaml)).
  7. Console H2 (URL, JDBC URL, usuário, senha).
  8. Configuração (`DB_USERNAME`, `DB_PASSWORD`, `APP_CORS_ALLOWED_ORIGINS`, `SPRING_PROFILES_ACTIVE`).
  9. Estrutura de pacotes.

**Checkpoint**: API executável e demonstrável por terceiros, local e em container.

---

## Phase 6: User Story 4 - Operar os cadastros pela interface web (Priority: P4)

**Goal**: SPA Vue 3 + TypeScript + Axios para listar, cadastrar, editar, desativar e excluir alunos
e professores, também empacotada em container.

**Independent Test**: [quickstart §5](quickstart.md) (9 passos, incluindo o modo de acesso direto com CORS) com a API no perfil `demo`; e
`docker compose up --build` servindo a interface em `http://localhost:8081` ([quickstart §6](quickstart.md)).

- [X] T062 [US4] Gerar o front-end na raiz com
  `npm create vue@3.24 frontend -- --typescript --router --vitest --eslint --prettier --bare`.
  Depois, `cd frontend && npm install && npm install axios@^1.20.0` e remover o conteúdo de exemplo
  que restar em `F/`.
- [X] T063 [P] [US4] Criar `frontend/.env.example` (`VITE_API_BASE_URL=/api`) e declarar
  `interface ImportMetaEnv { readonly VITE_API_BASE_URL?: string }` em `frontend/env.d.ts`.
- [X] T064 [P] [US4] Configurar `server.proxy` em `frontend/vite.config.ts`:
  `'/api': { target: 'http://localhost:8080', changeOrigin: true }` e `server.strictPort: true`.
  Com isso, o Vite falha em vez de trocar de porta silenciosamente, o que quebraria a lista de
  origens permitidas.
- [X] T065 [P] [US4] Criar `F/types/person.ts` espelhando o [contrato](contracts/openapi.yaml):
  - `type Status = 'ACTIVE' | 'DISABLE'`;
  - `Address` (5 campos);
  - `PersonBase`;
  - `StudentRequest`, `StudentResponse`, `ProfessorRequest` e `ProfessorResponse` (`salary: number`).
- [X] T066 [P] [US4] Criar `F/types/api.ts` com os tipos `FieldError {field, message}` e
  `ProblemDetail` (`title`, `status`, `detail`, `instance`, `timestamp`, `errors?`), e a classe
  `ApiError` (`status`, `title`, `detail`, `fieldErrors: Record<string, string>`,
  `isNetworkError`).
- [X] T067 [US4] Criar `F/services/http.ts`: instância Axios com
  `baseURL: import.meta.env.VITE_API_BASE_URL ?? '/api'` e um interceptor de resposta que converte
  cada erro em `ApiError`:
  - `ProblemDetail` → `fieldErrors` indexado por `field`;
  - sem resposta → `isNetworkError = true` e o `detail` "Não foi possível conectar à API".
- [X] T068 [P] [US4] Criar `F/services/studentService.ts`: `list(status?: Status)`, `get(id)`,
  `create(req)`, `update(id, req)` e `remove(id)` sobre `/students`, usando apenas `http.ts`.
- [X] T069 [P] [US4] Criar `F/services/professorService.ts` com a mesma API sobre `/professors`.
- [X] T070 [P] [US4] Criar `F/utils/formatters.ts` com:
  - `formatCurrencyBRL(n)` (`Intl.NumberFormat('pt-BR', { style: 'currency', currency: 'BRL' })`);
  - `parseCurrencyBRL(text)`: aceita `8.500,00`, `8500,5` e `8500.50`; remove `R$`, espaços e
    pontos de milhar; troca `,` por `.`; retorna `number | null`.
- [X] T071 [P] [US4] Criar `F/utils/validators.ts` com as regras do [data-model](data-model.md):
  - mesmos tamanhos e as mesmas regex de telefone, matrícula, foto, CEP e e-mail;
  - salário > 0, ≤ 1.000.000,00, até 2 casas;
  - pelo menos 1 endereço.

  Retorna `Record<string, string>` com chaves iguais às da API (ex.: `addresses[0].city`) e as
  mesmas mensagens da T015.
- [X] T072 [P] [US4] Criar `F/composables/useNotifications.ts`: fila de toasts (`success` |
  `error`), `notify()` e `dismiss()`, com remoção automática em 5 s.
- [X] T073 [US4] Criar `F/composables/usePersonList.ts`, genérico sobre o service e uma função
  `toRequest(response)`. Expõe:
  - `items`, `statusFilter` (`'ALL'` padrão, `'ACTIVE'`, `'DISABLE'`), `loading` e `error`;
  - `load()`;
  - `deactivate(person)`: consulta os dados atuais com `get(person.id)`, envia `PUT` com
    `toRequest(atual)` + `status: 'DISABLE'` e atualiza a linha local com a resposta (FR-023);
  - `remove(id)`: `DELETE` e remoção local.

  Notifica sucesso e erro via `useNotifications`.
- [X] T074 [US4] Criar `F/composables/usePersonForm.ts`. Ele mantém:
  - `form` reativo e `errors` (`Record<string, string>`);
  - `addAddress()` e `removeAddress(i)` (bloqueado com 1 endereço);
  - `validate()` (`validators.ts`);
  - `submit()`: `create` ou `update`; em `ApiError` com `fieldErrors`, preenche `errors` sem
    limpar o `form`; sem `fieldErrors`, mostra toast;
  - `loadForEdit(id)`: um 404 gera o toast "Registro não encontrado" + `router.push` para a lista.
- [X] T075 [P] [US4] Criar `F/assets/main.css` e `F/assets/avatar-placeholder.svg`:
  - tokens em `:root` (cores, espaçamentos, raio);
  - foco visível;
  - layout fluido a partir de 360 px;
  - classe `.table--cards` que transforma tabelas em cartões abaixo de 640 px.
- [X] T076 [P] [US4] Criar `F/components/StatusBadge.vue` (Ativo verde / Inativo cinza) e
  `F/components/StatusFilter.vue` (Todos / Ativos / Inativos, `v-model`), com `<script setup lang="ts">`.
- [X] T077 [P] [US4] Criar `F/components/PhotoAvatar.vue`: `<img>` com `alt`, troca para
  `avatar-placeholder.svg` no `@error` e quando não há URL.
- [X] T078 [P] [US4] Criar `F/components/LoadingState.vue` (`aria-busy`), `F/components/EmptyState.vue`
  (mensagem + slot de ação "Cadastrar") e `F/components/ErrorState.vue` (mensagem + botão
  "Tentar novamente", que emite `retry`).
- [X] T079 [P] [US4] Criar `F/components/ToastStack.vue`: região `aria-live="polite"` consumindo
  `useNotifications`.
- [X] T080 [P] [US4] Criar `F/components/ConfirmDialog.vue` com `<dialog>` nativo e duas variantes:
  - `default`: título + mensagem + "Cancelar"/"Confirmar";
  - `danger` (exclusão): texto "Esta ação é definitiva e irreversível", nome da pessoa e botão
    "Excluir definitivamente".

  O foco inicial fica em "Cancelar", Esc cancela e, ao fechar, o foco volta ao botão que abriu o
  diálogo (FR-025a).
- [X] T081 [US4] Criar `F/components/AddressFieldset.vue`:
  - um bloco por endereço (Rua, Cidade, Estado, CEP, País);
  - "Adicionar endereço";
  - "Remover" desabilitado quando há 1 endereço;
  - erros exibidos sob cada campo a partir de `errors['addresses[i].campo']`, ligados por
    `aria-describedby`.
- [X] T082 [US4] Criar `F/components/PersonForm.vue`:
  - campos comuns (Nome, Telefone, E-mail, Status), com `<label>` e erro por campo;
  - slot `specific-fields` para os campos do subtipo;
  - `AddressFieldset`;
  - botões "Salvar" (desabilitado durante o envio) e "Cancelar".
- [X] T083 [US4] Criar `F/components/PersonTable.vue`: recebe `columns` (key, label) e `items`,
  oferece slots por célula, e as ações "Editar", "Desativar" (só para `ACTIVE`) e "Excluir"
  (estilo secundário de perigo) emitem eventos. Usa `.table--cards` em telas estreitas.
- [X] T084 [US4] Criar `F/components/AppHeader.vue` (abas `RouterLink` "Alunos"/"Professores" com
  estado ativo) e ajustar `F/App.vue` (`AppHeader` + `<RouterView>` + `ToastStack`) e `F/main.ts`
  (importa `assets/main.css`, router).
- [X] T085 [P] [US4] Criar `F/views/StudentListView.vue` usando `usePersonList(studentService)`:
  - componentes `StatusFilter`, `PersonTable` (colunas Foto via `PhotoAvatar`, Nome, Matrícula,
    E-mail, Telefone, Status), `ConfirmDialog` para desativar e excluir, e os estados de
    carregamento, vazio e erro;
  - botão "Novo aluno".
- [X] T086 [P] [US4] Criar `F/views/ProfessorListView.vue`, igual à T085, mas com a coluna Salário
  (`formatCurrencyBRL`) no lugar de Foto/Matrícula e o botão "Novo professor".
- [X] T087 [P] [US4] Criar `F/views/StudentFormView.vue` (rotas new/edit) com
  `usePersonForm(studentService)` e `PersonForm`, preenchendo o slot com Matrícula e Foto (URL),
  com pré-visualização via `PhotoAvatar`.
- [X] T088 [P] [US4] Criar `F/views/ProfessorFormView.vue` com `usePersonForm(professorService)`,
  preenchendo o slot com Salário (campo texto com prefixo R$; converte com `parseCurrencyBRL`).
- [X] T089 [P] [US4] Criar `F/views/NotFoundView.vue` (mensagem + link para `/students`).
- [X] T090 [US4] Configurar `F/router/index.ts` com as rotas `/` (redirect `/students`),
  `/students`, `/students/new`, `/students/:id/edit`, `/professors`, `/professors/new`,
  `/professors/:id/edit` e `/:pathMatch(.*)*` (NotFound), conforme
  [frontend-ui.md](contracts/frontend-ui.md). Após cada navegação e após desativar ou excluir,
  mover o foco para o `h1` da view (`tabindex="-1"`), conforme FR-025a.
- [X] T091 [P] [US4] Criar `F/__tests__/validators.spec.ts` (Vitest) com casos válidos e inválidos
  para cada regra, inclusive as chaves `addresses[0].city`.
- [X] T092 [P] [US4] Criar `F/__tests__/formatters.spec.ts`: `formatCurrencyBRL(8500)` →
  `R$ 8.500,00` (normalizar o espaço não-quebrável) e `parseCurrencyBRL` com os 3 formatos e um
  texto inválido.
- [X] T093 [P] [US4] Criar `F/__tests__/http.spec.ts`: converte um `ProblemDetail` 400/409 em
  `ApiError.fieldErrors` e uma falha sem resposta em `isNetworkError`.
- [X] T094 [P] [US4] Criar `F/__tests__/usePersonForm.spec.ts` (service mockado): erros da API são
  mapeados aos campos sem perder os dados digitados, e `removeAddress` não remove o último
  endereço.
- [X] T095 [US4] Rodar `cd frontend && npm run type-check && npm run lint && npm run test:unit && npm run build`
  (todos sem erros). Depois, com a API em `SPRING_PROFILES_ACTIVE=demo`, executar os 9 passos da
  [quickstart §5](quickstart.md) em `npm run dev`. Confirmar também que não há `console.log` nem
  `axios` fora de `F/services/`. O comando abaixo não deve imprimir nada:
  `grep -rnE "axios|console\.log" frontend/src --include=*.vue --include=*.ts | grep -v -e "^frontend/src/services/" -e "^frontend/src/__tests__/"`.
  Conferir também:
  - FR-025a: percorrer as telas só pelo teclado;
  - FR-025b: no máximo 7 colunas e uma ação primária por tela;
  - FR-025c: Chrome, Firefox e Edge.
- [X] T096 [P] [US4] Criar `frontend/.dockerignore` excluindo `node_modules/`, `dist/`, `.env`,
  `.git/` e `coverage/`.
- [X] T097 [P] [US4] Criar `frontend/nginx.conf`:
  - `server { listen 8080; root /usr/share/nginx/html; index index.html; ... }`;
  - `location /api/ { proxy_pass http://backend:8080; proxy_set_header Host $http_host; proxy_set_header X-Forwarded-For $proxy_add_x_forwarded_for; }`,
    para que o `Location` saia com a porta 8081;
  - `location / { try_files $uri $uri/ /index.html; }`.
- [X] T098 [US4] Criar `frontend/Dockerfile` multi-stage:
  - **Estágio `build`** (`node:24-alpine`, `WORKDIR /app`): copiar `package.json` e
    `package-lock.json` e rodar `npm ci`; copiar o resto; `ARG VITE_API_BASE_URL=/api` e
    `ENV VITE_API_BASE_URL=$VITE_API_BASE_URL`; `npm run build`.
  - **Estágio final** (`nginxinc/nginx-unprivileged:stable-alpine`): copiar `nginx.conf` para
    `/etc/nginx/conf.d/default.conf` e `dist/` para `/usr/share/nginx/html`; `EXPOSE 8080`.
- [X] T099 [US4] Adicionar ao `docker-compose.yml` o serviço `frontend`: `build: ./frontend`,
  `ports: ["8081:8080"]` e `depends_on: { backend: { condition: service_healthy } }`.
- [X] T100 [US4] Validar `docker compose up --build` com o `docker-compose.yml` da raiz: interface em `http://localhost:8081`, API via
  `http://localhost:8081/api/students` e `http://localhost:8080/api/students`. Repetir em
  `:8081` os passos 1–6 da [quickstart §5](quickstart.md), sem erros de CORS no console do
  navegador.

**Checkpoint**: sistema completo (API + interface) em containers com um único comando.

---

## Phase 7: Polish & Cross-Cutting Concerns

**Purpose**: README final, roteiro dos vídeos, entrega e auditoria do Definition of Done.

- [X] T101 Completar o `README.md` com as seções do front-end:
  - pré-requisitos Node.js 24 / npm 11;
  - `npm ci`, `npm run dev` (proxy `/api` → 8080), `npm run build`, `npm run test:unit`;
  - variável `VITE_API_BASE_URL` e quando o CORS se aplica (acesso direto a `:8080` a partir de
    outra origem);
  - `docker compose up --build`, com a tabela de URLs: interface `:8081`, API `:8080`, console H2
    `:8080/h2-console`;
  - estrutura do repositório (`backend/`, `frontend/`, `docs/`).
- [X] T102 Escrever no `README.md` a seção "Roteiro para os vídeos" (FR-031), com tempo estimado
  por bloco e total entre 2 e 5 min:
  - **Vídeo de produto**, com a interface em `:8081` e o perfil `demo`:
    1. Abas.
    2. Filtro.
    3. Cadastrar um aluno com 2 endereços.
    4. Erro de validação.
    5. Conflito de e-mail.
    6. Editar.
    7. Desativar.
    8. Excluir com confirmação reforçada.
    9. Professores.
    10. Dados no console H2.
  - **Vídeo técnico**:
    1. Modelagem (diagrama, `JOINED`, composição de endereços).
    2. Camadas e pacotes.
    3. DTOs, MapStruct e validação.
    4. `GlobalExceptionHandler`/RFC 9457.
    5. Testes e relatório JaCoCo.
    6. Dockerfiles e compose.
    7. Front-end (services, composables, componentes).
    8. Como configurar e rodar.
  - Antes de gravar, conferir se os links das fotos de demonstração abrem. Se não abrirem, a
    imagem padrão aparece e o roteiro segue igual.
- [X] T103 Adicionar ao `README.md` a seção "Entrega", com espaços para o link do repositório
  público, do vídeo técnico e do vídeo de produto (YouTube "não listado").
- [X] T104 Validar o README do zero: apagar `backend/target`, `frontend/node_modules` e
  `frontend/dist`, executar cada comando na ordem documentada e corrigir qualquer divergência
  (Princípio VI, SC-001: subir e cadastrar o primeiro aluno em ≤ 10 min).
- [X] T105 Auditar o Definition of Done da constituição:
  - `./mvnw clean verify` verde;
  - `ControllerArchitectureTest` (T040) verde, e nenhum `Repository` nem entidade importados em
    controllers. O comando abaixo não deve imprimir nada (o enum `Status` é permitido):
    `grep -rnE "Repository|model\.(Person|Student|Professor|Address)" backend/src/main/java/br/com/bomjesus/academic/controller`;
  - nenhum `System.out` nem `printStackTrace` (`grep -rn` em `backend/src/main`);
  - os gates do front-end da T095 continuam verdes;
  - `docker compose up --build` funcional.

  Registrar o resultado na descrição da entrega.
- [X] T106 Medir a SC-004.
  - Criar `scripts/seed-volume.sh` (bash + `curl`, API em `http://localhost:8080`) que cria 1.000
    alunos e 1.000 professores com 2 endereços cada.
  - Com a API rodando localmente, fazer uma requisição de aquecimento e medir com
    `curl -s -o /dev/null -w '%{time_total}\n'`: a listagem completa de cada tipo, a busca por id,
    a criação, a atualização e a exclusão ([quickstart §7](quickstart.md)).
  - Todas devem ficar abaixo de 1 s; registrar os tempos na descrição da entrega.
  - Com a interface aberta, conferir que a lista de 1.000 alunos aparece em até 2 s (FR-025c).

---

## Dependencies & Execution Order

### Phase Dependencies

- **Setup (Phase 1)**: sem dependências.
- **Foundational (Phase 2)**: depende da Phase 1 e **bloqueia** todas as histórias.
- **US1 (Phase 3)** e **US2 (Phase 4)**: dependem só da Phase 2. São independentes entre si (podem
  rodar em paralelo); a ordem padrão é P1 → P2.
- **US3 (Phase 5)**: depende de US1 + US2. O loader de dados de demonstração usa os dois services,
  e a constituição exige o back-end completo antes da containerização.
- **US4 (Phase 6)**: depende de US1 + US2 (API) e de US3 (o compose recebe o `frontend`).
  T062–T095 podem começar após US2; T096–T100 precisam da T059.
- **Polish (Phase 7)**: depende de todas as histórias.

### Within Each User Story

- Entidade → repositório (+ teste de repositório) → DTOs → mapper (+ teste) → service (+ teste
  unitário) → controller (+ teste web) → integração → checkpoint.
- Front-end: tipos → `http.ts` → services → composables → componentes → views → router → testes →
  gates → container.

### Parallel Opportunities

- **Phase 1**: T004, T005 e T006.
- **Phase 2**:
  - T008, T009, T010, T011, T012, T013, T015, T016, T017, T018, T019, T020 e T023 (arquivos
    distintos);
  - depois T021/T022, seguidas de T024 e T025.
- **US1 × US2**: as duas fases inteiras podem rodar em paralelo após a Phase 2, porque cada uma
  tem o próprio arquivo de massa de teste (`StudentTestData`/`ProfessorTestData`). Dentro de cada
  uma: T027 ∥ T029 ∥ T030 e T041 ∥ T043 ∥ T044.
- **US4**:
  - T063–T066, T068–T072, T075–T080 e T091–T094;
  - views T085 ∥ T086 e T087 ∥ T088 ∥ T089;
  - T096 ∥ T097.

---

## Parallel Example: User Story 1

```bash
# Após a Phase 2, criar em paralelo:
Task: "T027 Criar entidade Student em backend/src/main/java/br/com/bomjesus/academic/model/Student.java"
Task: "T029 Criar record StudentRequest em backend/src/main/java/br/com/bomjesus/academic/dto/StudentRequest.java"
Task: "T030 Criar record StudentResponse em backend/src/main/java/br/com/bomjesus/academic/dto/StudentResponse.java"
```

## Parallel Example: User Story 2 (simultânea à US1)

```bash
Task: "T041 Criar entidade Professor em backend/src/main/java/br/com/bomjesus/academic/model/Professor.java"
Task: "T043 Criar record ProfessorRequest em backend/src/main/java/br/com/bomjesus/academic/dto/ProfessorRequest.java"
Task: "T044 Criar record ProfessorResponse em backend/src/main/java/br/com/bomjesus/academic/dto/ProfessorResponse.java"
```

## Parallel Example: User Story 4

```bash
Task: "T076 Criar StatusBadge.vue e StatusFilter.vue em frontend/src/components/"
Task: "T077 Criar PhotoAvatar.vue em frontend/src/components/PhotoAvatar.vue"
Task: "T080 Criar ConfirmDialog.vue em frontend/src/components/ConfirmDialog.vue"
Task: "T091 Criar validators.spec.ts em frontend/src/__tests__/validators.spec.ts"
```

---

## Implementation Strategy

### MVP First (User Story 1 Only)

1. Phase 1 (Setup) → Phase 2 (Foundational).
2. Phase 3 (US1): API de alunos completa.
3. **PARAR e VALIDAR** (T040): `./mvnw clean verify` + cenários da quickstart. Este já é um CRUD
   REST avaliável.

### Incremental Delivery

1. US1 → API de alunos (MVP).
2. US2 → API de professores. Com isso o back-end fica completo, cobrindo os itens obrigatórios do
   desafio: CRUD + testes.
3. US3 → Docker, dados de demonstração, H2 e README da API (diferencial: container).
4. US4 → interface Vue em container (diferencial: front-end).
5. Polish → README final, roteiro dos vídeos e auditoria do Definition of Done.

Cada checkpoint (T026, T040, T054, T060, T095, T100) deixa o projeto entregável até aquele ponto.

---

## Rastreabilidade: casos de borda → verificação

| Caso de borda (spec) | Verificação |
|----------------------|-------------|
| Id não numérico | T038 (`GET /abc`) |
| Id inexistente (inclusive 0 e negativos) | T038, T039 (mesmo caminho de "não encontrado") |
| Id de um tipo usado no recurso do outro | T046, T053 |
| Corpo vazio, JSON malformado, tipo incorreto | T038, T052 (`salary: "abc"`) |
| Vários campos inválidos na mesma requisição | T038 |
| Texto só com espaços | T038 |
| `addresses` ausente, nula ou vazia | T038 |
| Endereço incompleto (posição na lista) | T038 |
| `status` inválido (corpo, filtro, minúsculas, repetido) | T038 |
| `status` omitido na criação | T034, T036 |
| E-mail duplicado entre pessoas; manter o próprio | T024, T036, T039, T053 |
| Matrícula duplicada | T032, T036, T039 |
| `salary` inválido | T052 |
| `photo` inválida ou ausente | T038 |
| Excluir a mesma pessoa duas vezes | T039 |
| Exclusão remove os endereços | T032, T039 |
| `PUT` substitui a lista de endereços | T032, T036, T039 |
| Endereços idênticos na mesma pessoa | T032, T039 |
| `id` no corpo e campos desconhecidos | T038 |
| 405 e 415 | T038 |
| 500 genérico | T038 |
| Origem cruzada recusada (403) | T038 |
| Rota inexistente sob `/api` | T039 |
| `Accept` sem JSON (406) | T038 |
| Gravações simultâneas com o mesmo e-mail ou matrícula | T038 (mapeamento de `DataIntegrityViolationException` → 409); a corrida em si fica **fora da verificação automatizada** |
| Duas edições simultâneas (prevalece a última gravação) | **fora da verificação automatizada** (premissa da spec) |
| "Desativar" sobre dados alterados por outro usuário | T073 (consulta antes do `PUT`) + quickstart §5, passo 4 (manual); a corrida fica **fora da verificação automatizada** |
| Interface sem comunicação com a API | T093 + quickstart §5, passo 7 (manual) |
| Reinício apaga os dados | **fora da verificação automatizada** (comportamento do banco em memória; conferível manualmente) |

## Notes

- `[P]` = arquivos diferentes e sem dependência pendente. `[USn]` liga a tarefa à história da spec.
- O código de cada camada só é considerado pronto com o teste correspondente (Princípio IV).
- Boot 4.1.1: os pacotes de teste (`org.springframework.boot.webmvc.test.autoconfigure`,
  `org.springframework.boot.data.jpa.test.autoconfigure`), o Jackson 3 (`tools.jackson.*`) e o
  `@MockitoBean` estão em [research.md](research.md) R2.
- Correções do `/speckit-analyze` de 2026-10-19 aplicadas: U1–U8, I1–I8 e CG1–CG3 (I2 via
  FR-026a; CG2 via T106). As lacunas do [checklist de validação](checklists/validation.md) foram
  fechadas na spec, no data-model e nos contratos.
- Notas da implementação (2026-10-19):
  - T001: o Initializr já gerou `spring-boot-starter-webmvc`, `spring-boot-h2console` e os starters de
    teste do Boot 4; o `spring-boot-starter-validation-test` gerado foi mantido.
  - T021/T033/T047: `Address.id`, `Address.person` e `Person.id` não têm setter público, então o
    MapStruct não os trata como destino e só `addresses` precisa de `ignore`.
  - T029/T043: `@Valid` aplicado no argumento de tipo (`List<@Valid AddressRequest>`), forma
    recomendada pelo Hibernate Validator 9.
  - T067: respostas 502/503/504 do proxy (Vite/Nginx) também viram "falha de comunicação".
  - Ambiente local: o Java 21 instalado era só JRE; o build usou um JDK 21 portátil
    (`JAVA_HOME=~/.local/jdks/jdk-21.0.12.1+1`).

## Phase 8: Convergence

- [X] T107 CRITICAL: substituir em `README.md` (seção "Professores") o comando `curl -X PUT http://localhost:8080/api/professors/2 ... -d '{ ... }'` por um corpo JSON completo e válido, e executar todos os `curl` dessa seção na ordem documentada, per Constitution VI (contradicts)
- [X] T108 Acrescentar em `README.md` o exemplo de resposta (status e JSON ou corpo vazio) de cada operação que ainda só tem a requisição: listar, buscar, atualizar e excluir alunos, e listar, buscar, atualizar e excluir professores, per FR-030 (partial)
- [X] T109 Fazer `GET /api/students` com `Accept: text/html` retornar 406 sem corpo em `backend/src/main/java/br/com/bomjesus/academic/exception/GlobalExceptionHandler.java` (sobrescrevendo `handleHttpMediaTypeNotAcceptable`) e cobrir em `backend/src/test/java/br/com/bomjesus/academic/controller/StudentControllerTest.java`; ou registrar na spec que o corpo `application/problem+json` é aceito, per Spec Edge Cases/406 (contradicts)
- [ ] T110 Verificar manualmente em `http://localhost:8081` a navegação só por teclado (Tab, Enter/Espaço, Esc nos diálogos, foco no `h1` após salvar, desativar e excluir) e o funcionamento no Firefox e no Edge, registrando o resultado na descrição da entrega, per FR-025a/FR-025c (partial)
