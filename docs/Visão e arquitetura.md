# Canteiro Conectado — Backlog do MVP

Oct 10, 2026 · @Vitor Paulino

O MVP foi reduzido para 13 épicos, dois perfis de uso e uma stack que um time iniciante consegue manter em oito semanas: app Expo, API Spring Boot em camadas, Postgres com Flyway e nuvem gratuita. Esta aba fixa as decisões; as demais trazem as telas e o backlog.

Demais abas: Telas (Figma) v2 · Backlog 1 — Fundação v2 (E01 a E04) · Backlog 2 — Núcleo v2 (E05 a E09) · Backlog 3 — Fluxos e fechamento v2 (E10 a E13)

## Decisões-chave

A stack ficou na forma mais simples que cumpre o PDF: monorepo com app Expo, API Spring Boot em camadas, Postgres com Flyway e nuvem gratuita. A tabela fixa cada escolha e o motivo, para ninguém reabrir a discussão no meio da sprint.

| Decisão | Escolha | Motivo |
| --- | --- | --- |
| Repositório | Monorepo simples: `apps/mobile` e `apps/api`, sem Nx nem Turborepo | Uma app JavaScript e uma Java; ferramenta de monorepo não traz ganho |
| App | React Native + Expo (SDK estável mais recente no bootstrap), TypeScript, Expo Router | Rotas por arquivo e APK gerado na nuvem pelo EAS Build |
| Design system | React Native Paper (Material Design 3) | Componentes prontos e tema único, foco em Android |
| Dados e cache no app | TanStack Query + Axios, sem persistência em disco | O cache vem pronto; nada de fila offline |
| Resiliência | Timeout de 15 s e retry; o `id` do registro nasce no app | Reenviar nunca duplica uma retirada |
| API | Java 21 + Spring Boot 4.1 (Maven), arquitetura em camadas com pastas por funcionalidade | Estrutura conhecida por quem está começando; a linha 3.5 saiu do suporte em 30/06/2026 |
| Banco | PostgreSQL + Flyway | Transação ACID para o saldo; o esquema é igual em todas as máquinas |
| Segurança | Spring Security, JWT único de 12 horas, dois perfis | Sem refresh token: menos código e menos erro |
| Auditoria | Livro de movimentações só de inserção (trigger no banco) e estorno como correção | Atende "imutável, sem exclusão física" |
| Logs | Filtro de `requestId` em toda linha, Actuator `/actuator/health` | Sem tracing nem coletor externo |
| Testes | JUnit + Mockito para as regras; poucos testes de integração no Postgres do Docker Compose e do CI; sem Testcontainers | Roda em qualquer máquina, sem Docker dentro do teste |
| Nuvem | Render (API em Docker) + Neon (Postgres) + EAS Build (APK) | Planos gratuitos sem cartão no banco; o Postgres grátis do Render expira em 30 dias |
| Nomenclatura | pt-BR com sufixos técnicos em inglês (`Service`, `Controller`, `Config`) | Mesmo vocabulário do PDF em código, banco e telas |

## Escopo do MVP

O plano anterior tinha 14 épicos; este tem 13, com bem menos telas e tecnologias. A tabela lista cada corte e o que fica no lugar, para quem ler o PDF não achar que algo sumiu por esquecimento.

