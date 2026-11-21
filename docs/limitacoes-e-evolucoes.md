# Limitações e evoluções

O que a versão 1.0 não faz, de propósito ou por falta de tempo, e o que viria depois. Os cortes do escopo estão
justificados em [Visão e arquitetura](Vis%C3%A3o%20e%20arquitetura.md#escopo-do-mvp), e os riscos de segurança
aceitos em [riscos aceitos](decisoes/riscos-aceitos.md).

## Limitações da versão 1.0

| Limitação | Efeito para quem usa | Contorno hoje |
| --- | --- | --- |
| Só Android | O seletor de data e o APK são só para Android | Expo Go no Android |
| Precisa de internet | Sem conexão, nada é gravado; não há fila offline | Tentar de novo quando a rede voltar; o reenvio não duplica |
| Servidor gratuito dorme | A primeira chamada depois de 15 minutos parados demora até um minuto | Acordar a API antes de usar (roteiro de demonstração) |
| Sessão de 12 horas, sem "sair de todos os aparelhos" | Depois de 12 horas é preciso entrar de novo | Desativar o usuário corta o acesso na hora |
| Uma empresa por instalação; almoxarife em um canteiro só | Quem cuida de duas obras precisa de duas contas | O gestor enxerga todos os canteiros |
| Colaborador sem login | O colaborador não confirma a retirada no próprio celular | O almoxarife registra quem retirou e a frente de obra |
| Frente de obra em texto livre | A mesma frente pode aparecer escrita de jeitos diferentes | Combinar os nomes no canteiro |
| Requisição atendida não lança entrada | O almoxarife lança a entrada à parte, em Movimentar | Comprovante e histórico mostram os dois registros |
| Sem notificações | Ninguém é avisado de alerta ou de requisição nova | Contadores no Início, atualizados ao abrir e ao puxar |
| Sem relatórios nem exportação | Não há planilha nem PDF do estoque e do histórico | Consultar pelo app ou pela API |
| Sem importação de itens | O catálogo é cadastrado item a item | Dados de demonstração para apresentações |

## Evoluções

- **Suprimentos:** a requisição aprovada vira pedido de compra, com fornecedor, cotação, nota fiscal e entrada
  automática no recebimento. Hoje a previsão de chegada é só uma data informada por quem aprova.
- **Patrimonial:** ferramentas com número de patrimônio, rastreadas uma a uma (quem está com qual furadeira), com
  prazo de devolução e cobrança das atrasadas.
- **Notificações:** push para itens que ficaram abaixo do mínimo, requisição nova para decidir e requisição
  decidida.
- **Modo offline:** fila de envios no aparelho, aproveitando o `id` gerado no app, que já garante que reenviar não
  duplica. Exige cache persistido e tratamento de conflito de saldo.
- **Painel web:** para o escritório acompanhar vários canteiros, com relatórios, exportação em planilha e
  importação do catálogo.
- **Técnicas:** paginação por cursor no histórico e índice `pg_trgm` na busca, quando o volume crescer
  ([desempenho](qa/desempenho.md)); renovação de token; limite de tentativas por IP; versão iOS.
