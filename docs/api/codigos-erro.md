# Códigos de erro

Todo erro da API sai no formato Problem Details (RFC 9457), com um campo `codigo` estável em
MAIÚSCULAS. Uma vez publicado, um código não muda de significado nem é removido — se um fluxo
some, o código fica marcado como descontinuado aqui.

Fonte: `br.com.canteiroconectado.exception.ErrorCode`.

| Código | Status HTTP | Quando acontece |
| --- | --- | --- |
| `VALIDACAO_INVALIDA` | 400 | Campo obrigatório ausente ou formato inválido no corpo da requisição; corpo ilegível ou parâmetro ausente ou de tipo errado. O corpo traz também a lista `campos` com o detalhe de cada violação. |
| `NAO_AUTENTICADO` | 401 | Requisição sem token válido em um endpoint que exige login, token de usuário desativado ou token emitido antes de o administrador mudar o perfil ou o canteiro do usuário. |
| `ACESSO_NEGADO` | 403 | Usuário autenticado sem permissão para a ação (ex.: perfil errado, canteiro de outra pessoa). |
| `RECURSO_NAO_ENCONTRADO` | 404 | Identificador informado não corresponde a um registro existente, ou o endereço não existe. |
| `METODO_NAO_PERMITIDO` | 405 | O método HTTP usado não existe para o endereço. |
| `CONFLITO` | 409 | Estado atual do recurso impede a operação, inclusive violação de integridade no banco não tratada por uma regra específica. |
| `CREDENCIAIS_INVALIDAS` | 401 | Login com e-mail inexistente ou senha errada. A resposta é a mesma nos dois casos: "E-mail ou senha incorretos". |
| `USUARIO_INATIVO` | 403 | Login correto de um usuário desativado pelo administrador. |
| `CONTA_BLOQUEADA` | 423 | A quinta senha errada seguida bloqueia a conta por 15 minutos; enquanto durar, todo login recebe este código. O corpo traz `bloqueadoAte` (instante UTC) para o app mostrar o tempo restante. |
| `SENHA_ATUAL_INCORRETA` | 422 | Na troca de senha, a senha atual informada não confere. |
| `SENHA_FRACA` | 422 | A nova senha tem menos de 8 caracteres, não tem letra ou não tem número. O `detail` diz qual regra falhou. |
| `SENHA_IGUAL_A_ATUAL` | 422 | A nova senha é igual à atual. |
| `TROCA_DE_SENHA_OBRIGATORIA` | 403 | Funcionário com senha temporária chamou um endpoint diferente de `GET /autenticacao/eu` e `POST /autenticacao/trocar-senha`. |
| `CANTEIRO_DUPLICADO` | 409 | Já existe um canteiro com o mesmo nome (sem diferenciar maiúsculas). O corpo traz `campo: "nome"`. |
| `CANTEIRO_INATIVO` | 422 | Tentativa de gravar dados (movimentação, colaborador, requisição, usuário) num canteiro desativado. |
| `EMAIL_JA_CADASTRADO` | 409 | Já existe um funcionário com o mesmo e-mail (sem diferenciar maiúsculas). O corpo traz `campo: "email"`. |
| `CANTEIRO_OBRIGATORIO` | 422 | Funcionário ALMOXARIFE sem canteiro. O corpo traz `campo: "canteiroId"`. |
| `AUTODESATIVACAO_PROIBIDA` | 422 | O administrador tentou desativar a si mesmo. |
| `ADMINISTRADOR_PROTEGIDO` | 422 | Tentativa de mudar o perfil do administrador. |
| `CREDENCIAL_JA_TROCADA` | 409 | O administrador consultou a credencial inicial de um funcionário que já trocou a senha temporária. Para gerar outra, use `POST /usuarios/{id}/redefinir-senha`. |
| `COLABORADOR_DUPLICADO` | 409 | Já existe um colaborador com o mesmo nome no canteiro (sem diferenciar maiúsculas). O corpo traz `campo: "nome"`. |
| `COLABORADOR_INVALIDO` | 422 | O colaborador informado numa movimentação não existe, está desativado ou é de outro canteiro. O corpo traz `campo: "colaboradorId"`. |
| `CODIGO_DUPLICADO` | 409 | Já existe um item com o mesmo código (sem diferenciar maiúsculas nem espaços nas pontas). O corpo traz `campo: "codigo"`. |
| `ITEM_JA_MOVIMENTADO` | 422 | Tentativa de mudar o código ou a unidade de um item que já teve movimentação. O corpo traz `campo` com o campo recusado. |
| `ITEM_INATIVO` | 422 | O item informado numa movimentação ou requisição está desativado. O corpo traz `campo: "itemId"`. |
| `SALDO_INSUFICIENTE` | 422 | A saída (ou o estorno de uma entrada) deixaria o saldo negativo. O corpo traz `saldoAtual` e `campo: "quantidade"`. |
| `QUANTIDADE_INVALIDA` | 422 | Quantidade zero, negativa ou com mais de 3 casas decimais. O corpo traz `campo: "quantidade"`. |
| `ID_INVALIDO` | 400 | O `id` gerado no app (movimentação ou requisição) está ausente ou não é um UUID no formato `8-4-4-4-12`. O corpo traz `campo: "id"`. |
| `ID_JA_UTILIZADO` | 409 | O `id` já pertence a um registro com conteúdo diferente. Reenviar o mesmo pedido com o mesmo `id` não dá erro: devolve o registro gravado. |
| `ERRO_INESPERADO` | 500 | Falha não prevista. O cliente recebe mensagem genérica, sem stack trace; o detalhe completo fica no log, correlacionado pelo `requestId`. |

Códigos específicos de regra de negócio (ex.: `SALDO_INSUFICIENTE`, `ITEM_INATIVO`) são adicionados
por cada feature conforme ela é implementada, e entram nesta tabela no mesmo PR que os introduz.
