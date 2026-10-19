# Feature Specification: Gestão de Alunos e Professores (API REST + Interface Web)

**Feature Branch**: `002-students-professors-crud` (repositório ainda sem git; nome reservado)

**Created**: 2026-10-19

**Status**: Draft

**Input**: User description:

> Crie a especificação técnica detalhada para o sistema CRUD baseado no diagrama de classes
> fornecido: **Enum `Status`**: `ACTIVE`, `DISABLE`. **Classe `Person` (superclasse abstrata)**:
> `name`, `phoneNumber`, `emailAddress`, `status` (enum) e lista de `addresses` (`Address`,
> 1..\*). **Classe `Address`**: `street`, `city`, `state`, `zipCode`, `country`. **Classe
> `Student`**: herda de `Person` + `studentNumber`, `photo`. **Classe `Professor`**: herda de
> `Person` + `salary`. **Endpoints REST**: Gestão de Alunos (`/api/students`) e Gestão de
> Professores (`/api/professors`), cada uma com CRUD completo (POST, GET all, GET by ID, PUT,
> DELETE). **Front-end (Vue.js)**: interface SPA simples e limpa para listar, cadastrar, editar e
> desativar Alunos e Professores, consumindo os endpoints REST.

## Clarifications

### Session 2026-10-19

- Q: `DELETE` remove a pessoa definitivamente ou apenas a desativa (`DISABLE`)? → A: Remove
  definitivamente (busca posterior → 404); desativar é atualizar o status para `DISABLE`. A
  interface oferece "Desativar" como ação principal e também "Excluir" definitivo, com
  confirmação reforçada.
- Q: A foto do aluno é um link ou um arquivo enviado? → A: Link (URL `http`/`https`, até 500
  caracteres), opcional; sem upload de arquivos.
- Q: Que estratégia de herança o banco deve usar para guardar `Person`, `Student` e `Professor`?
  → A: `JOINED` — tabela `person` (campos comuns e status) + tabelas `student` e `professor`
  (campos próprios), ligadas pelo mesmo identificador.
- Q: Na atualização, os endereços enviados substituem a lista inteira ou são casados pelo
  identificador? → A: Substituição completa; endereços não têm identificador no contrato, e os
  que deixam a lista são removidos (cascata total + remoção de órfãos).
- Q: Onde o front-end em Vue.js deve ficar e como deve ser entregue junto com o back-end? → A:
  Mesmo repositório, pastas irmãs `backend/` e `frontend/` com builds independentes; em
  containers, duas imagens (API e interface servida por servidor web estático que repassa `/api`
  à API) iniciadas juntas por um único comando de orquestração na raiz.

## User Scenarios & Testing *(mandatory)*

### User Story 1 - Gerenciar alunos pela API (Priority: P1)

A secretaria acadêmica (consumidora da API) precisa cadastrar alunos com seus dados de contato,
número de matrícula, foto e um ou mais endereços; consultá-los; atualizar seus dados e retirá-los
do quadro ativo da instituição.

**Why this priority**: Aluno é a entidade central do domínio escolar e exercita toda a modelagem:
herança de pessoa, status e a relação com endereços. Sozinho, este fluxo já entrega um CRUD
completo, validado e demonstrável.

**Independent Test**: Pode ser testado integralmente por um cliente HTTP, criando, listando,
buscando, atualizando e excluindo alunos, sem depender de professores nem da interface web.

**Acceptance Scenarios**:

1. **Dado** que nenhum aluno existe, **Quando** o cliente envia um aluno válido com um endereço
   para `POST /api/students`, **Então** a API responde **201 Created** com o aluno criado no
   corpo (incluindo identificador gerado, status `ACTIVE` e endereços) e o endereço do novo
   recurso no cabeçalho `Location`.
2. **Dado** que existem alunos cadastrados, **Quando** o cliente consulta `GET /api/students`,
   **Então** recebe **200 OK** com todos os alunos em ordem alfabética de nome; com o filtro
   `?status=ACTIVE` ou `?status=DISABLE`, recebe apenas os alunos daquele status.
3. **Dado** que nenhum aluno existe, **Quando** o cliente consulta `GET /api/students`, **Então**
   recebe **200 OK** com uma lista vazia.
4. **Dado** um aluno existente, **Quando** o cliente consulta `GET /api/students/{id}`, **Então**
   recebe **200 OK** com todos os dados do aluno, incluindo seus endereços.
5. **Dado** um identificador que não corresponde a nenhum aluno, **Quando** o cliente consulta,
   atualiza ou exclui `/api/students/{id}`, **Então** recebe **404 Not Found** com mensagem
   informando que o aluno não foi encontrado.
