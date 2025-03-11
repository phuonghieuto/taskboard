# Stage 1: Build the microservice
FROM maven:3.9.8-eclipse-temurin-21 AS build
WORKDIR /app

# Copy the pom.xml and download dependencies
COPY pom.xml .
RUN --mount=type=cache,target=/root/.m2 mvn dependency:go-offline

# Copy the source code and build the project
COPY src ./src
RUN --mount=type=cache,target=/root/.m2 mvn clean install -DskipTests

# Stage 2: Create the final image
FROM openjdk:21-jdk-slim
WORKDIR /app

# Copy the built jar from the build stage
COPY --from=build /app/target/notification-service-0.0.1-SNAPSHOT.jar .

EXPOSE 8083

# Run the application
ENTRYPOINT ["java", "-jar", "notification-service-0.0.1-SNAPSHOT.jar"]