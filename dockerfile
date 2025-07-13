# Stage 1: Build the project
FROM maven:3.9.4-eclipse-temurin-17 AS build
WORKDIR /build
COPY . .
RUN mvn clean package -DskipTests

# Stage 2: Run the Spring Boot app
FROM openjdk:17-jdk-slim
WORKDIR /app
COPY --from=build /build/target/*.jar app.jar
EXPOSE 8080
ENTRYPOINT ["java", "-jar", "app.jar"]
