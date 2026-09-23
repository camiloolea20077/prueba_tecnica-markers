# syntax=docker/dockerfile:1

# ---------- Compilación ----------
FROM maven:3.9-eclipse-temurin-21 AS build
WORKDIR /app

COPY pom.xml .
COPY src ./src
# La caché de BuildKit conserva ~/.m2 entre builds (no se usa dependency:go-offline porque
# resuelve dependencias transitivas de ehcache:jakarta sin la gestión de versiones de Spring Boot)
RUN --mount=type=cache,target=/root/.m2 mvn -q -B -DskipTests package

# ---------- Ejecución ----------
FROM eclipse-temurin:21-jre-alpine
WORKDIR /app

RUN addgroup -S app && adduser -S app -G app
COPY --from=build /app/target/data_credits-*.jar app.jar
USER app

EXPOSE 8080
ENV JAVA_OPTS="-XX:MaxRAMPercentage=75"
ENTRYPOINT ["sh", "-c", "java $JAVA_OPTS -jar /app/app.jar"]