6. **Dado** um aluno com dois endereços, **Quando** o cliente envia para `PUT /api/students/{id}`
   dados completos e válidos com apenas um endereço, **Então** recebe **200 OK**, o aluno passa a
   ter somente o endereço enviado e consultas posteriores refletem a alteração.
7. **Dado** um aluno existente, **Quando** o cliente envia `DELETE /api/students/{id}`, **Então**
   recebe **204 No Content** sem corpo, o aluno e seus endereços são removidos definitivamente, e
   uma consulta posterior ao mesmo identificador retorna **404 Not Found**.
8. **Dado** um aluno com matrícula "2026001" ou e-mail "ana@escola.com", **Quando** o cliente
   cria ou atualiza outro aluno com a mesma matrícula, ou qualquer pessoa (aluno ou professor) com
   o e-mail "ANA@escola.com", **Então** recebe **409 Conflict** e nada é alterado.
9. **Dado** qualquer estado, **Quando** o cliente envia um aluno sem endereços, com e-mail mal
   formatado, telefone inválido, campos obrigatórios ausentes ou um endereço incompleto, **Então**
   recebe **400 Bad Request** listando cada campo inválido (incluindo a posição do endereço, ex.:
   `addresses[0].city`) com o respectivo motivo, e nada é gravado.

---

### User Story 2 - Gerenciar professores pela API (Priority: P2)

A secretaria acadêmica precisa cadastrar professores com dados de contato, salário e endereços,
consultá-los, atualizá-los e retirá-los do quadro ativo.

**Why this priority**: Completa a generalização do modelo (segundo subtipo de pessoa) reutilizando
as regras comuns de pessoa, status e endereços validadas na User Story 1.

**Independent Test**: Pode ser testado integralmente por um cliente HTTP, executando o CRUD
completo em `/api/professors`, sem depender de alunos nem da interface web.

**Acceptance Scenarios**:

1. **Dado** que nenhum professor existe, **Quando** o cliente envia um professor válido (com
   salário e ao menos um endereço) para `POST /api/professors`, **Então** recebe **201 Created**
   com o professor criado no corpo e o cabeçalho `Location`.
2. **Dado** professores cadastrados, **Quando** o cliente consulta `GET /api/professors` (com ou
   sem filtro de status) ou `GET /api/professors/{id}`, **Então** recebe **200 OK** com os dados
   correspondentes, com o salário com duas casas decimais.
3. **Dado** um professor existente, **Quando** o cliente envia dados completos e válidos para
   `PUT /api/professors/{id}`, **Então** recebe **200 OK** com o professor atualizado.
4. **Dado** um professor existente, **Quando** o cliente envia `DELETE /api/professors/{id}`,
   **Então** recebe **204 No Content**, o professor e seus endereços são removidos
   definitivamente, e uma consulta posterior retorna **404 Not Found**.
5. **Dado** qualquer estado, **Quando** o cliente envia um professor com salário ausente, zero,
   negativo, com mais de duas casas decimais ou acima do limite, **Então** recebe
   **400 Bad Request** identificando o campo `salary` e o motivo.
6. **Dado** um aluno com e-mail "ana@escola.com", **Quando** o cliente cria um professor com o
   mesmo e-mail, **Então** recebe **409 Conflict**.
7. **Dado** um identificador inexistente, **Quando** o cliente consulta, atualiza ou exclui
   `/api/professors/{id}`, **Então** recebe **404 Not Found**.

---

### User Story 3 - Executar, inspecionar e demonstrar o sistema (Priority: P3)

Uma pessoa avaliadora (ou o próprio candidato, ao gravar os vídeos) precisa subir o sistema
localmente ou em containers seguindo apenas o README, inspecionar os dados pelo console web do
banco em memória, carregar dados de demonstração e seguir um roteiro pronto para os vídeos técnico
e de produto.

**Why this priority**: O desafio é avaliado por terceiros, e a execução em container é um
diferencial importante. Sem execução reproduzível e instruções claras, as funcionalidades não
podem ser verificadas nem apresentadas.

**Independent Test**: Em uma máquina com apenas os pré-requisitos do README, seguir o README do
início ao fim: build, testes, execução local e em container, carga de dados de demonstração e
acesso ao console do banco.

**Acceptance Scenarios**:

1. **Dado** uma máquina com os pré-requisitos instalados, **Quando** a pessoa segue as seções de
   build e execução local do README, **Então** a API passa a responder na porta 8080.
2. **Dado** uma máquina com Docker instalado, **Quando** a pessoa executa os comandos de container
   descritos no README, **Então** a API (e a interface web, quando entregue) passa a responder sem
   nenhuma configuração adicional.
