# Multi-stage build for Spring Boot server
FROM maven:3.9-eclipse-temurin-21 AS build

WORKDIR /app

# Copy pom and sources
COPY pom.xml .
RUN mvn -q -e -B -DskipTests dependency:go-offline

COPY src ./src

# Build jar
RUN mvn -q -e -B -DskipTests package

# Runtime image
FROM eclipse-temurin:21-jre

WORKDIR /app

# Copy built jar
COPY --from=build /app/target/*.jar app.jar

# Expose default Spring Boot port (adjust if different)
EXPOSE 8080

# Run application
ENTRYPOINT ["java","-jar","/app/app.jar"]
