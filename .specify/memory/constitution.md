# Bom Jesus Challenge Constitution

Documento de governança técnica do projeto de CRUD acadêmico, com back-end Spring Boot e front-end
Vue.js. As palavras-chave **DEVE**, **NÃO DEVE**, **DEVERIA** e **PODE** têm o mesmo significado
de MUST, MUST NOT, SHOULD e MAY da RFC 2119.

## Core Principles

### I. Stack Back-end

- O back-end DEVE ser escrito em **Java 17 ou superior** (`java.version`/`maven.compiler.release`
  ≥ 17).
- O framework DEVE ser **Spring Boot 3.x ou superior**, usando `spring-boot-starter-web`,
  `spring-boot-starter-data-jpa` e `spring-boot-starter-validation`.
- A persistência DEVE usar **Spring Data JPA (Hibernate)** sobre **H2 em memória**
  (`jdbc:h2:mem:`). Nenhum outro banco de dados é permitido. A perda de dados ao reiniciar a
  aplicação é aceita.
- O build DEVE usar **Maven**. O projeto DEVERIA incluir o Maven Wrapper (`mvnw`, `mvnw.cmd`,
  `.mvn/`) para builds reproduzíveis sem Maven instalado.
- O código do back-end DEVE residir no diretório `backend/` como um projeto Maven independente.
- Dependências fora desta lista (ex.: Lombok, MapStruct, springdoc-openapi) PODEM ser adotadas
  somente se justificadas no `plan.md` da funcionalidade.

**Rationale**: uma stack fechada e conhecida elimina decisões repetidas, garante que o projeto
rode em qualquer máquina com JDK 17+ (ou Docker) e mantém o foco no domínio.

### II. API RESTful Rigorosa com DTOs

- Recursos DEVEM ser nomeados por substantivos em inglês no plural, coerentes com o modelo de
  domínio (ex.: `/api/<recursos>`, `/api/<recursos>/{id}`); verbos NÃO DEVEM aparecer nas URIs.
- Os métodos HTTP DEVEM seguir sua semântica: `GET` (consulta), `POST` (criação), `PUT`
  (atualização completa), `PATCH` (atualização parcial, opcional) e `DELETE` (remoção).
- Os códigos de status DEVEM ser exatamente:
  - **200 OK** — `GET` bem-sucedido; `PUT`/`PATCH` bem-sucedido com corpo de resposta.
  - **201 Created** — `POST` bem-sucedido, com cabeçalho `Location` apontando para o novo recurso
    e o recurso criado no corpo.
  - **204 No Content** — `DELETE` bem-sucedido, sem corpo.
  - **400 Bad Request** — falha de validação, JSON malformado ou parâmetro de tipo inválido.
  - **404 Not Found** — recurso inexistente em `GET /{id}`, `PUT`, `PATCH` ou `DELETE`.
  - **409 Conflict** — violação de unicidade ou conflito de regra de negócio, quando aplicável.
  - **500 Internal Server Error** — somente para erros inesperados, sem expor stack trace.
- Controllers DEVEM retornar `ResponseEntity` com o status explícito.
- Entidades JPA NÃO DEVEM aparecer em assinaturas de Controller (nem entrada, nem saída). Toda
  entrada e saída DEVE usar DTOs, separando DTO de requisição (com Bean Validation) e DTO de
  resposta. DTOs DEVERIAM ser `record`s Java.
- O cliente NÃO DEVE conseguir definir o `id` nem campos gerenciados pelo servidor via DTO de
  requisição.
- Exceções DEVEM ser convertidas em respostas HTTP exclusivamente por um **tratador global único**
  (`@RestControllerAdvice`). Controllers e Services NÃO DEVEM montar respostas de erro
  manualmente nem capturar exceções apenas para traduzi-las.
- Todas as respostas de erro DEVEM ter um formato único e consistente, preferencialmente
  RFC 9457 (Problem Details, via `ProblemDetail` do Spring), incluindo a lista de erros por campo
  em falhas de validação.

**Rationale**: códigos de status corretos, contratos via DTO e erros padronizados tornam a API
previsível para qualquer consumidor (incluindo o front-end) e desacoplam o contrato público do
modelo de persistência.

