# Matriz de rastreabilidade

Liga cada funcionalidade e cada requisito não funcional do Canteiro Conectado às issues, aos PRs e aos testes que
comprovam o comportamento. Também mapeia o que o projeto demonstra nas três disciplinas: Administração de TI,
Segurança e Auditoria, e Programação Mobile.

Como ler:

- Os testes da API ficam em `apps/api/src/test/java/br/com/canteiroconectado`, no pacote da funcionalidade.
  Os do app ficam em `apps/mobile/src/**/__tests__` (nome terminado em `.test`).
- "Figma (pendência manual)" e "Tarefa manual" estão detalhados em [pendências manuais](pendencias-manuais.md).
- Os PRs a partir do #196 estão empilhados, em cadeia. Cada um aponta para o anterior e traz no corpo como
  rebasear depois do merge da base.

## Jornadas

| Jornada | Passos | Testes da API | Testes do app |
| --- | --- | --- | --- |
| 1. Sebastião (ALMOXARIFE) entrega uma furadeira ao João na frente Instalações elétricas | Login, Movimentar, Saída, escolhe item e colaborador, confirma, comprovante | `SaidaTest`, `MovimentacaoControllerTest`, `CargaDeDemonstracaoTest` (2 furadeiras na busca) | `LoginTela.test`, `MovimentarTela.test`, `SaidaTela.test`, `Comprovante.test` |
| 2. Camila (GESTOR) vê o cimento abaixo do mínimo e pede material | Início com alerta, estoque filtrado, detalhe, nova requisição, acompanha a decisão | `Jornada2Test`, `ResumoTest`, `CriarRequisicaoTest`, `DecisaoDeRequisicaoTest` | `InicioTela.test`, `EstoqueTela.test`, `DetalheItemTela.test`, `NovaRequisicaoTela.test`, `DetalheRequisicaoTela.test` |

## Requisitos não funcionais

