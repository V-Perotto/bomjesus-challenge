# Feature Specification: Máscaras de Entrada nos Formulários de Aluno e Professor

**Feature Branch**: `003-form-input-masks`

**Created**: 2026-10-19

**Status**: Draft

**Input**: User description:

> Máscaras e validação de entrada nos formulários de aluno e professor: (1) telefone com máscara e
> prefixo +55 inserido automaticamente assim que o usuário começa a digitar; (2) campo de
> telefone aceita apenas dígitos, bloqueando letras e caracteres que não componham um telefone;
> (3) salário do professor com máscara monetária no padrão x.xxx,xx; (4) CEP com máscara no
> padrão XXXXX-XXX.

## User Scenarios & Testing *(mandatory)*

### User Story 1 - Digitar o telefone com máscara e +55 automático (Priority: P1)

Ao cadastrar ou editar um aluno ou professor, a secretaria acadêmica digita apenas os números do
telefone. O campo insere sozinho o código do Brasil (+55) assim que o primeiro dígito é digitado
e formata o restante (DDD entre parênteses, espaço e hífen) enquanto a pessoa digita. Letras e
símbolos que não fazem parte de um telefone são ignorados, de modo que o campo nunca contém
caracteres inválidos.

**Why this priority**: O telefone é obrigatório para alunos e professores e é o campo com mais
variações de digitação hoje (espaços, parênteses, hífens, com ou sem código do país). Padronizar
a entrada elimina a principal causa de erro de validação nos dois formulários e garante que todos
os telefones fiquem gravados no mesmo formato.

**Independent Test**: Pode ser testado abrindo o formulário de novo aluno (ou de novo professor),
digitando uma sequência de números e letras no campo Telefone e conferindo o valor exibido e o
valor salvo, sem depender das máscaras de salário e CEP.

**Acceptance Scenarios**:

1. **Dado** o campo Telefone vazio, **Quando** o usuário digita o primeiro dígito `4`, **Então**
   o campo passa a exibir `+55 (4`.
2. **Dado** o campo Telefone vazio, **Quando** o usuário digita `41999990001`, **Então** o campo
   exibe `+55 (41) 99999-0001`.
3. **Dado** o campo Telefone vazio, **Quando** o usuário digita `4133330001`, **Então** o campo
   exibe `+55 (41) 3333-0001`.
4. **Dado** o campo Telefone com `+55 (41) 9`, **Quando** o usuário digita letras (ex.: `a`, `B`)
   ou símbolos (ex.: `#`, `*`, `.`, `+`), **Então** o valor do campo não muda.
5. **Dado** o campo Telefone vazio, **Quando** o usuário cola `(41) 99999-0001` ou
   `+55 41 99999 0001`, **Então** o campo exibe `+55 (41) 99999-0001`.
6. **Dado** o campo Telefone com `+55 (41) 99999-0001`, **Quando** o usuário apaga todos os
   dígitos, **Então** o campo fica vazio (sem o `+55` residual) e, ao salvar, é apontado como
   obrigatório.
7. **Dado** o campo Telefone com `+55 (41) 9999`, **Quando** o usuário tenta salvar, **Então** o
   formulário não é enviado e o campo mostra uma mensagem informando o formato esperado
   (`+55 (00) 00000-0000` ou `+55 (00) 0000-0000`).
8. **Dado** um aluno ou professor salvo com telefone `+55 (41) 99999-0001`, **Quando** o usuário
   abre a edição, **Então** o campo exibe o mesmo telefone já formatado e o salva sem alterações
   se não for editado.

---

### User Story 2 - Digitar o salário do professor com máscara monetária (Priority: P2)

Ao cadastrar ou editar um professor, a secretaria digita somente os números do salário e o campo
os exibe no padrão monetário brasileiro (`x.xxx,xx`), com separador de milhar e duas casas
decimais, preenchendo os centavos da direita para a esquerda.

**Why this priority**: O salário é obrigatório para professores e hoje aceita formatos livres
(`8500`, `8500.50`, `8.500,00`), o que gera ambiguidade entre ponto e vírgula. A máscara elimina a
ambiguidade, mas afeta apenas o formulário de professor.

**Independent Test**: Pode ser testado no formulário de novo professor digitando números no campo
Salário e conferindo o valor exibido e o valor salvo, independentemente das máscaras de telefone
e CEP.

**Acceptance Scenarios**:

1. **Dado** o campo Salário vazio, **Quando** o usuário digita `850000`, **Então** o campo exibe
   `8.500,00`.
