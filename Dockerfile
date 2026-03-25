FROM maven:3.9.11-eclipse-temurin-25 AS build
WORKDIR /workspace

COPY .mvn .mvn
COPY mvnw pom.xml ./
RUN --mount=type=cache,target=/root/.m2 ./mvnw -q -DskipTests dependency:go-offline

COPY src src
RUN --mount=type=cache,target=/root/.m2 ./mvnw -q -DskipTests package

FROM eclipse-temurin:25-jre

RUN apt-get update && apt-get install -y curl && rm -rf /var/lib/apt/lists/*
RUN groupadd -r spring && useradd -r -g spring spring
USER spring:spring

WORKDIR /app

COPY --from=build --chown=spring:spring /workspace/target/library-0.0.1-SNAPSHOT.jar app.jar

EXPOSE 8080
ENTRYPOINT ["java", "-jar", "/app/app.jar"]