| Item | Situação | Como fica no MVP |
| --- | --- | --- |
| Cinco perfis | Cortado | Dois perfis, ALMOXARIFE e GESTOR; um GESTOR é o administrador |
| Colaborador e auditor com login | Cortado | Colaborador é um cadastro sem login; quem audita é o GESTOR |
| Devolução e ajuste como telas próprias | Cortado | Entrada e saída com campo "motivo" (devolução, ajuste, recebimento, retirada) |
| Meta de 3 toques (antigo D-09) | Removida | Diretriz de fluxo curto, sem critério numérico |
| Central de notificações e push | Cortado | Contadores no Início e puxar para atualizar |
| Refresh token com rotação | Cortado | JWT único de 12 horas |
| Fila de envios, modo offline, `netinfo`, `AsyncStorage` | Cortado | Timeout, retry e botão "Tentar de novo" |
| Cache persistido | Cortado | Cache em memória do TanStack Query |
| Idempotência por cabeçalho e tabela própria | Simplificada | O `id` do registro é gerado no app; repetir o envio devolve o registro já gravado |
| Tracing com Micrometer | Cortado | Filtro de `requestId` nos logs |
| Testcontainers | Cortado | Postgres do Docker Compose e do CI |
| Importar e exportar CSV | Cortado | Cadastro de itens pela tela |
| Frente de obra cadastrada | Cortado | Campo de texto curto na movimentação e na requisição |
| Usuário ligado a vários canteiros | Cortado | ALMOXARIFE em um canteiro; GESTOR enxerga todos |
| `react-hook-form` e `zod` | Cortado | `useState` e funções de validação simples |
| Flyway | Mantido | Um arquivo `V1` com o esquema inteiro, com uma pessoa responsável |
| Trilha de auditoria, estorno e requisições com decisão | Mantidos | São o núcleo de Segurança e Auditoria e da Jornada 2 |
| Bloqueio após senhas erradas | Mantido (Should) | Duas colunas e uma regra; sai primeiro se faltar tempo |

## Ajustes ao documento do PDF

Dez pontos do PDF são ambíguos ou incompletos para virar código. Cada linha traz o padrão adotado neste backlog; a equipe pode trocá-lo, desde que avise a trilha afetada. Os códigos foram renumerados depois da remoção da antiga regra de 3 toques.

| Cód. | O que o PDF diz | Decisão padrão do backlog | Afeta |
| --- | --- | --- | --- |
| D-01 | Três perfis: almoxarife, colaborador, gestor/auditor | Dois perfis, ALMOXARIFE e GESTOR. Um GESTOR tem a marca de administrador e é criado na inicialização da API | E05, E06 |
| D-02 | O almoxarife aprova ou recusa requisições; a tabela de stakeholders diz que o mestre de obras também aprova | Os dois perfis criam; quem decide é sempre a outra pessoa: o ALMOXARIFE decide as do GESTOR e o GESTOR decide as do ALMOXARIFE | E10 |
| D-03 | Só existem entrada e saída | Continuam só entrada e saída, com o campo "motivo": recebimento, devolução e ajuste na entrada; retirada e ajuste na saída. Correção é o estorno | E08, E11 |
| D-04 | A Jornada 2 termina em "está em compra", mas Suprimentos fica fora do MVP | Status PENDENTE, APROVADA, RECUSADA, ATENDIDA e CANCELADA; quem aprova pode informar a previsão de chegada | E10 |
| D-05 | O mestre de obras prioriza alertas e notificações | Sem central de notificações: o Início mostra contadores (itens abaixo do mínimo, requisições para decidir, minhas requisições) e a lista atualiza ao abrir e ao puxar | E09, E10 |
| D-06 | Resiliência de rede "sem offline completo" | Timeout de 15 s, retry automático e botão "Tentar de novo"; o `id` do registro nasce no app, então reenviar não duplica | E12 |
| D-07 | Empresa com múltiplos canteiros | Uma empresa por instalação, vários canteiros, estoque separado por canteiro; o ALMOXARIFE pertence a um canteiro e o GESTOR vê todos e escolhe o ativo | E06 |
| D-08 | Não há cadastro de usuário nem e-mail | O administrador é criado por variáveis de ambiente; ele cria os demais funcionários com senha temporária, que só ele enxerga até a primeira troca; só ele redefine a senha ou desativa | E05, E06 |
| D-09 | A Jornada 1 pede o colaborador responsável pela retirada | Cadastro simples de colaborador (nome e canteiro), sem login, criado por ALMOXARIFE ou GESTOR, inclusive direto no seletor | E06, E08 |
| D-10 | A saída deve ser vinculada a uma frente de obra | Campo de texto curto "frente de obra", sem cadastro próprio; vira cadastro se sobrar tempo | E08, E10 |

## Monorepo e fluxo de trabalho

Um repositório com duas aplicações independentes, cada uma com o próprio gerenciador de dependências, e uma pasta `docs/` para decisões e protótipos. Não há `package.json` na raiz: o app é o único projeto JavaScript.