### III. Clean Code e Separação Estrita de Camadas

- O back-end DEVE ser organizado nas camadas **Controller**, **Service**, **Repository**,
  **Model/Entity** e **DTO**, cada uma em seu próprio pacote (`controller`, `service`,
  `repository`, `model`, `dto`). Pacotes de suporte permitidos: `exception`, `mapper`, `config`.
- A direção de dependência DEVE ser **Controller → Service → Repository**. Controllers NÃO DEVEM
  injetar Repositories; Repositories NÃO DEVEM ser acessados fora da camada Service.
- Controllers DEVEM apenas receber a requisição, acionar a validação (`@Valid`), delegar ao
  Service e montar a `ResponseEntity`. Regras de negócio NÃO DEVEM existir em Controllers.
- Services DEVEM concentrar as regras de negócio, receber e retornar DTOs e delimitar transações
  (`@Transactional`, com `readOnly = true` em consultas). A conversão Entity ↔ DTO DEVE ficar em
  classes `mapper` dedicadas.
- A injeção de dependências DEVE ser por construtor, com campos `final`. Injeção por campo
  (`@Autowired` em atributo) NÃO DEVE ser usada.
- Práticas de Clean Code obrigatórias (back-end e front-end): nomes que revelam intenção; métodos
  e funções curtos com responsabilidade única; sem código morto ou comentado; sem
  números/strings mágicos; exceções NÃO DEVEM ser capturadas e ignoradas; erros de domínio DEVEM
  usar exceções específicas (ex.: `ResourceNotFoundException`).
- A organização do front-end segue o Princípio VIII.

**Rationale**: camadas com responsabilidades e dependências bem definidas tornam o código
legível, testável isoladamente e seguro para evoluir.

### IV. Testes Unitários Obrigatórios (NÃO NEGOCIÁVEL)

- Os testes do back-end DEVEM usar **JUnit Jupiter** (JUnit 5 ou superior, na versão gerenciada
  pelo Spring Boot adotado) e **Mockito**.
- Todo método público de Service DEVE ter testes unitários cobrindo o caminho de sucesso e cada
  caminho de falha (ex.: recurso inexistente, violação de unicidade). Esses testes DEVEM usar
  `@ExtendWith(MockitoExtension.class)` com o Repository mockado e sem subir o contexto Spring.
- Os testes DEVEM cobrir separadamente o comportamento específico de cada subtipo de `Person`
  (`Student` e `Professor`) e as regras de `Address` (ex.: obrigatoriedade de ao menos um
  endereço).
- Todo endpoint de Controller DEVE ter testes verificando o status HTTP, o corpo da resposta e os
  cenários de erro (400 de validação e 404 no mínimo), usando `@WebMvcTest` + `MockMvc` com o
  Service mockado (`@MockitoBean`) ou MockMvc standalone com Mockito.
- A cobertura do back-end DEVE ser medida com **JaCoCo**, com mínimo de **80% de linhas** nos
  pacotes `service` e `controller`; o build DEVE falhar abaixo desse limite.
- Os testes DEVEM seguir a estrutura Arrange-Act-Assert (ou Given-When-Then) e ter nomes que
  descrevam cenário e resultado esperado.
- `./mvnw test` (ou `mvn test`) DEVE passar sem nenhuma dependência externa.
- Uma funcionalidade NÃO DEVE ser considerada concluída sem seus testes. Testes de repositório
  (`@DataJpaTest`) e de integração PODEM ser adicionados como complemento.
- Testes do front-end (ex.: Vitest + Vue Test Utils) PODEM ser adicionados e não entram no gate de
  cobertura.

**Rationale**: testes automatizados são a evidência objetiva de que cada camada cumpre seu
contrato, incluindo os códigos de status exigidos pelo Princípio II e as regras do Princípio VII.

### V. Containerização com Docker

- O back-end DEVE ser empacotável e executável em container a partir de um `Dockerfile` versionado
  no repositório.