2. **Dado** o campo Salário vazio, **Quando** o usuário digita `5`, **Então** o campo exibe
   `0,05`; ao continuar com `0`, `0`, exibe `5,00`.
3. **Dado** o campo Salário vazio, **Quando** o usuário digita `12345678`, **Então** o campo exibe
   `123.456,78`.
4. **Dado** o campo Salário com `8.500,00`, **Quando** o usuário digita letras ou símbolos,
   **Então** o valor do campo não muda.
5. **Dado** o campo Salário com `8.500,00`, **Quando** o usuário apaga o último dígito, **Então** o
   campo exibe `850,00`.
6. **Dado** o campo Salário com `8.500,00`, **Quando** o usuário salva o professor, **Então** o
   salário gravado é exatamente 8500,00 e aparece como `R$ 8.500,00` na listagem.
7. **Dado** o campo Salário com valor acima de `1.000.000,00` (ex.: `1.500.000,00`), **Quando** o
   usuário tenta salvar, **Então** o formulário não é enviado e o campo mostra a mesma mensagem de
   limite já usada hoje (`deve ser no máximo 1.000.000,00`).
8. **Dado** um professor salvo com salário 9200,00, **Quando** o usuário abre a edição, **Então**
   o campo exibe `9.200,00`.

---

### User Story 3 - Digitar o CEP com máscara (Priority: P3)

Ao preencher cada endereço de um aluno ou professor, a secretaria digita somente os números do
CEP e o campo insere o hífen automaticamente no padrão `XXXXX-XXX`.

**Why this priority**: O CEP aparece em todo endereço, mas é um campo curto e de baixo risco; a
máscara padroniza o formato sem mudar o fluxo do formulário.

**Independent Test**: Pode ser testado no bloco de endereço de qualquer um dos dois formulários,
digitando números e letras no campo CEP e conferindo o valor exibido e salvo.

**Acceptance Scenarios**:

1. **Dado** o campo CEP vazio, **Quando** o usuário digita `80230010`, **Então** o campo exibe
   `80230-010`.
2. **Dado** o campo CEP vazio, **Quando** o usuário digita `80230`, **Então** o campo exibe
   `80230` (o hífen só aparece a partir do sexto dígito).
3. **Dado** o campo CEP com `80230-010` e o cursor no fim do campo, **Quando** o usuário digita
   mais dígitos, letras ou símbolos, **Então** o valor do campo não muda.
4. **Dado** o campo CEP vazio, **Quando** o usuário cola `80.230-010` ou `80230 010`, **Então** o
   campo exibe `80230-010`.
5. **Dado** o campo CEP com `8023`, **Quando** o usuário tenta salvar, **Então** o formulário não
   é enviado e o campo mostra uma mensagem informando o formato esperado `00000-000`.
6. **Dado** uma pessoa com dois endereços, **Quando** o usuário digita o CEP de cada um, **Então**
   cada campo CEP é formatado de forma independente.

---

### Edge Cases

- **Telefone com DDD iniciando em 5**: digitar `55…` como primeiros dígitos é tratado como DDD
  (ex.: `55 99999-0001` → `+55 (55) 99999-0001`); o código do país nunca é removido de algo que o
  usuário digitou tecla a tecla.
- **Telefone colado com código do país**: um valor colado com 12 ou 13 dígitos que começa com `55`
  tem o `55` inicial tratado como código do país e não é duplicado.
- **Telefone colado com dígitos demais**: dígitos além do 11º número nacional são descartados.
- **Edição de telefone ou CEP fora do padrão** (ex.: cadastrado direto pela API em formato
  internacional, como `+1 (555) 123-4567`, ou CEP com letras): o campo exibe o valor gravado sem
  alteração, para que nenhum número seja reinterpretado silenciosamente como brasileiro, e a
  validação o aponta como fora do formato ao salvar. Assim que o usuário edita o campo, a máscara
  passa a valer sobre os dígitos existentes.
- **Salário colado com formatação** (ex.: `R$ 8.500,00`): apenas os dígitos são aproveitados e o
  valor é reformatado como `8.500,00`.
- **Salário com zeros à esquerda** (ex.: digitar `000500`): exibido como `5,00`, sem zeros à
  esquerda supérfluos.
- **Salário apagado por completo**: o campo fica vazio e, ao salvar, é apontado como obrigatório.
  Isso vale também para Backspace repetido a partir de valores pequenos (`0,05` → vazio).
