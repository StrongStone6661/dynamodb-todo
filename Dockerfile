# Build stage
FROM maven:3.8.5-openjdk-17 AS build
WORKDIR /app
COPY pom.xml .
COPY src ./src
RUN mvn clean package -DskipTests

# Runtime stage
FROM openjdk:17-jdk-slim
WORKDIR /app
COPY --from=build /app/target/dynamodb-todo-0.0.1-SNAPSHOT.jar dynamodb-todo.jar
COPY src/main/resources/static ./static
EXPOSE 8080
ENTRYPOINT ["java", "-jar", "dynamodb-todo.jar"]