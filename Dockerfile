# Multi-stage build for optimized production image
FROM maven:3.9.4-eclipse-temurin-21 AS build

# Set working directory
WORKDIR /app

# Copy pom.xml and download dependencies (for better layer caching)
COPY pom.xml .
RUN mvn dependency:go-offline -B

# Copy source code
COPY src ./src

# Build the application
RUN mvn clean package -DskipTests

# Production stage
FROM eclipse-temurin:21-jre-alpine

# Install necessary packages
RUN apk add --no-cache curl

# Create app directory and user
RUN addgroup -g 1001 -S appuser && adduser -u 1001 -S appuser -G appuser
WORKDIR /app

# Copy built jar from build stage
COPY --from=build /app/target/*.jar app.jar

# Change ownership of the app directory
RUN chown -R appuser:appuser /app

# Switch to non-root user
USER appuser

# Expose port
EXPOSE 8080

# Health check - uses /api/health (no actuator dependency)
HEALTHCHECK --interval=30s --timeout=10s --start-period=90s --retries=3 \
    CMD curl -f http://localhost:8080/api/health || exit 1

# Run the application with container-optimized JVM flags
# -XX:+TieredCompilation -XX:TieredStopAtLevel=1 = faster startup (skip C2 compiler)
# -XX:+UseSerialGC = lower memory footprint for small containers
# -Xss256k = smaller thread stacks
# -Djava.security.egd = faster random number generation
ENTRYPOINT ["java", \
    "-XX:+UseContainerSupport", \
    "-XX:MaxRAMPercentage=75.0", \
    "-XX:+TieredCompilation", \
    "-XX:TieredStopAtLevel=1", \
    "-Xss256k", \
    "-Djava.security.egd=file:/dev/./urandom", \
    "-Dspring.jmx.enabled=false", \
    "-jar", "app.jar"]