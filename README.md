# Canteiro Conectado

Aplicativo Android com API própria para controlar o estoque de materiais e ferramentas de canteiros de obra.
O almoxarife registra entradas, saídas e estornos; o gestor acompanha o saldo dos canteiros, decide
requisições de material e consulta o histórico e a auditoria.

## Tecnologias

- **API:** Java 21, Spring Boot 4, PostgreSQL 16
- **App:** React Native com Expo (SDK 58) e TypeScript
- **Infra:** Docker Compose, EAS Build (APK)

## Como executar

Pré-requisitos: JDK 21, Node.js LTS, Docker e o Expo Go no celular Android (ou um emulador).

**API + banco**, na raiz do repositório:

```bash
cp .env.example .env    # preencha os valores
docker compose up --build
```

A API sobe em `http://localhost:8080` (Swagger em `/swagger-ui.html`).

**App**, em `apps/mobile`:

```bash
echo "EXPO_PUBLIC_API_URL=http://10.0.2.2:8080" > .env   # no celular físico, use o IP do computador
npm ci
npx expo start
```

Escaneie o QR code com o Expo Go ou pressione `a` para abrir no emulador.