```text
canteiro-conectado/
├─ apps/
│  ├─ api/                      # Spring Boot (Maven)
│  │  ├─ src/main/java/br/com/canteiroconectado/
│  │  ├─ src/main/resources/ (application.yml, db/migration)
│  │  ├─ src/test/java/
│  │  ├─ Dockerfile
│  │  └─ pom.xml
│  └─ mobile/                   # Expo + React Native
│     ├─ app/                   # rotas (Expo Router)
│     ├─ src/                   # funcionalidades, compartilhado, tema
│     ├─ app.config.ts · eas.json
│     └─ package.json
├─ docs/
│  ├─ decisoes/                 # um arquivo por decisão (ADR)
│  ├─ api/                      # requisições de exemplo (.http)
│  └─ design/                   # link do Figma e exportações
├─ .github/
│  ├─ workflows/                # api.yml · mobile.yml
│  └─ ISSUE_TEMPLATE/ · pull_request_template.md
├─ docker-compose.yml           # API + Postgres para desenvolvimento local
├─ .env.example                 # variáveis documentadas, sem segredos
├─ .editorconfig · .gitignore
└─ README.md
```

Convenções para todo o grupo:

- **Branches:** `main` sempre implantável; trabalho em `feat/E08-H2-T2-registrar-saida` ou `fix/...`, com PR pequeno.
- **Commits:** tipos do Conventional Commits (`feat`, `fix`, `docs`) com mensagem em pt-BR e o código da issue, como `feat(api): registra saída de estoque [E08-H2-T2]`.
- **Pull request:** CI verde e uma aprovação de outra pessoa; quem é iniciante pede revisão a quem tem experiência, e o contrário também.
- **Issues:** título `[E08-H2-T2] Resumo`; rótulos `epic`, `story`, `task`, `area:mobile`, `area:api`, `area:design`, `area:devops`, `must`, `should`.
- **Definição de pronto:** código revisado, regra de negócio testada, variável nova no `.env.example` e funcionalidade conferida na nuvem.

## Convenções de nomenclatura

Todo identificador é escrito em pt-BR, sem acento nem cedilha, e leva o sufixo técnico em inglês quando ele for o termo consagrado (`Service`, `Controller`, `Repository`, `Config`, `Filter`, `Request`, `Response`). O texto que o usuário lê, com acento, fica em pt-BR.

| Elemento | Regra | Exemplos |
| --- | --- | --- |
| Classes Java | PascalCase em pt + sufixo técnico | `MovimentacaoService`, `EstoqueController`, `UsuarioRepository`, `SegurancaConfig`, `RequestIdFilter` |
| Entidades e enums | Substantivo pt no singular | `Canteiro`, `MovimentacaoEstoque`, `TipoMovimentacao.SAIDA`, `Perfil.GESTOR` |
| DTOs | pt + `Request` ou `Response` | `MovimentacaoRequest`, `LoginResponse` |
| Métodos e variáveis | camelCase em pt, verbo no infinitivo para ações | `registrarSaida`, `saldoAtual` |
| Pacotes Java | minúsculos; camadas com nome consagrado e funcionalidades em pt | `controller.estoque`, `service.requisicao`, `dominio.item` |
| Tabelas e colunas | snake\_case em pt, tabela no singular | `movimentacao_estoque`, `quantidade_minima`, `criado_em` |
| Endpoints | pt, minúsculas, plural, hífen entre palavras | `/api/v1/canteiros/{id}/movimentacoes`, `/usuarios/{id}/redefinir-senha` |
| Códigos de erro | MAIÚSCULAS em pt | `SALDO_INSUFICIENTE`, `ITEM_INATIVO` |
| Variáveis de ambiente | MAIÚSCULAS em pt | `BANCO_URL`, `JWT_SEGREDO`, `ADMIN_EMAIL` |
| Pastas e arquivos do app | pt sem acento; componentes em PascalCase | `funcionalidades/estoque`, `BotaoPrimario.tsx`, `SeletorItem.tsx` |
| Hooks e funções do app | `use` + pt, camelCase | `useSessao`, `useEstoque`, `registrarSaida` |
| Rotas do app | minúsculas em pt | `movimentar/saida`, `requisicoes/nova` |

Exceções: nomes impostos por ferramenta ou plataforma (`PORT`, `SPRING_PROFILES_ACTIVE`, `EXPO_PUBLIC_API_URL`, `Dockerfile`, `pom.xml`, `application.yml`, `index.tsx`, `_layout.tsx`) e siglas universais (`JWT`, `API`, `ID`, `CI`). O glossário em `docs/decisoes/glossario.md` fixa os termos do domínio: canteiro, colaborador, item, saldo, movimentação, estorno, requisição.