- **Salário só com zeros**: digitar `0` com o campo vazio não altera o campo, porque zeros à
  esquerda não têm valor; o salário nunca fica preso em `0,00`.
- **Salário acima de 9.999.999,99**: o campo não aceita mais dígitos após 9 dígitos (7 inteiros e
  2 decimais); valores entre 1.000.000,01 e 9.999.999,99 são aceitos na digitação e barrados pela
  validação de limite.
- **Edição no meio do texto**: ao inserir ou apagar um dígito no meio do valor, a máscara é
  reaplicada e o cursor permanece logo após o dígito alterado, sem saltar para o fim do campo.
- **Apagar sobre um separador**: com o cursor logo após um caractere da máscara (parêntese,
  espaço, hífen, ponto ou vírgula), pressionar Backspace apaga o dígito anterior ao separador; o
  campo nunca fica "travado" recolocando o mesmo separador.
- **Teclados de celular**: os três campos abrem o teclado numérico em dispositivos móveis.
- **Erros devolvidos pela API**: se, mesmo com as máscaras, a API rejeitar um dos campos, a
  mensagem continua sendo exibida junto ao campo, como hoje.

## Requirements *(mandatory)*

### Functional Requirements

**Telefone (formulários de aluno e de professor)**

- **FR-001**: O campo Telefone DEVE aceitar somente dígitos digitados ou colados; letras,
  símbolos e espaços inseridos pelo usuário DEVEM ser descartados sem alterar o valor do campo.
- **FR-002**: Ao receber o primeiro dígito com o campo vazio, o campo DEVE passar a exibir o
  prefixo `+55` seguido do dígito, sem o usuário precisar digitar o código do país.
- **FR-003**: O campo DEVE formatar os dígitos nacionais durante a digitação como
  `+55 (DD) NNNN-NNNN` para números de 10 dígitos (fixo) e `+55 (DD) NNNNN-NNNN` para números de
  11 dígitos (celular), aceitando no máximo 11 dígitos nacionais.
- **FR-004**: Quando todos os dígitos nacionais forem apagados, o campo DEVE ficar vazio, sem
  manter o prefixo `+55`.
- **FR-005**: Ao colar um valor, o campo DEVE aproveitar apenas os dígitos; se o valor colado
  tiver 12 ou 13 dígitos e começar com `55`, esse `55` DEVE ser tratado como código do país.
- **FR-006**: Ao salvar, o telefone DEVE ser considerado válido somente com 10 ou 11 dígitos
  nacionais; caso contrário, o formulário NÃO DEVE ser enviado e o campo DEVE exibir uma mensagem
  em português informando o formato esperado.
- **FR-007**: O telefone DEVE ser enviado e gravado exatamente no formato exibido pela máscara
  (ex.: `+55 (41) 99999-0001`), compatível com os telefones já existentes nos dados de
  demonstração.

**Salário (formulário de professor)**

- **FR-008**: O campo Salário DEVE aceitar somente dígitos digitados ou colados; letras e
  símbolos DEVEM ser descartados sem alterar o valor do campo.
- **FR-009**: O campo DEVE exibir o valor no padrão monetário brasileiro `x.xxx,xx` (ponto como
  separador de milhar, vírgula e exatamente duas casas decimais), preenchendo os centavos da
  direita para a esquerda à medida que os dígitos são digitados.
- **FR-010**: O campo DEVE aceitar no máximo 9 dígitos (até `9.999.999,99`) e NÃO DEVE exibir
  zeros à esquerda supérfluos (ex.: `0,05`, `5,00`, `850,00`).
- **FR-011**: Quando todos os dígitos forem apagados, o campo DEVE ficar vazio.
- **FR-012**: As regras atuais do salário DEVEM continuar valendo (obrigatório, maior que zero e
  no máximo `1.000.000,00`), com as mesmas mensagens exibidas hoje.
- **FR-013**: O valor exibido DEVE ser convertido para o mesmo valor monetário enviado à API, sem
  arredondamentos nem perda de centavos (ex.: `8.500,00` → 8500,00).
- **FR-014**: Na edição de um professor, o salário gravado DEVE ser exibido já no padrão
  `x.xxx,xx`.

**CEP (bloco de endereços, nos dois formulários)**

- **FR-015**: O campo CEP de cada endereço DEVE aceitar somente dígitos digitados ou colados,
  limitado a 8 dígitos; letras, símbolos e dígitos excedentes DEVEM ser descartados.
- **FR-016**: O campo DEVE inserir o hífen automaticamente a partir do sexto dígito, exibindo o
  valor no padrão `XXXXX-XXX`.
