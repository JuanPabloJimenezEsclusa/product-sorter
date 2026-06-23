FROM maven:3-eclipse-temurin-25 AS build
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
    && groupadd -r appuser && useradd -r -g appuser -d /app appuser \
    && curl -fsSL "https://github.com/tianon/gosu/releases/download/1.17/gosu-amd64" -o /usr/local/bin/gosu \
    && chmod +x /usr/local/bin/gosu

WORKDIR /app
COPY --from=build /app/bootstrap/target/*.jar app.jar
RUN chown -R appuser:appuser /app

COPY <<'EOF' /usr/local/bin/entrypoint.sh
#!/bin/sh
exec gosu appuser "$@"
EOF
RUN chmod 755 /usr/local/bin/entrypoint.sh

EXPOSE 8880
ENTRYPOINT ["/usr/local/bin/entrypoint.sh"]
CMD ["java", "-jar", "app.jar"]