## Mobile: estrutura, dependências e design system

O app organiza o código por funcionalidade e deixa as rotas do Expo Router como arquivos finos que só montam telas. A regra de dependência é única: `app` importa de `funcionalidades`, `funcionalidades` importa de `compartilhado`, e nunca o contrário.

### Estrutura de diretórios

```text
apps/mobile/
├─ app/                              # só rotas e layouts (finos)
│  ├─ _layout.tsx                    # provedores: Paper, Query, Sessão
│  ├─ index.tsx                      # redireciona conforme a sessão
│  ├─ (autenticacao)/ login.tsx · trocar-senha.tsx
│  └─ (autenticado)/                 # área autenticada
│     ├─ _layout.tsx                 # guarda de sessão
│     ├─ (abas)/ inicio · estoque · movimentar · requisicoes · historico · mais
│     ├─ estoque/[id].tsx
│     ├─ movimentar/ saida · entrada · comprovante
│     ├─ movimentacao/[id].tsx · auditoria.tsx
│     ├─ requisicoes/ nova · [id]
│     └─ cadastros/ itens · colaboradores · canteiros · usuarios
├─ src/
│  ├─ funcionalidades/               # uma pasta por funcionalidade
│  │  ├─ autenticacao/ estoque/ movimentacoes/ requisicoes/
│  │  ├─ historico/ cadastros/ auditoria/
│  │  └─ <nome>/ telas · componentes · api.ts · hooks.ts · validacoes.ts
│  ├─ compartilhado/
│  │  ├─ api/ cliente.ts · erros.ts
│  │  ├─ componentes/ BotaoPrimario · CampoTexto · SeletorItem · ...
│  │  └─ utilitarios/ formatacao.ts · uuid.ts
│  ├─ tema/ tokens.ts · temaPaper.ts
│  └─ configuracao/ ambiente.ts       # lê EXPO_PUBLIC_API_URL
└─ app.config.ts · eas.json · tsconfig.json
```

As abas aparecem ou somem conforme o perfil (a aba Movimentar só para ALMOXARIFE; Histórico só para GESTOR). A tela de auditoria e os cadastros abrem a partir de Mais.

### Dependências

A lista é curta de propósito: pacote que não está aqui só entra por PR com justificativa.

| Pacote | Para quê | Entra em |
| --- | --- | --- |
| `expo`, `expo-router`, `react-native`, `typescript` | Base do projeto (template padrão do Expo) | E03 |
| `react-native-paper`, `react-native-safe-area-context`, `@expo/vector-icons` | Design system, áreas seguras e ícones | E03 |
| `@tanstack/react-query` | Cache em memória, estados de carregando e erro, retry | E03 |
| `axios` | Cliente HTTP com timeout e interceptor do token | E03 |
| `expo-secure-store` | Guarda o token no armazenamento seguro do Android | E05 |
| `expo-crypto` | `randomUUID()` para o `id` do registro | E08 |
| `@react-native-community/datetimepicker` | Data "necessário até" da requisição | E10 |
| `jest-expo`, `@testing-library/react-native`, `eslint-config-expo`, `prettier` | Testes, lint e formatação | E03 |

Ficam de fora, de propósito: Redux e Zustand (o único estado global é a sessão, em Context), `react-hook-form` e `zod` (formulários com `useState` e `validacoes.ts`), `date-fns` (datas com `toLocaleString('pt-BR')`), `netinfo`, `AsyncStorage`, bibliotecas de i18n, notificações e testes ponta a ponta.

### Cache, timeout e retry

O TanStack Query é a única camada de cache. Configuração padrão: `staleTime` de 30 s, consultas com 2 tentativas e espera crescente, e mutações com 1 nova tentativa só quando não houve resposta (rede ou timeout). O Axios usa timeout de 15 s. Depois de uma gravação com sucesso, o app invalida as consultas de estoque, resumo e histórico.

### Design system recomendado: React Native Paper