3. **Dado** o sistema em execução, **Quando** a pessoa acessa o console do banco no endereço e com
   as credenciais documentados, **Então** visualiza as tabelas de pessoas, alunos, professores e
   endereços com os registros criados.
4. **Dado** o sistema em execução, **Quando** a pessoa segue o procedimento de carga de dados de
   demonstração do README, **Então** passam a existir ao menos 3 alunos e 2 professores, com
   ambos os status e ao menos uma pessoa com mais de um endereço.
5. **Dado** o projeto clonado, **Quando** a pessoa executa o comando de testes do README, **Então**
   todos os testes passam e o relatório de cobertura é gerado no local indicado.
6. **Dado** o README, **Quando** a pessoa lê a seção de roteiro dos vídeos, **Então** encontra um
   roteiro de produto e um técnico, cada um planejado para 2 a 5 minutos.

---

### User Story 4 - Operar os cadastros pela interface web (Priority: P4)

A secretaria acadêmica quer uma interface web simples e limpa, sem precisar de um cliente HTTP,
para listar, cadastrar, editar, desativar e, quando necessário, excluir alunos e professores.

**Why this priority**: É um diferencial do desafio. Pela constituição, só é construída depois que
a API estiver completa, testada e containerizada (User Stories 1 a 3).

**Independent Test**: Com a API em execução, operar a interface para cadastrar, listar, editar e
desativar um aluno e um professor, conferindo cada resultado também pela API.

**Acceptance Scenarios**:

1. **Dado** a interface aberta, **Quando** a pessoa alterna entre as seções "Alunos" e
   "Professores", **Então** vê a lista correspondente com nome, e-mail, telefone, status e o campo
   específico (matrícula e foto para alunos; salário em reais para professores).
2. **Dado** a lista de alunos, **Quando** a pessoa filtra por "Ativos", "Inativos" ou "Todos",
   **Então** a lista mostra apenas as pessoas do status escolhido.
3. **Dado** o formulário de cadastro, **Quando** a pessoa preenche os dados, adiciona um ou mais
   endereços e salva, **Então** vê uma mensagem de sucesso e a nova pessoa aparece na lista.
4. **Dado** o formulário, **Quando** a pessoa tenta salvar com campos inválidos, sem endereço ou
   com e-mail/matrícula já existente, **Então** a interface exibe junto a cada campo a mensagem de
   erro correspondente (vinda da validação local ou da API) e não perde os dados digitados.
5. **Dado** uma pessoa na lista, **Quando** a pessoa escolhe "Editar", altera dados (inclusive
   adicionando ou removendo endereços, mantendo ao menos um) e salva, **Então** as alterações
   aparecem na lista.
6. **Dado** uma pessoa ativa na lista, **Quando** a pessoa escolhe "Desativar" e confirma,
   **Então** a pessoa passa a constar com status `DISABLE`, sem precisar recarregar a página; se
   cancelar a confirmação, nada muda.
7. **Dado** uma pessoa inativa, **Quando** a pessoa edita o status para `ACTIVE` e salva,
   **Então** a pessoa volta a constar como ativa.
8. **Dado** uma pessoa na lista, **Quando** a pessoa escolhe "Excluir", **Então** vê um diálogo
   avisando que a exclusão é definitiva e irreversível, com o nome da pessoa e um botão
   "Excluir definitivamente"; ao confirmar, a pessoa some da lista; ao cancelar, nada muda.
9. **Dado** o formulário de aluno, **Quando** a pessoa cola o link de uma foto, **Então** vê a
   pré-visualização da imagem; se o link não carregar, vê uma imagem padrão no lugar.
10. **Dado** que a API está indisponível, **Quando** a interface tenta carregar ou salvar dados,
   **Então** exibe uma mensagem clara de falha de comunicação com opção de tentar novamente.
11. **Dado** que não há registros, **Quando** a pessoa abre uma lista, **Então** vê uma mensagem
   de lista vazia com atalho para cadastrar.

---

### Edge Cases

- Identificador não numérico no caminho (ex.: `/api/students/abc`) → **400 Bad Request**.
- Identificador numérico inexistente (inclusive zero ou negativo) → **404 Not Found**.
- Identificador de aluno usado em `/api/professors/{id}` (ou o inverso) → **404 Not Found**: cada
  recurso só enxerga o próprio tipo.
- Corpo vazio, JSON malformado ou tipo de campo incorreto (ex.: texto em `salary`) →
  **400 Bad Request**.
- Vários campos inválidos na mesma requisição → uma única resposta **400** listando todos eles.
- Campo de texto obrigatório contendo apenas espaços → tratado como vazio (**400**).
- Lista `addresses` ausente, nula ou vazia → **400** (toda pessoa precisa de ao menos um
  endereço).
