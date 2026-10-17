# Como contribuir

Este guia vale para todo o grupo. A visão geral do projeto está em [Visão e arquitetura](docs/Vis%C3%A3o%20e%20arquitetura.md).

## Fluxo de trabalho

1. Pegue uma issue (`[E08-H2-T2] Resumo`) e atribua a você.
2. Crie a branch a partir de `develop`.
3. Faça commits pequenos, abra o PR **para `develop`** e peça revisão. Todo PR de trabalho abre para `develop`, nunca direto para `main`.
4. Depois da aprovação e do CI verde, faça o merge (squash). A branch é apagada automaticamente.
5. `main` só recebe **PR de release**, aberto de `develop` para `main` quando `develop` estiver estável e conferida na nuvem. Esse merge dispara o deploy da API.

`main` e `develop` são protegidas: ninguém faz push direto, e toda mudança entra por PR com uma aprovação.

## Nomes de branch

Formato: `tipo/CODIGO-DA-ISSUE-resumo-em-minusculas`, com hifens, sem acento nem cedilha.

| Tipo | Quando usar | Exemplo |
|---|---|---|
| `feat` | Funcionalidade nova | `feat/E08-H2-T2-registrar-saida` |
| `fix` | Correção de erro | `fix/E08-H2-T3-saldo-negativo` |
| `docs` | Só documentação | `docs/E01-H2-T1-contributing` |
| `chore` | Configuração, dependências, CI | `chore/E01-H1-T2-editorconfig` |

Branches de trabalho nascem de `develop` e voltam para `develop`. A única branch que chega à `main` é a própria `develop`, pelo PR de release.

## Mensagens de commit

Seguem o [Conventional Commits](https://www.conventionalcommits.org/pt-br/v1.0.0/), em pt-BR, com o código da issue no fim:

```
tipo(escopo): descrição no presente [CODIGO-DA-ISSUE]
```

- **Tipos:** `feat`, `fix`, `docs`, `refactor`, `test`, `chore`.
- **Escopos:** `api`, `mobile`, `design`, `devops` (os mesmos das etiquetas `area:*`).
- **Descrição:** verbo no presente, minúscula, sem ponto final, até cerca de 72 caracteres.

Exemplos:

```
feat(api): registra saída de estoque [E08-H2-T2]
fix(mobile): corrige saldo exibido após estorno [E08-H2-T3]
docs: cria CONTRIBUTING.md [E01-H2-T1]
```

Como o merge é sempre squash, **o título do PR vira a mensagem do commit** em `develop`. No PR de release (`develop` → `main`), use `chore: release vX.Y.Z`. Escreva o título do PR neste mesmo formato.

## Pull requests

- **Tamanho máximo: 600 linhas alteradas** (soma de adições e remoções), sem contar arquivos gerados e lockfiles. Passou disso, divida em PRs menores, de preferência um por tarefa.
- **Um assunto por PR.** Não misture funcionalidade com refatoração ou formatação.
- **Antes de abrir:** CI verde localmente, regra de negócio testada e variável nova registrada no `.env.example`.
- **Aprovação:** uma aprovação de outra pessoa do grupo. Quem é iniciante pede revisão a quem tem experiência, e o contrário também.
- **Prazo de revisão: até 1 dia útil** depois que o PR é aberto (ou marcado como pronto para revisão). Se o prazo estourar, avise a pessoa no grupo em vez de esperar.
- **Quem abre o PR** responde aos comentários e faz o merge depois da aprovação.
- **Quem revisa** comenta o que bloqueia e o que é sugestão, e aprova quando o que bloqueia estiver resolvido.

## Definição de pronto

Uma tarefa só termina quando:

- o código foi revisado e aprovado;
- a regra de negócio está testada;
- a variável nova está no `.env.example`;
- a funcionalidade foi conferida na nuvem.
