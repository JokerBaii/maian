FROM maven:3.9.11-eclipse-temurin-21 AS builder

WORKDIR /workspace
COPY pom.xml ./
RUN mvn -q -DskipTests dependency:resolve
COPY src ./src
RUN mvn -q -DskipTests package

FROM eclipse-temurin:21-jre

WORKDIR /app
RUN useradd --system --uid 10001 --create-home maian
COPY --from=builder /workspace/target/maian-server-0.1.0-SNAPSHOT.jar /app/maian-server.jar
RUN mkdir -p /app/data/uploads && chown -R maian:maian /app

USER maian
EXPOSE 8080
ENTRYPOINT ["java", "-jar", "/app/maian-server.jar"]
