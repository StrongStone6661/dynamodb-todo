FROM maven:3.9.6-eclipse-temurin-17 AS builder

WORKDIR /app

COPY pom.xml .
COPY src ./src

RUN mvn clean package -DskipTests

FROM eclipse-temurin:17-jre-focal AS runtime

WORKDIR /app

COPY --from=builder /app/target/*.jar app.jar
COPY src/main/resources/static/ ./static/

EXPOSE 8080

ENTRYPOINT ["java", "-jar", "app.jar"]