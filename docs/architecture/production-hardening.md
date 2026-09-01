# Backend Production Hardening & Deployment Readiness Architecture

This document describes the production-hardening measures, security controls, resilience configurations, and deployment-readiness practices enforced across all DevSphere microservices.

---

## 1. Environment & Secret Externalization

Sensitive configuration parameters across `user-service`, `auth-service`, `api-gateway`, `config-server`, and `service-discovery` are fully externalized via environment variables:

| Environment Variable | Description | Default (Local / Test) |
| :--- | :--- | :--- |
| `DB_HOST` | MySQL Host | `localhost` |
| `DB_PORT` | MySQL Port | `3306` |
| `DB_NAME` | Database Name | `devsphere_user` / `devsphere_auth` |
| `DB_USERNAME` | MySQL Username | `root` |
| `DB_PASSWORD` | MySQL Password | `""` (empty in test) |
| `DB_POOL_MAX_SIZE` | HikariCP Max Pool Size | `10` |
| `DB_POOL_MIN_IDLE` | HikariCP Min Idle Connections | `5` |
| `SPRING_REDIS_HOST` | Redis Host | `localhost` |
| `SPRING_REDIS_PORT` | Redis Port | `6379` |
| `SPRING_REDIS_PASSWORD` | Redis Password | `""` |
| `KAFKA_BOOTSTRAP_SERVERS` | Kafka Bootstrap Servers | `localhost:9092` |
| `JWT_SECRET` | HMAC-SHA JWT Signing Key | Dynamic Base64 Key |
| `JWT_EXPIRATION_SECONDS` | Access Token Validity | `3600` |

---

## 2. Docker Container Security & Optimization

Every microservice uses a security-hardened, multi-stage Alpine Dockerfile:

```dockerfile
# Stage 1: Build application
FROM maven:3.9.6-eclipse-temurin-21-alpine AS builder
WORKDIR /build
COPY pom.xml .
COPY src ./src
RUN mvn clean package -DskipTests

# Stage 2: Runtime image
FROM eclipse-temurin:21-jre-alpine
WORKDIR /app
RUN addgroup -S devsphere && adduser -S devsphere -G devsphere
COPY --from=builder /build/target/*.jar app.jar
RUN chown -R devsphere:devsphere /app
USER devsphere
ENTRYPOINT ["java", "-XX:+UseG1GC", "-XX:MaxRAMPercentage=75.0", "-Djava.security.egd=file:/dev/./urandom", "-jar", "app.jar"]
```

Key Hardening Controls:
- **Non-root Execution**: Runs under restricted user `USER devsphere`.
- **G1 Garbage Collector**: Optimized heap layout (`-XX:+UseG1GC`).
- **RAM Percentage Control**: Allocates up to 75% of container RAM dynamically (`-XX:MaxRAMPercentage=75.0`).
- **Entropy Performance**: Fast non-blocking random seed generator (`-Djava.security.egd=file:/dev/./urandom`).

---

## 3. Graceful Shutdown & Lifecycle Management

Spring Boot graceful shutdown is enabled across all services:

```yaml
server:
  shutdown: graceful

spring:
  lifecycle:
    timeout-per-shutdown-phase: 30s
```

Upon receiving `SIGTERM`:
1. Services stop accepting new incoming HTTP connections at the gateway/load balancer level.
2. In-flight requests complete within the 30-second shutdown window.
3. Kafka scheduled Outbox publisher threads complete current batch transactions before stopping.
4. Database HikariCP connections and Redis sockets are closed cleanly.

---

## 4. Container Health Probes & Actuator Exposure

Spring Boot health probes are enabled for orchestrator liveness and readiness monitoring:

```yaml
management:
  endpoint:
    health:
      show-details: when_authorized
      probes:
        enabled: true
```

- **Liveness Probe**: `/actuator/health/liveness` (Returns `UP` when JVM process is healthy).
- **Readiness Probe**: `/actuator/health/readiness` (Returns `UP` when DB, Redis, and Kafka connectors are ready to serve traffic).

---

## 5. Security & Error Response Sanitization

- **JWT Validation**: Strong HMAC-SHA signature verification with token expiration checks and header stripping in `JwtAuthenticationFilter`.
- **Sanitized Errors**: No internal stack traces, SQL queries, or JVM exception class names are returned to clients. Rejections return structured `timestamp`, `status`, `error`, `code`, `message`, `path`, and `traceId`.
- **IDOR Protection**: Enforced via `@PreAuthorize` authorization rules checking authenticated `user_id` against target resource owners.

---

## 6. Resilience & Distributed Observability

- **Circuit Breakers & Retries**: Resilience4j protects downstream microservice calls (`userServiceCircuitBreaker`, `authServiceCircuitBreaker`).
- **Trace Propagation**: W3C `traceparent` and `X-Trace-Id` headers are propagated seamlessly across HTTP calls and Kafka event records via Micrometer Tracer & OpenTelemetry.
- **Low-Cardinality Metrics**: All Prometheus counter/gauge metrics strictly enforce low-cardinality tags.