| Requisito | Como o projeto atende | Issues | Testes ou evidência |
| --- | --- | --- | --- |
| Autenticação | E-mail e senha devolvem um JWT HS256 de 12 horas; sem token, 401 em Problem Details | #97, #104 | `AutenticacaoControllerTest`, `SegurancaTest` |
| Autorização por perfil e por canteiro | `@PreAuthorize` nos services e `CanteiroAcessoService`; o app esconde o que o perfil não pode, e a API revalida | #104, #105, #106, #108 | `MatrizDePermissoesTest` (38 endpoints × 4 perfis), `CoberturaDaMatrizTest`, `CanteiroAcessoServiceTest`, `SomentePerfil.test` |
| Token desatualizado deixa de valer | O filtro confere a cada requisição se o usuário está ativo e se perfil, canteiro e administração continuam iguais | #104, #112 | `TokenDesatualizadoTest`, `UsuarioControllerTest`, `TrocaDeSenhaTest` |
| Bloqueio após senhas erradas | 5 erros bloqueiam por 15 minutos; o login certo zera o contador | #98 | `AutenticacaoServiceTest`, `AutenticacaoControllerTest`, `LoginTela.test` |
| Senhas | Política de 8 caracteres com letra e número; hash BCrypt; senha temporária cifrada com AES-GCM e apagada na troca | #101, #113 | `PoliticaDeSenhaTest`, `CifradorSenhaTemporariaTest`, `CredencialInicialTest`, `TrocaDeSenhaTest` |
| Segredos fora do código | Toda configuração vem do ambiente; em `prod`, nenhuma variável obrigatória tem padrão e a API não sobe sem ela | #69, #113, #173, #175 | `EnvExampleTest`, `EnvironmentValidatorTest`, `DemoDataInitializerTest`, `PerfilDeProducaoTest` |
| Livro de movimentações imutável | Trigger `bloquear_alteracao()` impede `UPDATE` e `DELETE`; correção só por estorno | #73, #127, #159 | `BloquearAlteracaoTest`, `SaldoEstoqueRepositoryTest`, `EstornoTest` |
| Trilha de auditoria | Eventos obrigatórios gravados na mesma transação da ação, com autor e `requestId`; consulta pelo GESTOR (S18) | #162, #164 | `EventosObrigatoriosTest`, `AuditoriaControllerTest`, `AuditoriaTela.test` |
| Saldo consistente | Saldo travado na transação, `CHECK` contra negativo, `saldo_apos` em cada linha | #127, #128, #130 | `BloqueioDoSaldoTest`, `SaidasSimultaneasTest`, `InvarianteDoSaldoTest`, `MovimentacaoServiceTest` |
| Reenviar não duplica | O `id` do registro nasce no app; o mesmo pedido devolve o já gravado, outro conteúdo dá `ID_JA_UTILIZADO` | #165, #166, #145, #159 | `AplicarUmaVezIntegracaoTest`, `GravacoesSimultaneasTest`, `RequisicoesSimultaneasTest`, `EstornosSimultaneosTest`, `MesmoPedidoDeMovimentacaoTest`, `MesmoPedidoDeRequisicaoTest` |
| Timeout, retry e estados de envio | Axios com 15 s; nova tentativa automática quando não há resposta, segura porque o `id` vem do app; o envio mostra Enviando, Salvo ou Tentar de novo | #84, #85, #167, #168 | `client.test`, `queryClient.test`, `envio.test`, `FaixaEstadoEnvio.test`, `SaidaTela.test` |
| Servidor acordando | O app avisa e repete enquanto o Render sai do modo de espera | #94 | `AvisoServidor.test`, `acordarServidor.test` |
| Logs rastreáveis e sem segredo | `requestId` e `usuarioId` em toda linha; linha de acesso sem query string; nenhuma senha ou token | #74, #175 | `RequestIdFilterTest`, `SegurancaTest`, `LogsSemSegredoTest` |
| Erros padronizados | Problem Details com `codigo` estável; o app traduz o código em mensagem; sem detalhe interno | #75, #85, #175 | `GlobalExceptionHandlerTest`, `PerfilDeProducaoTest`, `errors.test` |
| Swagger só fora de produção | Swagger local; em `prod` responde 404 | #76, #175 | `PerfilDeProducaoTest` |
| Desempenho | Nenhuma tela com N+1; listas abaixo de 30 ms com 2 mil itens e 10 mil movimentações | #176 | `ConsultasPorRequisicaoTest`, `VolumeDeDadosTest`, [desempenho](qa/desempenho.md) |
| Datas e fuso | API em UTC; "hoje" nas regras é o dia de São Paulo | #145, #156 | `CriarRequisicaoTest`, `DecisaoDeRequisicaoTest`, `HistoricoTest`, `filtros.test` |
| Integração contínua | Workflows da API (com Postgres e relatório de cobertura) e do app (lint, tipos, testes, APK) | #91, #92, #171 | `.github/workflows`, artefato `relatorios-api` |
| APK instalável | Perfil `preview` do EAS gera APK | #93 | README, "Como gerar, baixar e instalar o APK" |
| Dados de demonstração e reinício | Perfil `demo` monta as jornadas pelos services; procedimento para zerar o banco da Neon | #173, #174 | `CargaDeDemonstracaoTest`, `Jornada2Test`, [reinício do banco](reinicio-do-banco.md) |
| Dependências atualizadas | Dependabot semanal para Maven e npm; `npm audit` revisado | #175 | `.github/dependabot.yml`, [verificação de segurança](qa/verificacao-de-seguranca.md) |

## O que o projeto demonstra por disciplina

### Administração de TI

- **Ambientes e configuração:** perfis `local`, `prod` e `demo`. Toda configuração vem de variáveis de ambiente
  listadas no `.env.example` (`EnvExampleTest`). A validação na subida nomeia a variável que falta
  (`EnvironmentValidator`).
- **Containers:** Dockerfile em dois estágios com JRE Alpine e limite de 512 MB no Compose; Postgres no Compose
  para desenvolvimento.
