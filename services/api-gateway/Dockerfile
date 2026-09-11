FROM eclipse-temurin:21-jre-alpine
WORKDIR /app

# Create non-root user and group for container security
RUN addgroup -S devsphere && adduser -S devsphere -G devsphere

# Copy prebuilt jar from target directory
COPY target/*.jar app.jar

# Set ownership to non-root user
RUN chown -R devsphere:devsphere /app

# Switch to non-root user
USER devsphere

EXPOSE 8080

ENTRYPOINT ["java", "-XX:+UseG1GC", "-XX:MaxRAMPercentage=75.0", "-Djava.security.egd=file:/dev/./urandom", "-jar", "app.jar"]
