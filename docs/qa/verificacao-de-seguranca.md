# Verificação de segurança

Lista conferida antes da entrega (#175), em novembro de 2026. Cada item diz como foi conferido e qual teste
automatizado impede que ele volte a falhar. Nenhum achado foi crítico. Os riscos mantidos estão em
[riscos aceitos](../decisoes/riscos-aceitos.md).

| Item | Situação | Como foi conferido |
| --- | --- | --- |
| Nenhum segredo versionado | Ok | Busca no histórico de todas as branches (abaixo) |
| `JWT_SEGREDO` com 32 bytes ou mais | Ok | `JwtProperties` impede a subida com menos; `EnvironmentValidatorTest` |
| Mensagens de erro sem detalhe interno | Ok | `GlobalExceptionHandlerTest`, `PerfilDeProducaoTest` |
| Logs sem senha nem token | Ok | `LogsSemSegredoTest` |
| Swagger em produção | Desligado | `PerfilDeProducaoTest` |
| Senha temporária apagada depois da troca | Ok | `TrocaDeSenhaTest` |
| Acesso entre canteiros e entre perfis | Ok | `MatrizDePermissoesTest`, `CoberturaDaMatrizTest` |
| Dependências | Dependabot configurado; `npm audit` com 4 altas fora do APK | Abaixo |

## Segredos

- Nenhum `.env` entrou em commit algum; só o `.env.example`, sem valores.
- O histórico de todas as branches foi varrido com `git log --all -p` atrás de chaves privadas, tokens do GitHub,
  da AWS, da Expo e do Slack, JWTs, senhas da Neon (`npg_`) e URLs de banco com senha. Nada encontrado.
- Os únicos valores fixos no código são padrões de desenvolvimento: `canteiro`/`canteiro` do Postgres local e do
  CI, e `admin-local-123` no `application-local.yml`, que só vale no perfil `local`.
- No perfil `prod`, nenhuma variável obrigatória tem padrão. Sem `BANCO_*`, `JWT_SEGREDO` (32 caracteres ou mais),
  `ADMIN_*` ou `CHAVE_SENHA_TEMPORARIA`, a API não sobe e a mensagem nomeia a variável. Sem `DEMO_SENHA`, o perfil
  `demo` também não sobe.
- No app, o token fica no SecureStore (cifrado pelo Android) e nenhuma variável `EXPO_PUBLIC_*` carrega segredo.

## Erros e logs

- Todo erro sai em Problem Details com `codigo`, `detail` em português e `requestId`. O erro inesperado responde
  "Ocorreu um erro inesperado" e o JSON quebrado responde "A requisição é inválida", sem a mensagem do Jackson, do
  Hibernate ou do Postgres. A exceção completa vai só para o log, com o `requestId` para achar a linha.
- No perfil `prod`, `server.error.include-*` está em `never` e o `/actuator/health` responde só o `status`.
- `LogsSemSegredoTest` liga o nível DEBUG em todos os loggers, faz um login errado, um certo e uma chamada com o
  token, e confere que nenhuma linha de log contém a senha nem o token. O log de acesso registra método, caminho,
  status e tempo, sem a query string. `AdminProperties.toString()` mascara a senha.

## Swagger

Decisão: desligado no perfil `prod` (`springdoc.api-docs.enabled` e `springdoc.swagger-ui.enabled` em `false`).
O contrato fica documentado em `docs/api` e no Swagger local. `PerfilDeProducaoTest` sobe a API no perfil `prod` e
confere 404 em `/v3/api-docs`, `/swagger-ui.html` e `/swagger-ui/index.html`. Sem a configuração, o mesmo teste
recebe 200.

## Senha temporária

A senha temporária fica cifrada com AES-GCM (`CHAVE_SENHA_TEMPORARIA`) até o primeiro acesso, para o administrador
poder reexibi-la. Na troca de senha, o campo vira `NULL` (`TrocaDeSenhaTest` confere) e a credencial inicial passa
a responder `CREDENCIAL_JA_TROCADA`.

## Acesso entre canteiros e perfis

`MatrizDePermissoesTest` chama cada endpoint com o ALMOXARIFE do canteiro, o ALMOXARIFE de outro canteiro, o GESTOR
e o administrador. Também confere 401 sem token. `CoberturaDaMatrizTest` falha se surgir um endpoint fora da
matriz. O token antigo deixa de valer quando o usuário é desativado ou quando muda o perfil, o canteiro ou a
administração (`TokenDesatualizadoTest`, `UsuarioControllerTest`, `TrocaDeSenhaTest`). Cinco senhas erradas
bloqueiam a conta por 15 minutos.

## Dependências

`.github/dependabot.yml` abre PRs semanais para `develop` com as atualizações da API (Maven) e do app (npm), e
mensais para as GitHub Actions e a imagem Docker. As versões maiores do Expo, do React Native e do React ficam de
fora, porque precisam seguir o SDK do Expo (`npx expo install --fix`). O arquivo só passa a valer quando chegar à
`main`, que é a branch padrão do repositório.

`npm audit` em `apps/mobile`, em 1º/11/2026, contou 52 avisos: 0 críticos, 4 altos, 42 moderados e 6 baixos.

| Pacote (alto) | Vem de | Chega ao APK? |
| --- | --- | --- |
| `follow-redirects` 1.16.1 | `axios` | Não: o axios no React Native usa o adaptador XHR, e o `follow-redirects` só é usado no Node |
| `node-forge` 1.4.0 | `@expo/cli` | Não: ferramenta de linha de comando |
| `zod` 3.25 e 4.6.7 | `@expo/cli` e `eslint-plugin-react-hooks` | Não: ferramentas de build e lint |
| `@expo/cli` | `expo` | Não: ferramenta de linha de comando |

Os moderados e baixos vêm de `glob`, `minimatch`, `uuid` e `deepmerge`, usados pelo Jest, pelo ESLint e pelas
ferramentas da Expo. Os quatro altos têm correção sem mudar versão maior: rodar `npm audit fix` (sem `--force`) num
PR próprio e conferir `lint`, `typecheck`, `test` e `expo export`. Não use `npm audit fix --force`: ele propõe
voltar o Expo para o SDK 51.

A API não tem varredura de dependências no CI; os alertas do Dependabot cobrem o Maven pelo grafo de dependências
do GitHub.
