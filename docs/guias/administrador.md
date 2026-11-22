# Guia do administrador

O administrador é um gestor com acesso extra: é o único que cria canteiros e funcionários. A API cria o
administrador na primeira subida com `ADMIN_NOME`, `ADMIN_EMAIL` e `ADMIN_SENHA`. Ele pode trocar a senha em
**Mais → Trocar senha**, mas não é obrigado.

Tudo o que está no [guia do gestor](gestor.md) vale também para ele. Além disso, em **Mais → Administração**:

## Canteiros

**Canteiros → Novo canteiro**: nome (único) e endereço. Um canteiro desativado some das listas de escolha e não
aceita movimentações nem requisições.

## Funcionários

1. **Funcionários → Novo funcionário**: nome, e-mail, perfil e, para o almoxarife, o canteiro dele.
2. O app mostra a **credencial inicial**: e-mail e senha temporária. Copie e entregue pessoalmente; ela continua
   disponível na tela do funcionário até ele trocar a senha, e cada consulta fica registrada na auditoria.
3. No primeiro acesso o funcionário é obrigado a trocar a senha. Depois disso a senha temporária deixa de existir.

Na tela do funcionário:

- **Redefinir senha** gera uma nova senha temporária, para quem esqueceu a dele.
- **Desativar** corta o acesso na hora, mesmo de quem já está logado. **Ativar** devolve o acesso.
- Mudar o perfil ou o canteiro também encerra a sessão aberta do funcionário, que entra de novo com o acesso novo.

A API não deixa o administrador desativar a si mesmo nem mudar o próprio perfil.

## Antes de uma apresentação

Siga o [roteiro de demonstração](../roteiro-demonstracao.md). Para voltar aos dados de demonstração, use o
[reinício do banco](../reinicio-do-banco.md).
