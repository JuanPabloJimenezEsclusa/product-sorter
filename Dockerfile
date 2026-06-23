FROM maven:3-eclipse-temurin-26 AS build
WORKDIR /app

COPY pom.xml ./
COPY api-spec/pom.xml api-spec/
COPY domain/pom.xml domain/
COPY application/pom.xml application/
COPY infrastructure/pom.xml infrastructure/
COPY infrastructure-observability/pom.xml infrastructure-observability/
COPY adapter-rest/pom.xml adapter-rest/
COPY bootstrap/pom.xml bootstrap/
COPY coverage-jacoco/pom.xml coverage-jacoco/

RUN --mount=type=cache,id=mvn-repo,target=/app/.m2-repo \
    mvn dependency:go-offline -Dmaven.repo.local=/app/.m2-repo -B -ntp -e -q

COPY . .

RUN --mount=type=cache,id=mvn-repo,target=/app/.m2-repo \
    mvn package -Dmaven.repo.local=/app/.m2-repo -pl bootstrap -am -DskipTests -B -ntp -e -q

FROM eclipse-temurin:25-jre
RUN apt-get update && apt-get install -y curl && rm -rf /var/lib/apt/lists/* \
    && groupadd -r appuser && useradd -r -g appuser -d /app appuser

WORKDIR /app
COPY --from=build --chown=appuser:appuser /app/bootstrap/target/*.jar app.jar

USER appuser

HEALTHCHECK --interval=10s --timeout=5s --retries=5 --start-period=60s \
  CMD curl -sf http://localhost:8880/actuator/health || exit 1

EXPOSE 8880
CMD ["java", "-jar", "app.jar"]
