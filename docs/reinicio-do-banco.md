# Reinício do banco antes de uma apresentação

Este procedimento apaga tudo o que foi feito no ambiente de apresentação (Render e Neon) e o devolve aos dados de
demonstração: canteiro Residencial Aurora, Sebastião, Camila, 22 itens, saldos e uma requisição pendente. Leva
cerca de 10 minutos, quase todos esperando o Render subir.

**Apaga movimentações, requisições, usuários e a trilha de auditoria.** Use só no banco de apresentação. Confira o
nome do projeto e do branch na Neon antes de rodar o SQL.

## Antes de começar

- Acesso à conta da Neon e ao serviço da API no Render.
- `DEMO_SENHA` combinada com o grupo (8 caracteres ou mais, com letra e número). Ela não fica no repositório.
- Variáveis do serviço no Render (aba **Environment**), conferidas uma vez:

| Variável | Valor |
| --- | --- |
| `SPRING_PROFILES_ACTIVE` | `prod,demo` |
| `DEMO_SENHA` | a senha combinada |
| `ADMIN_NOME`, `ADMIN_EMAIL`, `ADMIN_SENHA` | o administrador, recriado a cada reinício |
| `BANCO_URL`, `BANCO_USUARIO`, `BANCO_SENHA` | o banco da Neon, sem mudança |

Sem `DEMO_SENHA` com o perfil `demo`, a API não sobe e o log do Render mostra
"O perfil demo exige a variável de ambiente DEMO_SENHA".

## 1. Guardar o estado atual (opcional)

Para poder voltar atrás, crie uma cópia antes de apagar: Neon Console → projeto → **Branches** → **Create branch**,
com o branch do banco como pai e um nome como `antes-reinicio-AAAA-MM-DD`. A cópia é instantânea e não muda a
conexão da API. Apague-a depois da apresentação, porque o plano gratuito limita a quantidade de branches.

## 2. Zerar o banco

Neon Console → projeto → **SQL Editor**. Escolha o branch e o banco da API e o mesmo papel de `BANCO_USUARIO`, e
rode:

```sql
DROP SCHEMA public CASCADE;
CREATE SCHEMA public;
```

- O `DROP` apaga as tabelas, os gatilhos de imutabilidade e o histórico do Flyway (`flyway_schema_history`). Por
  isso a API recria tudo do zero na próxima subida.
- Conferência: `SELECT count(*) FROM pg_tables WHERE schemaname = 'public';` devolve `0`.
- Se der `must be owner of schema public`, o papel escolhido no editor não é o dono do banco. Troque para o papel
  dono (o mesmo da `BANCO_URL`).

Não use `flyway clean`. Ele fica desligado por padrão no Spring Boot, e é bom que continue assim no perfil `prod`.

## 3. Reiniciar a API

Render → serviço da API → **Manual Deploy** → **Restart service**. Também vale **Deploy latest commit**.

Na subida, a API:

1. aplica as migrations de `V1` em diante;
2. cria o administrador com `ADMIN_*`;
3. carrega os dados de demonstração (o log mostra
   `Dados de demonstração carregados: 22 itens no canteiro Residencial Aurora`).

Se o banco já tiver o canteiro Residencial Aurora, a carga não roda de novo. Por isso o passo 2 é obrigatório.

## 4. Conferir

1. `https://<url-da-api-no-render>/actuator/health` devolve `{"status":"UP"}`.
2. No app, entre como Camila (`camila@canteiro.demo`, senha `DEMO_SENHA`):
   - o Início do Residencial Aurora mostra 2 itens abaixo do mínimo e 1 requisição para decidir;
   - o estoque filtrado mostra o "Cimento CP-II 50kg" com 30 saco, mínimo 50.
3. Entre como Sebastião (`sebastiao@canteiro.demo`): a busca por "furadeira" mostra 2 disponíveis.
4. Siga o [roteiro de demonstração](roteiro-demonstracao.md) para manter a API acordada até a apresentação.

O app guarda a sessão no aparelho. Se alguém entrou antes do reinício, o primeiro acesso depois dele volta para o
login, porque o usuário antigo não existe mais.

## Sem acesso à Neon

O mesmo SQL roda com `psql` usando a string de conexão da Neon (Console → **Connect**):

```bash
psql "postgresql://<usuario>:<senha>@<host>/<banco>?sslmode=require" \
  -c "DROP SCHEMA public CASCADE;" -c "CREATE SCHEMA public;"
```

No ambiente local, `docker compose down -v` e `docker compose up --build`, com `SPRING_PROFILES_ACTIVE=local,demo` e
`DEMO_SENHA` no `.env`, chegam ao mesmo estado.

## Como este procedimento foi conferido

Num Postgres 18 local, com a API no perfil `local,demo`: a primeira subida carregou os dados, uma requisição de
ensaio foi criada (2 requisições no banco), o SQL do passo 2 zerou o schema e a segunda subida aplicou as 7
migrations, recriou o administrador e a carga, e voltou a 1 requisição, 31 movimentações e 3 usuários. Na Neon, o
passo 2 depende de o papel escolhido ser o dono do banco, o que não foi possível testar daqui.
