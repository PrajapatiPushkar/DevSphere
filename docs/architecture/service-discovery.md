# Service Discovery Architecture with Netflix Eureka

This document details the architecture, configuration, routing, and operational dynamics of **Service Discovery** in the DevSphere microservices platform.

---

## 1. Overview & Purpose

Prior to Lesson 12, `API Gateway` maintained static, hardcoded downstream HTTP URLs (`http://localhost:8081`, `http://localhost:8082`). This created tight operational coupling and prevented horizontal auto-scaling or dynamic IP/port relocation.

With **Netflix Eureka Service Discovery**, downstream microservices dynamically register their network locations (`hostname`, `IP`, `port`) with a central Eureka Server. `API Gateway` queries Eureka to dynamically resolve service instances via Spring Cloud LoadBalancer using `lb://` URI schemes.

```
                           +-----------------------------------+
                           |    DEVSPHERE-SERVICE-DISCOVERY    | (Port 8761)
                           +-----------------+-----------------+
                                             |
             +-------------------------------+-------------------------------+
             | Register & Heartbeat          | Register & Heartbeat          | Register & Heartbeat
             v                               v                               v
 +-----------------------+       +-----------------------+       +-----------------------+
 | DEVSPHERE-API-GATEWAY |       | DEVSPHERE-AUTH-SERVICE|       | DEVSPHERE-USER-SERVICE|
 |      (Port 8080)      |       |      (Port 8081)      |       |      (Port 8082)      |
 +-----------+-----------+       +-----------------------+       +-----------------------+
             |                               ^                               ^
             | Client Request                |                               |
             +--- lb://DEVSPHERE-AUTH-SERVICE+                               |
             |                                                               |
             +--- lb://DEVSPHERE-USER-SERVICE-------------------------------+
```

---

## 2. Component Breakdown

### 2.1 Eureka Server (`services/service-discovery`)
- **Port**: `8761`
- **Application Name**: `DEVSPHERE-SERVICE-DISCOVERY`
- **Role**: Standalone Service Registry.
- **Self-Registration**: Disabled (`eureka.client.register-with-eureka: false`, `eureka.client.fetch-registry: false`).
- **Dashboard**: Web UI accessible at `http://localhost:8761`.

### 2.2 Eureka Clients
Each microservice includes `spring-cloud-starter-netflix-eureka-client` and registers itself upon startup:
- `DEVSPHERE-API-GATEWAY` (Port `8080`)
- `DEVSPHERE-AUTH-SERVICE` (Port `8081`)
- `DEVSPHERE-USER-SERVICE` (Port `8082`)

---

## 3. Dynamic Gateway Routing (`lb://`)

`API Gateway` replaces static downstream host URLs with discovery-based load-balanced URIs:

```yaml
spring:
  cloud:
    gateway:
      routes:
        - id: auth-service-route
          uri: lb://DEVSPHERE-AUTH-SERVICE
          predicates:
            - Path=/api/v1/auth/**

        - id: user-service-route
          uri: lb://DEVSPHERE-USER-SERVICE
          predicates:
            - Path=/api/v1/users/**
```

### Request Flow
1. Client issues request to `http://localhost:8080/api/v1/users/me`.
2. Gateway validates Bearer JWT and injects `X-Authenticated-User-Id`.
3. Gateway inspects route URI (`lb://DEVSPHERE-USER-SERVICE`).
4. Gateway queries Eureka client cache for healthy instances of `DEVSPHERE-USER-SERVICE`.
5. Gateway forwards the request to an available instance (e.g. `http://127.0.0.1:8082/api/v1/users/me`).

---

## 4. Heartbeats & Health Checks

- Clients send periodic heartbeats to Eureka Server (default interval: 30s).
- If Eureka fails to receive heartbeats from an instance for 90s (eviction duration), the instance is evicted from the registry.
- **Graceful Failure**: Running microservices maintain a local cached copy of the service registry. If Eureka Server goes offline temporarily, existing services continue routing to cached instances without crashing.

---

## 5. Local Startup Sequence

For local development, startup order should be:
1. `service-discovery` (Port 8761)
2. `auth-service` (Port 8081)
3. `user-service` (Port 8082)
4. `api-gateway` (Port 8080)

---

## 6. Production Considerations & Kubernetes Note

In a local standalone architecture, Netflix Eureka provides lightweight service discovery. 
In a cloud-native **Kubernetes** deployment, native Kubernetes Service DNS (`kube-dns` / CoreDNS) and Kubernetes ClusterIP Services are typically used for service discovery instead of Eureka. Eureka serves as our framework-level service discovery engine prior to container orchestration.
