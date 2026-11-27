FROM maven:3.9.11-eclipse-temurin-21 AS build

WORKDIR /workspace
COPY pom.xml ./
COPY config ./config
COPY src ./src
RUN mvn --batch-mode --no-transfer-progress -DskipTests package

FROM eclipse-temurin:21-jre-alpine AS runtime

RUN addgroup -S ankileet && adduser -S ankileet -G ankileet
WORKDIR /app
COPY --from=build /workspace/target/reviewhop-api-*.jar app.jar
USER ankileet:ankileet
EXPOSE 8080
ENTRYPOINT ["java", "-jar", "/app/app.jar"]
