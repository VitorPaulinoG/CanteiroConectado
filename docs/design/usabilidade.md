# Teste de uso

Roteiro da história E13-H4 (#59, tarefas #177 e #178). Ele é feito com três pessoas parecidas com as personas, de
preferência com experiência em obra ou comércio, usando o APK num aparelho real.

**Situação:** roteiro pronto, sessões ainda não realizadas.

## Critério de aceite

- Pelo menos duas das três pessoas registram uma saída sem ajuda.
- Cada problema encontrado vira uma issue com prioridade. Os críticos são tratados antes da entrega (#178).

## Preparação

- APK `preview` instalado e API acordada, com os dados de demonstração recém-carregados
  ([reinício do banco](../reinicio-do-banco.md)).
- Duas contas: Sebastião (`sebastiao@canteiro.demo`) para as tarefas 1 e 2, e Camila (`camila@canteiro.demo`) para
  as tarefas 3 a 5. Entregue a senha num papel.
- Um cronômetro e esta folha impressa, uma por pessoa.

## Como conduzir

1. Explique que o teste é do aplicativo, não da pessoa, e peça para ela pensar em voz alta.
2. Leia cada tarefa como está escrita abaixo e não aponte botões. Se a pessoa travar por mais de 2 minutos, ou
   pedir ajuda duas vezes, anote "com ajuda" e mostre o caminho.
3. Anote o tempo de cada tarefa, onde ela hesitou e as frases que disse.
4. No fim, pergunte: o que foi mais difícil? Usaria isto no canteiro? O que mudaria?

## Tarefas

| # | Perfil | O que dizer | Concluída quando |
| --- | --- | --- | --- |
| 1 | Sebastião | "Entre no aplicativo com este e-mail e esta senha." | Vê o Início |
| 2 | Sebastião | "O João vai levar uma furadeira para as instalações elétricas. Registre isso." | Comprovante com "Salvo no servidor" |
| 3 | Camila | "Antes de escalar a equipe, veja o que está faltando ou acabando no canteiro." | Abre a lista de alertas e acha o cimento |
| 4 | Camila | "Peça 40 sacos de cimento para daqui a cinco dias." | Requisição enviada |
| 5 | Camila | "O Sebastião já respondeu. Veja o que ele disse." | Abre a requisição e lê a previsão de chegada |

Entre as tarefas 4 e 5, aprove a requisição como Sebastião, num segundo aparelho ou pelo `docs/api/exemplos.http`.

## Registro por pessoa

| Pessoa (perfil, experiência) | Aparelho | Tarefa | Tempo | Sem ajuda? | Onde hesitou | O que disse |
| --- | --- | --- | --- | --- | --- | --- |
| | | 1 | | | | |
| | | 2 | | | | |
| | | 3 | | | | |
| | | 4 | | | | |
| | | 5 | | | | |

## Problemas encontrados

| Problema | Tela | Quantas pessoas | Gravidade (crítico, sério, leve) | Issue |
| --- | --- | --- | --- | --- |
| | | | | |

Crítico: impede concluir a tarefa ou faz gravar errado. Sério: atrasa ou exige ajuda. Leve: incomoda, mas a pessoa
conclui.
