# Roteiro de falhas de rede

Roteiro da história E12-H3 (#54, tarefa #170). Ele é executado num aparelho Android real, com o APK `preview`. O
resultado é registrado aqui com a data, o aparelho e a versão. Para cada cenário que falhar, abra uma issue com os
passos e o resultado e repita o cenário depois da correção.

**Situação:** ainda não executado. Os comportamentos esperados abaixo estão cobertos por testes automatizados
(`envio.test`, `queryClient.test`, `client.test`, `FaixaEstadoEnvio.test`, `SaidaTela.test`,
`AplicarUmaVezIntegracaoTest`, `GravacoesSimultaneasTest`), mas falta conferir no aparelho.

## Preparação

- APK `preview` instalado. A versão aparece em **Mais → Sobre**.
- API acordada (ver o [roteiro de demonstração](../roteiro-demonstracao.md)) e dados de demonstração carregados.
- Entrar como Sebastião, com o Residencial Aurora e a furadeira com saldo 2.
- Para conferir o resultado no servidor, entrar como Camila num segundo aparelho, ou pelo `docs/api/exemplos.http`,
  e olhar o **Histórico**.
- Rede limitada: no Android, **Opções do desenvolvedor → Perfil de rede** ou um roteador com limite de banda. Outra
  forma é um hotspot de celular com sinal fraco.

## Cenários

Para cada saída, anote a hora e confira no Histórico da Camila quantas linhas foram gravadas.

| # | Cenário | Como provocar | Esperado | Resultado |
| --- | --- | --- | --- | --- |
| 1 | Queda antes do envio | Ligar o modo avião, preencher a saída e tocar em **Confirmar saída** | O app tenta mais uma vez; depois mostra "Não foi possível enviar" e **Tentar de novo**. Ao desligar o modo avião e tocar em Tentar de novo: **Salvo no servidor** e 1 linha no Histórico | |
| 2 | Resposta perdida depois do envio | Rede limitada (2G ou perda alta) e confirmar a saída. Se a resposta não chegar em 15 s, tocar em **Tentar de novo** | Uma única linha no Histórico, mesmo que o servidor tenha gravado na primeira tentativa. O comprovante mostra o saldo que sobrou | |
| 3 | Toque duplo | Tocar duas vezes rápido em **Confirmar saída** | O botão vira **Enviando…** e só um envio parte; 1 linha no Histórico | |
| 4 | Sessão perdida durante o envio | Com a API local (`docker compose up`), preencher a saída, reiniciar a API (o perfil `local` sorteia um `JWT_SEGREDO` novo a cada subida) e confirmar. Na nuvem: o administrador desativa o Sebastião e o reativa | O app volta ao login com "Entre de novo para continuar." e nada é gravado. Depois de entrar, a saída precisa ser registrada de novo | |
| 5 | API dormindo | Deixar a API do Render parada por mais de 15 minutos e abrir o app | Aviso "O servidor está acordando…" por até 90 s; depois o app segue normalmente | |
| 6 | Erro de regra | Pedir 3 furadeiras com saldo 2. Para passar pelo bloqueio do campo, outro aparelho baixa o saldo antes do envio | O app não repete; mostra a recusa no campo de quantidade, com o saldo atual | |
| 7 | Consulta sem rede | Modo avião e abrir **Estoque** | O app tenta mais 2 vezes e mostra o erro com **Tentar de novo** | |

## Registro

| Data | Aparelho e Android | Versão do app | Versão da API | Rede | Cenários ok | Issues abertas |
| --- | --- | --- | --- | --- | --- | --- |
| | | | | | | |
