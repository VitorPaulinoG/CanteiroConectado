# Revisão de desempenho

Revisão da história E13-H3 (#176), feita em novembro de 2026: consultas repetidas (N+1) nas listas principais e
tempo de resposta com 2 mil itens e 10 mil movimentações.

## Consultas repetidas (N+1)

`ConsultasPorRequisicaoTest` conta os SQL que cada tela faz, tanto os do Hibernate quanto os do `JdbcTemplate`
(`support/ContadorDeConsultas`). Cada tela é medida com 1 linha na página e depois com 15. Se o número de
consultas cresce com a página, há N+1 e o teste falha listando os SQL. Ele roda no `./mvnw verify` de sempre.

| Tela | Endpoint | Consultas por chamada |
| --- | --- | --- |
| S06 Estoque | `GET /canteiros/{id}/estoque` | 4 |
| S07 Detalhe do item | `GET /canteiros/{id}/estoque/{itemId}` | 5 |
| S04 Início | `GET /canteiros/{id}/estoque/resumo` | 6 |
| S16 Histórico | `GET /movimentacoes` | 3 |
| S12 Requisições | `GET /requisicoes` | 3 |
| S18 Auditoria | `GET /auditoria` | 3 |
| S20 Itens | `GET /itens` | 2 |
| S22 Colaboradores | `GET /colaboradores` | 2 |
| S24 Funcionários | `GET /usuarios` | 2 |
| S23 Canteiros | `GET /canteiros` | 2 |

Os números incluem a consulta que o `SituacaoDoUsuarioFilter` faz em toda requisição autenticada. Nenhuma tela tem
N+1. As listas trazem os nomes (item, colaborador, autor, canteiro) por `JOIN` na mesma consulta, e a lista de
requisições busca os itens de todas as requisições da página numa segunda consulta só (`WHERE requisicao_id IN`).
O teste também falha se alguma tela passar de 6 consultas.

## Volume de dados

`VolumeDeDadosTest` carrega num canteiro:

- 2.000 itens (30% ferramentas, um quarto com mínimo 100);
- 10.000 movimentações: um recebimento e quatro retiradas por item, espalhados por 90 dias e por 20 colaboradores;
- 500 requisições com 2 itens cada, nos cinco status.

Depois mede cada tela: 3 chamadas de aquecimento e 20 medidas, com o tempo dentro da API (filtros, consultas e
JSON), sem a rede. O teste grava os dados de verdade, por isso só roda quando pedido e num banco próprio:

```bash
createdb -O canteiro canteiroconectado_volume
cd apps/api
BANCO_URL=jdbc:postgresql://localhost:5432/canteiroconectado_volume \
  ./mvnw test -Dtest=VolumeDeDadosTest -Ddesempenho=true -Djacoco.skip=true
cat target/desempenho/resultado.md
```

Resultado num notebook com Postgres 18 local:

| Tela | Mediana (ms) | Pior (ms) |
| --- | --- | --- |
| S06 estoque, 1ª página | 11 | 17 |
| S06 estoque, busca "cimento" | 9 | 10 |
| S06 estoque, só alertas | 8 | 11 |
| S06 estoque, página 50 | 16 | 24 |
| S07 detalhe do item | 6 | 7 |
| S04 resumo do Início | 8 | 11 |
| S16 histórico, 1ª página | 6 | 9 |
| S16 histórico, últimos 7 dias | 7 | 10 |
| S16 histórico de um item | 3 | 5 |
| S16 histórico de um colaborador | 5 | 6 |
| S16 histórico, página 400 | 19 | 26 |
| S12 requisições, todas | 3 | 4 |
| S12 requisições, para decidir | 4 | 7 |

O teste falha se alguma mediana passar de 300 ms. No Render gratuito, que tem bem menos CPU, e com a rede até a
Neon, os tempos serão maiores. A ordem de grandeza mostra que o banco não é o gargalo; o que pesa na nuvem é o
servidor acordando (ver o roteiro de demonstração).

## Planos das consultas (`EXPLAIN ANALYZE`)

| Consulta | Plano | Tempo no banco |
| --- | --- | --- |
| Histórico, 1ª página | `movimentacao_canteiro_data`, lê só as 21 linhas da página | 0,2 ms |
| Histórico de um colaborador | `movimentacao_canteiro_data` filtrando o colaborador | 0,3 ms |
| Histórico, página 400 (`OFFSET 8000`) | Ordena as 10 mil linhas | 14 ms |
| Contagem do histórico | Index only scan em `movimentacao_canteiro_data` | 1 ms |
| Últimas movimentações do item | `movimentacao_canteiro_item_data` | 0,1 ms |
| Estoque, 1ª página | Leitura de `item` e `saldo_estoque` inteiras, top-N | 3,5 ms |
| Estoque, busca | Leitura de `item` com o `LIKE`, top-N | 2,2 ms |
| Requisições | Leitura de `requisicao` (500 linhas), top-N | 0,3 ms |
| Item já movimentado | `movimentacao_canteiro_item_data` (skip scan do Postgres 18) | 0,1 ms |

## Decisão sobre índices

Nenhum índice novo, então não há migration V8. Os índices das V6 e V7 atendem o histórico, o detalhe do item e as
requisições. O estoque lê o catálogo inteiro por desenho, porque o status (zerado, abaixo do mínimo) e a ordem
dependem do saldo de cada item; com 2 mil itens isso custa 3,5 ms.

Quando revisar:

- **Histórico com páginas muito fundas:** o `OFFSET` ordena tudo o que vem antes. A rolagem do app raramente passa
  de poucas páginas; se o histórico de um canteiro passar de 100 mil movimentações, troque por paginação pelo
  último `criado_em` e `id` vistos.
- **Busca no catálogo:** o `LIKE '%termo%'` não usa o índice `item_descricao`. Com dezenas de milhares de itens,
  um índice `pg_trgm` resolve.
- **Postgres 16 (Docker Compose e, talvez, a Neon):** sem o skip scan do 18, a checagem "item já movimentado"
  (`existsByItemId`) lê a tabela de movimentações inteira, cerca de 1 ms a cada 10 mil linhas. Um índice em
  `movimentacao_estoque (item_id)` resolve, se essa checagem ficar lenta.
