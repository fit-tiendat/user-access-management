FROM maven:3.9.9-eclipse-temurin-17 AS build
WORKDIR /workspace

COPY pom.xml ./pom.xml
COPY core/pom.xml ./core/pom.xml
COPY user-service/pom.xml ./user-service/pom.xml
COPY auth-service/pom.xml ./auth-service/pom.xml

COPY core ./core
COPY user-service ./user-service
COPY auth-service ./auth-service

RUN mvn -q -DskipTests -pl user-service -am package

FROM eclipse-temurin:17-jre-jammy
WORKDIR /app

RUN apt-get update \
    && apt-get install -y --no-install-recommends curl \
    && rm -rf /var/lib/apt/lists/* \
    && groupadd --system app \
    && useradd --system --gid app --home-dir /app --shell /usr/sbin/nologin app \
    && chown app:app /app

COPY --from=build --chown=app:app /workspace/user-service/target/user-service-*-SNAPSHOT.jar app.jar

USER app
EXPOSE 8082
HEALTHCHECK --interval=30s --timeout=5s --start-period=30s --retries=3 \
    CMD curl --fail --silent http://localhost:8082/actuator/health/readiness || exit 1

ENTRYPOINT ["java", "-jar", "/app/app.jar"]
