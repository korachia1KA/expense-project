# =====================================================================================
# Backend image: builds the Spring Boot jar, then runs it on a minimal Java runtime.
# Built by docker-compose.yml (`docker compose up --build`) or by hand:
#   docker build -t expense-tracker-api ./backend
# =====================================================================================

# ---------- Stage 1: build the jar with Maven ----------
# A multi-stage build keeps the final image small: Maven, the JDK and the source code stay
# in this throw-away stage; only the finished jar is copied into stage 2.
FROM maven:3.9-eclipse-temurin-17 AS build
WORKDIR /app
# Copy pom.xml first and download dependencies. Docker caches each step ("layer"), so
# dependencies are only re-downloaded when pom.xml changes, not on every code change.
COPY pom.xml .
RUN mvn -q -B dependency:go-offline
COPY src ./src
# Tests already run in CI (see .github/workflows/ci.yml), so skip them here for faster images.
RUN mvn -q -B package -DskipTests

# ---------- Stage 2: run it on a small JRE-only image ----------
# JRE (runtime) instead of JDK (compiler too) + Alpine Linux = a much smaller, safer image.
FROM eclipse-temurin:17-jre-alpine
WORKDIR /app
# Never run as root inside the container: if the app were compromised, the attacker
# would only have the rights of this unprivileged "app" user.
RUN addgroup -S app && adduser -S app -G app
COPY --from=build /app/target/*.jar app.jar
USER app
# Documentation only: the app listens on 8080 in the prod profile (application-prod.yml).
EXPOSE 8080
# The prod profile (Postgres) is selected by SPRING_PROFILES_ACTIVE in docker-compose.yml.
ENTRYPOINT ["java", "-jar", "app.jar"]