- O `Dockerfile` DEVERIA ser **multi-stage**: um estágio de build com Maven + JDK 17+ e um
  estágio de runtime apenas com JRE 17+ contendo somente o JAR final. Se for usado um
  `Dockerfile` padrão (single-stage, copiando um JAR pré-gerado), o README DEVE documentar o
  passo de build anterior.
- O empacotamento do front-end em container é OPCIONAL. Se existir, DEVE seguir **uma** das
  abordagens abaixo, justificada no `plan.md`:
  - **Imagens separadas**: `backend/Dockerfile` e `frontend/Dockerfile` (multi-stage: build com
    Node.js e runtime com servidor estático enxuto, ex.: Nginx), orquestrados por um
    `docker-compose.yml` na raiz que sobe ambos com um único comando.
  - **Imagem única**: `Dockerfile` na raiz, multi-stage (Node.js → Maven/JDK → JRE), com o
    front-end compilado sendo servido pelo próprio Spring Boot.
- Sem front-end containerizado, o `Dockerfile` do back-end DEVE ficar em `backend/Dockerfile`.
- Os `Dockerfile`s DEVEM ser otimizados: resolver dependências (`pom.xml`, `package.json`) antes
  de copiar o código-fonte (cache de camadas); usar imagens base oficiais e enxutas (ex.:
  `eclipse-temurin`, `node`, `nginx`); executar como usuário não-root; expor portas explícitas
  (back-end: `8080`).
- Cada contexto de build DEVE ter um `.dockerignore` excluindo `target/`, `node_modules/`,
  `dist/`, `.git/` e arquivos de IDE.
- Os comandos documentados (`docker build` + `docker run`, ou `docker compose up`) DEVEM resultar
  em uma aplicação funcional sem configuração adicional. O build da imagem PODE pular os testes,
  pois eles são um gate separado.

**Rationale**: a imagem garante execução idêntica em qualquer ambiente; o multi-stage reduz o
tamanho da imagem e a superfície de ataque; um único comando facilita a avaliação.

### VI. Documentação via README.md

- O repositório DEVE conter um `README.md` na raiz, redigido em português, com as seções:
  1. Descrição do projeto e do domínio (incluindo o diagrama do modelo, versionado em `docs/`).
  2. Pré-requisitos (versões de Java, Maven ou Maven Wrapper, Docker e, se houver front-end,
     Node.js).
  3. Build do back-end (`./mvnw clean package`) e, se houver, do front-end (`npm run build`).
  4. Execução local (back-end e front-end) e via Docker (`docker build`/`docker run` ou
     `docker compose up`), com as URLs resultantes.
  5. Testes: como executá-los e onde encontrar o relatório de cobertura JaCoCo.
  6. Endpoints: método, caminho, exemplo de requisição/resposta e códigos de status possíveis.
  7. Acesso ao H2 Console, se habilitado (URL, JDBC URL, usuário).
  8. Configuração (variáveis de ambiente, ex.: URL da API usada pelo front-end).
  9. Estrutura do repositório e de pacotes.
- Todo comando do README DEVE ser copiável e ter sido executado com sucesso antes da entrega.
- Qualquer mudança em endpoints, build, execução, configuração ou testes DEVE atualizar o README
  na mesma alteração.

**Rationale**: o README é o ponto de entrada de avaliadores e mantenedores; documentação
desatualizada ou não verificada equivale a não ter documentação.

### VII. Modelagem de Domínio Acadêmico

- O modelo de domínio DEVE seguir a modelagem de referência do desafio
  (`modelagem-bomjesus.png`), que é a fonte da verdade:
  - **`Person`** — superclasse **abstrata** com `name`, `phoneNumber`, `emailAddress` e `status`.
  - **`Student`** — estende `Person` e acrescenta `studentNumber` e `photo`.
  - **`Professor`** — estende `Person` e acrescenta `salary`.
  - **`Status`** — enum com exatamente os valores `ACTIVE` e `DISABLE`.
  - **`Address`** — `street`, `city`, `state`, `zipCode` e `country`.
  - **`Person` 1 → 1..\* `Address`** — toda pessoa DEVE ter ao menos um endereço; cada endereço
    pertence a exatamente uma pessoa.
