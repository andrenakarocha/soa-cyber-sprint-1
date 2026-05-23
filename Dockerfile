# ── Stage 1: Build ──────────────────────────────────────────────────────────
FROM maven:3.9.6-eclipse-temurin-21-alpine AS builder
WORKDIR /app

# Copia o pom primeiro para cachear as dependências separado do código-fonte
COPY pom.xml .
RUN mvn dependency:go-offline -q

COPY src ./src
RUN mvn package -DskipTests -q

# ── Stage 2: Runtime ─────────────────────────────────────────────────────────
FROM eclipse-temurin:21-jre-alpine
WORKDIR /app

# curl é usado pelo healthcheck do docker-compose
RUN apk add --no-cache curl

COPY --from=builder /app/target/henry-telemetry-service-1.0.0.jar app.jar

EXPOSE 8443

ENTRYPOINT ["java", "-jar", "app.jar"]
