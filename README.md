# Canteiro Conectado

Canteiro Conectado é um aplicativo Android com API própria para controlar o estoque de materiais e ferramentas de canteiros de obra. O almoxarife registra entradas e saídas (com o colaborador responsável e a frente de obra) e corrige lançamentos errados por estorno; o gestor acompanha o saldo de todos os canteiros, decide requisições de material e consulta o histórico e a trilha de auditoria, que é imutável. O MVP tem dois perfis (ALMOXARIFE e GESTOR) e é composto por um app React Native com Expo, uma API Spring Boot e um banco PostgreSQL, tudo em um único monorepo.

## Pré-requisitos

- JDK 25
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

- `docker compose down` para; os dados do Postgres ficam no volume `banco-dados`.
- `docker compose down -v` apaga o volume e zera o banco.
- A API roda com limite de 512 MB; `JAVA_TOOL_OPTIONS` no `.env` soma às opções de memória da imagem.

> O restante (rodar sem Docker, testes) será preenchido na épica E02.

## Como rodar o app

> A ser preenchido na épica E03.

## Documentação

- [Visão e arquitetura](docs/Vis%C3%A3o%20e%20arquitetura.md): decisões, escopo do MVP, estrutura do monorepo, contrato da API e modelo de dados
- [Protótipos no Figma](https://www.figma.com/design/ECl5KwGt8IQcdXNRebLEuD/CanteiroConectado?node-id=0-1&t=aKGlq4yb4zU1bGhc-1)