- Endereço com campo obrigatório ausente → **400** indicando a posição do endereço na lista.
- Valor de `status` fora de `ACTIVE`/`DISABLE` (no corpo ou no filtro da listagem) → **400**.
- `status` omitido na criação → a pessoa é criada como `ACTIVE`.
- E-mail já usado por qualquer pessoa (aluno ou professor), ignorando maiúsculas/minúsculas e
  espaços nas extremidades → **409 Conflict**; atualizar uma pessoa mantendo o próprio e-mail não
  é conflito.
- Matrícula (`studentNumber`) já usada por outro aluno, ignorando maiúsculas/minúsculas →
  **409 Conflict**.
- `salary` zero, negativo, com mais de 2 casas decimais ou acima de 1.000.000,00 → **400**.
- `photo` que não seja uma URL `http`/`https` válida ou com mais de 500 caracteres → **400**;
  `photo` ausente ou vazia → aceita (aluno sem foto).
- Excluir a mesma pessoa duas vezes → a primeira retorna **204**, a segunda **404**.
- Excluir uma pessoa remove também todos os seus endereços; nenhum endereço fica sem dono.
- Atualização (`PUT`) substitui a lista de endereços inteira: endereços não enviados são
  removidos.
- Identificador enviado no corpo da requisição → ignorado; em atualizações prevalece o
  identificador do caminho. Campos desconhecidos → ignorados, sem erro.
- Método HTTP não suportado → **405 Method Not Allowed**; tipo de conteúdo não suportado →
  **415 Unsupported Media Type**; ambos no formato padrão de erro.
- Falha inesperada → **500 Internal Server Error** com mensagem genérica, sem detalhes internos.
- Requisição vinda de uma origem fora da lista de origens permitidas → recusada com **403** pelo
  mecanismo de origem cruzada, sem o formato padrão de erro. É aceito como exceção à FR-017,
  porque essa resposta é consumida pelo navegador, e não exibida pela interface.
- Rota inexistente sob `/api` (ex.: `/api/teachers`) → **404 Not Found** no formato padrão de
  erro.
- Cabeçalho `Accept` que não aceita JSON (ex.: `Accept: text/html`) → **406 Not Acceptable**, sem
  corpo, já que o cliente não aceita o formato de erro.
- Filtro `?status=active` (minúsculas) → **400**; `?status=ACTIVE&status=DISABLE` → vale o
  primeiro valor.
- Dois endereços idênticos na mesma pessoa → aceitos e gravados como enviados; o sistema não
  remove duplicidades.
- "Desativar" uma pessoa alterada por outro usuário depois que a lista foi exibida → a interface
  consulta os dados atuais da pessoa no momento da ação e altera somente o status, sem desfazer a
  alteração anterior.
- Duas edições simultâneas da mesma pessoa → prevalece a última gravação.
- Interface sem comunicação com a API → mensagem de falha e opção de tentar novamente; nenhum dado
  digitado é perdido.
- Reinício do sistema → todos os dados são perdidos (comportamento esperado do banco em memória).

## Requirements *(mandatory)*

### Functional Requirements

**Regras comuns de pessoa (alunos e professores)**

- **FR-001**: O sistema DEVE manter, para toda pessoa, nome, telefone, e-mail, status e ao menos
  um endereço, com identificador único gerado pelo próprio sistema.
- **FR-002**: O sistema DEVE atribuir o status `ACTIVE` quando nenhum status for informado na
  criação e aceitar apenas os valores `ACTIVE` e `DISABLE`.
- **FR-003**: O sistema DEVE garantir que o e-mail seja único entre **todas** as pessoas (alunos e
  professores), sem diferenciar maiúsculas/minúsculas, gravando-o em letras minúsculas, e
  responder com conflito em caso de duplicidade.
- **FR-004**: O sistema DEVE tratar os endereços como parte da pessoa: eles são enviados e
  retornados junto com ela, não existem sem ela, são removidos com ela e, na atualização, a lista
  enviada substitui integralmente a anterior (endereços que não constam da nova lista são
  apagados). Endereços não possuem identificador próprio no contrato da API. Os endereços são
  retornados na mesma ordem em que foram enviados na última criação ou atualização, e endereços
  repetidos (os mesmos cinco campos) são aceitos e gravados como enviados.
- **FR-005**: O sistema DEVE listar pessoas em ordem alfabética de nome, segundo as regras do
  português: sem diferenciar maiúsculas/minúsculas nem acentos ("Álvaro" vem antes de "Bruno").
  Nomes iguais são desempatados pelo identificador, em ordem crescente. A listagem pode ser
  filtrada por um único status; o valor do filtro diferencia maiúsculas/minúsculas (apenas
  `ACTIVE` ou `DISABLE`) e, se o parâmetro for repetido, vale o primeiro valor.
