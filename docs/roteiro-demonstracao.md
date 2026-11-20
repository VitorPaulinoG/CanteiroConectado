# Roteiro de demonstração

Checklist para rodar antes de cada apresentação. A API fica no Render (plano gratuito) e o banco na Neon
(plano gratuito): os dois dormem por inatividade, então a primeira chamada do dia é lenta.

## 0. Se o ambiente já foi usado: reiniciar o banco

Para voltar aos dados de demonstração, siga o [reinício do banco](reinicio-do-banco.md) antes do passo 1.

## 1. Uma hora antes: acordar a API e o banco

1. Abra no navegador `https://<url-da-api-no-render>/api/v1/ping`.
   - O Render desliga o serviço depois de cerca de 15 minutos sem tráfego. A primeira resposta pode levar
     até um minuto; enquanto isso a página fica carregando ou mostra a tela do Render.
   - Quando responder, o JSON traz `nome`, `versao` e `horaServidor`. Confira se a versão é a esperada.
2. Abra `https://<url-da-api-no-render>/actuator/health` e confirme `{"status":"UP"}`.
   - O banco da Neon também suspende por inatividade (cerca de 5 minutos no plano gratuito). O `ping` não
     toca no banco; o `health` toca. A primeira consulta depois da suspensão pode demorar alguns segundos.
3. Repita o `ping` a cada 10 minutos até a apresentação (ou deixe uma aba recarregando), para a API não
   voltar a dormir.

## 2. Validar o login

1. No app instalado, entre com o administrador e com um usuário de cada perfil (ALMOXARIFE e GESTOR).
2. Confirme que cada perfil vê as abas certas e que o Início carrega os contadores.
3. Se o app mostrar "O servidor está acordando", aguarde: ele repete a chamada por até 90 segundos.

## 3. Validar o APK

1. Confirme que o APK instalado é o da versão a apresentar: tela Mais → Sobre mostra a versão do app e a da API.
2. A tela Sobre deve mostrar a hora do servidor. Se mostrar erro, o APK aponta para a URL errada ou a API
   não acordou.
3. Tenha o link do build no painel da Expo à mão para reinstalar, se for preciso.

## 4. Plano B

| Problema | O que fazer |
| --- | --- |
| A API do Render não acorda | Suba a API e o banco no notebook com `docker compose up --build` e use um build com `EXPO_PUBLIC_API_URL` apontando para o IP do notebook (ver README, "Apontar o app para a API local") |
| O banco da Neon está fora | Mesma solução acima: Postgres do Docker Compose no notebook |
| O APK não instala ou está desatualizado | Rode o app pelo Expo Go com `npx expo start` no notebook, na mesma rede do celular |
| Sem internet no local | Roteador ou hotspot próprio; celular e notebook na mesma rede; API local |
| Cota do EAS Build esgotada | `eas build --platform android --profile preview --local` (ver README, "Plano B: build local") |

## 5. Durante a apresentação

- Mantenha o celular com brilho alto e a tela sem bloqueio automático.
- Siga as Jornadas 1 (saída de ferramenta pelo almoxarife) e 2 (alerta de estoque mínimo e requisição pela
  gestora) com os dados de demonstração.