- **Nuvem:** API no Render, banco na Neon com SSL obrigatório, APK pelo EAS Build. O roteiro de demonstração cobre
  o serviço que dorme, e o reinício do banco devolve o ambiente ao estado da apresentação.
- **Banco:** migrations Flyway de V1 a V7 com `ddl-auto: validate`, índices conferidos com `EXPLAIN ANALYZE`
  ([desempenho](qa/desempenho.md)).
- **Observabilidade:** `/actuator/health`, `/api/v1/ping` com versão e hora, `requestId` em todos os logs e nas
  respostas de erro.
- **Fluxo de trabalho e CI:** `CONTRIBUTING.md` com convenção de branch, commit e PR; workflows do GitHub Actions
  com relatório de cobertura; Dependabot.

### Segurança e Auditoria

- **Identidade e acesso:** JWT, Spring Security com `@PreAuthorize` e acesso por canteiro. A matriz de permissões é
  testada endpoint a endpoint e falha se surgir um endpoint sem linha.
- **Segregação de funções:** o ALMOXARIFE opera o estoque e o GESTOR audita; quem pede uma requisição não a decide;
  só o administrador gere canteiros e funcionários.
- **Proteção de credenciais:** BCrypt, política de senha, bloqueio por tentativas, senha temporária cifrada com
  AES-GCM e apagada na troca. Troca obrigatória no primeiro acesso. Token no SecureStore do Android.
- **Integridade:** livro de movimentações só de inserção (trigger no banco), estorno vinculado à original (no
  máximo um por movimentação), saldo travado por transação.
- **Rastreabilidade:** `log_auditoria` com os eventos obrigatórios (`AcaoAuditoria.OBRIGATORIAS`), autor,
  entidade, detalhes e `requestId`; histórico de movimentações e trilha consultáveis pelo GESTOR (S16, S17, S18).
- **Revisão:** [verificação de segurança](qa/verificacao-de-seguranca.md) e [riscos aceitos](decisoes/riscos-aceitos.md).

### Programação Mobile

- **Plataforma:** Expo SDK 58, React Native 0.88, TypeScript estrito, Expo Router com rotas protegidas pela sessão,
  pelo perfil e pelo canteiro ativo.
- **Interface:** React Native Paper (Material 3), abas por perfil, 25 telas, estados de carregando, vazio e erro.
- **Dados:** TanStack Query para cache, invalidação após gravar e atualização ao voltar para a tela; Axios com
  timeout e retry; erros da API traduzidos por código.
- **Envio seguro:** `id` gerado no aparelho (`expo-crypto`), estados Enviando, Salvo e Tentar de novo, comprovante
  com os dados do servidor.
- **Recursos do aparelho:** SecureStore para a sessão, área de transferência para a senha temporária, seletor de
  data nativo do Android.
- **Qualidade:** ESLint impedindo uma funcionalidade de importar outra, 297 testes com Jest e Testing Library.

## Funcionalidades por épico

### E01 · Setup do repositório (monorepo) (#1)