- **FR-006**: O sistema DEVE permitir a atualização completa de uma pessoa (todos os campos
  editáveis, inclusive status e endereços), com as mesmas validações da criação. O status é
  opcional na atualização: se omitido, o status atual é mantido.
- **FR-007**: Ao receber a operação de exclusão de uma pessoa, o sistema DEVE removê-la
  definitivamente, junto com todos os seus endereços; consultas posteriores DEVEM retornar "não
  encontrado". A desativação NÃO usa a exclusão: é feita atualizando o status para `DISABLE`.

**Alunos (`/api/students`)**

- **FR-008**: O sistema DEVE permitir criar, listar, buscar por identificador, atualizar e excluir
  alunos.
- **FR-009**: O sistema DEVE exigir a matrícula (`studentNumber`) de todo aluno e garantir que ela
  seja única entre os alunos (sem diferenciar maiúsculas/minúsculas), gravando-a em letras
  maiúsculas e respondendo com conflito em caso de duplicidade.
- **FR-010**: O sistema DEVE armazenar e retornar a foto do aluno (`photo`) como um endereço web
  (URL `http` ou `https`, até 500 caracteres) de uma imagem hospedada externamente. A foto é
  opcional e o sistema não recebe nem guarda arquivos de imagem.

**Professores (`/api/professors`)**

- **FR-011**: O sistema DEVE permitir criar, listar, buscar por identificador, atualizar e excluir
  professores.
- **FR-012**: O sistema DEVE exigir o salário (`salary`) de todo professor, como valor monetário
  exato, maior que zero, com no máximo duas casas decimais e até 1.000.000,00.

**Validação e respostas**

- **FR-013**: O sistema DEVE validar toda entrada conforme as regras da seção Key Entities e, em
  caso de violação, rejeitar a requisição informando todos os campos inválidos e seus motivos em
  uma única resposta (incluindo a posição de endereços inválidos), sem gravar nenhum dado.
- **FR-014**: O sistema DEVE remover espaços no início e no fim dos campos de texto antes de
  validar e gravar; texto composto apenas por espaços DEVE ser tratado como vazio.
- **FR-015**: O sistema DEVE ignorar o identificador e campos desconhecidos enviados no corpo da
  requisição; em atualizações, prevalece o identificador do caminho.
- **FR-016**: O sistema DEVE responder com os códigos de status definidos no Contrato da API.
- **FR-017**: Toda resposta de erro DEVE seguir um formato único contendo: código de status,
  título, mensagem legível em português, caminho requisitado, instante da ocorrência e, em falhas
  de validação e em conflitos de unicidade (409), a lista de erros por campo (campo e motivo).
  Exceções: a recusa de origem cruzada e a resposta 406 a clientes que não aceitam JSON (ver
  Edge Cases).
- **FR-018**: Erros inesperados DEVEM resultar em resposta de erro interno com mensagem genérica,
  sem expor detalhes internos (rastros de pilha, comandos de banco, nomes de classes).

**Interface web**

- **FR-019**: A interface DEVE oferecer seções separadas para Alunos e Professores, acessíveis por
  um menu de navegação sempre visível.
- **FR-020**: Cada seção DEVE listar as pessoas com nome, e-mail, telefone e status (destacado
  visualmente), além de matrícula e miniatura da foto (alunos) ou salário formatado em reais
  (professores), com filtro "Ativos", "Inativos" e "Todos" (padrão: "Todos", para que a mudança
  de status após "Desativar" permaneça visível na lista).
- **FR-021**: A interface DEVE oferecer um formulário de cadastro e edição com todos os campos da
  pessoa, permitindo adicionar e remover endereços e impedindo remover o último. Para alunos, o
  formulário DEVE pré-visualizar a foto a partir do link informado, exibindo uma imagem padrão
  quando o link não carregar (o mesmo vale para a miniatura na lista).
- **FR-022**: A interface DEVE validar campos obrigatórios e formatos antes de enviar e DEVE
  exibir, junto a cada campo, os erros retornados pela API (validação e conflito), preservando os
  dados digitados.
- **FR-023**: A interface DEVE oferecer a ação "Desativar" para pessoas ativas, com confirmação
  prévia, refletindo o novo status na lista sem recarregar a página; a reativação ocorre pela
  edição do status. Ao confirmar, a interface DEVE consultar os dados atuais da pessoa e alterar
  somente o status, para não sobrescrever alterações feitas por outro usuário desde a exibição
  da lista.
- **FR-023a**: A interface DEVE oferecer a ação "Excluir" para qualquer pessoa, visualmente
  secundária em relação a "Desativar", com diálogo de confirmação reforçada que informe o nome da
  pessoa e que a exclusão é definitiva e irreversível, exigindo o clique explícito em "Excluir
  definitivamente".