A recomendação é o **React Native Paper** (Material Design 3): funciona no Expo sem configuração nativa, entrega botões, campos, listas, chips, diálogos e *snackbars* prontos, e o tema é um objeto único. As alternativas (Gluestack, NativeWind) exigem montar os componentes à mão; Tamagui tem curva de aprendizado alta para o prazo.

Diretrizes pensadas para o Sebastião (luvas, sol, pressa): cor primária laranja de segurança com contraste AA, texto de 16 sp ou mais, alvos de toque de 48 dp ou mais, botão primário de largura total fixo no rodapé e três cores de status de estoque (normal, abaixo do mínimo, zerado) sempre com texto ou ícone, nunca só cor.

## API: camadas divididas por funcionalidade

A API é um serviço Java 21 com Spring Boot 4.1 em arquitetura em camadas (`controller`, `service`, `repository`, `dominio`, `dto`), e cada camada tem uma pasta por funcionalidade. Quem procura "tudo de estoque" abre a mesma subpasta em cada camada. Regra de dependência: o controller chama só o service; o service chama repositórios e outros services, nunca controllers; sem ciclo (`estoque` usa `item`, e `item` nunca usa `estoque`).

### Estrutura de diretórios

```text
apps/api/src/main/java/br/com/canteiroconectado/
├─ CanteiroConectadoApplication.java
├─ controller/
│  ├─ autenticacao/   AutenticacaoController
│  ├─ usuario/        UsuarioController
│  ├─ canteiro/       CanteiroController
│  ├─ colaborador/    ColaboradorController
│  ├─ item/           ItemController
│  ├─ estoque/        EstoqueController · MovimentacaoController
│  ├─ requisicao/     RequisicaoController
│  └─ auditoria/      AuditoriaController
├─ service/           AutenticacaoService · UsuarioService · EstoqueService ...
│  └─ (mesmas pastas)  MovimentacaoService · RequisicaoService · AuditoriaService
├─ repository/        UsuarioRepository · SaldoEstoqueRepository ... (mesmas pastas)
├─ dominio/           Usuario · Perfil · Canteiro · Colaborador · Item · TipoItem
│  └─ (mesmas pastas)  MovimentacaoEstoque · TipoMovimentacao · MotivoMovimentacao
│                      SaldoEstoque · Requisicao · ItemRequisicao · StatusRequisicao · LogAuditoria
├─ dto/               LoginRequest · LoginResponse · MovimentacaoRequest ... (mesmas pastas)
├─ seguranca/         SegurancaConfig · JwtService · UsuarioLogado · CanteiroAcessoService
├─ config/            OpenApiConfig · JwtProperties · AdminProperties · DbProperties · AmbienteValidator
├─ filtro/            RequestIdFilter
├─ excecao/           RegraDeNegocioException · CodigoErro · TratadorGlobalDeErros
└─ inicializacao/     AdminInicializador
apps/api/src/main/resources/
├─ application.yml · application-local.yml · application-prod.yml
└─ db/migration/      V1__esquema_inicial.sql · V2__... (Flyway)
```

### Dependências (geradas em start.spring.io)

| Dependência | Para quê |
| --- | --- |
| Spring Web e Validation | Endpoints REST e validação de entrada |
| Spring Data JPA, PostgreSQL Driver, Flyway Migration | Persistência e migrações versionadas |
| Spring Security e OAuth2 Resource Server | Login com JWT e autorização por perfil, sem biblioteca externa de token |
| Spring Boot Actuator | `/actuator/health` para a nuvem |
| springdoc-openapi (versão compatível com Spring Boot 4) | Swagger UI para a trilha mobile testar a API |
| Spring Boot Test e Spring Security Test | JUnit, Mockito e testes de endpoint |

Sem Lombok (DTOs são `record`), sem MapStruct, sem Micrometer Tracing, sem Testcontainers, sem Redis e sem biblioteca de rate limit.

### Segurança, logs e rastreabilidade