- **FR-017**: Ao salvar, o CEP DEVE ser considerado válido somente com exatamente 8 dígitos;
  caso contrário, o formulário NÃO DEVE ser enviado e o campo DEVE exibir uma mensagem em
  português informando o formato esperado (`00000-000`).
- **FR-018**: O CEP DEVE ser enviado e gravado no formato exibido pela máscara (ex.:
  `80230-010`).

**Comportamento comum**

- **FR-019**: As máscaras DEVEM valer tanto na criação quanto na edição. Na edição, telefone e
  CEP gravados já no formato da máscara DEVEM ser exibidos sem alteração; valores gravados em
  outro formato DEVEM ser exibidos como estão e apontados pela validação ao salvar, até que o
  usuário os corrija. O salário segue o FR-014.
- **FR-020**: Ao inserir ou apagar um dígito no meio do valor, a máscara DEVE ser reaplicada sem
  mover o cursor para o fim do campo.
- **FR-021**: Os campos Telefone, Salário e CEP DEVEM solicitar o teclado numérico em
  dispositivos móveis.
- **FR-022**: As máscaras NÃO DEVEM alterar a acessibilidade atual dos campos: rótulo, indicação
  de obrigatório e associação da mensagem de erro ao campo DEVEM ser mantidos.
- **FR-023**: As mensagens de erro devolvidas pela API para esses campos DEVEM continuar sendo
  exibidas junto ao campo correspondente.

### Key Entities

- **Telefone da pessoa (`phoneNumber`)**: atributo já existente de aluno e professor. Passa a ser
  digitado sempre no formato brasileiro `+55 (DD) NNNN-NNNN` ou `+55 (DD) NNNNN-NNNN`.
- **Salário do professor (`salary`)**: atributo monetário já existente, em reais. Muda apenas a
  forma de digitação e exibição no formulário; o valor gravado continua numérico com duas casas.
- **CEP do endereço (`zipCode`)**: atributo já existente de cada endereço. Passa a ser digitado
  sempre no formato `XXXXX-XXX`.

Nenhuma entidade ou atributo novo é criado.

## Success Criteria *(mandatory)*

### Measurable Outcomes

- **SC-001**: Em 100% das tentativas, é impossível deixar letras ou símbolos digitados pelo
  usuário nos campos Telefone, Salário e CEP.
- **SC-002**: 100% dos alunos e professores cadastrados ou editados pela interface têm o telefone
  gravado em um dos dois formatos brasileiros definidos e o CEP gravado no formato `XXXXX-XXX`.
- **SC-003**: 100% dos salários digitados pela interface são gravados com o mesmo valor exibido
  no campo, sem diferença de centavos.
- **SC-004**: Um usuário preenche telefone, salário e CEP de um professor digitando apenas
  números, sem precisar digitar `+55`, parênteses, pontos, vírgula ou hífen.
- **SC-005**: Erros de validação de formato de telefone, CEP e salário devolvidos pela API ao
  salvar pela interface caem a zero, pois os formatos inválidos são barrados antes do envio.
- **SC-006**: Os fluxos de cadastro e edição de alunos e professores continuam funcionando
  integralmente, sem regressão nas demais regras dos formulários.

## Assumptions

- A mudança vale apenas para os formulários da interface web. As regras da API para `phoneNumber`,
  `zipCode` e `salary` não mudam: os formatos produzidos pelas máscaras já são aceitos por ela, e
  clientes que usam a API diretamente continuam podendo enviar os formatos mais amplos hoje
  permitidos.
- A interface passa a tratar todos os telefones como brasileiros (+55) e todos os CEPs no padrão
  brasileiro, independentemente do país informado no endereço, porque o sistema atende uma
  instituição de ensino brasileira. Isso restringe, só na interface, a premissa da especificação
  `002-students-professors-crud` de aceitar endereços em formatos internacionais.
- O DDD é tratado como dois dígitos quaisquer; não há validação da lista oficial de DDDs nem da
  regra de o celular começar com 9.
- A máscara monetária usa o preenchimento da direita para a esquerda (os últimos dois dígitos
  digitados são sempre os centavos), padrão comum em sistemas brasileiros.
- O prefixo visual `R$` que já acompanha o campo Salário permanece fora do valor digitado.
- Não há consulta a serviços externos de CEP nem preenchimento automático de endereço.
- Testes automatizados da interface são opcionais pela constituição, mas as regras de máscara e
  validação devem ser cobertas pela suíte de testes unitários do front-end.
