# API v1

Base: `/api/v1`. O contrato completo, com os campos e os exemplos de cada resposta, está no Swagger local
(`http://localhost:8080/swagger-ui.html`), que fica desligado no perfil `prod`. Exemplos prontos para rodar estão
em [`exemplos.http`](exemplos.http). Os códigos de erro estão em [`codigos-erro.md`](codigos-erro.md).

## Regras gerais

- **Autenticação:** `POST /autenticacao/login` devolve um JWT de 12 horas. As demais rotas exigem o cabeçalho
  `Authorization: Bearer <token>`. São públicos só o login, o `GET /ping`, o `/actuator/health` e, fora de `prod`,
  o Swagger.
- **Troca obrigatória:** enquanto o funcionário não troca a senha temporária, só `GET /autenticacao/eu` e
  `POST /autenticacao/trocar-senha` respondem; o resto devolve 403 `TROCA_DE_SENHA_OBRIGATORIA`.
- **Canteiro:** o ALMOXARIFE só acessa o próprio canteiro (403 `ACESSO_NEGADO` nos outros). O GESTOR acessa todos e
  informa o canteiro nas consultas.
- **Erros:** Problem Details (RFC 9457) com `codigo` estável, `detail` em português, `requestId` e, quando o erro é
  de um campo, `campo`. Regra de negócio responde 422; conflito de estado ou unicidade, 409.
- **Paginação:** `pagina` começa em 0 e `tamanho` vale 20 por padrão (máximo 100). A resposta traz `conteudo`,
  `pagina`, `tamanho`, `totalElementos` e `totalPaginas`.
- **Datas:** instantes em UTC (ISO 8601, com `Z`); datas sem hora (`necessarioAte`, `previsaoChegada`) em
  `AAAA-MM-DD`. "Hoje", nas regras de data, é o dia em São Paulo.
- **Reenvio seguro:** movimentação, estorno e requisição recebem o `id` (UUID) gerado pelo app. O mesmo pedido
  reenviado devolve 200 com o registro já gravado, em vez de 201; o mesmo `id` com outro conteúdo devolve 409
  `ID_JA_UTILIZADO`.

## Endpoints

| Método e caminho | Quem pode | Para quê |
| --- | --- | --- |
| `GET /ping` | Público | Nome, versão e hora do servidor |
| `POST /autenticacao/login` | Público | Entrar |
| `GET /autenticacao/eu` | Todos | Dados do usuário logado |
| `POST /autenticacao/trocar-senha` | Todos | Trocar a própria senha |
| `GET /canteiros` | Todos | Canteiros que o usuário enxerga (`ativo`) |
| `GET /canteiros/{id}` | Todos, no próprio canteiro | Detalhe do canteiro |
| `POST /canteiros`, `PATCH /canteiros/{id}` | Administrador | Criar e editar canteiros |
| `GET /usuarios`, `GET /usuarios/{id}` | Administrador | Funcionários (`q`, `perfil`, `ativo`) |
| `POST /usuarios`, `PATCH /usuarios/{id}` | Administrador | Criar e editar funcionários |
| `GET /usuarios/{id}/credencial-inicial` | Administrador | Senha temporária, até a primeira troca |
| `POST /usuarios/{id}/redefinir-senha`, `/desativar`, `/ativar` | Administrador | Acesso do funcionário |
| `GET /colaboradores`, `GET /colaboradores/{id}` | Todos, no próprio canteiro | Colaboradores (`canteiroId`, `q`, `ativo`) |
| `POST /colaboradores`, `PATCH /colaboradores/{id}` | Todos, no próprio canteiro | Cadastrar e editar colaboradores |
| `GET /itens`, `GET /itens/{id}` | Todos | Catálogo (`q`, `tipo`, `ativo`) |
| `POST /itens`, `PATCH /itens/{id}` | Todos | Cadastrar e editar itens |
| `GET /canteiros/{id}/estoque` | Todos, no próprio canteiro | Saldo e status por item (`q`, `status`, `tipo`) |
| `GET /canteiros/{id}/estoque/{itemId}` | Todos, no próprio canteiro | Detalhe do item com as últimas movimentações |
| `GET /canteiros/{id}/estoque/resumo` | Todos, no próprio canteiro | Contadores do Início |
| `POST /canteiros/{id}/movimentacoes` | ALMOXARIFE do canteiro | Registrar entrada ou saída |
| `GET /movimentacoes` | GESTOR | Histórico (`canteiroId`, `de`, `ate`, `tipo`, `itemId`, `colaboradorId`) |
| `GET /movimentacoes/{id}` | Todos, no próprio canteiro | Detalhe da movimentação |
| `POST /movimentacoes/{id}/estorno` | ALMOXARIFE do canteiro | Corrigir um lançamento |
| `POST /requisicoes` | Todos, no próprio canteiro | Criar requisição |
| `GET /requisicoes`, `GET /requisicoes/{id}` | Todos, no próprio canteiro | Listar (`escopo=minhas` ou `para-decidir`, `status`) e detalhar |
| `POST /requisicoes/{id}/aprovar`, `/recusar` | O outro perfil | ALMOXARIFE decide as do GESTOR e vice-versa |
| `POST /requisicoes/{id}/atender` | ALMOXARIFE do canteiro | Marcar a aprovada como atendida |
| `POST /requisicoes/{id}/cancelar` | Quem criou | Cancelar enquanto pendente |
| `GET /auditoria` | GESTOR | Trilha de auditoria (`usuarioId`, `acao`, `de`, `ate`) |

A matriz completa, perfil por perfil, é testada em `MatrizDePermissoesTest`.
