# Guia do almoxarife

Você controla o estoque de um canteiro: registra o que entra e o que sai, corrige lançamentos errados e responde
às requisições do gestor. Abas: **Início**, **Estoque**, **Movimentar**, **Requisições** e **Mais**.

## Primeiro acesso

1. Entre com o e-mail e a senha temporária que o administrador entregou.
2. O app pede uma senha nova: 8 caracteres ou mais, com letra e número. Sem trocar, nada mais abre.
3. Cinco senhas erradas seguidas bloqueiam a conta por 15 minutos.

## Entregar material ou ferramenta (saída)

1. **Movimentar → Saída**.
2. Escolha o item (a busca aceita código ou descrição e mostra o saldo) e a quantidade. O app não deixa passar do
   saldo.
3. Em **Retirada**, escolha o colaborador que leva o item (dá para cadastrar um novo ali mesmo) e escreva a frente
   de obra. Em **Ajuste** (perda, furto, dano), conte o que aconteceu na observação.
4. **Confirmar saída**. O comprovante mostra **Enviando…** e depois **Salvo no servidor**, com o saldo que sobrou.
   Só vale quando aparecer "Salvo no servidor".

## Receber material (entrada)

**Movimentar → Entrada**, motivo **Recebimento**, item e quantidade, e **Confirmar entrada**. Use **Devolução**
quando um colaborador devolver uma ferramenta, e **Ajuste** para sobra encontrada na contagem.

## Sem internet ou servidor lento

Se o envio falhar sem resposta, toque em **Tentar de novo**: o app reenvia o mesmo registro e a API não duplica.
Se o servidor recusar (por exemplo, colaborador desativado), toque em **Corrigir** e ajuste o formulário. Na
primeira abertura do dia, o aviso "O servidor está acordando" pode durar até um minuto e meio.

## Corrigir um lançamento (estorno)

Nada é apagado. Abra o item em **Estoque**, toque na movimentação errada e em **Estornar**, e conte o motivo. O
estorno lança o movimento contrário, devolve o saldo e fica ligado ao original. Cada movimentação aceita um
estorno só.

## Requisições

- **Requisições → Minhas:** as que você pediu. Para pedir, toque em **Nova requisição**, adicione até 20 itens,
  informe até quando precisa e **Enviar requisição**. Dá para cancelar enquanto estiver pendente.
- **Requisições → Para decidir:** as que o gestor pediu. Aprove (com a previsão de chegada, se souber) ou recuse
  com o motivo. Quando o material chegar, abra a aprovada e toque em **Marcar como atendida**. Lance também a
  entrada do material em **Movimentar**, porque a requisição não mexe no estoque.

## Cadastros

Em **Mais → Itens** você cadastra e edita o catálogo (código, descrição, unidade, tipo e quantidade mínima). Em
**Mais → Colaboradores**, cadastra quem retira material no seu canteiro.