**E01-H1 · Criar o repositório e a estrutura do monorepo** (#2)

| Tarefa | Issue | PR | Testes ou evidência |
| --- | --- | --- | --- |
| Criar o repositório no GitHub | #61 | — | Repositório criado no GitHub |
| Criar pastas e arquivos de raiz | #62 | — | Estrutura do monorepo |
| Escrever o README da raiz | #63 | — | `README.md` |

**E01-H2 · Definir o fluxo de trabalho e registrar as decisões** (#3)

| Tarefa | Issue | PR | Testes ou evidência |
| --- | --- | --- | --- |
| Proteger a main e criar modelos | #64 | — | Proteção da `main` e modelos de issue |
| Montar o quadro e importar o backlog | #65 | — | Quadro do projeto |
| Escrever CONTRIBUTING, decisões e glossário | #66 | #181 | `CONTRIBUTING.md` |

### E02 · Setup da API (Spring Boot em camadas) (#4)

**E02-H1 · Bootstrap da API e estrutura em camadas** (#5)

| Tarefa | Issue | PR | Testes ou evidência |
| --- | --- | --- | --- |
| Gerar e commitar o projeto | #67 | — | Projeto gerado no start.spring.io |
| Criar a estrutura de pacotes e o ping | #68 | #196 | PingControllerTest |

**E02-H2 · Configuração por perfis e variáveis de ambiente** (#6)

| Tarefa | Issue | PR | Testes ou evidência |
| --- | --- | --- | --- |
| Arquivos de configuração e propriedades tipadas | #69 | #182 | EnvExampleTest, EnvironmentValidatorTest |

**E02-H3 · Dockerização da API** (#7)

| Tarefa | Issue | PR | Testes ou evidência |
| --- | --- | --- | --- |
| Escrever Dockerfile e .dockerignore | #70 | #183 | Imagem Docker da API |
| Escrever o docker-compose.yml | #71 | #183 | `docker-compose.yml` |
| Preencher "Como rodar a API" no README | #189 | #195 | README, "Como rodar a API" |

**E02-H4 · Banco PostgreSQL e Flyway** (#8)

| Tarefa | Issue | PR | Testes ou evidência |
| --- | --- | --- | --- |
| Configurar datasource e pool | #72 | #184 | Datasource e pool Hikari |
| Escrever o V1 do esquema | #73 | #184 | BloquearAlteracaoTest |

**E02-H5 · Logs com requestId, erros padronizados e Swagger** (#9)

| Tarefa | Issue | PR | Testes ou evidência |
| --- | --- | --- | --- |
| RequestIdFilter e log de acesso | #74 | #185, #192 | RequestIdFilterTest |
| Tratamento global de erros | #75 | #185 | GlobalExceptionHandlerTest |
| Configurar o Swagger | #76 | #185 | Swagger local |

### E03 · Setup do app mobile (Expo) (#10)

**E03-H1 · Bootstrap do app Expo** (#11)

| Tarefa | Issue | PR | Testes ou evidência |
| --- | --- | --- | --- |
| Criar o projeto Expo | #77 | #186 | Projeto Expo |
| Configurar app.config.ts | #78 | #186 | `app.config.ts` |
| Configurar qualidade de código | #79 | #186 | ESLint e Prettier |

**E03-H2 · Estrutura de diretórios e dependências** (#12)

| Tarefa | Issue | PR | Testes ou evidência |
| --- | --- | --- | --- |
| Instalar as dependências do núcleo | #80 | #187 | Dependências do núcleo |
| Criar a estrutura e as regras de importação | #81 | #187 | Regras de importação no ESLint |

**E03-H3 · Tema Material 3 e componentes próprios com React Native Paper** (#13)

| Tarefa | Issue | PR | Testes ou evidência |
| --- | --- | --- | --- |
| Configurar o provedor e o tema | #82 | #193 | Tema em `src/theme` |
| Criar os componentes próprios | #83 | #193 | components.test |

**E03-H4 · Cliente HTTP, ambiente, timeout e retry** (#14)

| Tarefa | Issue | PR | Testes ou evidência |
| --- | --- | --- | --- |
| Configuração de ambiente e cliente Axios | #84 | #197 | client.test, env.test |
| Tradução de erros e TanStack Query | #85 | #198 | SobreTela.test, errors.test, queryClient.test |
| Apontar o app para a API local | #190 | #199 | README, "Apontar o app para a API local" |

**E03-H5 · Protótipo no Figma baseado no Material 3** (#15)

| Tarefa | Issue | PR | Testes ou evidência |
| --- | --- | --- | --- |
| Criar o arquivo e os fundamentos | #86 | — | Figma (pendência manual) |
| Desenhar os estados globais | #87 | — | Figma (pendência manual) |

### E04 · Nuvem, containers e CI/CD (#16)

**E04-H1 · Criar as contas e os serviços de nuvem** (#17)

| Tarefa | Issue | PR | Testes ou evidência |
| --- | --- | --- | --- |
| Criar banco e contas | #88 | — | Contas na Neon, no Render e na Expo |

**E04-H2 · Deploy contínuo da API no Render** (#18)

| Tarefa | Issue | PR | Testes ou evidência |
| --- | --- | --- | --- |
| Criar o Web Service | #89 | — | Web Service no Render |
| Validar memória e tempo de subida | #90 | — | Memória e subida conferidas no Render |
| Converter a conexão da Neon para JDBC | #191 | — | URL JDBC da Neon |

**E04-H3 · Integração contínua com GitHub Actions** (#19)

| Tarefa | Issue | PR | Testes ou evidência |
| --- | --- | --- | --- |
| Fluxo da API | #91 | #195 | Workflow `api.yml` |
| Fluxo do app e proteção da main | #92 | #195 | Workflow `mobile.yml`; proteção da `main` pendente |

**E04-H4 · Geração do APK com EAS Build** (#20)

| Tarefa | Issue | PR | Testes ou evidência |
| --- | --- | --- | --- |
| Configurar eas.json e gerar o primeiro APK | #93 | #195 | `eas.json`; instalação em dois aparelhos pendente |

**E04-H5 · Servidor acordando e roteiro de demonstração**

| Tarefa | Issue | PR | Testes ou evidência |
| --- | --- | --- | --- |
| Aviso de servidor acordando | #94 | #200 | AvisoServidor.test, acordarServidor.test |
| Escrever o roteiro de demonstração | #95 | #201 | `docs/roteiro-demonstracao.md` |

### E05 · Autenticação e autorização (#22)

**E05-H1 · Administrador na inicialização e login** (#23)

| Tarefa | Issue | PR | Testes ou evidência |
| --- | --- | --- | --- |
| Criar o administrador na inicialização | #96 | #203 | AdminInitializerIntegracaoTest, AdminInitializerTest |
| Usuário, perfil e login | #97 | #202 | AutenticacaoControllerTest |
| Bloqueio por tentativas (Should) | #98 | #204 | AutenticacaoControllerTest, AutenticacaoServiceTest |
| Prototipar S01 e S02 | #99 | — | Figma (pendência manual) |
| Implementar S01, S02 e a sessão | #100 | #208 | LoginTela.test, SessaoProvider.test, client.test, navegacao.test, storage.test, validations.test |

**E05-H2 · Troca de senha e primeiro acesso** (#25)

| Tarefa | Issue | PR | Testes ou evidência |
| --- | --- | --- | --- |
| Endpoint de troca de senha | #101 | #206 | PoliticaDeSenhaTest, TrocaDeSenhaTest |
| Prototipar S03 | #102 | — | Figma (pendência manual) |
| Implementar S03 e a guarda de primeiro acesso | #103 | #209 | TrocarSenhaTela.test, navegacao.test, validations.test |

**E05-H3 · Acesso por perfil e por canteiro** (#26)

| Tarefa | Issue | PR | Testes ou evidência |
| --- | --- | --- | --- |
| Configurar segurança e autorização | #104 | #205 | SegurancaTest |
| Conferir o canteiro | #105 | #207 | CanteiroAcessoServiceTest |
| Testes da matriz de permissões | #106 | #254 | AdminInitializerIntegracaoTest, MatrizDePermissoesTest |
| Prototipar as abas por perfil e S19 | #107 | — | Figma (pendência manual) |
| Implementar abas por perfil e S19 | #108 | #210 | MaisTela.test, SomentePerfil.test, navegacao.test, rotulos.test |

### E06 · Cadastros-base: canteiros, funcionários e colaboradores (#27)

**E06-H1 · Gerir canteiros** (#28)

| Tarefa | Issue | PR | Testes ou evidência |
| --- | --- | --- | --- |
| Entidade e endpoints | #109 | #211 | CanteiroControllerTest, SegurancaTest |
| Prototipar S23 | #110 | — | Figma (pendência manual) |
| Implementar S23 | #111 | #215 | CanteiroFormTela.test, CanteirosTela.test, MaisTela.test, validations.test |

**E06-H2 · Gerir funcionários e a credencial inicial** (#29)

| Tarefa | Issue | PR | Testes ou evidência |
| --- | --- | --- | --- |
| Endpoints de usuários | #112 | #212 | CanteiroControllerTest, GeradorSenhaTemporariaTest, UsuarioControllerTest |
| Senha temporária cifrada | #113 | #213 | ChaveLocalEnvironmentPostProcessorTest, CifradorSenhaTemporariaTest, CredencialInicialTest, EnvironmentValidatorTest |
| Prototipar S24 e S25 | #114 | — | Figma (pendência manual) |
| Implementar S24 e S25 | #115 | #216 | FuncionarioFormTela.test, FuncionariosTela.test, validations.test |

**E06-H3 · Cadastrar colaboradores** (#30)

| Tarefa | Issue | PR | Testes ou evidência |
| --- | --- | --- | --- |
| Endpoints de colaboradores | #116 | #214 | ColaboradorControllerTest |
| Prototipar S22 e M2 | #117 | — | Figma (pendência manual) |
| Implementar S22 e `SeletorColaborador` | #118 | #218 | ColaboradoresTela.test, SeletorColaborador.test |

**E06-H4 · Escolher o canteiro ativo (GESTOR)** (#31)

| Tarefa | Issue | PR | Testes ou evidência |
| --- | --- | --- | --- |
| Prototipar S05 | #119 | — | Figma (pendência manual) |
| Implementar o canteiro ativo | #120 | #217 | CanteiroAtivoProvider.test, EscolherCanteiroTela.test, FuncionarioFormTela.test, MaisTela.test, navegacao.test |

### E07 · Catálogo de itens (#32)

**E07-H1 · Cadastrar, editar e desativar itens** (#33)

| Tarefa | Issue | PR | Testes ou evidência |
| --- | --- | --- | --- |
| Entidade, regras e endpoints | #121 | #219 | ItemControllerTest |
| Prototipar S20 e S21 | #122 | — | Figma (pendência manual) |
| Implementar S20 e S21 | #123 | #221 | ItemFormTela.test, ItensTela.test, quantidade.test, validations.test |

**E07-H2 · Buscar itens com rapidez** (#34)

| Tarefa | Issue | PR | Testes ou evidência |
| --- | --- | --- | --- |
| Busca paginada | #124 | #220 | ItemControllerTest |
| Prototipar o seletor M1 | #125 | — | Figma (pendência manual) |
| Implementar o `SeletorItem` | #126 | #231 | ItemFormTela.test, SeletorItem.test, api.test |

### E08 · Movimentações de estoque (entrada e saída) (#35)

**E08-H1 · Base do livro de movimentações e do saldo** (#36)

| Tarefa | Issue | PR | Testes ou evidência |
| --- | --- | --- | --- |
| Entidades e repositórios | #127 | #222 | BloqueioDoSaldoTest, SaldoEstoqueRepositoryTest |
| MovimentacaoService transacional | #128 | #224 | MovimentacaoServiceIntegracaoTest, MovimentacaoServiceTest |
| Endpoint de escrita e testes | #129 | #227 | MovimentacaoControllerTest, SaidasSimultaneasTest |

**E08-H2 · Registrar saída de material ou ferramenta** (#37)

| Tarefa | Issue | PR | Testes ou evidência |
| --- | --- | --- | --- |
| Regras da saída | #130 | #228 | MovimentacaoControllerTest, SaidaTest, SaidasSimultaneasTest |
| Prototipar S04 (ALMOXARIFE), S08, S09, M2 e S11 | #131 | — | Figma (pendência manual) |
| Implementar S08 e S09 | #132 | #233 | MovimentarTela.test, SaidaTela.test, validations.test |
| Implementar S11 e atualizar as consultas | #133 | #236 | SaidaTela.test, hooks.test |

**E08-H3 · Registrar entrada de material** (#38)

| Tarefa | Issue | PR | Testes ou evidência |
| --- | --- | --- | --- |
| Regras da entrada | #134 | #229 | EntradaTest |
| Prototipar S10 | #135 | — | Figma (pendência manual) |
| Implementar S10 | #136 | #237 | EntradaTela.test, validations.test |

### E09 · Consulta de estoque e alertas de estoque mínimo (#39)

**E09-H1 · Consultar o estoque do canteiro** (#40)

| Tarefa | Issue | PR | Testes ou evidência |
| --- | --- | --- | --- |
| Consulta de estoque | #137 | #223 | EstoqueControllerTest, StatusEstoqueTest |
| Detalhe do item | #138 | #230 | DetalheDoItemTest |
| Prototipar S06 e S07 | #139 | — | Figma (pendência manual) |
| Implementar S06 e S07 | #140 | #232 | DetalheItemTela.test, EstoqueTela.test |

**E09-H2 · Alertas e contadores no Início** (#41)

| Tarefa | Issue | PR | Testes ou evidência |
| --- | --- | --- | --- |
| Endpoint de resumo | #141 | #242 | ResumoTest |
| Prototipar S04 para os dois perfis | #142 | — | Figma (pendência manual) |
| Implementar S04 | #143 | #247 | InicioTela.test, navegacao.test |
| Conferir a Jornada 2 até o alerta | #144 | #256 | Jornada2Test |

### E10 · Requisições de material (#42)

**E10-H1 · Criar uma requisição de material** (#43)

| Tarefa | Issue | PR | Testes ou evidência |
| --- | --- | --- | --- |
| Entidades e criação | #145 | #238 | CriarRequisicaoTest, RequisicoesSimultaneasTest |
| Prototipar S13 | #146 | — | Figma (pendência manual) |
| Implementar S13 | #147 | #243 | NovaRequisicaoTela.test, data.test, validations.test |

**E10-H2 · Listar e acompanhar requisições** (#44)

| Tarefa | Issue | PR | Testes ou evidência |
| --- | --- | --- | --- |
| Consultas | #148 | #239 | ConsultaDeRequisicoesTest |
| Prototipar S12 e S14 | #149 | — | Figma (pendência manual) |
| Implementar S12 e S14 | #150 | #244 | DetalheRequisicaoTela.test, NovaRequisicaoTela.test, RequisicoesTela.test, useAtualizarAoFocar.test |

**E10-H3 · Aprovar ou recusar uma requisição** (#45)

| Tarefa | Issue | PR | Testes ou evidência |
| --- | --- | --- | --- |
| Endpoints de decisão | #151 | #240 | DecisaoDeRequisicaoTest |
| Prototipar S15 | #152 | — | Figma (pendência manual) |
| Implementar a decisão | #153 | #245 | DecisaoModal.test, validations.test |

**E10-H4 · Atender e cancelar requisições** (#46)

| Tarefa | Issue | PR | Testes ou evidência |
| --- | --- | --- | --- |
| Atender e cancelar | #154 | #241 | AtenderECancelarTest |
| Botões e confirmações em S14 | #155 | #246 | AtenderECancelar.test |

### E11 · Histórico e trilha de auditoria (#47)

**E11-H1 · Consultar o histórico de movimentações** (#48)

| Tarefa | Issue | PR | Testes ou evidência |
| --- | --- | --- | --- |
| Consulta paginada | #156 | #248 | HistoricoTest |
| Prototipar S16 | #157 | — | Figma (pendência manual) |
| Implementar S16 | #158 | #251 | HistoricoTela.test, filtros.test |

**E11-H2 · Detalhe da movimentação e estorno** (#49)

| Tarefa | Issue | PR | Testes ou evidência |
| --- | --- | --- | --- |
| Detalhe e estorno | #159 | #249 | EstornoTest, EstornosSimultaneosTest |
| Prototipar S17 | #160 | — | Figma (pendência manual) |
| Implementar S17 | #161 | #252 | DetalheItemTela.test, DetalheMovimentacaoTela.test |

**E11-H3 · Trilha de auditoria do sistema** (#50)

| Tarefa | Issue | PR | Testes ou evidência |
| --- | --- | --- | --- |
| Consulta e cobertura de eventos | #162 | #250 | AuditoriaControllerTest, EventosObrigatoriosTest |
| Prototipar S18 | #163 | — | Figma (pendência manual) |
| Implementar S18 | #164 | #253 | AuditoriaTela.test, MaisTela.test |

### E12 · Resiliência de rede (timeout, retry e envio seguro) (#51)

**E12-H1 · API aceita o id do cliente e não duplica** (#52)

| Tarefa | Issue | PR | Testes ou evidência |
| --- | --- | --- | --- |
| Tratar o `id` do cliente | #165 | #225 | AplicarUmaVezIntegracaoTest, IdDoClienteTest, MovimentacaoServiceTest |
| Testes | #166 | #226 | GravacoesSimultaneasTest, MovimentacaoServiceTest |

**E12-H2 · Timeout, retry e estados de envio no app** (#53)

| Tarefa | Issue | PR | Testes ou evidência |
| --- | --- | --- | --- |
| Mutação com estados de envio | #167 | #234 | SaidaTela.test, envio.test |
| Componente de estado e testes | #168 | #235 | FaixaEstadoEnvio.test, SaidaTela.test |
| Prototipar os estados de envio | #169 | — | Figma (pendência manual) |

**E12-H3 · Roteiro de validação das falhas de rede** (#54)

| Tarefa | Issue | PR | Testes ou evidência |
| --- | --- | --- | --- |
| Executar e registrar o roteiro | #170 | — | Tarefa manual: roteiro em `docs/qa/rede.md` |

### E13 · Qualidade, dados de demonstração e entrega do MVP (#55)

**E13-H1 · Testes nas regras críticas** (#56)

| Tarefa | Issue | PR | Testes ou evidência |
| --- | --- | --- | --- |
| Completar os testes da API | #171 | #258 | CoberturaDaMatrizTest, InvarianteDoSaldoTest, MatrizDePermissoesTest, MesmoPedidoDeMovimentacaoTest, MesmoPedidoDeRequisicaoTest, RegrasDoAndamentoTest, TokenDesatualizadoTest |
| Completar os testes do app | #172 | #259 | Comprovante.test, LoginTela.test, NovaRequisicaoTela.test, SaidaTela.test, validations.test |

**E13-H2 · Dados de demonstração realistas** (#57)

| Tarefa | Issue | PR | Testes ou evidência |
| --- | --- | --- | --- |
| Carga de demonstração | #173 | #255 | CargaDeDemonstracaoTest, DemoDataInitializerTest |
| Procedimento de reinício | #174 | #257 | `docs/reinicio-do-banco.md` |

**E13-H3 · Revisão de segurança e de desempenho** (#58)

| Tarefa | Issue | PR | Testes ou evidência |
| --- | --- | --- | --- |
| Lista de verificação de segurança | #175 | #260 | LogsSemSegredoTest, PerfilDeProducaoTest |
| Revisão de desempenho | #176 | #261 | ConsultasPorRequisicaoTest, VolumeDeDadosTest |

**E13-H4 · Teste de uso com pessoas parecidas com as personas** (#59)

| Tarefa | Issue | PR | Testes ou evidência |
| --- | --- | --- | --- |
| Conduzir as sessões | #177 | — | Figma (pendência manual) |
| Ajustes de usabilidade | #178 | — | Depende das sessões da #177 |

**E13-H5 · Documentação final e entrega da versão 1.0** (#60)

| Tarefa | Issue | PR | Testes ou evidência |
| --- | --- | --- | --- |
| Matriz de rastreabilidade | #179 | PR desta tarefa | `docs/rastreabilidade.md` |
| Documentação, versão e ensaio | #180 | PR desta tarefa | README, guias por perfil, `docs/api` |
