# syntax=docker/dockerfile:1.6
# ---------------------------------------------------------------------------
# Multi-stage build для Spring Boot приложения.
#
# В лекции примеры с maven:3.8.5-openjdk-17 и openjdk:17-jdk-slim – оба образа
# на Docker Hub уже помечены deprecated. Берём актуальные эквиваленты:
#   - сборка:   maven:3.9-eclipse-temurin-17
#   - рантайм:  eclipse-temurin:17-jre-jammy  (без Maven и без JDK, только JRE)
# ---------------------------------------------------------------------------

# --- Этап 1: Сборка (Builder) -----------------------------------------------
FROM maven:3.9-eclipse-temurin-17 AS builder

WORKDIR /build

# Сначала только pom – это отдельный слой Docker-кэша. Если зависимости
# не менялись, повторный build переиспользует загруженный ~/.m2 и пропускает шаг.
COPY pom.xml .
RUN mvn -B -q -e dependency:go-offline

# Теперь исходники. Изменение Java-кода НЕ инвалидирует слой с зависимостями.
COPY src ./src

# -DskipTests – тесты обычно гоняются на этапе CI (см. вебинар), а не внутри
# Dockerfile. Финальная сборка должна быть быстрой и воспроизводимой.
RUN mvn -B -q package -DskipTests

# --- Этап 2: Рантайм (Runner) -----------------------------------------------
FROM eclipse-temurin:17-jre-jammy

WORKDIR /app

# Создаём непривилегированного пользователя – контейнер не должен крутиться от root.
RUN groupadd -r app && useradd -r -g app -d /app app \
    && chown -R app:app /app
USER app

# finalName=app в pom.xml -> на выходе всегда target/app.jar.
COPY --from=builder /build/target/app.jar app.jar

EXPOSE 8080

ENTRYPOINT ["java", "-jar", "/app/app.jar"]
