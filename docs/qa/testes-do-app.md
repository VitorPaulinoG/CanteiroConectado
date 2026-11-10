# Testes do app

Em `apps/mobile`: `npm test` roda o Jest. Para ver a cobertura, rode `npx jest --coverage`; o relatório sai em
`apps/mobile/coverage/lcov-report/index.html`, fora do git.

## Revisão de novembro de 2026

Total: 297 testes em 55 suítes. Linhas cobertas no app inteiro: de 87,2% para 88,0%.

| Arquivo | Linhas antes | Linhas depois |
| --- | --- | --- |
| `validations.ts` de autenticação, cadastros, movimentações, requisições e colaborador | 67% a 100% | 100% em todos |
| S02 `LoginTela` | 100% (ramos 91,9%) | 100% (ramos 94,6%) |
| S09 `SaidaTela` | 91,1% | 93,3% |
| S11 `Comprovante` | 100%, só pelas telas de saída e entrada | 100%, com teste próprio |
| S13 `NovaRequisicaoTela` | 93,5% | 96,8% |

O que a revisão acrescentou:

- **Validações:** limites de tamanho da frente, da observação, do motivo da recusa e do estorno, do nome do
  funcionário e da descrição do item; e-mail vazio; entrada sem item. O nome do colaborador não tinha teste.
- **S02:** "1 minuto" no singular quando o bloqueio está acabando; um segundo toque em Entrar não manda outro
  login enquanto o primeiro está em andamento.
- **S09:** "Nova saída" no comprovante abre um formulário novo; corrigir o valor tira do campo a recusa da API.
- **S11:** teste do componente nos quatro estados. Enviando: mostra o pedido, o aviso de que ainda não vale e
  nenhum botão de concluir. Salvo: mostra os dados do servidor no lugar dos digitados. Sem resposta: oferece
  Tentar de novo. Recusado: oferece Corrigir.
- **S13:** frente e observação vão sem os espaços das pontas; a recusa da API que não é de um campo aparece acima
  do botão; fechar o seletor não adiciona item.

Continuam sem teste os ramos que a interface não alcança, como o acesso negado da S13: os dois perfis podem
pedir material.
