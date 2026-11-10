# Conferência da Jornada 2 até o alerta

A Jornada 2 é a da Camila (GESTOR, mestre de obras): antes de escalar a equipe, ela confere se o material
está no canteiro, vê o alerta de estoque mínimo, pede o que falta e acompanha a resposta.

## Conferência automatizada

O teste `apps/api/src/test/java/br/com/canteiroconectado/demo/Jornada2Test.java` carrega os dados de
demonstração (#173) e percorre a jornada pela API, na mesma ordem do app:

| Passo | Tela | Chamada | Resultado esperado |
| --- | --- | --- | --- |
| 1 | S02 | `POST /autenticacao/login` com `camila@canteiro.demo` | Token de GESTOR |
| 2 | S05 | `GET /canteiros` | Residencial Aurora na lista |
| 3 | S04 | `GET /canteiros/{aurora}/estoque/resumo` | 2 itens abaixo do mínimo e os três mais críticos |
| 4 | S06 | `GET /canteiros/{aurora}/estoque?status=ZERADO&status=ABAIXO_DO_MINIMO` | "Cimento CP-II 50kg" na lista filtrada |
| 5 | S07 | `GET /canteiros/{aurora}/estoque/{cimento}` | Saldo 30 saco, mínimo 50, status ABAIXO_DO_MINIMO |
| 6 | S13 | `POST /requisicoes` com 40 sacos | Requisição PENDENTE em Minhas |
| 7 | S04 (Sebastião) | `GET /canteiros/{aurora}/estoque/resumo` | 1 requisição para decidir |
| 8 | S15 (Sebastião) | `POST /requisicoes/{id}/aprovar` com previsão | APROVADA |
| 9 | S14 | `GET /requisicoes/{id}` | Previsão de chegada e "Decidida por Sebastião Ferreira" |

Resultado: o teste passa no `./mvnw verify` (ver o relatório do CI).

Os mesmos números foram conferidos subindo a API com `SPRING_PROFILES_ACTIVE=local,demo` em banco vazio. O resumo
da Camila mostrou 3 zerados, 2 abaixo do mínimo (Bloco de concreto e Cimento CP-II) e 1 requisição para decidir.

## Conferência no aparelho

Falta a conferência manual no APK, com a API na nuvem. Está em [pendências manuais](../pendencias-manuais.md):
entrar como Camila, tocar no cartão de alerta do Início, abrir o cimento e conferir os números acima.
