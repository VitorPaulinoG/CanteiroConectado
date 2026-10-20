# Etapa 1: build com JDK 21 e Maven (via wrapper, mesma versão do desenvolvimento local).
FROM eclipse-temurin:21-jdk AS build
WORKDIR /build

# Dependências primeiro: esta camada só é refeita quando o pom.xml muda.
COPY .mvn/ .mvn/
COPY mvnw pom.xml ./
RUN ./mvnw -B -q dependency:go-offline

# Os testes rodam no CI; o build da imagem não depende de Postgres.
COPY src/ src/
RUN ./mvnw -B -q -DskipTests package

# Etapa 2: JRE enxuto, sem código-fonte nem cache do Maven, com usuário sem privilégios.
FROM eclipse-temurin:21-jre-alpine
RUN addgroup -S canteiro && adduser -S -G canteiro canteiro
WORKDIR /app
COPY --from=build --chown=canteiro:canteiro /build/target/*.jar app.jar

USER canteiro
EXPOSE 8080

HEALTHCHECK --interval=30s --timeout=5s --start-period=60s --retries=3 \
  CMD wget -qO- "http://localhost:${PORT:-8080}/actuator/health" | grep -q '"status":"UP"' || exit 1

# As opções de memória ficam no entrypoint (e não em ENV) para valerem mesmo com
# JAVA_TOOL_OPTIONS vazio no .env; o JAVA_TOOL_OPTIONS do ambiente soma por cima.
ENTRYPOINT ["java", "-XX:MaxRAMPercentage=75", "-XX:+UseSerialGC", "-jar", "app.jar"]