- **FR-024**: A interface DEVE exibir estados de carregamento, de lista vazia, de sucesso após
  salvar/desativar e de falha de comunicação com a API (com opção de tentar novamente).
- **FR-025**: A interface DEVE estar em português, ter rótulos em todos os campos e ser
  utilizável em telas a partir de 360 px de largura.
- **FR-025a**: A interface DEVE ser operável somente pelo teclado:
  - a ordem de foco segue a ordem visual;
  - todo controle é alcançável por Tab e acionável por Enter ou Espaço;
  - diálogos abrem com o foco em "Cancelar", fecham com Esc e devolvem o foco ao controle que os
    abriu;
  - após salvar, desativar ou excluir, o foco vai para o título da lista.
- **FR-025b**: A interface DEVE ser simples:
  - cada tela tem no máximo uma ação primária destacada;
  - toda operação (cadastrar, editar, desativar, excluir) é iniciada com 1 clique a partir da
    lista e concluída sem passar por mais de uma tela adicional;
  - as listas exibem no máximo 7 colunas.
- **FR-025c**: A interface DEVE funcionar nas duas versões estáveis mais recentes do Chrome,
  Firefox e Edge para desktop e do Chrome para Android, e DEVE exibir uma lista de 1.000 pessoas,
  sem paginação, em até 2 segundos após a resposta da API.
- **FR-026**: O endereço da API usado pela interface DEVE ser configurável sem alterar o código.
- **FR-026a**: Quando a interface acessar a API diretamente a partir de outra origem (outro
  endereço ou porta), a API DEVE aceitar somente as origens configuradas — por padrão
  `http://localhost:5173` e `http://127.0.0.1:5173` —, alteráveis por configuração sem mudar o
  código; nenhuma origem curinga é aceita. No modo padrão (desenvolvimento com proxy e
  containers), interface e API compartilham a mesma origem, e essa configuração não é usada.

**Banco de dados, execução e documentação**

- **FR-027**: O sistema DEVE armazenar os dados em um banco em memória que inicia vazio a cada
  execução e DEVE disponibilizar o console web desse banco, em endereço fixo, tanto na execução
  local quanto em container, com credenciais documentadas.
- **FR-028**: O sistema DEVE oferecer uma forma documentada de carregar dados de demonstração (ao
  menos 3 alunos e 2 professores, com ambos os status e ao menos uma pessoa com mais de um
  endereço), sem que isso afete a execução dos testes automatizados.
- **FR-029**: A API DEVE poder ser empacotada em imagem de container e executada, expondo a porta
  8080, sem dependências externas. Quando a interface web for entregue, API e interface DEVEM
  poder ser iniciadas juntas com um único comando, cada uma em seu próprio container, com a
  interface acessando a API pela mesma origem (sem configuração de origem cruzada nesse modo).
  A API DEVE continuar podendo ser construída, testada e executada sem o front-end.
  No modo container, os endereços são:
  - interface: `http://localhost:8081`;
  - API: `http://localhost:8080/api` (também acessível via `http://localhost:8081/api`);
  - console do banco: `http://localhost:8080/h2-console`.

  A configuração é feita por variáveis de ambiente: dados de demonstração
  (`SPRING_PROFILES_ACTIVE=demo`), origens permitidas (`APP_CORS_ALLOWED_ORIGINS`) e credenciais
  do banco (`DB_USERNAME`, `DB_PASSWORD`; padrão `sa`, sem senha).
- **FR-030**: O README DEVE conter as seções exigidas pela constituição (Princípio VI), incluindo
  o diagrama do modelo de domínio, exemplos de requisição/resposta para cada operação e o
  procedimento de carga de dados de demonstração. Para a interface web, o README DEVE trazer
  pré-requisitos, execução em desenvolvimento, build, configuração do endereço da API e quando o
  acesso entre origens (FR-026a) se aplica. Os comandos são escritos para o shell bash (Linux,
  macOS ou WSL no Windows).
- **FR-031**: O README DEVE conter a seção "Roteiro para os vídeos", com:
  - **Vídeo de produto (2–5 min)**: demonstração, do ponto de vista do usuário final, de listar,
    cadastrar, editar, desativar e excluir alunos e professores (pela interface web quando
    entregue, ou por requisições prontas), incluindo um erro de validação e um conflito de e-mail.
  - **Vídeo técnico (2–5 min)**: processo de desenvolvimento e decisões técnicas (modelagem com
    herança e endereços, camadas, contratos e validação, tratamento centralizado de erros,
    testes e cobertura, containers, front-end) e como configurar e rodar o sistema.

