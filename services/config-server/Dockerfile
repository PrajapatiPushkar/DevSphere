# Stage 1: Build application
FROM maven:3.9.6-eclipse-temurin-21-alpine AS builder
WORKDIR /build

# Copy pom.xml and source files
COPY pom.xml .
COPY src ./src

# Package application jar
RUN mvn clean package -DskipTests

# Stage 2: Runtime image
FROM eclipse-temurin:21-jre-alpine
WORKDIR /app

# Create non-root user and group for container security
RUN addgroup -S devsphere && adduser -S devsphere -G devsphere

# Copy built jar from builder stage
COPY --from=builder /build/target/*.jar app.jar

# Set ownership to non-root user
RUN chown -R devsphere:devsphere /app

# Switch to non-root user
USER devsphere

EXPOSE 8888

ENTRYPOINT ["java", "-jar", "app.jar"]
