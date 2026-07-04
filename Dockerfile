# =========================================================================
# Stage 1: Build the Application
# =========================================================================
FROM maven:3.9.6-eclipse-temurin-21-alpine AS build
WORKDIR /app

# Cache dependencies by copying pom.xml first
COPY pom.xml .
RUN mvn dependency:go-offline -B

# Copy source code and package the application
COPY src ./src
RUN mvn clean package -DskipTests

# =========================================================================
# Stage 2: Optimized Lightweight Runtime
# =========================================================================
FROM eclipse-temurin:21-jre-alpine
WORKDIR /app

# Run as non-root user for production security hardening
RUN addgroup -S spring && adduser -S spring -G spring
USER spring:spring

# Copy the fat jar from the build stage
COPY --from=build /app/target/*.jar app.jar

EXPOSE 8080

# Production performance tuning optimizations for JVM memory handling
ENTRYPOINT ["java", \
            "-XX:+UseG1GC", \
            "-XX:+ExitOnOutOfMemoryError", \
            "-jar", \
            "app.jar"]