### Contrato da API

Todas as rotas aceitam e retornam JSON. Qualquer operação pode retornar **500** em falha
inesperada. Rotas inexistentes sob `/api` retornam **404**, e clientes que não aceitam JSON
recebem **406**. Campos opcionais sem valor (ex.: `photo`) são retornados com valor `null`,
nunca omitidos.

| Operação | Método e caminho | Sucesso | Erros possíveis |
|----------|------------------|---------|-----------------|
| Criar aluno | `POST /api/students` | 201 + `Location` | 400, 409 (e-mail ou matrícula duplicados) |
| Listar alunos | `GET /api/students[?status=ACTIVE\|DISABLE]` | 200 | 400 (status inválido) |
| Buscar aluno | `GET /api/students/{id}` | 200 | 400 (id inválido), 404 |
| Atualizar aluno | `PUT /api/students/{id}` | 200 | 400, 404, 409 |
| Excluir aluno | `DELETE /api/students/{id}` | 204 | 400, 404 |
| Criar professor | `POST /api/professors` | 201 + `Location` | 400, 409 (e-mail duplicado) |
| Listar professores | `GET /api/professors[?status=ACTIVE\|DISABLE]` | 200 | 400 (status inválido) |
| Buscar professor | `GET /api/professors/{id}` | 200 | 400 (id inválido), 404 |
| Atualizar professor | `PUT /api/professors/{id}` | 200 | 400, 404, 409 |
| Excluir professor | `DELETE /api/professors/{id}` | 204 | 400, 404 |

**Campos de entrada e saída** (nomes exatamente como no modelo):

- Endereço: `street`, `city`, `state`, `zipCode`, `country` (sem `id`, na entrada e na saída).
- Aluno (entrada): `name`, `phoneNumber`, `emailAddress`, `status` (opcional), `studentNumber`,
  `photo` (opcional), `addresses` (lista de endereços).
- Aluno (saída): `id` + todos os campos de entrada, com `status` sempre preenchido.
- Professor (entrada): `name`, `phoneNumber`, `emailAddress`, `status` (opcional), `salary`,
  `addresses`.
- Professor (saída): `id` + todos os campos de entrada, com `status` sempre preenchido.

### Key Entities *(include if feature involves data)*

- **Status**: situação da pessoa na instituição. Valores: `ACTIVE` (ativa) e `DISABLE` (inativa).
- **Person** (abstrata — nunca cadastrada diretamente): dados comuns a alunos e professores.
  - Identificador: gerado pelo sistema, imutável.
  - `name`: obrigatório; de 3 a 100 caracteres.
  - `phoneNumber`: obrigatório; dígitos com `+` inicial, espaços, parênteses e hífens
    opcionais; entre 8 e 15 dígitos.
  - `emailAddress`: obrigatório; formato de e-mail válido; até 150 caracteres; único entre todas
    as pessoas (sem diferenciar maiúsculas/minúsculas); gravado em minúsculas.
  - `status`: obrigatório no registro; padrão `ACTIVE` na criação.
  - `addresses`: um ou mais endereços, sem limite máximo (decisão intencional, conforme a
    multiplicidade `1..*` do modelo).
- **Student** (é uma Person): aluno da instituição.
  - `studentNumber`: obrigatório; de 1 a 20 caracteres (letras, dígitos e hífen); único entre os
    alunos (sem diferenciar maiúsculas/minúsculas).
  - `photo`: opcional; URL `http`/`https` de até 500 caracteres.
- **Professor** (é uma Person): professor da instituição.
  - `salary`: obrigatório; valor monetário em reais, maior que zero, até 1.000.000,00, com no
    máximo duas casas decimais.
- **Address**: endereço de uma pessoa; pertence a exatamente uma pessoa.
  - `street`: obrigatório; até 150 caracteres.
  - `city`: obrigatório; até 100 caracteres.
  - `state`: obrigatório; de 2 a 100 caracteres (sigla ou nome).
  - `zipCode`: obrigatório; de 3 a 20 caracteres (letras, dígitos, espaços e hífen).
  - `country`: obrigatório; de 2 a 100 caracteres.

## Success Criteria *(mandatory)*

### Measurable Outcomes

- **SC-001**: Uma pessoa avaliadora sem contato prévio com o projeto, com os pré-requisitos já
  instalados, consegue subir o sistema e cadastrar o primeiro aluno seguindo apenas o README em
  até 10 minutos.
- **SC-002**: 100% das operações do Contrato da API retornam o resultado e o código de status
  especificados, comprovado por testes automatizados.
- **SC-003**: 100% das entradas inválidas listadas nos casos de borda são rejeitadas com mensagem
  que identifica o campo e o motivo, e nenhuma delas resulta em dado gravado.
