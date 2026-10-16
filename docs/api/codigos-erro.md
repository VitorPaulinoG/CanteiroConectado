# Códigos de erro

Todo erro da API sai no formato Problem Details (RFC 9457), com um campo `codigo` estável em
MAIÚSCULAS. Uma vez publicado, um código não muda de significado nem é removido — se um fluxo
some, o código fica marcado como descontinuado aqui.

Fonte: `br.com.canteiroconectado.excecao.ErrorCode`.

| Código | Status HTTP | Quando acontece |
| --- | --- | --- |
| `VALIDACAO_INVALIDA` | 400 | Campo obrigatório ausente ou formato inválido no corpo da requisição. O corpo traz também a lista `campos` com o detalhe de cada violação. |
| `NAO_AUTENTICADO` | 401 | Requisição sem token válido em um endpoint que exige login. |
| `ACESSO_NEGADO` | 403 | Usuário autenticado sem permissão para a ação (ex.: perfil errado, canteiro de outra pessoa). |
| `RECURSO_NAO_ENCONTRADO` | 404 | Identificador informado não corresponde a um registro existente. |
| `CONFLITO` | 409 | Estado atual do recurso impede a operação (ex.: `ID_JA_UTILIZADO`, tratado por feature). |
| `ERRO_INESPERADO` | 500 | Falha não prevista. O cliente recebe mensagem genérica, sem stack trace; o detalhe completo fica no log, correlacionado pelo `requestId`. |

Códigos específicos de regra de negócio (ex.: `SALDO_INSUFICIENTE`, `ITEM_INATIVO`) são adicionados
por cada feature conforme ela é implementada, e entram nesta tabela no mesmo PR que os introduz.