- A generalização DEVE ser mapeada com herança JPA real: `Person` como `@Entity` abstrata com
  `@Inheritance`. A estratégia DEVERIA ser `InheritanceType.JOINED`; outra estratégia DEVE ser
  justificada no `plan.md`. `@MappedSuperclass` NÃO DEVE ser usado para `Person`, pois impede a
  associação polimórfica com `Address` e a unicidade entre subtipos.
- `Address` DEVE ser tratado como composição de `Person`: ciclo de vida em cascata e remoção de
  órfãos (`cascade = ALL`, `orphanRemoval = true`). Um endereço NÃO DEVE existir sem sua pessoa.
- Enums DEVEM ser persistidos como texto (`@Enumerated(EnumType.STRING)`); `ORDINAL` NÃO DEVE ser
  usado.
- Valores monetários (`salary`) DEVEM usar `BigDecimal`; `double`/`float` NÃO DEVEM ser usados.
- Identificadores de código (classes, atributos, campos JSON, valores de enum) DEVEM usar
  exatamente os nomes em inglês do modelo. Mensagens ao usuário e documentação permanecem em
  português.
- Atributos ou entidades fora da modelagem de referência DEVEM ser justificados na especificação
  da funcionalidade.

**Rationale**: o modelo é um requisito explícito do desafio; segui-lo fielmente, com herança e
composição mapeadas corretamente, é o que os avaliadores verificam primeiro.

### VIII. Front-end Vue.js (Diferencial)

- O front-end DEVE usar **Vue.js 3** com **Composition API** (`<script setup>`), **Vite** como
  ferramenta de build e **Axios** para HTTP. A Options API NÃO DEVE ser usada.
- O código do front-end DEVE residir no diretório `frontend/`, organizado em `views/` (páginas),
  `components/` (componentes reutilizáveis), `services/` (acesso à API), `composables/` (lógica
  reutilizável) e `router/` (Vue Router).
- Todo acesso HTTP DEVE passar por uma instância Axios centralizada em `services/`, com a URL base
  definida por variável de ambiente (`VITE_API_BASE_URL`). Componentes e views NÃO DEVEM chamar o
  Axios diretamente.
- O front-end DEVE consumir exclusivamente a API REST pública. O back-end é a fonte da verdade
  das regras de negócio; o front-end PODE validar campos para melhorar a experiência, mas DEVE
  exibir ao usuário as mensagens de erro retornadas pela API (400 por campo, 404, 409).
- Dependências adicionais (ex.: Pinia, biblioteca de componentes de UI, TypeScript) PODEM ser
  adotadas se justificadas no `plan.md`. ESLint e Prettier DEVERIAM estar configurados.
- O front-end é um **diferencial**: o back-end DEVE estar completo, testado e utilizável de forma
  independente (via cliente HTTP) antes de qualquer trabalho de front-end, e o front-end NÃO DEVE
  comprometer os gates do back-end.

**Rationale**: Vue 3 com Composition API e Vite é a stack moderna e enxuta pedida como
diferencial; centralizar o acesso HTTP mantém a interface desacoplada da API e fácil de
configurar por ambiente.

## Padrões de Implementação e Restrições Técnicas

- **Estrutura do repositório**: `backend/` (projeto Maven), `frontend/` (projeto Vite, quando
  existir), `docs/` (diagrama do modelo e materiais de apoio), `README.md` e, quando aplicável,
  `docker-compose.yml` na raiz.
- **Tratamento de erros**: o tratador global (Princípio II) DEVE mapear:
  `ResourceNotFoundException` → 404; `MethodArgumentNotValidException`,
  `HttpMessageNotReadableException` e `MethodArgumentTypeMismatchException` → 400; conflitos de
  integridade/negócio → 409; demais exceções → 500 com mensagem genérica.
- **Validação**: restrições de formato DEVEM ser declaradas com Bean Validation nos DTOs de
  requisição, incluindo DTOs aninhados (`@Valid` em listas de endereços); regras que dependem de
  estado (ex.: unicidade) DEVEM ser verificadas no Service.