- **SC-004**: Cada operação individual responde em menos de 1 segundo, em execução local, com até
  1.000 alunos e 1.000 professores cadastrados. Condições de medição:
  - cada pessoa com 2 endereços;
  - cliente HTTP na mesma máquina, após uma requisição de aquecimento;
  - tempo total de resposta de cada requisição;
  - operações medidas: listagem completa (com endereços) de cada tipo, busca por identificador,
    criação, atualização e exclusão.
- **SC-005**: A suíte de testes automatizados executa com sucesso sem nenhum serviço externo e
  cobre no mínimo 80% das linhas das camadas de regras de negócio e de entrada da API.
- **SC-006**: O sistema completo pode ser iniciado em containers com no máximo 2 comandos, sem
  configuração adicional.
- **SC-007**: Pela interface web, um usuário de primeira viagem cadastra um aluno com um endereço
  em menos de 2 minutos e desativa uma pessoa em no máximo 2 cliques (ação + confirmação).
- **SC-008**: 100% dos erros retornados pela API durante o uso da interface são exibidos ao
  usuário em português, junto ao campo afetado quando aplicável; nenhuma falha é silenciosa.
- **SC-009**: Os roteiros de vídeo cobrem 100% das operações de listar, cadastrar, editar,
  desativar e excluir para ambos os tipos de pessoa, e cada vídeo gravado a partir deles dura entre 2 e 5
  minutos.

## Assumptions

- A modelagem de referência (`modelagem-bomjesus.png`) e o enunciado (`Desafio técnico.md`) do
  desafio são a fonte da verdade do domínio, conforme o Princípio VII da constituição. Esta
  especificação substitui a `001-alunos-cursos-crud` (já removida), criada antes de a modelagem
  ser localizada.
- Há um único perfil de usuário (secretaria acadêmica) e o sistema é aberto, sem autenticação nem
  autorização, por se tratar de um projeto de avaliação executado localmente.
- Uma mesma pessoa não pode ser aluno e professor ao mesmo tempo; por isso o e-mail é único entre
  todas as pessoas.
- A matrícula é informada pela secretaria (não é gerada pelo sistema) e pode ser corrigida em
  atualizações, desde que continue única.
- Os endereços aceitam formatos internacionais (o modelo possui `country`); não há validação
  específica de CEP ou UF brasileiros nem consulta a serviços externos de endereço.
- O salário é expresso em reais (R$).
- A foto do aluno é apenas um link para uma imagem pública; upload e armazenamento de arquivos
  estão fora do escopo. Para a demonstração, são usados links de imagens públicas.
- As fotos de demonstração vêm de um único serviço público de imagens. Se ele estiver fora do ar,
  a interface mostra a imagem padrão (FR-021) e nada deixa de funcionar. Antes de gravar os
  vídeos, o roteiro orienta conferir os links; os dados de demonstração continuam válidos sem
  foto.
- Os dados usados em demonstração, testes e vídeos são fictícios. Obrigações de tratamento de
  dados pessoais reais (LGPD: consentimento, retenção, anonimização) estão fora do escopo, porque
  o sistema não é implantado em produção e o banco é volátil.
- As listagens não são paginadas; o volume esperado é de até alguns milhares de registros.
- A atualização é sempre completa (`PUT`); atualização parcial (`PATCH`) está fora do escopo.
- Em edições simultâneas prevalece a última gravação; não há controle de concorrência otimista.
- Mensagens de erro, interface e documentação são escritas em português (pt-BR); nomes de campos
  e valores do modelo permanecem em inglês.
- O console do banco fica habilitado também em container, porque o sistema é destinado apenas a
  avaliação e demonstração, sem implantação em produção.
- A gravação e publicação dos vídeos são feitas pelo candidato; o projeto fornece o roteiro.
- A stack, a forma de mapeamento entre dados internos e contratos da API e a organização do código
  são definidas pela constituição (v1.1.1) e detalhadas no plano técnico (`/speckit-plan`).
- Decisões técnicas já acordadas para o plano: a herança de pessoa é persistida com a estratégia
  `JOINED`, de modo que obrigatoriedade de `studentNumber`/`salary`, unicidade de e-mail entre
  todas as pessoas e vínculo único de endereço com pessoa são garantidos também pelo banco. Os
  endereços são composição da pessoa, com cascata total e remoção de órfãos. O repositório é
  único, com `backend/` (Maven) e `frontend/` (Vite) como pastas irmãs de builds independentes;
  em containers, a interface é servida por um servidor web estático (ex.: Nginx) que repassa
  `/api` à API, e um `docker-compose.yml` na raiz sobe ambos.
