# Testes da API

`./mvnw verify` em `apps/api` roda os testes no Postgres e gera o relatório de cobertura do JaCoCo em
`apps/api/target/site/jacoco/index.html`. No CI, o workflow `API` sobe um Postgres 16 como serviço, mostra a
cobertura total no resumo do job e publica os relatórios dos testes e da cobertura no artefato `relatorios-api`
(14 dias).

## Revisão de novembro de 2026

Total: 531 testes. Cobertura por pacote, com os pacotes de regra em destaque:

| Pacote | Linhas antes | Linhas depois | Ramos antes | Ramos depois |
| --- | --- | --- | --- | --- |
| `estoque` (saldo e estorno) | 98% | 98,5% | 89% | 94,6% |
| `requisicao` | 96% | 96,4% | 86% | 94,2% |
| `security` (permissões) | 97% | 98,4% | 88% | 100% |
| Total da API | 96,1% | 96,7% | 84,8% | 88,6% |

O que a revisão acrescentou:

- **Matriz de permissões:** `CoberturaDaMatrizTest` compara os endpoints registrados no Spring com as linhas da
  matriz e falha se um endpoint novo não tiver linha. Na primeira execução ele achou `GET /colaboradores/{id}`
  fora da matriz, e a linha foi incluída.
- **Token desatualizado:** o token antigo recebe 401 quando o perfil muda, quando a administração é retirada e
  quando o usuário não existe. A troca de canteiro e a desativação já tinham teste.
- **Saldo:** numa sequência com todos os motivos (recebimento, retirada, devolução, ajuste, estorno), cada
  `saldo_apos` é igual ao acumulado do livro e o saldo final é igual à soma das movimentações.
- **Estorno:** 404 para movimentação inexistente.
- **Reenvio com o mesmo id:** cada campo que muda torna o pedido diferente, na movimentação e na requisição; ordem
  dos itens e escala da quantidade não contam.
- **Requisição:** o ALMOXARIFE não decide a requisição de outro ALMOXARIFE; 404 nas quatro ações para id
  inexistente; a mensagem do cancelamento explica o status quando a requisição já foi recusada, atendida ou
  cancelada.

## O que continua sem teste automatizado

- Ramos de corrida que dependem do banco recusar a chave no último instante (por exemplo, o
  `DataIntegrityViolationException` do estorno quando dois pedidos passam pela checagem ao mesmo tempo). Os testes
  de concorrência cobrem o resultado, com um só estorno e um só registro, mas nem sempre passam por esse ramo.
- Mensagens de falha que só aparecem com o banco inconsistente ("saldo não encontrado depois de criado").