- **Administrador na inicialização:** o `AdminInicializador` lê `ADMIN_NOME`, `ADMIN_EMAIL` e `ADMIN_SENHA` e cria um GESTOR administrador se ainda não houver nenhum; no primeiro acesso o app oferece a troca de senha (opcional para ele, obrigatória para os funcionários).
- **Senha temporária visível ao administrador:** a senha gerada para um funcionário é guardada cifrada (AES-GCM, chave em `CHAVE_SENHA_TEMPORARIA`) só para o administrador consultar; ela é apagada na troca e cada consulta vira um evento em `log_auditoria`. O login usa apenas o hash BCrypt.
- **Autenticação:** e-mail e senha devolvem um JWT de 12 horas com `sub`, perfil, marca de administrador e canteiro; expirou, entra de novo.
- **Autorização:** `@PreAuthorize` nos services e o `CanteiroAcessoService` conferindo que o ALMOXARIFE só acessa o próprio canteiro.
- **Proteções básicas:** bloqueio de 15 minutos após 5 senhas erradas (Should), segredos só em variáveis de ambiente, nenhum token ou senha em log.
- **Logs:** o `RequestIdFilter` gera ou repassa `X-Request-Id`, guarda `requestId` e `usuarioId` no MDC e escreve uma linha de acesso por requisição; o padrão de log imprime o `requestId` em toda linha.
- **Erros:** respostas em Problem Details (RFC 9457) com campo `codigo` estável, que o app traduz em mensagem.

### Contrato da API v1 (base `/api/v1`)

| Endpoint | Quem acessa | Épico |
| --- | --- | --- |
| `POST /autenticacao/login`; `GET /ping` | Público | E05, E02 |
| `POST /autenticacao/trocar-senha`; `GET /autenticacao/eu` | Qualquer usuário logado | E05 |
| `GET/POST/PATCH /usuarios`, `GET /usuarios/{id}/credencial-inicial`, `POST /usuarios/{id}/redefinir-senha`, `/desativar`, `/ativar` | GESTOR administrador | E06 |
| `GET /canteiros`; `POST/PATCH /canteiros` | Leitura: todos; escrita: administrador | E06 |
| `GET/POST/PATCH /colaboradores?canteiroId=` | ALMOXARIFE, GESTOR | E06 |
| `GET /itens`, `GET /itens/{id}`; `POST/PATCH /itens` | Leitura: todos; escrita: ALMOXARIFE, GESTOR | E07 |
| `GET /canteiros/{id}/estoque`, `.../estoque/{itemId}`, `.../estoque/resumo` | Todos (ALMOXARIFE só do seu canteiro) | E09 |
| `POST /canteiros/{id}/movimentacoes` | ALMOXARIFE | E08 |
| `GET /movimentacoes/{id}`; `POST /movimentacoes/{id}/estorno` | Detalhe: ambos; estorno: ALMOXARIFE | E11 |
| `GET /movimentacoes` (histórico com filtros) | GESTOR | E11 |
| `POST /requisicoes`; `GET /requisicoes`, `GET /requisicoes/{id}` | ALMOXARIFE, GESTOR | E10 |
| `POST /requisicoes/{id}/aprovar`, `/recusar`, `/atender`, `/cancelar` | Decidir: a outra pessoa; atender: ALMOXARIFE; cancelar: autor | E10 |
| `GET /auditoria` | GESTOR | E11 |

## Modelo de dados

O coração do banco é o par `movimentacao_estoque` (livro de movimentações, só de inserção) e `saldo_estoque` (saldo por canteiro e item, atualizado na mesma transação). As chaves primárias são UUID e as datas são `timestamptz` em UTC. Todo o esquema nasce em um único `V1__esquema_inicial.sql`; ajustes posteriores entram em `V2`, `V3`, sempre por quem é responsável pelo banco.

