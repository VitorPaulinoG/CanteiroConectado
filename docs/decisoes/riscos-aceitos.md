# Riscos aceitos

Riscos encontrados na verificação de segurança (#175) e mantidos no MVP, cada um com a justificativa e o que
faria mudar a decisão. Nenhum achado foi crítico, então nenhum virou issue `must`.

## Avisos altos do `npm audit` em ferramentas de build

**Risco:** o `npm audit` de `apps/mobile` aponta 4 avisos altos: `follow-redirects`, `node-forge`, `zod` e
`@expo/cli`.

**Por que aceitar:** nenhum deles chega ao APK. O `follow-redirects` só é usado pelo axios no Node; no React
Native o axios usa o adaptador XHR. Os outros três são da CLI da Expo e do ESLint, que rodam só na máquina de quem
desenvolve e no CI. A correção existe sem mudar versão maior (`npm audit fix`), mas mexe em muitas linhas do
`package-lock.json`. Fica para um PR próprio, conferido com `lint`, `typecheck`, `test` e `expo export`.

**Muda se:** algum aviso alto ou crítico aparecer num pacote que vai para o APK, como `axios`, `react-native`,
`expo-router` ou `react-native-paper`.

## Bloqueio por conta, sem limite por IP

**Risco:** cinco senhas erradas bloqueiam a conta por 15 minutos, mas não há limite de tentativas por endereço
IP. Alguém pode tentar uma senha comum em várias contas, e também pode bloquear a conta de outra pessoa de
propósito.

**Por que aceitar:** o MVP tem poucos usuários, todos cadastrados pelo administrador, e a política de senha exige 8
caracteres com letra e número. O plano gratuito do Render não oferece limite de requisições na borda, e fazer isso
na API exigiria guardar estado por IP, que se perde a cada vez que o serviço dorme.

**Muda se:** o app sair do ambiente de apresentação ou o número de usuários crescer.

## Token válido por 12 horas, sem renovação

**Risco:** o JWT vale 12 horas (`JWT_VALIDADE_HORAS`) e não existe logout no servidor. Um token copiado continua
válido até expirar.

**Por que aceitar:** o token fica no SecureStore do Android. Ele deixa de valer na hora quando o administrador
desativa o usuário ou muda o perfil, o canteiro ou a administração (`SituacaoDoUsuarioFilter`, que consulta o banco
a cada requisição). Trocar o `JWT_SEGREDO` invalida todos os tokens de uma vez.

**Muda se:** surgir a necessidade de "sair de todos os aparelhos" ou de sessões mais longas que um turno.

## Perfil `demo` combinável com `prod`

**Risco:** o perfil `demo` cria dois usuários com a mesma senha conhecida pelo grupo. Se ele for ligado no ambiente
real, essas contas ficam acessíveis.

**Por que aceitar:** o único ambiente na nuvem é o de apresentação. O perfil nunca liga por padrão, exige
`DEMO_SENHA` (que não fica no repositório e passa pela política de senha) e só carrega em banco sem o canteiro de
demonstração.

**Muda se:** o projeto ganhar um ambiente de produção com dados reais. Nesse caso, nunca ligue o `demo` nele.

## Swagger desligado em produção

**Decisão:** em `prod`, `/v3/api-docs` e `/swagger-ui.html` respondem 404. A documentação do contrato fica no
Swagger local e em `docs/api`. A decisão evita expor a lista completa de endpoints e de campos para quem não tem
conta.
