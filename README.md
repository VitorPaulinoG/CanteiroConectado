# Canteiro Conectado

Canteiro Conectado é um aplicativo Android com API própria para controlar o estoque de materiais e ferramentas de canteiros de obra. O almoxarife registra entradas e saídas (com o colaborador responsável e a frente de obra) e corrige lançamentos errados por estorno; o gestor acompanha o saldo de todos os canteiros, decide requisições de material e consulta o histórico e a trilha de auditoria, que é imutável. O MVP tem dois perfis (ALMOXARIFE e GESTOR) e é composto por um app React Native com Expo, uma API Spring Boot e um banco PostgreSQL, tudo em um único monorepo.

## Pré-requisitos

- JDK 21
- Node.js (versão LTS)
- Docker
- Expo Go no celular Android ou um emulador Android

## Como rodar a API

Com Docker (API + Postgres), na raiz do repositório:

```bash
cp .env.example .env     # preencha os valores (BANCO_*, JWT_SEGREDO, ADMIN_*)
docker compose up --build
curl localhost:8080/actuator/health   # {"status":"UP",...}
```

- `docker compose down` para; os dados do Postgres ficam no volume `db-dados`.
- `docker compose down -v` apaga o volume e zera o banco.
- A API roda com limite de 512 MB; `JAVA_TOOL_OPTIONS` no `.env` soma às opções de memória da imagem.

Sem Docker para a API (só o Postgres no Compose), em `apps/api`:

```bash
docker compose up -d db         # na raiz: sobe só o Postgres
cd apps/api && ./mvnw spring-boot:run
```

- O perfil `local` é o padrão e já traz valores de desenvolvimento (banco `canteiroconectado`, usuário e senha `canteiro`).
  Variáveis definidas no `.env` da raiz ou no ambiente têm precedência.
- Swagger UI em `http://localhost:8080/swagger-ui.html` e saúde em `http://localhost:8080/actuator/health`.
- Testes: `./mvnw verify` em `apps/api`. Os testes de contexto e o de trigger do banco **precisam do Postgres no ar**
  (`docker compose up -d db`). O `EnvExampleTest` falha se a API ler uma variável que não esteja no `.env.example`.

## Como rodar o app

Em `apps/mobile`:

```bash
npm ci
npx expo start
```

- Escaneie o QR code com o Expo Go (Android) ou pressione `a` para abrir num emulador Android.
- A API é lida de `EXPO_PUBLIC_API_URL` (ver `.env.example` na raiz); sem essa variável no ambiente,
  o app sobe mas não encontra a API.
- SDK do Expo fixo em `~58.0.14` (`apps/mobile/package.json`).
- Qualidade: `npm run lint`, `npm run typecheck` e `npm test` (ESLint com `eslint-config-expo`,
  TypeScript em modo estrito e Jest com `jest-expo`).

## Como gerar, baixar e instalar o APK

O APK é compilado na nuvem pelo EAS Build (conta Expo `vitorpaulinogs-team`, projeto `canteiroconectado`).
O perfil `preview` do `apps/mobile/eas.json` gera um APK instalável direto no aparelho; o `production` gera AAB
(formato da Google Play, que não instala direto).

### Gerar

**Automático (CI):** todo push na `main` que altere `apps/mobile/**` roda `lint`, `typecheck` e `test` e, se passarem,
dispara `eas build --platform android --profile preview --no-wait` (`.github/workflows/mobile.yml`). O job do GitHub fica
verde assim que o build entra na fila; se o build falhar depois, o erro só aparece no painel da Expo. Requer o segredo
`EXPO_TOKEN` no repositório (token de um robot user da conta `vitorpaulinogs-team`).

**Manual**, em `apps/mobile`:

```bash
npm install -g eas-cli     # uma vez
eas login
eas build --platform android --profile preview
```

- Na primeira vez o EAS pergunta se deve gerar a keystore do Android: aceite. Ela fica guardada na conta Expo
  e é a identidade de assinatura do app.
- `EXPO_PUBLIC_API_URL` vem do ambiente `preview` do EAS (`eas env:list preview`), e não do `.env` local,
  porque o build roda nos servidores da Expo. Para alterar: `eas env:update` ou `eas env:create`.
- Só o que o git conhece é enviado ao build: commite assets e configurações novas antes.

### Baixar

- Painel: expo.dev → conta `vitorpaulinogs-team` → projeto `canteiroconectado` → **Builds**. Cada build tem
  link, QR code e botão de download. Os builds expiram depois de cerca de 30 dias no plano free.
- Terminal: `eas build:list` mostra os builds existentes e `eas build:download` baixa o artefato. Nenhum dos dois gera build novo.

### Instalar

1. No celular Android, abra o link do build (ou escaneie o QR code) e baixe o APK.
2. Permita a instalação de fontes desconhecidas para o navegador ou gerenciador de arquivos usado.
3. Abra o arquivo baixado e instale.

Por USB, com `adb` instalado: `adb install caminho/do/app.apk`.

### Plano B: build local

Se a cota do plano free acabar ou a fila estiver longa, o mesmo build roda na sua máquina:

```bash
cd apps/mobile
eas build --platform android --profile preview --local
```

- Pré-requisitos: Android SDK (por exemplo via Android Studio), `ANDROID_HOME` configurado e o JDK que o Gradle do SDK
  do Expo exige (hoje o 17; se o `JAVA_HOME` apontar para o JDK 21 do restante do projeto e o Gradle reclamar, aponte para o 17).
  Só Android roda em Linux; iOS exige macOS.
- O build local ainda precisa de login (`eas login`) para buscar a keystore e as variáveis de ambiente do EAS, mas não
  consome a cota de builds. O APK sai em um arquivo `.apk` na pasta atual (ou no caminho passado em `--output`).
- É mais lento e mais sujeito a diferenças de ambiente que o build na nuvem; use como contingência.

## Documentação

- [Visão e arquitetura](docs/Vis%C3%A3o%20e%20arquitetura.md): decisões, escopo do MVP, estrutura do monorepo, contrato da API e modelo de dados
- [Protótipos no Figma](https://www.figma.com/design/ECl5KwGt8IQcdXNRebLEuD/CanteiroConectado?node-id=0-1&t=aKGlq4yb4zU1bGhc-1)