- **Identificadores**: entidades DEVERIAM usar `Long id` com
  `@GeneratedValue(strategy = GenerationType.IDENTITY)`, definido na raiz da hierarquia.
- **CORS**: quando front-end e back-end rodarem em origens diferentes, o CORS DEVE ser configurado
  de forma centralizada no pacote `config`, com as origens permitidas definidas por
  propriedade/variável de ambiente (padrão: origem do servidor de desenvolvimento do Vite). O
  curinga `*` NÃO DEVE ser usado.
- **Configuração**: DEVE ficar em `application.properties` ou `application.yml` (back-end) e em
  arquivos `.env` (front-end); valores sensíveis NÃO DEVEM ser fixados no código-fonte. O H2
  Console, se habilitado, DEVE ser documentado no README.
- **Logging**: o back-end DEVE usar SLF4J; `System.out`/`System.err` e `printStackTrace()` NÃO
  DEVEM ser usados. O front-end NÃO DEVE deixar `console.log` de depuração no código entregue.
- **Simplicidade (YAGNI)**: abstrações sem necessidade concreta (frameworks CRUD genéricos,
  camadas extras, arquitetura hexagonal) NÃO DEVEM ser introduzidas. Qualquer complexidade além
  desta constituição DEVE ser justificada na seção "Complexity Tracking" do `plan.md`.

## Fluxo de Desenvolvimento e Quality Gates

- Funcionalidades DEVEM seguir o fluxo Spec Kit: `/speckit-specify` → `/speckit-clarify`
  (quando houver ambiguidade) → `/speckit-plan` → `/speckit-tasks` → `/speckit-implement`.
- O "Constitution Check" do `plan.md` DEVE verificar explicitamente os Princípios I–VIII antes da
  pesquisa (Phase 0) e novamente após o design (Phase 1).
- A ordem de entrega DEVE ser: back-end completo e testado → containerização → front-end.
- Uma entrega só é considerada pronta (Definition of Done) quando:
  1. `./mvnw clean verify` passa (compilação, testes e limite de cobertura JaCoCo).
  2. Todos os endpoints retornam os status do Princípio II, comprovados por testes de Controller.
  3. O modelo implementado corresponde ao Princípio VII (herança, enum, relação `1..*`).
  4. O build Docker documentado conclui com sucesso e a API responde na porta 8080.
  5. Se houver front-end: `npm run build` e o lint concluem sem erros, e os fluxos CRUD
     funcionam contra a API em execução.
  6. O README está atualizado e seus comandos foram validados.
  7. A revisão confirma: nenhuma Entity exposta em Controller, nenhum Repository injetado em
     Controller e nenhuma chamada Axios fora de `services/`.
- Commits DEVERIAM seguir Conventional Commits (`feat:`, `fix:`, `test:`, `docs:`, `chore:`).

## Governance

- Esta constituição prevalece sobre quaisquer outras práticas, preferências ou convenções do
  projeto. Em caso de conflito entre um artefato (spec, plan, tasks, código) e a constituição, a
  constituição vence e o artefato DEVE ser corrigido.
- **Emendas** DEVEM ser feitas via `/speckit-constitution`, registradas com um Sync Impact
  Report, e DEVEM vir acompanhadas da atualização de specs, plans e tasks afetados.
- **Versionamento** segue SemVer:
  - MAJOR — remoção ou redefinição incompatível de princípios ou regras de governança.
  - MINOR — novo princípio/seção ou expansão material de orientação existente.
  - PATCH — esclarecimentos, redação e correções sem mudança semântica.
- **Conformidade**: toda revisão de código DEVE verificar aderência a esta constituição. Desvios
  DEVEM ser registrados com justificativa na tabela "Complexity Tracking" do `plan.md`;
  desvios do Princípio IV não são admitidos. `/speckit-analyze` DEVERIA ser executado após
  `/speckit-tasks` para detectar inconsistências entre artefatos e a constituição.
- Orientações de execução e uso do projeto ficam no `README.md` (Princípio VI).

**Version**: 1.1.1 | **Ratified**: 2026-10-19 | **Last Amended**: 2026-10-19
