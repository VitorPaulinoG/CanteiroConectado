# Pendências manuais

O que falta para a entrega da versão 1.0 e não pode ser feito por código: os protótipos no Figma, os testes em
aparelho, as sessões com pessoas, as configurações que exigem acesso de administrador e a entrega em si. Cada
item aponta a issue e o documento que guia a execução.

## 1. Antes do primeiro deploy com estes PRs

| O que fazer | Onde | Por quê |
| --- | --- | --- |
| Criar `CHAVE_SENHA_TEMPORARIA` com `openssl rand -base64 32` | Render, aba Environment | Obrigatória no perfil `prod` desde #113: sem ela a API não sobe |
| Definir `SPRING_PROFILES_ACTIVE=prod,demo` e `DEMO_SENHA` | Render | Carrega os dados de demonstração (#173). Para um ambiente sem demonstração, deixe só `prod` |
| Recriar o banco local de quem rodou a V2 antiga | Máquina de quem testou o rascunho da #97 | A V2 mudou e o Flyway recusa o checksum antigo: `docker compose down -v` ou `dropdb` e `createdb` |

## 2. Integração contínua e repositório (precisa de administrador)

| O que fazer | Issue | Detalhe |
| --- | --- | --- |
| Rodar os workflows também na `develop` | #91, #92 | `api.yml` e `mobile.yml` (PR #195) disparam só em `push` e `pull_request` para a `main`. Os PRs de trabalho vão para a `develop` e hoje não são verificados. Inclua `develop` em `branches` nos dois gatilhos |
| Marcar os dois workflows como obrigatórios na `develop` e na `main` | #92 | Settings → Rules (ou Branches). Os nomes dos jobs são `coverage` (API) e `check` (Mobile) |
| Conferir a primeira execução do `api.yml` com Postgres | #171 | O serviço Postgres e o artefato `relatorios-api` entraram no PR #258; o resumo da cobertura aparece na página do job |
| Conferir o Dependabot | #175 | `.github/dependabot.yml` só vale depois de chegar à `main`, que é a branch padrão |
| Opcional: `npm audit fix` sem `--force` num PR próprio | #175 | Corrige os 4 avisos altos (nenhum chega ao APK); conferir `lint`, `typecheck`, `test` e `expo export` |

## 3. Testes em aparelho e com pessoas

| O que fazer | Issue | Guia |
| --- | --- | --- |
| Gerar o APK `preview` e instalar em pelo menos dois aparelhos | #93 | README, "Como gerar, baixar e instalar o APK". Confira antes que `EXPO_PUBLIC_API_URL` do ambiente `preview` do EAS aponta para a API no Render |
| Conferir a Jornada 2 no aparelho | #144 | [Jornada 2](qa/jornada-2.md), seção "Conferência no aparelho" |
| Executar o roteiro de falhas de rede | #170 | [Roteiro de rede](qa/rede.md): 7 cenários, com tabela de registro |
| Conduzir as sessões de uso com três pessoas | #177 | [Teste de uso](design/usabilidade.md): 5 tarefas, critério de aceite e fichas de registro |
| Tratar os problemas críticos das sessões | #178 | Issues abertas a partir do teste de uso |
| Rodar o reinício do banco uma vez na Neon | #174 | [Reinício do banco](reinicio-do-banco.md). Falta confirmar na Neon que o papel do SQL Editor é dono do schema |

## 4. Entrega da versão 1.0 (#180)

1. Com a `develop` estável e conferida na nuvem, abrir o PR de release da `develop` para a `main`.
2. Depois do deploy, reiniciar o banco de apresentação ([reinício do banco](reinicio-do-banco.md)).
3. Gerar o APK `preview` a partir da `main` e instalar nos aparelhos da apresentação.
4. Criar a tag: `git tag -a v1.0.0 -m "Canteiro Conectado 1.0" origin/main && git push origin v1.0.0`. Opcional:
   publicar uma release no GitHub com o link do build da Expo.
5. Ensaiar a demonstração inteira com o [roteiro](roteiro-demonstracao.md), incluindo acordar a API, as Jornadas 1
   e 2 com Sebastião e Camila e o plano B.

## 5. Protótipos no Figma

O app já está implementado com React Native Paper. Os protótipos podem partir das telas prontas: rode o app com os
dados de demonstração e reproduza cada tela no arquivo do Figma com o Material 3 Design Kit, usando só os
componentes listados. A coluna "No app" diz onde a tela está no código, em `apps/mobile/src`.

| Issue | Telas | Componentes do Paper | No app |
| --- | --- | --- | --- |
| #86 | Arquivo, fundamentos e ícone provisório | Material 3 Design Kit; páginas organizadas e link no README | `theme/` |
| #87 | Seis estados globais: carregando, vazio, erro com "Tentar de novo", servidor acordando, enviando e falha de envio | Só os componentes permitidos; revisar a lista com o time mobile | `shared/components` (`LoadingState`, `EmptyState`, `ErrorState`, `FaixaEstadoEnvio`), `shared/servidor/AvisoServidor` |
| #99 | S01 e S02 | `TextInput` com `TextInput.Icon`, `Button` contained com `loading`, `HelperText`, `Banner`, `ActivityIndicator` | `features/autenticacao/telas/AberturaTela`, `LoginTela` |
| #102 | S03 | `TextInput`, `List.Item` com ícone, `Button` contained e text | `features/autenticacao/telas/TrocarSenhaTela` |
| #107 | Abas por perfil e S19 | `BottomNavigation`, `Appbar`, `List.Item`, `Avatar.Text`, `Dialog`, `Button` | `app/(autenticado)/(abas)/_layout`, `features/mais/telas/MaisTela` |
| #110 | S23 | `List.Item`, `Chip`, `FAB`, `TextInput`, `Switch` | `features/cadastros/telas/CanteirosTela`, `CanteiroFormTela` |
| #114 | S24 e S25 | `Searchbar`, `Chip`, `List.Item`, `Card`, `TextInput`, `SegmentedButtons` ou `Menu`, `IconButton`, `Button`, `Dialog` | `features/cadastros/telas/FuncionariosTela`, `FuncionarioFormTela`, `componentes/CredencialInicialCard` |
| #117 | S22 e M2 | `Searchbar`, `List.Item`, `FAB`, `Modal` com `TextInput` | `features/cadastros/telas/ColaboradoresTela`, `shared/colaborador/SeletorColaborador` |
| #119 | S05 | `List.Item` com `RadioButton`, `Button`, nome do canteiro tocável em `Appbar` | `features/canteiro/telas/EscolherCanteiroTela` |
| #122 | S20 e S21 | `Searchbar`, `Chip`, `List.Item`, `FAB`, `TextInput`, `Menu` ou `SegmentedButtons`, `Switch`, `HelperText`, `Dialog` | `features/cadastros/telas/ItensTela`, `ItemFormTela` |
| #125 | M1, seletor de item | `Modal` em tela cheia, `Searchbar`, `List.Item`, `Chip` | `shared/item/SeletorItem` |
| #131 | S04 (ALMOXARIFE), S08, S09, M2 e S11, com os estados sem saldo, acima do saldo, enviando, falhou e salvo | `Card`, `Button`, `TextInput`, `Modal`, `IconButton`, `Chip`, `Banner` | `features/inicio/telas/InicioTela`, `features/movimentacoes/telas/MovimentarTela`, `SaidaTela`, `componentes/Comprovante` |
| #135 | S10 | `TextInput`, `SegmentedButtons`, `Button` | `features/movimentacoes/telas/EntradaTela` |
| #139 | S06 e S07, com carregando, vazio, erro e servidor acordando | `Searchbar`, `Chip`, `List.Item`, `Card`, `Button` | `features/estoque/telas/EstoqueTela`, `DetalheItemTela` |
| #142 | S04 para os dois perfis | `Appbar`, `Card`, `Button`, `List.Item`, `ActivityIndicator` | `features/inicio/telas/InicioTela` |
| #146 | S13 | `List.Item`, `IconButton`, `Button`, `TextInput`; data no diálogo nativo do `datetimepicker` | `features/requisicoes/telas/NovaRequisicaoTela` |
| #149 | S12 e S14, nos cinco status | `SegmentedButtons`, `Card`, `Chip`, `List.Item` com ícone | `features/requisicoes/telas/RequisicoesTela`, `DetalheRequisicaoTela` |
| #152 | S15 | `Modal` ancorado na base, `SegmentedButtons`, `TextInput`, `Button` | `features/requisicoes/componentes/DecisaoModal` |
| #157 | S16, com vazio por filtro, carregando e erro | `Chip`, `Modal` ancorado na base, `List.Subheader`, `List.Item`, `Badge` | `features/historico/telas/HistoricoTela`, `componentes/FiltrosModal` |
| #160 | S17 | `Card` ou `List.Item`, `Chip`, `Button`, `Dialog` com `TextInput` | `features/movimentacoes/telas/DetalheMovimentacaoTela`, `componentes/EstornoDialog` |
| #163 | S18 | `List.Item`, `Chip`, `List.Accordion` | `features/auditoria/telas/AuditoriaTela` |
| #169 | Estados de envio: faixa e selo nos três estados e "Não foi possível enviar" | `Banner`, `Chip`, `Snackbar`, `Button` | `shared/components/FaixaEstadoEnvio` |

## 6. Issues que já podem ser fechadas

| Issue | Feita em |
| --- | --- |
| #82, #83 (tema e componentes próprios) | PR #193 |
| #91 (workflow da API), #189 (README "Como rodar a API") | PR #195 |
| #93 (`eas.json` e README do APK) | PR #195; falta só instalar em dois aparelhos (item 3) |

## 7. Como fazer o merge da pilha

Os PRs #196 a #264 estão empilhados: cada um aponta para a branch do anterior, e o primeiro aponta para
`feature/integracao-continua` (PR #195). A ordem do merge é a da numeração, começando pelo #195.

- **Com "Create a merge commit":** faça o merge do PR na sua base e apague a branch. O GitHub muda a base do PR
  seguinte para `develop` sozinho, e o diff dele continua limpo, porque os commits são os mesmos.
- **Com squash:** depois de cada merge, rebaseie o PR seguinte sobre a `develop` com o comando do fim do corpo do
  PR (`git rebase --onto origin/develop <base-antiga> <branch>`), dê `git push --force-with-lease` e troque a base
  para `develop`.