| Tabela | Campos principais | Observações |
| --- | --- | --- |
| `usuario` | `id`, `nome`, `email` (único), `senha_hash`, `perfil` (ALMOXARIFE ou GESTOR), `administrador`, `canteiro_id`, `ativo`, `senha_temporaria_cifrada`, `precisa_trocar_senha`, `tentativas_falhas`, `bloqueado_ate`, `criado_em` | Nunca excluído, só desativado; `canteiro_id` é obrigatório para ALMOXARIFE e nulo para GESTOR; `senha_temporaria_cifrada` fica nula depois da troca |
| `canteiro` | `id`, `nome`, `endereco`, `ativo` | O "local" do canteiro |
| `colaborador` | `id`, `nome`, `canteiro_id`, `ativo` | Quem retira material; não tem login |
| `item` | `id`, `codigo` (único), `descricao`, `unidade`, `tipo` (MATERIAL ou FERRAMENTA), `quantidade_minima`, `ativo` | Catálogo único da empresa |
| `saldo_estoque` | `canteiro_id`, `item_id`, `quantidade`, `versao` | Chave composta; `CHECK (quantidade >= 0)` |
| `movimentacao_estoque` | `id` (gerado no app), `canteiro_id`, `item_id`, `tipo` (ENTRADA ou SAIDA), `motivo`, `quantidade`, `saldo_apos`, `colaborador_id`, `frente_obra`, `observacao`, `estorna_id`, `registrado_por`, `criado_em` | Imutável; motivos: RECEBIMENTO, DEVOLUCAO, RETIRADA, AJUSTE, ESTORNO |
| `requisicao` | `id` (gerado no app), `numero`, `canteiro_id`, `solicitante_id`, `frente_obra`, `necessario_ate`, `status`, `decidido_por`, `decidido_em`, `observacao_decisao`, `previsao_chegada`, `criado_em` | Status: PENDENTE, APROVADA, RECUSADA, ATENDIDA, CANCELADA |
| `item_requisicao` | `id`, `requisicao_id`, `item_id`, `quantidade` | Uma requisição pode ter vários itens |
| `log_auditoria` | `id`, `usuario_id`, `acao`, `entidade`, `entidade_id`, `detalhes` (jsonb), `request_id`, `criado_em` | Só de inserção, como o livro de movimentações |

Regras de integridade que a API garante:

- Saldo e movimentação mudam na mesma transação, com bloqueio de linha em `saldo_estoque` (`@Lock(PESSIMISTIC_WRITE)`); duas saídas simultâneas do mesmo item nunca geram saldo negativo.
- `UPDATE` e `DELETE` em `movimentacao_estoque` e `log_auditoria` são bloqueados por trigger criada no Flyway; erro de lançamento se corrige com um estorno, que é uma movimentação contrária apontando para a original, e cada movimentação só pode ser estornada uma vez.
- A quantidade é sempre positiva; o `tipo` define se soma ou subtrai. Quantidades usam `NUMERIC(14,3)` para aceitar kg e m³.
- Se o `id` de uma movimentação ou requisição já existir, a API devolve o registro gravado em vez de criar outro; se existir com conteúdo diferente, responde 409 `ID_JA_UTILIZADO`.
- Itens, usuários, canteiros e colaboradores nunca são excluídos, só desativados, para o histórico continuar legível.

## Perfis e permissões

Só há dois perfis, e o ALMOXARIFE, que opera o estoque, nunca é quem o audita. O GESTOR administrador é um GESTOR com a marca `administrador`, criado na inicialização da API, e é o único que gerencia canteiros e funcionários. A matriz é a fonte para os testes de autorização (E05) e para esconder botões no app.

| Capacidade | ALMOXARIFE | GESTOR | GESTOR administrador |
| --- | --- | --- | --- |
| Entrar e trocar a própria senha | Sim | Sim | Sim |
| Consultar estoque e o detalhe do item (com as 10 últimas movimentações) | Sim | Sim | Sim |
| Registrar entrada, saída e estorno | Sim | — | — |
| Ver o histórico completo de movimentações e a trilha de auditoria | — | Sim | Sim |
| Criar requisição | Sim | Sim | Sim |
| Aprovar ou recusar requisição | Só as do GESTOR | Só as do ALMOXARIFE | Só as do ALMOXARIFE |
| Marcar requisição como atendida | Sim | — | — |
| Cancelar a própria requisição | Sim | Sim | Sim |
| Gerir catálogo de itens | Sim | Sim | Sim |
| Cadastrar colaboradores | Sim | Sim | Sim |
| Gerir canteiros | — | — | Sim |
| Criar funcionários, ver a senha temporária, redefinir senha e desativar | — | — | Sim |

Regras que valem para toda a matriz:

- O ALMOXARIFE só enxerga o próprio canteiro; o GESTOR enxerga todos e escolhe o canteiro ativo no app.
- A senha temporária só aparece para o administrador, e só enquanto o funcionário não trocou a senha; depois da troca ela deixa de existir no banco.
- O app esconde o que o perfil não pode fazer, mas a API sempre revalida: esconder botão não é segurança.
